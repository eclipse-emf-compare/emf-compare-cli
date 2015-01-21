/*******************************************************************************
 * Copyright (c) 2014, 2015 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.git.pgm.internal.app;

import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.TAB;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.waitEgitJobs;
import static org.eclipse.emf.compare.git.pgm.internal.util.GitUtils.SHORT_REV_COMMIT_ID_LENGTH;
import static org.eclipse.emf.compare.git.pgm.internal.util.GitUtils.getCommitsBetween;
import static org.eclipse.emf.compare.git.pgm.internal.util.GitUtils.getOneLineCommitMsg;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.CherryPickOperation;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.args.RevCommitHandler;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DeathType;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DiesOn;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.args4j.Argument;

/**
 * Logical cherry-pick command. <h3>Name</h3>
 * <p>
 * logicalcherry-pick - Git Logical cherry-pick
 * </p>
 * <h4>Synopsis</h4>
 * <p>
 * logicalcherry-pick &lt;setup&gt; &lt;commit&gt; [--show-stack-trace] [--git-dir &lt;gitDirectory&gt;]
 * </p>
 * <h4>Description</h4>
 * <p>
 * The logical cherry-pick is used to cherry-pick one revision using logical model.
 * </p>
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings({"restriction", "nls" })
public class CherryPickApplication extends AbstractLogicalApplication {
	/**
	 * Holds {@link RevCommit} that needs to be merged.
	 */
	@Argument(index = 2, required = true, metaVar = "<commit>", usage = "Commit ID to cherry pick.", handler = RevCommitHandler.class)
	private RevCommit commit;

	/** Holds a reference to the HEAD before any operation. */
	private ObjectId oldHead;

	@Override
	protected Integer performGitCommand() throws Die {
		try {
			oldHead = repo.getRef(Constants.HEAD).getObjectId();
		} catch (RevisionSyntaxException | IOException e) {
			throw new DiesOn(DeathType.ERROR).duedTo(e).displaying(e.getMessage()).ready();
		}
		final CherryPickResult result;
		try {
			result = startCherryPick();
			waitEgitJobs();
			return handleCherryPickResult(result);
		} catch (CoreException | IOException e) {
			throw new DiesOn(DeathType.ERROR).duedTo(e).displaying(e.getMessage()).ready();
		}
	}

	/**
	 * Starts to cherry pick commit.
	 * 
	 * @return {@link CherryPickResult}.
	 * @throws CoreException
	 *             from {@link CherryPickOperation#execute(org.eclipse.core.runtime.IProgressMonitor)}
	 */
	private CherryPickResult startCherryPick() throws CoreException {
		final CherryPickResult result;
		CherryPickOperation cherryPickOperation = new CherryPickOperation(repo, commit);
		cherryPickOperation.execute(new NullProgressMonitor());
		result = cherryPickOperation.getResult();
		return result;
	}

	/**
	 * Handles the cherry pick result.
	 * <p>
	 * It also prints a message to the user.
	 * </p>
	 * 
	 * @param cherryPickResult
	 *            result to handle.
	 * @return the return code of this operation.
	 * @throws Die
	 *             if an error is found in the result.
	 * @throws IOException
	 *             propagates JGit {@link IOException}.
	 */
	private Integer handleCherryPickResult(CherryPickResult cherryPickResult) throws Die, IOException {
		final Integer result;
		final String message;

		switch (cherryPickResult.getStatus()) {
			case OK:
				result = Returns.COMPLETE.code();
				message = getSuccessfullCherryPickMessage() + "Complete.";
				break;
			case CONFLICTING:
				// FIXME write the conflict commit message is .git/COMMIT_MSG and propose to the user to
				// modify it
				result = Returns.ABORTED.code();
				message = getConflictMessage(cherryPickResult.getFailingPaths());
				break;
			case FAILED:
				result = Returns.ERROR.code();
				message = "failed";
				break;
			default:
				throw new DiesOn(DeathType.ERROR).displaying(
						"Invalid rebase result:" + cherryPickResult.getStatus()).ready();
		}
		System.out.println(message);
		return result;
	}

	/**
	 * Gets the message to display in case the rebase result is a
	 * {@link org.eclipse.jgit.api.RebaseResult.Status#CONFLICTS}.
	 * 
	 * @param failingPaths
	 *            List of failing paths
	 * @return the message to display.
	 * @throws IOException
	 *             propagates JGIt exception.
	 */
	private String getConflictMessage(Map<String, MergeFailureReason> failingPaths) throws IOException {
		StringBuilder messageBuilder = new StringBuilder();

		// Displays the conflicting files
		if (failingPaths != null && !failingPaths.isEmpty()) {
			messageBuilder.append("error: There is some conflicts on the following files:").append(EOL);
			for (String conflictingFile : failingPaths.keySet()) {
				messageBuilder.append(conflictingFile);
			}
		}

		// Prints the hint message to the user
		if (commit != null) {
			String id = commit.abbreviate(SHORT_REV_COMMIT_ID_LENGTH).name();
			String cmMsg = commit.getShortMessage();
			String msg = "error: Could not apply [" + id + "]... " + cmMsg + EOL;
			msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
			msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
			msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
			msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
			msg += "hint: Then DO commit." + EOL;

			messageBuilder.append(msg);
		}

		return messageBuilder.toString();
	}

	/**
	 * Gets the message that notifies the user of new successfully cherry-picked commit.
	 * 
	 * @return the message that notifies the user of new successfully cherry-picked commit.
	 * @throws IOException
	 *             propagates JGit exception.
	 */
	private String getSuccessfullCherryPickMessage() throws IOException {
		ObjectId head = repo.getRef(Constants.HEAD).getObjectId();
		List<RevCommit> successfullCommits = getCommitsBetween(repo, head, oldHead);
		if (successfullCommits != null && !successfullCommits.isEmpty()) {
			final String message;
			StringBuilder messageBuilder = new StringBuilder();
			messageBuilder.append("The following revision was successfully cherry-picked:").append(EOL);
			for (RevCommit successfullCommit : successfullCommits) {
				messageBuilder.append(TAB).append(getOneLineCommitMsg(successfullCommit)).append(EOL);
			}
			message = messageBuilder.toString();
			return message;
		}
		return "";
	}

}

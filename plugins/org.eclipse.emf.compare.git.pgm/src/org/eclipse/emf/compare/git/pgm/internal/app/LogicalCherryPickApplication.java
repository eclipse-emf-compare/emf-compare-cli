/*******************************************************************************
 * Copyright (c) 2014 Obeo.
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
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.waitEgitJobs;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.CherryPickOperation;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.args.RevCommitOptionHandler;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DeathType;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DiesOn;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Logical cherry-pick command. <h3>Name</h3>
 * <p>
 * logicalcherry-pick - Git Logical cherry-pick
 * </p>
 * <h4>Synopsis</h4>
 * <p>
 * logicalcherry-pick &lt;setup&gt; &lt;commit...&gt; [--abort] [--continue] [--quit] [--show-stack-trace]
 * [--git-dir &lt;gitDirectory&gt;]
 * </p>
 * <h4>Description</h4>
 * <p>
 * The logical cherry-pick is used to cherry-pick one or more revision using logical model.
 * </p>
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings({"restriction", "nls" })
public class LogicalCherryPickApplication extends AbstractLogicalApplication {

	/** Short commit ID length. */
	private static final int SHORT_REV_COMMIT_ID_LENGTH = 7;

	/**
	 * Message to display when there is nothing to commit.
	 */
	private static final String NOTING_TO_COMMIT_MESSAGE = "No changes detected" + EOL //
			+ EOL //
			+ "If there is nothing left to stage, chances are that something" + EOL//
			+ "else already introduced the same changes; you might want to skip" + EOL//
			+ "this patch using git logicalmerge --quit" + EOL;

	/**
	 * Holds {@link RevCommit}s that need to be merged.
	 */
	@Argument(index = 2, required = false, multiValued = true, metaVar = "<commit>", usage = "Commit IDs to cherry pick.", handler = RevCommitOptionHandler.class)
	private List<RevCommit> commits;

	/** Continue option. */
	@Option(required = false, name = "--continue", usage = "Use this option to continue a in going cherry-pick")
	private boolean continueOpt;

	/** Abort option. */
	@Option(required = false, name = "--abort", usage = "Use this option to abort a in going cherry-pick")
	private boolean abortOpt;

	/** Quit option. */
	@Option(required = false, name = "--quit", usage = "Use this option to qui a in going cherry-pick")
	private boolean quitOpt;

	/** Holds a reference to the HEAD before any operation. */
	private ObjectId oldHead;

	@Override
	protected Integer performGitCommand() throws Die {
		try {
			oldHead = repo.getRef(Constants.HEAD).getObjectId();
		} catch (RevisionSyntaxException | IOException e) {
			throw new DiesOn(DeathType.ERROR).duedTo(e).displaying(e.getMessage()).ready();
		}
		final RebaseResult result;
		try {
			if (!continueOpt && !abortOpt && !quitOpt) {
				result = startCherryPick();
			} else {
				result = processRebaseStep();
			}
		} catch (CoreException e) {
			throw new DiesOn(DeathType.ERROR).duedTo(e).displaying(e.getMessage()).ready();
		}
		waitEgitJobs();
		return handleRebaseResult(result);
	}

	/**
	 * Processes a rebase step. This method handles one of the following option:
	 * <ul>
	 * <li>--continue</li>
	 * <li>--abort</li>
	 * <li>--quit</li>
	 * </ul>
	 * 
	 * @return {@link RebaseResult}.
	 * @throws CoreException
	 *             from {@link RebaseOperation#execute(org.eclipse.core.runtime.IProgressMonitor)}
	 */
	private RebaseResult processRebaseStep() throws CoreException {
		final RebaseResult result;
		// If one of the option is used then launch a rebase operation
		final Operation op;
		if (abortOpt) {
			op = Operation.ABORT;
		} else if (quitOpt) {
			op = Operation.SKIP;
		} else {
			op = Operation.CONTINUE;
		}
		RebaseOperation rebaseOperation = new RebaseOperation(repo, op);
		rebaseOperation.execute(new NullProgressMonitor());
		result = rebaseOperation.getResult();
		return result;
	}

	/**
	 * Starts to cherry pick commits.
	 * 
	 * @return {@link RebaseResult}.
	 * @throws CoreException
	 *             from {@link CherryPickOperation#execute(org.eclipse.core.runtime.IProgressMonitor)}
	 */
	private RebaseResult startCherryPick() throws CoreException {
		final RebaseResult result;
		// Cherrypick operation expects commits in reverse order. See thee
		// org.eclipse.jgit.api.RebaseCommand.InteractiveHandler CherrypickOPeration
		Collections.reverse(commits);

		CherryPickOperation cherryPickOperation = new CherryPickOperation(repo, commits);
		cherryPickOperation.execute(new NullProgressMonitor());
		result = cherryPickOperation.getResult();
		return result;
	}

	/**
	 * Handles the rebase result.
	 * <p>
	 * It also prints a message to the user.
	 * </p>
	 * 
	 * @param rebaseResult
	 *            result to handle.
	 * @return the return code of this operation.
	 * @throws Die
	 *             if an error is found in the result.
	 */
	private Integer handleRebaseResult(RebaseResult rebaseResult) throws Die {
		final Integer result;
		final String message;

		switch (rebaseResult.getStatus()) {
			case OK:
				result = Returns.COMPLETE.code();
				message = getSuccessfulCherryPickMessage(getNewCommits()) + "Complete.";
				break;
			case FAST_FORWARD:
				// FIXME: Test this use case again after correction of
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=451159
				result = Returns.COMPLETE.code();
				message = "Fast forward." + EOL //
						+ getSuccessfulCherryPickMessage(getNewCommits());
				break;
			case UP_TO_DATE:
				result = Returns.COMPLETE.code();
				message = "Up to date.";
				break;
			case ABORTED:
				result = Returns.ABORTED.code();
				message = "Aborted.";
				break;
			case NOTHING_TO_COMMIT:
				result = Returns.ABORTED.code();
				message = NOTING_TO_COMMIT_MESSAGE;
				break;
			case STOPPED:
			case CONFLICTS:
				// FIXME write the conflict commit message is .git/COMMIT_MSG and propose to the user to
				// modify it
				result = Returns.ABORTED.code();
				message = getConflictMessage(rebaseResult.getConflicts(), rebaseResult.getCurrentCommit());
				break;
			case FAILED:
				result = Returns.ERROR.code();
				message = "failed";
				break;
			case UNCOMMITTED_CHANGES:// Should never happen since a validation is done before
				// Should never ever happen during cherry pick
			case INTERACTIVE_PREPARED:
			case STASH_APPLY_CONFLICTS:
			case EDIT:
			default:
				throw new DiesOn(DeathType.ERROR).displaying(
						"Invalid rebase result:" + rebaseResult.getStatus()).ready();
		}
		System.out.println(message);
		return result;
	}

	/**
	 * Gets the message to display in case the rebase result is a
	 * {@link org.eclipse.jgit.api.RebaseResult.Status#CONFLICTS}.
	 * 
	 * @param conflictingFiles
	 *            List of conflicting files.
	 * @param currentCommit
	 *            {@link RevCommit} where the rebase has stopped.
	 * @return the message to display.
	 */
	private String getConflictMessage(List<String> conflictingFiles, RevCommit currentCommit) {
		StringBuilder messageBuilder = new StringBuilder();
		messageBuilder.append(getSuccessfulCherryPickMessage(getNewCommits()));

		// Displays the list on conflicting files
		if (conflictingFiles != null && !conflictingFiles.isEmpty()) {
			messageBuilder.append("error: There is some conflicts on the following files:").append(EOL);
			for (String conflictingFile : conflictingFiles) {
				messageBuilder.append(conflictingFile);
			}
		}

		// Prints the hint message to the user
		if (currentCommit != null) {
			String id = currentCommit.abbreviate(SHORT_REV_COMMIT_ID_LENGTH).name();
			String cmMsg = currentCommit.getShortMessage();
			String msg = "error: Could not apply [" + id + "]... " + cmMsg + EOL;
			msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
			msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
			msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
			msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
			msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
			msg += "hint:  git logical-cherrypick --continue : to continue the cherry pick" + EOL;
			msg += "hint:  git logical-cherrypick --abort : to abort the cherry pick" + EOL;
			msg += "hint:  git logical-cherrypick --quit : to skip this commit" + EOL;

			messageBuilder.append(msg);
		}

		return messageBuilder.toString();
	}

	/**
	 * Gets the message that notifies the user of new successfully cherry-picked commits.
	 * 
	 * @param successfullCommits
	 *            List of {@link RevCommit}s to display.
	 * @return the message that notifies the user of new successfully cherry-picked commits.
	 */
	private String getSuccessfulCherryPickMessage(List<RevCommit> successfullCommits) {
		if (successfullCommits != null && !successfullCommits.isEmpty()) {
			final String message;
			StringBuilder messageBuilder = new StringBuilder();
			messageBuilder.append("The following revisions were successfully cherry-picked:").append(EOL);
			for (RevCommit commit : successfullCommits) {
				messageBuilder.append(getCommitMessage(commit));
			}
			message = messageBuilder.toString();
			return message;
		}
		return "";
	}

	/**
	 * Gets a message to display one {@link RevCommit}.
	 * 
	 * @param revCommit
	 *            {@link RevCommit} to print
	 * @return a message to display one {@link RevCommit}.
	 */
	private String getCommitMessage(RevCommit revCommit) {
		String id = revCommit.abbreviate(SHORT_REV_COMMIT_ID_LENGTH).name();
		String message = revCommit.getShortMessage();
		return "\t[" + id + "] " + message + EOL;
	}

	/**
	 * Gets the list of new {@link RevCommit}s since {@link #oldHead}.
	 * 
	 * @return the list of new {@link RevCommit}s since {@link #oldHead}
	 */
	private List<RevCommit> getNewCommits() {
		RevWalk revWak = new RevWalk(repo);
		try {
			RevCommit oldHeadRev = revWak.parseCommit(oldHead);
			revWak.markStart(revWak.parseCommit(repo.getRef(Constants.HEAD).getObjectId()));
			revWak.markUninteresting(oldHeadRev);
			return Lists.newArrayList(revWak);
		} catch (IOException e) {
			if (isShowStackTrace()) {
				e.printStackTrace();
			}
			return Collections.emptyList();
		} finally {
			revWak.release();
		}
	}
}

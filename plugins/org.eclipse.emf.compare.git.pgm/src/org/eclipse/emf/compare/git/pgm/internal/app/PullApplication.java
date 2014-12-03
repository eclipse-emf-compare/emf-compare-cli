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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.PullOperation;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DeathType;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DiesOn;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;

/**
 * Logical pull application. <h3>Name</h3>
 * 
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 */
@SuppressWarnings({"restriction" })
public class PullApplication extends AbstractLogicalApplication {

	/** Timeout of the pull operation in seconds. */
	private static final int TIMEOUT = 30;

	/**
	 * Length of a short commit id.
	 */
	private static final int SHORT_COMMIT_ID_LENGTH = 7;

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IOException
	 */
	@Override
	protected Integer performGitCommand() throws Die {
		try {
			PullOperation operation = new PullOperation(Sets.newHashSet(repo), TIMEOUT);
			operation.execute(new NullProgressMonitor());
			Object value = operation.getResults().get(repo);
			if (value instanceof PullResult) {
				PullResult result = (PullResult)value;
				Ref oldHead = repo.getRef(Constants.HEAD);
				return handleResult(result, oldHead, MergeStrategy.RECURSIVE);
			} else if (value instanceof IStatus) {
				throw new DiesOn(DeathType.ERROR).displaying(((IStatus)value).getMessage()).ready();
			} else {
				throw new DiesOn(DeathType.ERROR).displaying("No results to display for pull operation.")
						.ready();
			}
		} catch (CoreException | IOException e) {
			throw new DiesOn(DeathType.ERROR).duedTo(e).displaying(e.getMessage()).ready();
		}
	}

	/**
	 * Handles the pull result. This method return the {@link Returns} depending of the pull status and
	 * display a message to the user.
	 * 
	 * @param result
	 *            The result of the pull.
	 * @param oldHead
	 *            the old HEAD reference before merge.
	 * @param strategy
	 *            The strategy used for the merge.
	 * @return a {@link Returns}.
	 * @throws Die
	 *             if the merge ends on error.
	 */
	private Integer handleResult(PullResult result, Ref oldHead, MergeStrategy strategy) throws Die {
		final Integer returnCode;
		final String messageToPrint;
		MergeResult mergeResult = result.getMergeResult();
		switch (mergeResult.getMergeStatus()) {
			case MERGED:
				messageToPrint = new StringBuilder().append("Merge made by '").append(strategy.getName())
						.append("' strategy.").append(EOL).toString();
				returnCode = Returns.COMPLETE.code();
				break;
			case ALREADY_UP_TO_DATE:
				messageToPrint = new StringBuilder().append("Already up to date.").append(EOL).toString();
				returnCode = Returns.COMPLETE.code();
				break;
			case FAST_FORWARD:
				messageToPrint = buildFastForwardMessage(mergeResult, oldHead);
				returnCode = Returns.COMPLETE.code();
				break;
			case CONFLICTING:
				returnCode = Returns.ABORTED.code();
				messageToPrint = buildConflictingMessage(mergeResult);
				break;
			case FAILED:
				throw new DiesOn(DeathType.ERROR).displaying(getFailedMessage(mergeResult)).ready();
			case ABORTED:
				throw new DiesOn(DeathType.ERROR).displaying("There is no pull to abort.").ready();
			case NOT_SUPPORTED:
			case MERGED_NOT_COMMITTED:
			case CHECKOUT_CONFLICT:
			case MERGED_SQUASHED:
			case FAST_FORWARD_SQUASHED:
			case MERGED_SQUASHED_NOT_COMMITTED:
			default:
				throw new DiesOn(DeathType.SOFTWARE_ERROR).displaying(getDefaultErrorMessage(mergeResult))
						.ready();
		}
		System.out.println(messageToPrint);
		return returnCode;

	}

	/**
	 * Gets the default message error.
	 * 
	 * @param mergeResult
	 *            {@link MergeResult}.
	 * @return An error message.
	 */
	private String getDefaultErrorMessage(MergeResult mergeResult) {
		return new StringBuilder().append("Unsupported merge status '").append(
				mergeResult.getMergeStatus().toString()).append("'").append(EOL).toString();
	}

	/**
	 * Builds the message to display to the user when the merge ends on a FAST_FORWARD state.
	 * 
	 * @param mergeResult
	 *            The merge result.
	 * @param oldHead
	 *            The previous head before merge.
	 * @return a message.
	 */
	private String buildFastForwardMessage(MergeResult mergeResult, Ref oldHead) {
		final StringBuilder messageBuilder = new StringBuilder();
		ObjectId oldHeadId = oldHead.getObjectId();
		messageBuilder.append("Updating ").append(oldHeadId.abbreviate(SHORT_COMMIT_ID_LENGTH).name())
				.append("..").append(mergeResult.getNewHead().abbreviate(SHORT_COMMIT_ID_LENGTH).name())
				.append(EOL);
		messageBuilder.append(mergeResult.getMergeStatus().toString());
		return messageBuilder.toString();
	}

	/**
	 * Builds the message to display to the user when the merge ends on a FAILED status.
	 * 
	 * @param mergeResult
	 *            The merge result.
	 * @return a message.
	 */
	private String getFailedMessage(MergeResult mergeResult) {
		final StringBuilder errorMessage = new StringBuilder();
		List<String> dirtyFiles = Lists.newArrayList();
		List<String> notDeletedFiles = Lists.newArrayList();
		for (Entry<String, MergeFailureReason> mergeFailure : mergeResult.getFailingPaths().entrySet()) {

			switch (mergeFailure.getValue()) {
				case DIRTY_INDEX:
				case DIRTY_WORKTREE:
					dirtyFiles.add(mergeFailure.getKey());
					break;
				case COULD_NOT_DELETE:
					notDeletedFiles.add(mergeFailure.getKey());
					break;
				default:
					break;
			}
		}
		if (!dirtyFiles.isEmpty()) {
			errorMessage.append("Your local changes to the following files would be overwritten by merge:"
					+ EOL);
			errorMessage.append(Joiner.on(EOL).join(dirtyFiles));
			errorMessage.append("Please, commit your changes or stash them before you can merge.");
		}
		if (!notDeletedFiles.isEmpty()) {
			errorMessage.append("Could not delete following files:" + EOL);
			errorMessage.append(Joiner.on(EOL).join(notDeletedFiles));
		}
		errorMessage.append("Aborting." + EOL);
		return errorMessage.toString();
	}

	/**
	 * Builds the message to display to the user when merge ends on a conflicting state.
	 * 
	 * @param mergeResult
	 *            {@link MergeResult}.
	 * @return a message.
	 */
	private String buildConflictingMessage(MergeResult mergeResult) {
		final StringBuilder messageBuildder = new StringBuilder();
		try {
			// Should use mergeResult.getConflicting() however due to its random result we prefer using the
			// status of the git repository.
			final Status status = Git.wrap(repo).status().call();
			List<String> conflictingFile = Lists.newArrayList(status.getConflicting());
			// In order to have a determinist order.
			Collections.sort(conflictingFile);
			for (String conflicting : conflictingFile) {
				messageBuildder.append("Auto-merging failed in ").append(conflicting).append(EOL);

			}
		} catch (NoWorkTreeException e) {
			// Does nothing since this for console message
		} catch (GitAPIException e) {
			// Does nothing since this for console message
		}
		messageBuildder.append("Automatic merge failed; fix conflicts and then commit the result.").append(
				EOL);

		return messageBuildder.toString();
	}
}

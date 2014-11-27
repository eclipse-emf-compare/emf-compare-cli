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

import static org.eclipse.emf.compare.git.pgm.internal.cmd.LogicalRebaseCommand.ABORT_OPT;
import static org.eclipse.emf.compare.git.pgm.internal.cmd.LogicalRebaseCommand.CONTINUE_OPT;
import static org.eclipse.emf.compare.git.pgm.internal.cmd.LogicalRebaseCommand.SKIP_OPT;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.TAB;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.waitEgitJobs;
import static org.eclipse.emf.compare.git.pgm.internal.util.GitUtils.getCommitsBetween;
import static org.eclipse.emf.compare.git.pgm.internal.util.GitUtils.getCurrentBranchRemoteTrackingRef;
import static org.eclipse.emf.compare.git.pgm.internal.util.GitUtils.getOneLineCommitMsg;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.args.RefOptionHandler;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DeathType;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DiesOn;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Logical rebase application. <h3>Name</h3>
 * <p>
 * logicalrebase - Git Logical rebase
 * </p>
 * <h4>Synopsis</h4>
 * <p>
 * logicalrebase &lt;setup&gt; [&lt;upstream&gt;] [&lt;branch&gt;] [--abort] [--continue] [--quit]
 * [--show-stack-trace] [--git-dir &lt;gitDirectory&gt;]
 * </p>
 * <h4>Description</h4>
 * <p>
 * The logical rebase is used to rebase a branch using the logical model.
 * </p>
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings({"restriction", "nls" })
public class LogicalRebaseApplication extends AbstractLogicalApplication {

	/** Reference on top of which commits will be rebased. */
	@Argument(index = 2, required = false, metaVar = "<upstream>", usage = "Upstream reference on top of which commits will be rebased.", handler = RefOptionHandler.class)
	private Ref upstream;

	/** Reference to rebase. */
	@Argument(index = 3, required = false, metaVar = "<branch>", usage = "Branch to rebase.", handler = RefOptionHandler.class)
	private Ref toRebase;

	/** Continue options. */
	@Option(required = false, name = CONTINUE_OPT, usage = "Use this option to continue an in going cherry-pick.")
	private boolean continueOpt;

	/** Abort option. */
	@Option(required = false, name = ABORT_OPT, usage = "Use this option to abort an in going cherry-pick.")
	private boolean abortOpt;

	/** Skip option. */
	@Option(required = false, name = SKIP_OPT, usage = "Use this option to skip the current commit being rebased.")
	private boolean skipOpt;

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.git.pgm.internal.app.AbstractLogicalApplication#performGitCommand()
	 */
	@Override
	protected Integer performGitCommand() throws Die {

		final RebaseResult result;
		try {

			if (toRebase != null) {
				checkoutNewHead(toRebase);
			} else {
				toRebase = repo.getRef(Constants.HEAD).getLeaf();
			}

			if (continueOpt || abortOpt || skipOpt) {
				result = processRebaseStep();
			} else {
				result = startRebase();
			}
			waitEgitJobs();
			return handleRebaseResult(result);

		} catch (CoreException | IOException e) {
			throw new DiesOn(DeathType.ERROR).duedTo(e).displaying(e.getMessage()).ready();
		}
	}

	/**
	 * Checkouts a new a {@link RevCommit}.
	 * 
	 * @param ref
	 *            {@link Ref} to checkout.
	 * @throws Die
	 *             Throws {@link Die} in case of an unexpected {@link CheckoutResult} status or in case of an
	 *             error during the checkout operation.
	 * @throws IOException
	 *             Propagates JGit exceptions.
	 */
	private void checkoutNewHead(Ref ref) throws Die, IOException {
		try {
			BranchOperation checkoutOperation = new BranchOperation(repo, ref.getName());
			checkoutOperation.execute(new NullProgressMonitor());

			waitEgitJobs();

			CheckoutResult result = checkoutOperation.getResult();
			switch (result.getStatus()) {
				case NONDELETED:
					String message = "Moving HEAD to " + getOneLineCommitMsg(toRevCommit(ref)) + EOL;
					message += "Failed to deleted the following files:" + EOL;
					for (String notDeleted : result.getUndeletedList()) {
						message += TAB + notDeleted + EOL;
					}
					message += "Please remove those files before rebasing." + EOL;
					throw new DiesOn(DeathType.FATAL) //
							.displaying(message) //
							.ready();
				case OK:
					System.out.println("Switched to branch '" + ref.getName() + "'" + EOL);
					break;
				case CONFLICTS:
					// Fallback to default since it should not happen since we force the repository to have no
					// uncommitted change neither
					// untracked file before rebasing.
				case ERROR:
				case NOT_TRIED:
				default:
					String errorMsg = "Error while checkouting " + getOneLineCommitMsg(toRevCommit(ref))
							+ EOL;
					errorMsg += "Invalid rebase status result: " + result.getStatus() + EOL;
					throw new DiesOn(DeathType.FATAL) //
							.displaying(errorMsg) //
							.ready();
			}
		} catch (CoreException e) {
			throw new DiesOn(DeathType.FATAL).displaying(
					"Error while checkouting " + ref.getName() + ": " + e.getMessage()) //
					.duedTo(e) //
					.ready();
		}
	}

	/**
	 * Transforms a {@link Ref} to {@link RevCommit}.
	 * 
	 * @param ref
	 *            to transform
	 * @return a {@link RevCommit} (never <code>null</code>)
	 * @throws Die
	 *             if the ref is not targetting a {@link RevCommit}.
	 */
	private RevCommit toRevCommit(Ref ref) throws Die {
		ObjectId objectId = ref.getObjectId();
		if (objectId instanceof RevCommit) {
			return (RevCommit)objectId;
		} else {
			RevWalk revWalk = new RevWalk(repo);
			try {
				return revWalk.parseCommit(objectId);
			} catch (IOException e) {
				throw new DiesOn(DeathType.FATAL).displaying("Invalid ref " + ref.getName()).duedTo(e)
						.ready();
			} finally {
				revWalk.release();
			}
		}
	}

	/**
	 * Handles the {@link RebaseResult} after a {@link RebaseOperation}.
	 * 
	 * @param rebaseResult
	 *            from a {@link RebaseOperation}.
	 * @return return code.
	 * @throws IOException
	 *             Propagation of JGit exceptions.
	 * @throws Die
	 *             in case of an unexpected rebaseResult status.
	 */
	private Integer handleRebaseResult(RebaseResult rebaseResult) throws IOException, Die {
		final Integer result;

		switch (rebaseResult.getStatus()) {
			case OK:
				result = Returns.COMPLETE.code();
				System.out.println(getSuccessfullRebaseMessage());
				break;
			case UP_TO_DATE:
				result = Returns.COMPLETE.code();
				System.out.println("Current branch '" + toRebase.getName() + "' is up to date." + EOL);
				break;
			case FAST_FORWARD:
				// FIXME: Test this use case again after correction of
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=451159
				result = Returns.COMPLETE.code();
				System.out.println("Fast forwarded '" + toRebase.getName() + "' to '" + upstream.getName()
						+ "'." + EOL);
				break;
			case ABORTED:
				result = Returns.COMPLETE.code();
				System.out.println("Aborted." + EOL);
				break;
			case STOPPED:
				result = handleStopped(rebaseResult.getCurrentCommit());
				break;
			case CONFLICTS:
				result = Returns.ABORTED.code();
				System.out.println(getConflictDuringCheckoutMessage(rebaseResult.getConflicts()));
				break;
			case FAILED:
				result = Returns.ERROR.code();
				System.out.println("Failed.");
				break;
			case NOTHING_TO_COMMIT:
				result = handleNothingToCommit();
				break;
			case UNCOMMITTED_CHANGES:// Should never happen since a validation is done before
			case INTERACTIVE_PREPARED: // Should never ever happen during cherry pick
			case STASH_APPLY_CONFLICTS: // Fallback to default since it should not happen while we do not
										// handle interactive rebase.
			case EDIT: // Fallback to default since it should not happen while we do not handle
						// interactive rebase.
			default:
				throw new DiesOn(DeathType.ERROR).displaying(
						"Invalid rebase result:" + rebaseResult.getStatus()).ready();
		}
		return result;
	}

	/**
	 * Handles a {@link org.eclipse.jgit.api.RebaseResult.Status#NOTHING_TO_COMMIT} status.
	 * 
	 * @return the return code
	 */
	private Integer handleNothingToCommit() {
		final Integer result;
		result = Returns.ABORTED.code();
		String msg = "No changes - did you forget to use 'git add'?" + EOL;
		msg += "If there is nothing left to stage, chances are that something else" + EOL;
		msg += "already introduced the same changes; you might want to skip this patch." + EOL;
		msg += EOL;
		msg += "When you have resolved this problem, run \"git logicalrebase --continue\"." + EOL;
		msg += "If you prefer to skip this patch, run \"git logicalrebase --skip\" instead." + EOL;
		msg += "To check out the original branch and stop rebasing, run \"git logicalrebase --abort\"." + EOL;
		System.out.println(msg);
		return result;
	}

	/**
	 * Handles a {@link org.eclipse.jgit.api.RebaseResult.Status#STOPPED} status.
	 * 
	 * @param currentCommit
	 *            commit where the rebased has stopped.
	 * @return the return code.
	 * @throws IOException
	 *             propagates JGit exception.
	 * @throws Die
	 *             on a unexpected repository state.
	 */
	private Integer handleStopped(RevCommit currentCommit) throws IOException, Die {
		final Integer result;
		result = Returns.ABORTED.code();
		Status status;
		try {
			status = getGit().status().call();
		} catch (NoWorkTreeException e) {
			status = null;
		} catch (GitAPIException e) {
			status = null;
		}
		if (status != null && !status.getConflicting().isEmpty()) {
			// FIXME Write the conflict commit message is .git/COMMIT_MSG and propose to the user to
			// modify it
			System.out.println(getStopOnConflictMessage(currentCommit));
		} else {
			// While we do not handle interactive rebase the only use case of a stopping rebase is a
			// conflict (which is handled above). The other use cases should not happen.
			throw new DiesOn(DeathType.ERROR).displaying("Invalid stop after rebase").ready();
		}
		return result;
	}

	/**
	 * Gets the message to display in case the rebase result is a
	 * {@link org.eclipse.jgit.api.RebaseResult.Status#CONFLICTS}.
	 * 
	 * @param conflictingFiles
	 *            List of conflicting files.
	 * @return the message to display.
	 * @throws IOException
	 *             propagates JGIt exception.
	 */
	private String getConflictDuringCheckoutMessage(List<String> conflictingFiles) throws IOException {
		StringBuilder messageBuilder = new StringBuilder();
		messageBuilder.append(getSuccessfullRebaseMessage());

		String msg = "error: Failed to checkout HEAD" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL;

		messageBuilder.append(msg);

		// Displays the list on conflicting files
		if (conflictingFiles != null && !conflictingFiles.isEmpty()) {
			messageBuilder.append("error: There is some conflicts on the following files:").append(EOL);
			for (String conflictingFile : conflictingFiles) {
				messageBuilder.append(conflictingFile);
			}
		}
		return messageBuilder.toString();
	}

	/**
	 * Gets the message to display in case the {@link RebaseResult} is a
	 * {@link org.eclipse.jgit.api.RebaseResult.Status#STOPPED} and the repository contains files with
	 * conflicts.
	 * 
	 * @param currentCommit
	 *            {@link RevCommit} where the rebase has stopped.
	 * @return a message.
	 * @throws IOException
	 *             Propagates JGit exception.
	 */
	private String getStopOnConflictMessage(RevCommit currentCommit) throws IOException {
		StringBuilder messageBuilder = new StringBuilder();
		messageBuilder.append(getSuccessfullRebaseMessage());
		// Prints the hint message to the user
		if (currentCommit != null) {
			String msg = "error: Could not apply " + getOneLineCommitMsg(currentCommit) + EOL;
			msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
			msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
			msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
			msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
			msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
			msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
			msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
			msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL;

			messageBuilder.append(msg);
		}

		return messageBuilder.toString();
	}

	/**
	 * Starts a new {@link RebaseOperation}.
	 * 
	 * @return {@link RebaseResult} of the operation.
	 * @throws CoreException
	 *             Propagates EGit exceptions.
	 * @throws IOException
	 *             Propagates JGit exceptions.
	 * @throws Die
	 *             Throws a {@link Die} exception if it was not able to find a valid reference for upstream.
	 */
	private RebaseResult startRebase() throws CoreException, Die, IOException {
		if (upstream == null) {
			// If <upstream> is not specified, the upstream configured in branch.<name>.remote and
			// branch.<name>.merge options will be used; see git-config(1) for details. If you are currently
			// not on any branch or if the current branch does not have a configured
			// upstream, the rebase will abort.
			upstream = getCurrentBranchRemoteTrackingRef(repo);
		}
		// Should never failed since it has been checked during command launch
		if (upstream == null) {
			throw new DiesOn(DeathType.FATAL).displaying(
					"Please specify which branch you want to rebase against.").ready();
		}
		RebaseOperation rebaseOperation = new RebaseOperation(repo, upstream);
		rebaseOperation.execute(new NullProgressMonitor());
		return rebaseOperation.getResult();
	}

	/**
	 * Gets the message that notifies the user of new successfully rebased commits.
	 * 
	 * @return the message that notifies the user of new successfully rebased commits.
	 * @throws IOException
	 *             propagates JGit exception.
	 */
	private String getSuccessfullRebaseMessage() throws IOException {
		ObjectId head = repo.getRef(Constants.HEAD).getObjectId();

		List<RevCommit> successfullCommits = getCommitsBetween(repo, head, upstream.getObjectId());

		String message;
		if (!continueOpt && !abortOpt && !skipOpt) {
			// If we have just started to rebase notify the user that the HEAD has moved
			message = "Has rewinded head to replay your work on top of.." + EOL;
		} else {
			message = "";
		}

		if (successfullCommits != null && !successfullCommits.isEmpty()) {
			for (RevCommit commit : successfullCommits) {
				message += "Applied " + getOneLineCommitMsg(commit) + EOL;
			}
		}
		return message;
	}

	/**
	 * Processes a rebase step. This method handles one of the following option:
	 * <ul>
	 * <li>--continue</li>
	 * <li>--abort</li>
	 * <li>--skip</li>
	 * </ul>
	 * 
	 * @return {@link RebaseResult}.
	 * @throws CoreException
	 *             from {@link RebaseOperation#execute(org.eclipse.core.runtime.IProgressMonitor)}
	 * @throws IOException
	 *             Propagates JGit exceptions.
	 */
	private RebaseResult processRebaseStep() throws CoreException, IOException {
		upstream = repo.getRef(Constants.HEAD);
		final Operation op;
		if (abortOpt) {
			op = Operation.ABORT;
		} else if (skipOpt) {
			op = Operation.SKIP;
		} else {
			op = Operation.CONTINUE;
		}
		RebaseOperation rebaseOperation = new RebaseOperation(repo, op);
		rebaseOperation.execute(new NullProgressMonitor());
		return rebaseOperation.getResult();
	}
}

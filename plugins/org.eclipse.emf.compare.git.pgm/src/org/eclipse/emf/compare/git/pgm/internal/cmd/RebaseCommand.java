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
package org.eclipse.emf.compare.git.pgm.internal.cmd;

import static org.eclipse.emf.compare.git.pgm.internal.args.ValidationStatus.createErrorStatus;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.TAB;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.toFileWithAbsolutePath;
import static org.eclipse.emf.compare.git.pgm.internal.util.GitUtils.getCurrentBranchRemoteTrackingRef;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.args.RefHandler;
import org.eclipse.emf.compare.git.pgm.internal.args.ValidationStatus;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.util.LogicalApplicationLauncher;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.oomph.util.OS;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Logical rebase command. <h3>Name</h3>
 * <p>
 * logicalrebase - Git Logical rebase
 * </p>
 * <h4>Synopsis</h4>
 * <p>
 * logicalrebase &lt;setup&gt; [&lt;upstream&gt;] [&lt;branch&gt;] [--abort] [--continue] [--skip]
 * [--show-stack-trace] [--git-dir &lt;gitDirectory&gt;]
 * </p>
 * <h4>Description</h4>
 * <p>
 * The logical rebase is used to rebase a branch using the logical model.
 * </p>
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public class RebaseCommand extends AbstractLogicalCommand {

	/** Continue option key. */
	public static final String CONTINUE_OPT = "--continue"; //$NON-NLS-1$

	/** Abort option key. */
	public static final String ABORT_OPT = "--abort"; //$NON-NLS-1$

	/** Skip option key. */
	public static final String SKIP_OPT = "--skip"; //$NON-NLS-1$

	/** Command name. */
	static final String LOGICAL_REBASE_CMD_NAME = "logicalrebase"; //$NON-NLS-1$

	/** Id of the logicalrebase application. */
	static final String LOGICAL_REBASE_APP_ID = "emf.compare.git.logicalrebase"; //$NON-NLS-1$

	/** Reference on top of which commits will be rebased. */
	@Argument(index = 1, required = false, metaVar = "<upstream>", usage = "Upstream reference on top of which commits will be rebased.", handler = RefHandler.class)
	private Ref upstream;

	/** Reference to rebase. */
	@Argument(index = 2, required = false, metaVar = "<branch>", usage = "Branch to rebase.", handler = RefHandler.class)
	private Ref toRebase;

	/** Continue option. */
	@Option(required = false, name = CONTINUE_OPT, usage = "Use this option to continue an in going rebase operation.")
	private boolean continueOpt;

	/** Abort option. */
	@Option(required = false, name = ABORT_OPT, usage = "Use this option to abort an in going rebase operation.")
	private boolean abortOpt;

	/** Skip option. */
	@Option(required = false, name = SKIP_OPT, usage = "Use this option to skip the current commit being rebased.")
	private boolean skipOpt;

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.git.pgm.internal.cmd.AbstractLogicalCommand#internalRun()
	 */
	@Override
	protected Integer internalRun() throws Die, IOException {
		OS os = getPerformer().getOS();
		if (!os.isCurrent()) {
			return Returns.ERROR.code();
		}

		String setupFileAbsolutePath = this.getSetupFile().getAbsolutePath();
		String setupFileBasePath = Paths.get(setupFileAbsolutePath).getParent().toString();

		String eclipseExecutable = os.getEclipseExecutable();
		Path installationPath = Paths.get(getPerformer().getInstallationLocation().getPath(), os
				.getEclipseDir(), eclipseExecutable);
		File eclipseFile = toFileWithAbsolutePath(setupFileBasePath, installationPath.toString());

		final String workspaceLocation;
		File performerWorkspaceLocation = getPerformer().getWorkspaceLocation();
		if (performerWorkspaceLocation != null) {
			workspaceLocation = performerWorkspaceLocation.toString();
		} else {
			workspaceLocation = null;
		}

		//@formatter:off
		LogicalApplicationLauncher appLauncher = new LogicalApplicationLauncher(out())
				.setApplicationName(LOGICAL_REBASE_APP_ID)
				.setEclipsePath(eclipseFile.toString())
				.setSetupFilePath(setupFileAbsolutePath)
				.setWorkspaceLocation(workspaceLocation)
				.setRepositoryPath(getRepository().getDirectory().getAbsolutePath())
				.showStackTrace(isShowStackTrace());
		//@formatter:on
		if (continueOpt) {
			appLauncher.addAttribute(CONTINUE_OPT);
		} else if (abortOpt) {
			appLauncher.addAttribute(ABORT_OPT);
		} else if (skipOpt) {
			appLauncher.addAttribute(SKIP_OPT);
		} else {
			if (upstream != null) {
				appLauncher.addAttribute(upstream.getName());
			}
			if (toRebase != null) {
				appLauncher.addAttribute(toRebase.getName());
			}
		}

		return appLauncher.launch();
	}

	@Override
	protected ValidationStatus getValidationStatus() {
		final ValidationStatus result;
		if (isRebasing()) {
			result = checkRebasingArgs();
		} else {
			result = checkStartingRebaseArguments();
		}
		return result;
	}

	/**
	 * Checks that the command arguments are valid while there is no current cherry-pick.
	 * 
	 * @return {@link ValidationStatus}
	 */

	private ValidationStatus checkStartingRebaseArguments() {
		final ValidationStatus result;
		if (abortOpt || skipOpt || continueOpt) {
			result = createErrorStatus("No rebase in progress.");
		} else if (upstream == null && getCurrentBranchRemoteTrackingRef(getRepository()) == null) {
			// If <upstream> is not specified, the upstream configured in branch.<name>.remote and
			// branch.<name>.merge options will be used; see git-config(1) for details. If you are currently
			// not on any branch or if the current branch does not have a configured
			// upstream, the rebase will abort.
			String message = "There is no tracking information for the current branch." + EOL;
			message += "Please specify which branch you want to rebase against." + EOL;
			message += "    git logicalrebase <setup> <branch>" + EOL;
			message += "Use 'git logicalrebase --help' command for details." + EOL;
			message += "If you wish to set tracking information for this branch you can do so with:" + EOL;
			message += "    git branch --set-upstream-to=<remote>/<branch> <currentBranchName>" + EOL;
			result = createErrorStatus(message);
		} else {
			try {
				result = getCleanRepoStatus();
			} catch (NoWorkTreeException | GitAPIException e) {
				return createErrorStatus("Could not read the status of the repository.");
			}
		}
		return result;
	}

	/**
	 * Returns {@link ValidationStatus#OK_STATUS} if the repository is clean (no uncommitted change neither
	 * untracked file), an error status with a message otherwise.
	 * 
	 * @return {@link ValidationStatus#OK_STATUS} if the repository is clean (no uncommitted change neither
	 *         untracked file), error status with a message otherwise.
	 * @throws GitAPIException
	 *             propagates JGit exception on the status command call.
	 */
	private ValidationStatus getCleanRepoStatus() throws GitAPIException {
		final ValidationStatus result;
		Status repoStatus = new Git(getRepository()).status().call();
		if (!repoStatus.getUncommittedChanges().isEmpty()) {
			String message = "Your local changes would be overwritten by rebase." + EOL;
			message += "hint: Please commit or stash the following files before rebasing:" + EOL;
			for (String uncommittedChange : repoStatus.getUncommittedChanges()) {
				message += TAB + uncommittedChange + EOL;
			}
			result = createErrorStatus(message);
		} else if (!repoStatus.getUntracked().isEmpty()) {
			String message = "The repository is not in a clean state. Please clean the following files before rebasing:"
					+ EOL;
			for (String untractedFile : repoStatus.getUntracked()) {
				message += TAB + untractedFile + EOL;
			}
			result = createErrorStatus(message);
		} else {
			result = super.getValidationStatus();
		}
		return result;
	}

	/**
	 * Checks that the command arguments are valid while rebasing.
	 * 
	 * @return {@link ValidationStatus}
	 */
	private ValidationStatus checkRebasingArgs() {
		final ValidationStatus result;
		boolean oneOptionSet = abortOpt || skipOpt || continueOpt;
		if (!oneOptionSet || upstream != null) {
			String msg = "We are currently rebasing. Please use one of the following options:" + EOL;
			msg += TAB + CONTINUE_OPT + EOL;
			msg += TAB + ABORT_OPT + EOL;
			msg += TAB + SKIP_OPT + EOL;
			result = createErrorStatus(msg);
		} else if (abortOpt && continueOpt) {
			result = createErrorStatus("logical rebase: --continue cannot be used with --abort");
		} else if (skipOpt && continueOpt) {
			result = createErrorStatus("logical rebase: --skip cannot be used with --continue");
		} else if (skipOpt && abortOpt) {
			result = createErrorStatus("logical rebase: --skip cannot be used with --abort");
		} else {
			RepositoryState state = getRepository().getRepositoryState();
			// We have to check both state see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=452623
			if (continueOpt
					&& (state == RepositoryState.REBASING_MERGE || state == RepositoryState.REBASING_INTERACTIVE)) {
				result = validatesNoConflict();

			} else {
				result = super.getValidationStatus();
			}

		}
		return result;
	}

	/**
	 * Validates that there is not conflicting file left.
	 * 
	 * @return {@link ValidationStatus#OK_STATUS} if no conflict left to merge, an error validation status
	 *         with a message otherwise.
	 */
	private ValidationStatus validatesNoConflict() {
		final ValidationStatus result;
		Status status;
		try {
			StringBuilder msgBuilder = new StringBuilder();
			status = new Git(getRepository()).status().call();
			Set<String> conflicting = status.getConflicting();
			if (!conflicting.isEmpty()) {
				msgBuilder.append("Some files are in conflict:").append(EOL);
				for (String conflict : conflicting) {
					msgBuilder.append(TAB).append(conflict).append(EOL);
				}
				msgBuilder.append("hint: You must edit all merge conflicts and then").append(EOL);
				msgBuilder.append("hint: mark them as resolved using git add.").append(EOL);
				result = createErrorStatus(msgBuilder.toString());
			} else {
				result = ValidationStatus.OK_STATUS;
			}
		} catch (NoWorkTreeException | GitAPIException e) {
			return createErrorStatus(e.getMessage());
		}
		return result;
	}

	/**
	 * Returns <code>true</code> if the {@link Repository} is currently rebasing.
	 * 
	 * @return <code>true</code> if the {@link Repository} is currently rebasing.
	 */
	private boolean isRebasing() {
		final boolean result;
		Repository repository = getRepository();
		if (repository != null) {
			switch (repository.getRepositoryState()) {
				case REBASING_INTERACTIVE:
				case REBASING:
				case REBASING_REBASING:
				case REBASING_MERGE:
					result = true;
					break;
				default:
					result = false;
			}
		} else {
			result = false;
		}
		return result;
	}

}

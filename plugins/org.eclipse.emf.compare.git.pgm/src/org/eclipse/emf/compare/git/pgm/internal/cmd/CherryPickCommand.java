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
package org.eclipse.emf.compare.git.pgm.internal.cmd;

import static org.eclipse.emf.compare.git.pgm.internal.args.ValidationStatus.createErrorStatus;
import static org.eclipse.emf.compare.git.pgm.internal.args.ValidationStatus.createErrorStatusWithUsage;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;

import java.io.IOException;

import org.eclipse.emf.compare.git.pgm.internal.args.RevCommitHandler;
import org.eclipse.emf.compare.git.pgm.internal.args.ValidationStatus;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.util.LogicalApplicationLauncher;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

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
public class CherryPickCommand extends AbstractLogicalCommand {

	/** Command name. */
	static final String LOGICAL_CHERRY_PICK_CMD_NAME = "logicalcherry-pick"; //$NON-NLS-1$

	/** Id of the logicalcherry-pick application. */
	static final String LOGICAL_CHERRYPICK_APP_ID = "emf.compare.git.logicalcherry-pick"; //$NON-NLS-1$

	/** Quit option key. */
	static final String QUIT_OPT = "--quit"; //$NON-NLS-1$

	/** Abort option key. */
	static final String ABORT_OPT = "--abort"; //$NON-NLS-1$

	/** Continue option key. */
	static final String CONTINUE_OPT = "--continue"; //$NON-NLS-1$

	/** Tab character. */
	private static final String TAB = "\t"; //$NON-NLS-1$

	/** Holds the {@link RevCommit} that needs to be merged. */
	@Argument(index = 1, required = true, metaVar = "<commit>", usage = "Commit ID to cherry pick.", handler = RevCommitHandler.class)
	private RevCommit commit;

	/** Option debug. */
	@Option(name = "--debug", usage = "Launches the provisionned eclipse in debug mode.", aliases = {"-d" })
	private boolean debug;

	@Override
	protected Integer internalRun() throws Die, IOException {

		String setupFileAbsolutePath = this.getSetupFile().getAbsolutePath();

		String eclipsePath = getEclipsePath(setupFileAbsolutePath);

		// Can not be null since it has been set in
		// org.eclipse.emf.compare.git.pgm.internal.cmd.AbstractLogicalCommand.createSetupTaskPerformer(String,
		// URI)
		final String workspacePath = getPerformer().getWorkspaceLocation().toString();

		//@formatter:off
		LogicalApplicationLauncher launcher = new LogicalApplicationLauncher(out())
				.setApplicationName(LOGICAL_CHERRYPICK_APP_ID)
				.setEclipsePath(eclipsePath)
				.debug(debug)
				.setSetupFilePath(setupFileAbsolutePath)
				.setWorkspaceLocation(workspacePath)
				.setRepositoryPath(getRepository().getDirectory().getAbsolutePath())
				.showStackTrace(isShowStackTrace());
		//@formatter:on
		launcher.addAttribute(commit.getName());

		return launcher.launch();

	}

	@Override
	protected ValidationStatus getValidationStatus() {
		final ValidationStatus result;
		if (isRebasingInteractive()) {
			result = checkRebasingCherryPickArgs();
		} else {
			result = checkStartingCherryPickArguments();
		}
		return result;
	}

	/**
	 * Checks that the command arguments are valid while there is no current cherry-pick.
	 * 
	 * @return {@link ValidationStatus}
	 */
	private ValidationStatus checkStartingCherryPickArguments() {
		final ValidationStatus result;
		boolean commitsProvided = commit != null;
		if (!commitsProvided) {
			result = createErrorStatusWithUsage("Argument \"<commit>\" is required");
		} else {
			try {
				Status repoStatus = new Git(getRepository()).status().call();
				if (!repoStatus.getUncommittedChanges().isEmpty()) {
					String message = "Your local changes would be overwritten by cherry-pick." + EOL;
					message += "hint: Please commit or stash the following files before cherry-picking:"
							+ EOL;
					for (String uncommittedChange : repoStatus.getUncommittedChanges()) {
						message += TAB + uncommittedChange + EOL;
					}
					result = createErrorStatus(message);
				} else if (!repoStatus.getUntracked().isEmpty()) {
					String message = "The repository is not in a clean state. Please clean the following files before cherry-picking:"
							+ EOL;
					for (String untractedFile : repoStatus.getUntracked()) {
						message += TAB + untractedFile + EOL;
					}
					result = createErrorStatus(message);
				} else {
					result = super.getValidationStatus();
				}
			} catch (NoWorkTreeException | GitAPIException e) {
				return createErrorStatus("Could read the status of the repository.");
			}
		}
		return result;
	}

	/**
	 * Checks that the command arguments are valid while rebasing.
	 * 
	 * @return {@link ValidationStatus}
	 */
	private ValidationStatus checkRebasingCherryPickArgs() {
		final ValidationStatus result;
		boolean commitsProvided = commit != null;
		if (commitsProvided) {
			String msg = "We are currently cherry-picking commit. Please use one of the following options:"
					+ EOL;
			msg += TAB + CONTINUE_OPT + EOL;
			msg += TAB + ABORT_OPT + EOL;
			msg += TAB + QUIT_OPT + EOL;
			result = createErrorStatus(msg);
		} else {
			RepositoryState state = getRepository().getRepositoryState();
			if (state == RepositoryState.REBASING_MERGE) {
				StringBuilder msgBuilder = new StringBuilder();
				Status status;
				try {
					status = new Git(getRepository()).status().call();
					for (String conflict : status.getConflicting()) {
						msgBuilder.append(conflict).append(": needs merge").append(EOL);
					}
					msgBuilder.append("You must edit all merge conflicts and then").append(EOL);
					msgBuilder.append("mark them as resolved using git add").append(EOL);
					result = createErrorStatus(msgBuilder.toString());
				} catch (NoWorkTreeException | GitAPIException e) {
					return createErrorStatus(e.getMessage());
				}

			} else {
				result = super.getValidationStatus();
			}
		}
		return result;
	}

	/**
	 * Returns <code>true</code> if the {@link Repository} is currently rebasing.
	 * 
	 * @return <code>true</code> if the {@link Repository} is currently rebasing.
	 */
	private boolean isRebasingInteractive() {
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

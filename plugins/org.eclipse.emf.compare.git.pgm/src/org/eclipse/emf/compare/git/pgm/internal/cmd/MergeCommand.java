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

import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;

import org.eclipse.emf.compare.git.pgm.internal.args.RevCommitHandler;
import org.eclipse.emf.compare.git.pgm.internal.args.ValidationStatus;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.util.LogicalApplicationLauncher;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Logical merge command. <h3>Name</h3>
 * <p>
 * logicalmerge - Git Logical Merge
 * </p>
 * <h4>Synopsis</h4>
 * <p>
 * logicalmerge [--show-stack-trace] [--git-dir &lt;gitDirectory&gt;] &lt;setup&gt; &lt;commit&gt;
 * </p>
 * <h4>Description</h4>
 * <p>
 * The logical merge command is used to merge logical models. Instead of merging each file one by one like git
 * would do, it uses a set of semantically interconnected files. It avoids semantical breakage of models.
 * </p>
 * </p>
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("restriction")
public class MergeCommand extends AbstractLogicalCommand {

	/**
	 * Command name.
	 */
	static final String LOGICAL_MERGE_CMD_NAME = "logicalmerge"; //$NON-NLS-1$

	/** Id of the logicalmerge application. */
	static final String LOGICAL_MERGE_APP_ID = "emf.compare.git.logicalmerge"; //$NON-NLS-1$

	/**
	 * Holds a ObjectId that need to be merged.
	 */
	@Argument(index = 1, required = true, metaVar = "<commit>", usage = "Commit ID or branch name to merge.", handler = RevCommitHandler.class)
	private RevCommit commit;

	/**
	 * Optional message used for the merge commit.
	 */
	@Option(name = "-m", metaVar = "message", required = false, usage = "Set the commit message to be used for the merge commit (in case one is created).")
	private String message;

	/**
	 * Option debug.
	 */
	@Option(name = "--debug", usage = "Launched the provisionned eclipse in debug mode.", aliases = {"-d" })
	private boolean debug;

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.git.pgm.internal.cmd.AbstractLogicalCommand#run()
	 */
	@Override
	protected Integer internalRun() throws Die {

		String setupFileAbsolutePath = this.getSetupFile().getAbsolutePath();

		String eclipsePath = getEclipsePath(setupFileAbsolutePath);

		// Can not be null since it has been set in
		// org.eclipse.emf.compare.git.pgm.internal.cmd.AbstractLogicalCommand.createSetupTaskPerformer(String,
		// URI)
		final String workspacePath = getPerformer().getWorkspaceLocation().toString();

		//@formatter:off
		LogicalApplicationLauncher launcher = new LogicalApplicationLauncher(out())
				.setApplicationName(LOGICAL_MERGE_APP_ID)
				.setEclipsePath(eclipsePath)
				.debug(debug)
				.setSetupFilePath(setupFileAbsolutePath)
				.setWorkspaceLocation(workspacePath)
				.setRepositoryPath(getRepository().getDirectory().getAbsolutePath())
				.showStackTrace(isShowStackTrace());
		//@formatter:on

		if (commit != null) {
			launcher.addAttribute(commit.name());
		} else {
			launcher.addAttribute("HEAD"); //$NON-NLS-1$
		}

		if (message != null) {
			launcher.addAttribute("-m"); //$NON-NLS-1$
			launcher.addAttribute(message);
		}

		return launcher.launch();
	}

	@Override
	protected ValidationStatus getValidationStatus() {
		// Checks we are not already in a conflict state
		if (getRepository().getRepositoryState() == RepositoryState.MERGING) {
			String errorMessage = "Exiting because of an unresolved conflict." + EOL;
			errorMessage += "error: 'merge' is not possible because you have unmerged files." + EOL;
			errorMessage += "hint: Use the logicalmergetool command to fix them up un the work tree" + EOL;
			errorMessage += "hint: and then use the 'git add/rm <file>' as" + EOL;
			errorMessage += "hint: appropriate to mark resolution" + EOL;
			return ValidationStatus.createErrorStatus(errorMessage);
		}
		return super.getValidationStatus();
	}

	// For testing purpose.
	String getMessage() {
		return message;
	}

	// For testing purpose.
	RevCommit getCommit() {
		return commit;
	}
}

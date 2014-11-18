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

import org.eclipse.emf.compare.git.pgm.internal.args.PathFilterHandler;
import org.eclipse.emf.compare.git.pgm.internal.args.RevCommitOptionHandler;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.util.LogicalApplicationLauncher;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Logical diff command. <h3>Name</h3>
 * <p>
 * logicaldiff - Git Logical Diff
 * </p>
 * <h4>Synopsis</h4>
 * <p>
 * logicaldiff &lt;setup&gt; [--show-stack-trace] [--git-dir &lt;gitDirectory&gt;] &lt;commit&gt;
 * [&lt;compareWithCommit&gt;] [ -- &lt;paths...&gt;]
 * </p>
 * <h4>Description</h4>
 * <p>
 * The logical diff is used to display differences using logical model.
 * </p>
 * </p>
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("restriction")
public class LogicalDiffCommand extends AbstractLogicalCommand {

	/**
	 * Command name.
	 */
	static final String LOGICAL_DIFF_CMD_NAME = "logicaldiff"; //$NON-NLS-1$

	/** Command name. */
	private static final String LOGICALDIFF_APP_ID = "emf.compare.git.logicaldiff"; //$NON-NLS-1$

	/**
	 * Holds the reference from which the differences should be displayed.
	 */
	@Argument(index = 1, multiValued = false, required = true, metaVar = "<commit>", usage = "Commit ID or branch name.", handler = RevCommitOptionHandler.class)
	private RevCommit commit;

	/**
	 * Optional reference used to view the differences between {@link #commit} and {@link #commitWith}.
	 */
	@Argument(index = 2, multiValued = false, required = false, metaVar = "<compareWithCommit>", usage = "Commit ID or branch name. This is to view the changes between <commit> and <compareWithCommit> or HEAD if not specified.", handler = RevCommitOptionHandler.class)
	private RevCommit commitWith;

	/**
	 * {@link TreeFilter} use to filter file on which differences should be shown.
	 */
	@Option(name = "--", metaVar = "<path...>", multiValued = false, handler = PathFilterHandler.class, usage = "This is used to limit the diff to the named paths (you can give directory names and get diff for all files under them).")
	private PathFilter treeFilter;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Integer internalRun() throws Die {

		String setupFileAbsolutePath = this.getSetupFile().getAbsolutePath();

		String eclipsePath = getEclipsePath(setupFileAbsolutePath);

		// Can not be null since it has been set in
		// AbstractLogicalCommand.createSetupTaskPerformer(String,
		// URI)
		final String workspacePath = getPerformer().getWorkspaceLocation().toString();

		//@formatter:off
		LogicalApplicationLauncher launcher = new LogicalApplicationLauncher(out())
				.setApplicationName(LOGICALDIFF_APP_ID)
				.setEclipsePath(eclipsePath)
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
		if (commitWith != null) {
			launcher.addAttribute(commitWith.name());
		} else {
			launcher.addAttribute("HEAD"); //$NON-NLS-1$
		}
		if (treeFilter != null) {
			launcher.addAttribute("--"); //$NON-NLS-1$
			launcher.addAttribute(treeFilter.getPath());
		}

		return launcher.launch();
	}

	// For testing purpose
	RevCommit getCommit() {
		return commit;
	}

	// For testing purpose
	RevCommit getOptionalCommit() {
		return commitWith;
	}

	// For testing purpose
	TreeFilter getPathFilter() {
		return treeFilter;
	}

}

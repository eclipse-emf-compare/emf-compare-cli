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

import org.eclipse.emf.compare.git.pgm.internal.args.ValidationStatus;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.util.LogicalApplicationLauncher;

/**
 * Logical merge tool command. <h3>Name</h3>
 * <p>
 * logicalmergetool - Git Logical Merge Tool
 * </p>
 * <h4>Synopsis</h4>
 * <p>
 * logicalmergetool [--show-stack-trace] &lt;setup&gt;
 * </p>
 * <h4>Description</h4>
 * <p>
 * The logical merge tool command is used to open a merge tool using logical model if the current repository
 * is in conflict state.
 * </p>
 * </p>
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("restriction")
public class LogicalMergeToolCommand extends AbstractLogicalCommand {

	/**
	 * Command name.
	 */
	static final String LOGICAL_MERGE_TOOL_CMD_NAME = "logicalmergetool"; //$NON-NLS-1$

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.git.pgm.internal.cmd.AbstractLogicalCommand#internalRun()
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
				.setEclipsePath(eclipsePath)
				.setSetupFilePath(setupFileAbsolutePath)
				.setWorkspaceLocation(workspacePath)
				.setRepositoryPath(getRepository().getDirectory().getAbsolutePath())
				.showStackTrace(isShowStackTrace());
		//@formatter:on
		return launcher.launch();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.git.pgm.internal.cmd.AbstractLogicalCommand#getValidationStatus()
	 */
	@Override
	protected ValidationStatus getValidationStatus() {
		switch (getRepository().getRepositoryState()) {
			case MERGING:
			case REBASING_INTERACTIVE:
			case REBASING:
			case REBASING_REBASING:
			case REBASING_MERGE:
				return super.getValidationStatus();
			default:
				return ValidationStatus.createErrorStatus("No conflict to merge");

		}
	}
}

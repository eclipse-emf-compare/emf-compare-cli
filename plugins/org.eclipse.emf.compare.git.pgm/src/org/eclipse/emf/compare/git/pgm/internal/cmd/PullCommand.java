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

import java.io.IOException;

import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.util.LogicalApplicationLauncher;
import org.kohsuke.args4j.Option;

/**
 * Logical pull command. <h3>Name</h3>
 * 
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 */
public class PullCommand extends AbstractLogicalCommand {

	/**
	 * Command name.
	 */
	static final String LOGICAL_PULL_CMD_NAME = "logicalpull"; //$NON-NLS-1$

	/** Id of the logicalpull application. */
	static final String LOGICAL_PULL_APP_ID = "emf.compare.git.logicalpull"; //$NON-NLS-1$

	/**
	 * Option debug.
	 */
	@Option(name = "--debug", usage = "Launches the provisionned eclipse in debug mode.", aliases = {"-d" })
	private boolean debug;

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.git.pgm.internal.cmd.AbstractLogicalCommand#internalRun()
	 */
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
				.setApplicationName(LOGICAL_PULL_APP_ID)
				.setEclipsePath(eclipsePath)
				.debug(debug)
				.setSetupFilePath(setupFileAbsolutePath)
				.setWorkspaceLocation(workspacePath)
				.setRepositoryPath(getRepository().getDirectory().getAbsolutePath())
				.showStackTrace(isShowStackTrace());
		//@formatter:on

		return launcher.launch();
	}

}

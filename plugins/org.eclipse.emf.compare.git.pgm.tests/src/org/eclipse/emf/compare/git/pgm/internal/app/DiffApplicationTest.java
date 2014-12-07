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

import static org.junit.Assert.assertEquals;

import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.app.data.ContextSetup;
import org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil;
import org.eclipse.equinox.app.IApplication;
import org.junit.Test;

/**
 * Tests the logical diff application.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public class DiffApplicationTest extends AbstractApplicationTest {

	private ContextSetup contextSetup;

	/**
	 * Test if there is no difference to display then the software display the "No differende to display"
	 * message.
	 * 
	 * @throws Exception
	 */
	@Test
	public void nothingToDo() throws Exception {
		setCmdLocation(getRepositoryPath().toString());

		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupDIFnothingToDo();
		// No reference
		getContext().addArg(getRepositoryPath().resolve(".git").toString(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "master", "master");
		Object result = getApp().start(getContext());
		assertOutputMessageEnd("No difference to display." + EMFCompareGitPGMUtil.EOL);
		assertEmptyErrorMessage();
		assertEquals(Returns.COMPLETE.code(), result);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.git.pgm.internal.app.AbstractApplicationTest#buildApp()
	 */
	@Override
	protected IApplication buildApp() {
		return new DiffApplication();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.git.pgm.internal.app.AbstractApplicationTest#getApp()
	 */
	@Override
	protected DiffApplication getApp() {
		return (DiffApplication)super.getApp();
	}
}

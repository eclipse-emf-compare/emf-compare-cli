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
package org.eclipse.emf.compare.git.pgm.oomph.wizard;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This page handles the path and name of the model created by this
 * wizard.
 * 
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 *
 */
public class SetupCreationPage extends WizardNewFileCreationPage {

	/** The relative path to workspace of the model created by this wizard. */
	private String relativePath;

	/**
	 * Default constructor.
	 * 
	 * @param pageName
	 *            the name of the page.
	 * @param selection
	 *            the current resource selection.
	 */
	public SetupCreationPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle("Setup for EMF Compare Git PGM");
		setDescription("This wizard creates a new setup model for EMF Compare Git PGM.");
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				Activator.PLUGIN_ID, "icons/emfcompare-logo-wiz.png"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validatePage()
	 */
	@Override
	protected boolean validatePage() {
		final boolean isValid;
		if (super.validatePage()) {
			String extension = new Path(getFileName()).getFileExtension();
			if (extension == null || !"setup".equals(extension)) {
				setErrorMessage("The model extension should be \".setup\"");
				isValid = false;
			} else {
				computeRelativePathToWorkspace();
				isValid = true;
			}
		} else {
			isValid = false;
		}
		return isValid;
	}

	/**
	 * Get the relative path to workspace of the model created by this wizard.
	 * 
	 * @return the relative path to workspace of the model created by this
	 *         wizard.
	 */
	public String getRelativePathToWorkspace() {
		return relativePath;
	}

	/**
	 * Computes the relative path to workspace of the model created by this
	 * wizard.
	 */
	private void computeRelativePathToWorkspace() {
		IPath containerPath = getContainerFullPath();
		String fileName = getFileName();
		relativePath = containerPath.toOSString() + File.separator + fileName;
	}
}

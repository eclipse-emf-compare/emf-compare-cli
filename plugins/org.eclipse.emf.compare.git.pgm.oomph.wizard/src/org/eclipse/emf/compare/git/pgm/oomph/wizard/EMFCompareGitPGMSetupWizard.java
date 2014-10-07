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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.oomph.resources.ResourcesFactory;
import org.eclipse.oomph.resources.SourceLocator;
import org.eclipse.oomph.setup.Project;
import org.eclipse.oomph.setup.SetupFactory;
import org.eclipse.oomph.setup.SetupPackage;
import org.eclipse.oomph.setup.VariableTask;
import org.eclipse.oomph.setup.projects.ProjectsFactory;
import org.eclipse.oomph.setup.projects.ProjectsImportTask;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * This wizards helps to create a setup model to use with EMF Compare Git PGM.
 * 
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 *
 */
public class EMFCompareGitPGMSetupWizard extends Wizard implements INewWizard {

	/**
	 * Wizard Page that handles the creation of the model.
	 */
	private SetupCreationPage setupCreationPage;

	/**
	 * Wizard Page that handles the content of the model.
	 */
	private SetupContentPage setupContentPage;

	/**
	 * The selection of the container of the future setup model.
	 */
	private IStructuredSelection select;

	/**
	 * Default constructor.
	 */
	public EMFCompareGitPGMSetupWizard() {
		setNeedsProgressMonitor(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		setupCreationPage = new SetupCreationPage(
				"EMFCompareGitPGMSetupWizard", select);
		addPage(setupCreationPage);
		setupContentPage = new SetupContentPage();
		addPage(setupContentPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		return super.canFinish();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		boolean finish = true;
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					doFinish();
				} catch (IOException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			finish  = false;
		} catch (InvocationTargetException e) {
			Activator.getDefault().log(e);
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error",
					realException.getMessage());
			finish = false;
		}

		return finish;
	}

	/**
	 * Initializes this creation wizard using the passed workbench and object
	 * selection.
	 * 
	 * @param workbench
	 *            the current workbench.
	 * @param selection
	 *            the current object selection.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.select = selection;
	}

	/**
	 * Perform the creation of model.
	 * 
	 * @throws IOException e
	 */
	private void doFinish() throws IOException {

		// Creates the URI of the resource
		String fullPath = setupCreationPage.getRelativePathToWorkspace();
		URI uri = URI.createPlatformResourceURI(fullPath, true);

		// Create a resource set to hold the resources.
		ResourceSet resourceSet = new ResourceSetImpl();
		// Register the appropriate resource factory to handle all file
		// extensions.
		resourceSet
				.getResourceFactoryRegistry()
				.getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION,
						new XMIResourceFactoryImpl());

		// Register the package to ensure it is available during loading.
		resourceSet.getPackageRegistry().put(SetupPackage.eNS_URI,
				SetupPackage.eINSTANCE);

		// Creates resource
		Resource resource = resourceSet.createResource(uri);

		// Creates model's content
		Project project = SetupFactory.eINSTANCE.createProject();
		project.setName(setupContentPage.getRootObjectName());
		project.setLabel(setupContentPage.getRootObjectName());

		resource.getContents().add(project);

		if (!setupContentPage.useDefaultWorkspacePath()) {
			VariableTask wsVariable = SetupFactory.eINSTANCE
					.createVariableTask();
			wsVariable.setName("workspace.location");
			wsVariable.setValue(setupContentPage.getWorkspacePath());
			project.getSetupTasks().add(wsVariable);
		}

		if (!setupContentPage.useDefaultInstallationPath()) {
			VariableTask installVariable = SetupFactory.eINSTANCE
					.createVariableTask();
			installVariable.setName("installation.location");
			installVariable.setValue(setupContentPage.getInstallationPath());
			project.getSetupTasks().add(installVariable);
		}

		if (!setupContentPage.importAll()) {
			ProjectsImportTask importTask = ProjectsFactory.eINSTANCE
					.createProjectsImportTask();
			SourceLocator sourceLocator = ResourcesFactory.eINSTANCE
					.createSourceLocator();
			importTask.getSourceLocators().add(sourceLocator);
			project.getSetupTasks().add(importTask);
		}

		resource.save(null);
	}
}

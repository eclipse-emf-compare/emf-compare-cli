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
package org.eclipse.emf.compare.git.pgm.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.oomph.p2.P2Factory;
import org.eclipse.oomph.resources.ResourcesFactory;
import org.eclipse.oomph.resources.SourceLocator;
import org.eclipse.oomph.setup.Project;
import org.eclipse.oomph.setup.SetupFactory;
import org.eclipse.oomph.setup.VariableTask;
import org.eclipse.oomph.setup.p2.P2Task;
import org.eclipse.oomph.setup.p2.SetupP2Factory;
import org.eclipse.oomph.setup.projects.ProjectsFactory;
import org.eclipse.oomph.setup.projects.ProjectsImportTask;

/**
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
public class OomphUserModelBuilder {

	private String installationLocation;

	private String workspaceLocation;

	private String[] requirements;

	private String[] projectPaths;

	private String[] repositories;

	public OomphUserModelBuilder setInstallationLocation(String installationTaskLocation) {
		this.installationLocation = installationTaskLocation;
		return this;
	}

	public OomphUserModelBuilder setWorkspaceLocation(String workspaceLocation) {
		this.workspaceLocation = workspaceLocation;
		return this;
	}

	public OomphUserModelBuilder setRequirements(String... requirements) {
		this.requirements = requirements;
		return this;
	}

	public OomphUserModelBuilder setProjectPaths(String... projectPaths) {
		this.projectPaths = projectPaths;
		return this;
	}

	public OomphUserModelBuilder setRepositories(String... repositories) {
		this.repositories = repositories;
		return this;
	}

	private String[] getRequirements() {
		if (requirements == null) {
			requirements = new String[] {};
		}
		return requirements;
	}

	private String[] getProjectPaths() {
		if (projectPaths == null) {
			projectPaths = new String[] {};
		}
		return projectPaths;
	}

	private String[] getRepositories() {
		if (repositories == null) {
			repositories = new String[] {};
		}
		return repositories;
	}

	public File saveTo(String setupFilePath) throws IOException {
		Resource newResource = new XMIResourceImpl();
		Project project = SetupFactory.eINSTANCE.createProject();
		newResource.getContents().add(project);

		if (installationLocation != null) {
			VariableTask installationVariableTask = SetupFactory.eINSTANCE.createVariableTask();
			installationVariableTask.setName("installation.location"); //$NON-NLS-1$
			installationVariableTask.setValue(installationLocation);
		}

		if (workspaceLocation != null) {
			VariableTask workspaceVariableTask = SetupFactory.eINSTANCE.createVariableTask();
			workspaceVariableTask.setName("workspace.location"); //$NON-NLS-1$
			workspaceVariableTask.setValue(workspaceLocation);
		}

		Stream.of(getProjectPaths()).distinct().forEach(projectPath -> {
			ProjectsImportTask importTask = ProjectsFactory.eINSTANCE.createProjectsImportTask();
			project.getSetupTasks().add(importTask);
			SourceLocator sourceLocator = ResourcesFactory.eINSTANCE.createSourceLocator();
			importTask.getSourceLocators().add(sourceLocator);
			sourceLocator.setRootFolder(projectPath);
		});

		if (getRepositories().length > 0 || getRequirements().length > 0) {
			P2Task p2Task = SetupP2Factory.eINSTANCE.createP2Task();
			project.getSetupTasks().add(p2Task);
			Stream.of(getRequirements()).distinct().forEach(
					req -> p2Task.getRequirements().add(P2Factory.eINSTANCE.createRequirement(req)));
			Stream.of(getRepositories()).distinct().forEach(
					repo -> p2Task.getRepositories().add(P2Factory.eINSTANCE.createRepository(repo)));
		}

		File setupFile = new File(setupFilePath);
		FileOutputStream fileOutputStram = new FileOutputStream(setupFile);
		try {
			newResource.save(fileOutputStram, null);
		} finally {
			fileOutputStram.close();
		}
		return setupFile;
	}
}

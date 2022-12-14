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
package org.eclipse.emf.compare.git.pgm.internal.app;

import static org.eclipse.emf.compare.git.pgm.internal.Options.SHOW_STACK_TRACE_OPT;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EMPTY_STRING;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.SEP;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.toFileWithAbsolutePath;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.waitEgitJobs;

import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.ProgressPageLog;
import org.eclipse.emf.compare.git.pgm.internal.args.CmdLineParserRepositoryBuilder;
import org.eclipse.emf.compare.git.pgm.internal.args.GitDirHandler;
import org.eclipse.emf.compare.git.pgm.internal.args.SetupFileHandler;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DeathType;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DiesOn;
import org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil;
import org.eclipse.emf.compare.ide.ui.internal.logical.EMFModelProvider;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.oomph.base.provider.BaseEditUtil;
import org.eclipse.oomph.internal.setup.SetupPrompter;
import org.eclipse.oomph.resources.ResourcesFactory;
import org.eclipse.oomph.resources.SourceLocator;
import org.eclipse.oomph.setup.Project;
import org.eclipse.oomph.setup.SetupPackage;
import org.eclipse.oomph.setup.SetupTask;
import org.eclipse.oomph.setup.Trigger;
import org.eclipse.oomph.setup.internal.core.SetupContext;
import org.eclipse.oomph.setup.internal.core.SetupTaskPerformer;
import org.eclipse.oomph.setup.internal.core.util.ECFURIHandlerImpl;
import org.eclipse.oomph.setup.internal.core.util.SetupCoreUtil;
import org.eclipse.oomph.setup.projects.ProjectsFactory;
import org.eclipse.oomph.setup.projects.ProjectsImportTask;
import org.eclipse.oomph.util.Confirmer;
import org.eclipse.oomph.util.IOUtil;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

/**
 * Abstract class for any logical application.
 * 
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 */
@SuppressWarnings("restriction")
public abstract class AbstractLogicalApplication implements IApplication {

	/**
	 * Logs any message from oomph.
	 */
	protected ProgressPageLog progressPageLog;

	/**
	 * Git repository for this command to be executed in.
	 */
	protected Repository repo;

	/**
	 * Holds git directory location.
	 */
	@Argument(index = 0, metaVar = "gitFolderPath", usage = "Path to the .git folder of your repository.", handler = GitDirHandler.class)
	protected String gitdir;

	/**
	 * Holds the Oomph model setup file.
	 */
	@Argument(index = 1, metaVar = "<setup>", required = true, usage = "Path to the setup file. The setup file is a Oomph model.", handler = SetupFileHandler.class)
	protected File setupFile;

	/**
	 * Holds true if the java stack trace should be displayed in the console if any.
	 */
	@Option(name = SHOW_STACK_TRACE_OPT, usage = "Use this option to display java stack trace in console on error.")
	private boolean showStackTrace;

	/**
	 * Instance of {@link Git} from {@link #repo}.
	 */
	private Git git;

	/**
	 * {@inheritDoc}.
	 */
	@Override
	public Object start(IApplicationContext context) throws Exception {
		Integer code;
		// Prevents VM args if the application exits on something different that 0
		System.setProperty(IApplicationContext.EXIT_DATA_PROPERTY, EMPTY_STRING);
		final Map<?, ?> args = context.getArguments();
		final String[] appArgs = (String[])args.get("application.args"); //$NON-NLS-1$

		// This time it creates the repository using EGit code in order to add the repository to the EGit
		// cache
		final CmdLineParserRepositoryBuilder clp = CmdLineParserRepositoryBuilder
				.newEGitRepoBuilderCmdParser(this);
		try {
			clp.parseArgument(appArgs);
			repo = clp.getRepo();
			git = new Git(repo);
		} catch (CmdLineException err) {
			if (showStackTrace) {
				err.printStackTrace();
			}
			System.err.println(err.getMessage());
			dispose();
			return Returns.ERROR;
		}
		// CHECKSTYLE.OFF: IllegalCatch - We want to hide the strack trace if the showStackTrace option does
		// not holds true.
		try {
			performStartup();
			code = performGitCommand();
		} catch (Die e) {
			code = EMFCompareGitPGMUtil.handleDieError(e, showStackTrace);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			if (showStackTrace) {
				e.printStackTrace();
			}
			code = Returns.ERROR.code();
		} finally {
			dispose();
		}
		// CHECKSTYLE.ON: IllegalCatch

		return code;
	}

	/**
	 * {@inheritDoc}.
	 */
	public void stop() {
		// Nothing to do.
	}

	/**
	 * Performs the logical git command (diff or merge).
	 * 
	 * @return a {@link org.eclipse.emf.compare.git.pgm.Returns}.
	 * @throws Die
	 *             e
	 */
	protected abstract Integer performGitCommand() throws Die;

	/**
	 * Creates and configure the setup task performer to execute the imports of projects referenced in the
	 * user setup model. Then call the {@link #performGitCommand()}.
	 * 
	 * @throws Die
	 *             e
	 */
	protected void performStartup() throws Die {
		ComposedAdapterFactory adapterFactory = new ComposedAdapterFactory(BaseEditUtil
				.createAdapterFactory());

		ResourceSet rs = SetupCoreUtil.createResourceSet();
		rs.eAdapters().add(
				new AdapterFactoryEditingDomain.EditingDomainProvider(new AdapterFactoryEditingDomain(
						adapterFactory, null, rs)));
		rs.getLoadOptions().put(ECFURIHandlerImpl.OPTION_CACHE_HANDLING,
				ECFURIHandlerImpl.CacheHandling.CACHE_WITHOUT_ETAG_CHECKING);

		URI startupSetupURI = URI.createFileURI(setupFile.getAbsolutePath());
		Resource startupSetupResource = rs.getResource(startupSetupURI, true);

		Project startupSetupProject = (Project)EcoreUtil.getObjectByType(startupSetupResource.getContents(),
				SetupPackage.Literals.PROJECT);

		SetupContext setupContext = SetupContext.create(rs);
		// CHECKSTYLE.OFF: IllegalCatch - No choice since Oomph launch such an exception. We want to handle
		// exception ourself
		try {
			Trigger triggerStartup = Trigger.STARTUP;
			URIConverter uriConverter = rs.getURIConverter();
			SetupTaskPerformer performerStartup = SetupTaskPerformer.create(uriConverter,
					SetupPrompter.CANCEL, triggerStartup, setupContext, false);
			Confirmer confirmer = Confirmer.ACCEPT;
			performerStartup.put(ILicense.class, confirmer);
			performerStartup.put(Certificate.class, confirmer);

			progressPageLog = new ProgressPageLog(System.out);
			performerStartup.setProgress(progressPageLog);

			cleanWorkspace();

			handleImportProjects(startupSetupProject, performerStartup);

			performerStartup.perform(new NullProgressMonitor());

			validatePerform(performerStartup);

			waitEgitJobs();
		} catch (Die e) {
			throw e;
		} catch (Exception e) {
			final String message;
			if (e instanceof CoreException) {
				message = EMFCompareGitPGMUtil.getStatusMessage(((CoreException)e).getStatus());
			} else {
				message = "Error during Oomph operation";
			}
			throw new DiesOn(DeathType.FATAL).duedTo(e).displaying(message).ready();
		}
		// CHECKSTYLE.ON: IllegalCatch
	}

	/**
	 * Returns a {@link Git} from the current {@link Repository}.
	 * 
	 * @return a {@link Git} from the current {@link Repository}.
	 */
	protected Git getGit() {
		return git;
	}

	/**
	 * Close the repository and the log.
	 */
	protected void dispose() {
		if (git != null) {
			git.close();
		}
		if (repo != null) {
			repo.close();
		}
		if (progressPageLog != null) {
			progressPageLog.setTerminating();
		}
	}

	/**
	 * Check if the file to test is EMFCompare compliant.
	 * 
	 * @see org.eclipse.egit.ui.internal.CompareUtils#canDirectlyOpenInCompare(IFile)
	 * @param mergeContext
	 *            a resource mapping context.
	 * @param file
	 *            the file to test.
	 * @return true if the file to test is EMFCompare compliant, false otherwise.
	 */
	protected boolean isEMFCompareCompliantFile(RemoteResourceMappingContext mergeContext, IFile file) {
		try {
			EMFModelProvider modelProvider = new EMFModelProvider();
			ResourceMapping[] modelMappings = modelProvider.getMappings(file, mergeContext,
					new NullProgressMonitor());
			if (modelMappings.length > 0) {
				return true;
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Gets the tree iterator of the id located in the repository.
	 * 
	 * @param repository
	 *            the repository containing the id.
	 * @param id
	 *            the id for which we want the tree iterator.
	 * @return the tree iterator of the id located in the repository.
	 * @throws IOException
	 *             e
	 */
	protected AbstractTreeIterator getTreeIterator(Repository repository, ObjectId id) throws IOException {
		final CanonicalTreeParser p = new CanonicalTreeParser();
		try (ObjectReader or = repository.newObjectReader()) {
			p.reset(or, new RevWalk(repository).parseTree(id));
			return p;
		}
	}

	/**
	 * Returns <code>true</code> if the user has required to display error stack trace in the console.
	 * 
	 * @return <code>true</code> if the user has required to display error stack trace in the console,
	 *         <code>false</code> otherwise.
	 */
	protected boolean isShowStackTrace() {
		return showStackTrace;
	}

	/**
	 * Simulate a comparison between the two given references and returns back the subscriber that can provide
	 * all computed synchronization information.
	 * 
	 * @param repository
	 *            the current repository.
	 * @param sourceRef
	 *            Source reference (i.e. "left" side of the comparison).
	 * @param targetRef
	 *            Target reference (i.e. "right" side of the comparison).
	 * @param comparedFile
	 *            The file we are comparing (that would be the file right-clicked into the workspace).
	 * @return The created subscriber.
	 * @throws IOException
	 *             e
	 */
	protected RemoteResourceMappingContext createSubscriberForComparison(Repository repository,
			ObjectId sourceRef, ObjectId targetRef, IFile comparedFile) throws IOException {
		final GitSynchronizeData data = new GitSynchronizeData(repository, sourceRef.getName(), targetRef
				.getName(), false);
		final GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(dataSet);
		subscriber.init(new NullProgressMonitor());
		return new GitSubscriberResourceMappingContext(subscriber, dataSet);
	}

	/**
	 * This will query all model providers for those that are enabled on the given file and list all mappings
	 * available for that file.
	 * 
	 * @param mergeContext
	 *            a resource mapping context.
	 * @param file
	 *            The file for which we need the associated resource mappings.
	 * @return All mappings available for that file.
	 */
	protected ResourceMapping[] getResourceMappings(RemoteResourceMappingContext mergeContext, IFile file) {
		final Set<ResourceMapping> mappings = new LinkedHashSet<ResourceMapping>();
		try {
			EMFModelProvider modelProvider = new EMFModelProvider();
			ResourceMapping[] modelMappings = modelProvider.getMappings(file, mergeContext,
					new NullProgressMonitor());
			for (ResourceMapping mapping : modelMappings) {
				mappings.add(mapping);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return mappings.toArray(new ResourceMapping[mappings.size()]);
	}

	/**
	 * Create copy of the projects import task with all root folders of source locators convert in absolute
	 * paths.
	 * 
	 * @param task
	 *            the {@link ProjectsImportTask} to copy.
	 * @param resourceBasePath
	 *            the resourceBasePath needed to compute absolute paths.
	 * @return copy of the projects import task with all root folders of source locators convert in absolute
	 *         paths.
	 */
	private ProjectsImportTask createCopyWithAbsolutePath(ProjectsImportTask task,
			final String resourceBasePath) {
		ProjectsImportTask importTask = EcoreUtil.copy(task);
		EList<SourceLocator> sourceLocators = importTask.getSourceLocators();
		for (SourceLocator sourceLocator : sourceLocators) {
			String projectAbsolutePath = toFileWithAbsolutePath(resourceBasePath,
					sourceLocator.getRootFolder()).toString();
			sourceLocator.setRootFolder(projectAbsolutePath);
		}
		return importTask;
	}

	/**
	 * Validate that the perform operation has been successfully executed for the given
	 * {@link SetupTaskPerformer}.
	 * 
	 * @param performerStartup
	 *            the given {@link SetupTaskPerformer}.
	 * @throws Die
	 *             e
	 */
	private void validatePerform(SetupTaskPerformer performerStartup) throws Die {
		if (!performerStartup.hasSuccessfullyPerformed()) {
			throw new DiesOn(DeathType.FATAL).displaying("Error during Oomph operation: Failure").ready();
		}

		if (performerStartup.isCanceled()) {
			throw new DiesOn(DeathType.FATAL).displaying("Error during Oomph operation: Canceled").ready();
		}

		if (performerStartup.isRestartNeeded()) {
			throw new DiesOn(DeathType.FATAL).displaying("Error during Oomph operation: Need restart")
					.ready();
		}
	}

	/**
	 * Clean the workspace.
	 * 
	 * @throws Die
	 *             e
	 */
	private void cleanWorkspace() throws Die {
		final IWorkspace workspace = org.eclipse.core.resources.ResourcesPlugin.getWorkspace();
		try {
			workspace.run(new WorkspaceCleaner(workspace), null);
		} catch (CoreException e) {
			throw new DiesOn(DeathType.FATAL).duedTo(e).ready();
		}
	}

	/**
	 * Handle ProjectsImport tasks.
	 * 
	 * @param startupSetupProject
	 *            the root of the setup model.
	 * @param performerStartup
	 *            the SetupTaskPerformer.
	 */
	private void handleImportProjects(Project startupSetupProject, SetupTaskPerformer performerStartup) {
		List<ProjectsImportTask> projectToImport = new ArrayList<ProjectsImportTask>();

		// Import Projects & execute other startup tasks.
		final String resourcePath = startupSetupProject.eResource().getURI().toFileString();
		final String resourceBasePath = resourcePath.substring(0, resourcePath.lastIndexOf(SEP));
		for (SetupTask setupTask : startupSetupProject.getSetupTasks()) {
			if (setupTask instanceof ProjectsImportTask) {
				// Convert locations of projects to absolute paths.
				ProjectsImportTask importTask = createCopyWithAbsolutePath((ProjectsImportTask)setupTask,
						resourceBasePath);
				performerStartup.getTriggeredSetupTasks().add(importTask);
				projectToImport.add(importTask);
			} else {
				performerStartup.getTriggeredSetupTasks().add(setupTask);
			}
		}

		// If no ProjectsImportTask found, import all projects in repo
		if (projectToImport.isEmpty()) {
			ProjectsImportTask importTask = ProjectsFactory.eINSTANCE.createProjectsImportTask();
			SourceLocator sourceLocator = ResourcesFactory.eINSTANCE.createSourceLocator(repo.getWorkTree()
					.getAbsolutePath(), false);
			importTask.getSourceLocators().add(sourceLocator);
			performerStartup.getTriggeredSetupTasks().add(importTask);
		}
	}

	/**
	 * An workspace action that removes all projects from the workspace. It also deletes
	 * ".plugins/org.eclipse.oomph.setup.projects/import-history.properties"file since it contains references
	 * of projects imported by Oomph.
	 *
	 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
	 */
	private final class WorkspaceCleaner implements IWorkspaceRunnable {
		/** Workspace to clean. */
		private final IWorkspace workspace;

		/**
		 * Constructor.
		 *
		 * @param workspace
		 *            Workspace to clean
		 */
		private WorkspaceCleaner(IWorkspace workspace) {
			this.workspace = workspace;
		}

		@Override
		public void run(IProgressMonitor monitor) throws CoreException {
			IWorkspaceRoot root = workspace.getRoot();
			for (IProject project : root.getProjects()) {
				project.delete(false, true, monitor);
			}
			for (File file : root.getLocation().toFile().listFiles()) {
				if (file.isDirectory()) {
					if (".metadata".equals(file.getName())) { //$NON-NLS-1$
						// Deletes the Oomph import-history.properties to force new import
						File importHistory = new File(file.getAbsolutePath() + SEP
								+ ".plugins/org.eclipse.oomph.setup.projects/import-history.properties"); //$NON-NLS-1$
						if (importHistory.exists()) {
							IOUtil.deleteBestEffort(importHistory);
							try {
								importHistory.createNewFile();
							} catch (IOException e) {
								throw new CoreException(
										new Status(IStatus.ERROR, "org.eclipse.emf.compare.git.pgm",
												"Unable to delete the file .plugins/org.eclipse.oomph.setup.projects/import-history.properties"));
							}
						}
					}
				}
			}
		}
	}
}

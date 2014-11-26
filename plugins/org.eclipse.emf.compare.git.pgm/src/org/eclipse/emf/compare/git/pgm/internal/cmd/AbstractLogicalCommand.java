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

import static org.eclipse.emf.compare.git.pgm.internal.Options.GIT_DIR_OPT;
import static org.eclipse.emf.compare.git.pgm.internal.Options.HELP_OPT;
import static org.eclipse.emf.compare.git.pgm.internal.Options.SHOW_STACK_TRACE_OPT;
import static org.eclipse.emf.compare.git.pgm.internal.exception.Die.DeathType.FATAL;
import static org.eclipse.emf.compare.git.pgm.internal.exception.Die.DeathType.SOFTWARE_ERROR;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.SEP;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.toFileWithAbsolutePath;

import com.google.common.base.Preconditions;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Collection;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.ProgressPageLog;
import org.eclipse.emf.compare.git.pgm.internal.args.CmdLineParserRepositoryBuilder;
import org.eclipse.emf.compare.git.pgm.internal.args.GitDirHandler;
import org.eclipse.emf.compare.git.pgm.internal.args.SetupFileOptionHandler;
import org.eclipse.emf.compare.git.pgm.internal.args.ValidationStatus;
import org.eclipse.emf.compare.git.pgm.internal.exception.ArgumentValidationError;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DeathType;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DiesOn;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.io.ThrowingPrintWriter;
import org.eclipse.oomph.base.provider.BaseEditUtil;
import org.eclipse.oomph.internal.setup.SetupPrompter;
import org.eclipse.oomph.setup.Index;
import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.InstallationTask;
import org.eclipse.oomph.setup.Product;
import org.eclipse.oomph.setup.ProductCatalog;
import org.eclipse.oomph.setup.ProductVersion;
import org.eclipse.oomph.setup.Project;
import org.eclipse.oomph.setup.SetupFactory;
import org.eclipse.oomph.setup.SetupPackage;
import org.eclipse.oomph.setup.SetupTask;
import org.eclipse.oomph.setup.Trigger;
import org.eclipse.oomph.setup.User;
import org.eclipse.oomph.setup.VariableTask;
import org.eclipse.oomph.setup.Workspace;
import org.eclipse.oomph.setup.WorkspaceTask;
import org.eclipse.oomph.setup.internal.core.SetupContext;
import org.eclipse.oomph.setup.internal.core.SetupTaskPerformer;
import org.eclipse.oomph.setup.internal.core.util.ECFURIHandlerImpl;
import org.eclipse.oomph.setup.internal.core.util.SetupUtil;
import org.eclipse.oomph.setup.log.ProgressLog;
import org.eclipse.oomph.setup.p2.P2Task;
import org.eclipse.oomph.setup.util.OS;
import org.eclipse.oomph.util.Confirmer;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

/**
 * Abstract class for any logical command.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("restriction")
public abstract class AbstractLogicalCommand {

	/** Buffer size of the array use to generate an unique id. */
	private static final int GEN_ID_BUFFER_SIZE = 1024;

	/** Eclipse string. */
	private static final String ECLIPSE = "eclipse"; //$NON-NLS-1$

	/**
	 * Holds true if a user has set the help option to true.
	 */
	@Option(name = HELP_OPT, usage = "Dispays help for this command.", aliases = {"-h" })
	private boolean help;

	/**
	 * Holds the Oomph model setup file.
	 */
	@Argument(index = 0, metaVar = "<setup>", required = true, usage = "Path to the setup file. The setup file is a Oomph model.", handler = SetupFileOptionHandler.class)
	private File setupFile;

	/**
	 * Holds true if the java stack trace should be displayed in the console if any.
	 */
	@Option(name = SHOW_STACK_TRACE_OPT, usage = "Use this option to display java stack trace in console on error.")
	private boolean showStackTrace;

	/**
	 * Holds git directory location.
	 */
	@Option(name = GIT_DIR_OPT, metaVar = "gitFolderPath", usage = "Path to the .git folder of your repository.", handler = GitDirHandler.class)
	private String gitdir;

	/**
	 * Name of this command.
	 */
	private String commandName;

	/**
	 * Writer to output to, typically this is standard output.
	 */
	private ThrowingPrintWriter out;

	/**
	 * Usage of this command. This field is filled only if the help parameter has been provided.
	 */
	private String usage;

	/**
	 * Git repository for this command to be executed in.
	 */
	private Repository repo;

	/**
	 * SetupTaskPerformer of this command.
	 */
	private SetupTaskPerformer performer;

	/**
	 * Log.
	 */
	private ProgressLog progressPageLog;

	/**
	 * Holds a reference to the {@link CmdLineParserRepositoryBuilder} used to build this command to be able
	 * to print the usage any time necessary.
	 * 
	 * @see org.kohsuke.args4j.CmdLineParser#printUsage(java.io.OutputStream)
	 */
	private CmdLineParserRepositoryBuilder cmdLineParser;

	/**
	 * Constructor.
	 */
	protected AbstractLogicalCommand() {
		// Force the command to be built in the CommandFactory.
	}

	/**
	 * Executes the command.
	 * 
	 * @return The {@link Returns} for this command.
	 * @throws Die
	 *             if the program stop prematurely.
	 * @throws IOException
	 *             propagation.
	 */
	public final Integer execute() throws Die, IOException {
		final Integer result;
		if (!help) {
			OS os = getPerformer().getOS();
			if (!os.isCurrent()) {
				result = Returns.ERROR.code();
			} else {
				result = internalRun();
			}

		} else {
			out.print(usage);
			result = Returns.COMPLETE.code();
		}
		return result;
	}

	/**
	 * Builds this command.
	 * 
	 * @param args
	 *            The arguments for this command.
	 * @param environmentSetupURI
	 *            URI to the environment setup file.
	 * @throws Die
	 *             exception on error.
	 * @throws IOException
	 *             e
	 */
	public void build(Collection<String> args, URI environmentSetupURI) throws Die, IOException {

		repo = parseArgumentsAndBuildRepo(args);
		if (!help) {
			ValidationStatus validationStatus = getValidationStatus();
			if (!validationStatus.isValid()) {
				if (validationStatus.isPrintUsage()) {
					ByteArrayOutputStream localOut = new ByteArrayOutputStream();
					PrintWriter printWritter = new PrintWriter(localOut);
					printUsage(validationStatus.getMessage(), printWritter);
					printWritter.close();
					throw new DiesOn(FATAL).displaying(localOut.toString()).ready();
				} else {
					throw new DiesOn(FATAL).displaying(validationStatus.getMessage()).ready();
				}
			}
		}

		try {
			final String outputEncoding;
			if (repo != null) {
				outputEncoding = repo.getConfig().getString("i18n", null, "logOutputEncoding"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				outputEncoding = null;
			}
			BufferedWriter outbufw;
			if (outputEncoding != null) {
				outbufw = new BufferedWriter(new OutputStreamWriter(System.out, outputEncoding));
			} else {
				outbufw = new BufferedWriter(new OutputStreamWriter(System.out));
			}
			out = new ThrowingPrintWriter(outbufw);
		} catch (IOException e) {
			throw new DiesOn(SOFTWARE_ERROR).displaying("Cannot create input stream").ready();
		}

		if (!help) {
			// CHECKSTYLE.OFF: IllegalCatch - No choice since Oomph launch such an exception
			try {
				// Loads eclipse environment setup model.
				performer = createSetupTaskPerformer(setupFile.getAbsolutePath(), environmentSetupURI);
				performer.perform();

				if (!performer.hasSuccessfullyPerformed()) {
					throw new DiesOn(DeathType.FATAL).displaying("Error during Oomph operation").ready();
				}
			} catch (Die e) {
				throw e;
			} catch (Exception e) {
				throw new DiesOn(DeathType.FATAL).duedTo(e).ready();
			}
			// CHECKSTYLE.ON: IllegalCatch
		}

	}

	/**
	 * Returns the value of the show-stack-trace argument.
	 * 
	 * @return the value of the show-stack-trace argument.
	 */
	public boolean isShowStackTrace() {
		return showStackTrace;
	}

	/**
	 * Returns the user setup file associated with this command.
	 * 
	 * @return the user setup file associated with this command.
	 */
	public File getSetupFile() {
		return setupFile;
	}

	/**
	 * Flush the out stream. This is mainly use to handle premature exit.
	 * 
	 * @throws IOException
	 *             if any problem with the stream.
	 */
	public void flushOutW() throws IOException {
		if (out != null) {
			out.flush();
		}
	}

	/**
	 * Runs the command.
	 * 
	 * @return Return code.
	 * @throws Die
	 *             e
	 * @throws IOException
	 *             exception on error.
	 */
	protected abstract Integer internalRun() throws Die, IOException;

	/**
	 * Gets the git repository.
	 * 
	 * @return the repository this command uses.
	 */
	protected Repository getRepository() {
		return repo;
	}

	/**
	 * Gets the SetupTaskPerformer.
	 * 
	 * @return the SetupTaskPerformer.
	 */
	protected SetupTaskPerformer getPerformer() {
		return performer;
	}

	/**
	 * Parses the arguments related to this command. It also in charge of building the git repository.
	 * <p>
	 * Since the --git-dir option can be passed through the command line, the parser is also in charge of
	 * building the repository
	 * </p>
	 * 
	 * @param args
	 *            arguments.
	 * @return the Repository.
	 * @throws Die
	 *             if the program exits prematurely.
	 */
	protected Repository parseArgumentsAndBuildRepo(Collection<String> args) throws Die {

		cmdLineParser = CmdLineParserRepositoryBuilder.newJGitRepoBuilderCmdParser(this);
		try {
			cmdLineParser.parseArgument(args);
		} catch (ArgumentValidationError err) {
			// Only throw an error if the user has not required help.
			if (!help) {
				if (err.getCause() instanceof Die) {
					// Do not wrap a Die exception
					throw (Die)err.getCause();
				} else {
					throw new DiesOn(FATAL).displaying(err.getMessage()).ready();
				}
			}
		} catch (CmdLineException err) {
			// Only throw an error if the user has not required help.
			if (!help) {
				ByteArrayOutputStream localOut = new ByteArrayOutputStream();
				PrintWriter printWritter = new PrintWriter(localOut);
				printUsage(err.getMessage(), printWritter);
				printWritter.close();
				throw new DiesOn(FATAL).displaying(localOut.toString()).ready();
			}
		}

		if (help) {
			// The user has used the help option. Saves the usage message for later
			ByteArrayOutputStream localOut = new ByteArrayOutputStream();
			PrintWriter printWritter = new PrintWriter(localOut);
			printUsage(null, printWritter);
			printWritter.close();
			usage = localOut.toString();
		}
		return cmdLineParser.getRepo();
	}

	/**
	 * <p>
	 * Inherited class may override this method to validate their arguements.
	 * </p>
	 * 
	 * @return {@link ValidationStatus}
	 */
	protected ValidationStatus getValidationStatus() {
		return ValidationStatus.OK_STATUS;
	}

	/**
	 * Prints the usage of this command.
	 * <p>
	 * The {@link #cmdLineParser} field should have been set
	 * </p>
	 * 
	 * @param message
	 *            a prefix message.
	 * @param printWritter
	 *            A {@link PrintWriter}.
	 */
	protected void printUsage(final String message, PrintWriter printWritter) {
		Preconditions.checkNotNull(cmdLineParser);
		if (message != null) {
			printWritter.println(message + " in:");
		}
		printWritter.print(commandName);
		cmdLineParser.printSingleLineUsage(printWritter, null);
		printWritter.println();
		printWritter.println();

		cmdLineParser.printUsage(printWritter, null);
		printWritter.println();

		printWritter.flush();
	}

	/**
	 * Returns the printer to write message in console.
	 * 
	 * @return {@link ThrowingPrintWriter}
	 */
	protected ThrowingPrintWriter out() {
		return out;
	}

	/**
	 * Sets the command name.
	 * 
	 * @param name
	 *            the command name.
	 */
	final void setCommandName(final String name) {
		commandName = name;
	}

	/**
	 * Create and configure the setup task performer to provision the eclipse environment.
	 * 
	 * @param userSetupFilePath
	 *            the path of the user setup model.
	 * @param environmentSetupURI
	 *            URI of the setup file that contains the environment used to execute the logical commands.
	 * @return a SetupTaskPerformer.
	 * @throws Die
	 *             e
	 * @throws IOException
	 *             e
	 */
	private SetupTaskPerformer createSetupTaskPerformer(String userSetupFilePath, URI environmentSetupURI)
			throws IOException, Die {
		// Load user setup model.
		ComposedAdapterFactory adapterFactory = new ComposedAdapterFactory(BaseEditUtil
				.createAdapterFactory());

		ResourceSet rs = SetupUtil.createResourceSet();
		rs.eAdapters().add(
				new AdapterFactoryEditingDomain.EditingDomainProvider(new AdapterFactoryEditingDomain(
						adapterFactory, null, rs)));
		rs.getLoadOptions().put(ECFURIHandlerImpl.OPTION_CACHE_HANDLING,
				ECFURIHandlerImpl.CacheHandling.CACHE_WITHOUT_ETAG_CHECKING);

		URI startupSetupURI = URI.createFileURI(userSetupFilePath);
		Resource startupSetup = null;
		// CHECKSTYLE.OFF: IllegalCatch - No choice since EMF launch can run a runtime exception if it does
		// not succeed in creating the resource. We want to handle this exception ourself
		try {
			startupSetup = rs.getResource(startupSetupURI, true);
		} catch (RuntimeException e) {
			// Does nothing handle later
		}
		// CHECKSTYLE.ON: IllegalCatch
		if (startupSetup == null) {
			throw new DiesOn(FATAL).displaying(userSetupFilePath + " is not a valid setup file").ready();
		}

		Object startupSetupRoot = EcoreUtil.getObjectByType(startupSetup.getContents(),
				SetupPackage.Literals.PROJECT);
		final Project startupSetupProject;
		if (startupSetupRoot instanceof Project) {
			startupSetupProject = (Project)startupSetupRoot;
		} else {
			throw new DiesOn(SOFTWARE_ERROR).displaying(
					"The root of the setup file should be a Setup::Project").ready();
		}

		progressPageLog = new ProgressPageLog(System.out);

		Resource environmentSetup = rs.getResource(environmentSetupURI, true);
		Index eclipseSetupIndex = (Index)EcoreUtil.getObjectByType(environmentSetup.getContents(),
				SetupPackage.Literals.INDEX);

		final String wsPath = handleWorkspace(startupSetupProject, eclipseSetupIndex);
		final String installPath = handleInstallation(startupSetupProject, eclipseSetupIndex);

		EList<ProductCatalog> productCatalogs = eclipseSetupIndex.getProductCatalogs();
		ProductCatalog catalog = productCatalogs.get(0);
		Product product = catalog.getProducts().get(0);
		ProductVersion productVersion = product.getVersions().get(0);

		// Add extra plugins to install from user setup model.
		for (SetupTask setupTask : startupSetupProject.getSetupTasks()) {
			if (setupTask instanceof P2Task) {
				SetupTask copy = EcoreUtil.copy(setupTask);
				catalog.getSetupTasks().add(copy);
			}
		}

		// Create Oomph setup context.
		Installation installation = SetupContext.createInstallation();
		installation.setProductVersion(productVersion);
		Workspace workspace = SetupContext.createWorkspace();
		User user = SetupContext.createUser();
		Resource installationResource = rs.createResource(SetupContext.INSTALLATION_SETUP_URI);
		installationResource.getContents().add(installation);
		Resource workspaceResource = rs.createResource(SetupContext.WORKSPACE_SETUP_URI);
		workspaceResource.getContents().add(workspace);
		Resource userResource = rs.createResource(SetupContext.USER_SETUP_URI);
		userResource.getContents().add(user);
		try {
			userResource.save(null);
		} catch (IOException ex) {
			throw new DiesOn(DeathType.ERROR).duedTo(ex).displaying("Error while processing the setup model")
					.ready();
		}

		final SetupContext setupContext = SetupContext.create(installation, workspace, user);
		Trigger triggerBootstrap = Trigger.BOOTSTRAP;
		URIConverter uriConverter = rs.getURIConverter();
		SetupTaskPerformer aPerformer;
		// CHECKSTYLE.OFF: IllegalCatch - No choice since Oomph launch such an exception
		try {
			aPerformer = SetupTaskPerformer.create(uriConverter, SetupPrompter.CANCEL, triggerBootstrap,
					setupContext, false);
		} catch (Exception e) {
			throw new DiesOn(DeathType.ERROR).duedTo(e).displaying("Error while processing the setup model")
					.ready();
		}
		// CHECKSTYLE.ON: IllegalCatch
		Confirmer confirmer = Confirmer.ACCEPT;
		aPerformer.put(ILicense.class, confirmer);
		aPerformer.put(Certificate.class, confirmer);
		aPerformer.setProgress(progressPageLog);
		aPerformer.setOffline(false);
		aPerformer.setMirrors(true);

		if (installationPathContainsExistingEclipse(installPath)) {
			aPerformer.getTriggeredSetupTasks().clear();
			progressPageLog.log("Existing eclipse environment found at : " + installPath); //$NON-NLS-1$
			// Add installation task and workspace task.
			InstallationTask installationTask = SetupFactory.eINSTANCE.createInstallationTask();
			installationTask.setLocation(installPath);
			aPerformer.getTriggeredSetupTasks().add(installationTask);
			WorkspaceTask workspaceTask = SetupFactory.eINSTANCE.createWorkspaceTask();
			workspaceTask.setLocation(wsPath);
			aPerformer.getTriggeredSetupTasks().add(workspaceTask);
		} else {
			// Ensure we use user installation and workspace paths
			for (SetupTask setupTask : aPerformer.getTriggeredSetupTasks()) {
				if (setupTask instanceof WorkspaceTask) {
					((WorkspaceTask)setupTask).setLocation(wsPath);
				} else if (setupTask instanceof InstallationTask) {
					((InstallationTask)setupTask).setLocation(installPath);
				}
			}
		}

		return aPerformer;
	}

	/**
	 * Check if there is an existing eclipse environment at the given path.
	 * 
	 * @param installationPath
	 *            the given installation path.
	 * @return true if there is an existing eclipse environment at the given path, false otherwise.
	 */
	private boolean installationPathContainsExistingEclipse(String installationPath) {
		if (installationPath != null) {
			File file = new File(installationPath);
			if (file.exists()) {
				File[] eclipseFolder = file.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return ECLIPSE.equals(name);
					}
				});
				if (eclipseFolder != null && eclipseFolder.length == 1) {
					return isEclipseIntallationFolder(eclipseFolder[0]);
				}
			}
		}
		return false;
	}

	/**
	 * Returns <code>true</code> if the file is a eclipse installation folder, <code>false</code> otherwise.
	 *
	 * @param eclipseFolder
	 *            file to test.
	 * @return <code>true</code> if the file is a eclipse installation folder, <code>false</code> otherwise.
	 */
	private boolean isEclipseIntallationFolder(File eclipseFolder) {
		if (eclipseFolder.exists()) {
			String[] eclipseExe = eclipseFolder.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return ECLIPSE.equals(name) || "eclipse.exe".equals(name); //$NON-NLS-1$
				}
			});
			if (eclipseExe.length == 1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Handle the creation/reuse of the workspace path.
	 * 
	 * @param project
	 *            the root object of the user model.
	 * @param index
	 *            the root object of the environment model.
	 * @return the workspace path.
	 * @throws IOException
	 *             e
	 * @throws Die
	 *             e
	 */
	private String handleWorkspace(Project project, Index index) throws IOException, Die {
		final String workspaceLocation;
		if (modelDefinesWorkspacePath(project)) {
			workspaceLocation = getWorkspacePath(project);
		} else {
			workspaceLocation = genWorkspacePath(project);
		}

		for (ProductCatalog productCatalog : index.getProductCatalogs()) {
			for (SetupTask setupTask : productCatalog.getSetupTasks()) {
				if (setupTask instanceof WorkspaceTask) {
					((WorkspaceTask)setupTask).setLocation(workspaceLocation);
					break;
				}
			}
		}
		return workspaceLocation;
	}

	/**
	 * Handle the creation/reuse of the installation path.
	 * 
	 * @param project
	 *            the root object of the user model.
	 * @param index
	 *            the root object of the environment model.
	 * @return the installation path.
	 * @throws IOException
	 *             e
	 * @throws Die
	 *             e
	 */
	private String handleInstallation(Project project, Index index) throws IOException, Die {
		final String installationLocation;
		if (modelDefinesWorkspacePath(project)) {
			installationLocation = getInstallationPath(project);
		} else {
			installationLocation = genInstallationPath(project);
		}

		for (ProductCatalog productCatalog : index.getProductCatalogs()) {
			for (SetupTask setupTask : productCatalog.getSetupTasks()) {
				if (setupTask instanceof InstallationTask) {
					((InstallationTask)setupTask).setLocation(installationLocation);
					break;
				}
			}
		}
		return installationLocation;
	}

	/**
	 * Returns true if the given Project contains a variable task with a non null workspace location.
	 * 
	 * @param project
	 *            the given Project.
	 * @return true if the given Project contains a variable task with a non null workspace location, false
	 *         otherwise.
	 */
	private boolean modelDefinesWorkspacePath(Project project) {
		String workspacePath = getWorkspacePath(project);
		if (workspacePath != null) {
			return true;
		}
		return false;
	}

	/**
	 * Search a variable task with name "workspace.location" in the given Project.
	 * 
	 * @param project
	 *            the given Project.
	 * @return the variable task if found, null otherwise.
	 */
	private VariableTask getVariableTaskForWorkspace(Project project) {
		for (SetupTask setupTask : project.getSetupTasks()) {
			if (setupTask instanceof VariableTask) {
				if ("workspace.location".equals(((VariableTask)setupTask).getName())) { //$NON-NLS-1$
					return (VariableTask)setupTask;
				}
			}
		}
		return null;
	}

	/**
	 * Search a variable task with name "installation.location" in the given Project.
	 * 
	 * @param project
	 *            the given Project.
	 * @return the variable task if found, null otherwise.
	 */
	private VariableTask getVariableTaskForInstallation(Project project) {
		for (SetupTask setupTask : project.getSetupTasks()) {
			if (setupTask instanceof VariableTask) {
				if ("installation.location".equals(((VariableTask)setupTask).getName())) { //$NON-NLS-1$
					return (VariableTask)setupTask;
				}
			}
		}
		return null;
	}

	/**
	 * Search a workspace task in the given Project. If found, return his location attribute value.
	 * 
	 * @param project
	 *            the given Project.
	 * @return the location attribute value of the workspace task if found, null otherwise.
	 */
	private String getWorkspacePath(Project project) {
		final String path;
		final VariableTask task = getVariableTaskForWorkspace(project);
		if (task != null) {
			String resourcePath = project.eResource().getURI().toFileString();
			String resourceBasePath = resourcePath.substring(0, resourcePath.lastIndexOf(SEP));
			path = toFileWithAbsolutePath(resourceBasePath, task.getValue()).toString();
		} else {
			path = null;
		}

		return path;
	}

	/**
	 * Search an installation task in the given Project. If found, return his location attribute value.
	 * 
	 * @param project
	 *            the given Project.
	 * @return the location attribute value of the installation task if found, null otherwise.
	 */
	private String getInstallationPath(Project project) {
		final String path;
		final VariableTask task = getVariableTaskForInstallation(project);
		if (task != null) {
			String resourcePath = project.eResource().getURI().toFileString();
			String resourceBasePath = resourcePath.substring(0, resourcePath.lastIndexOf(SEP));
			path = toFileWithAbsolutePath(resourceBasePath, task.getValue()).toString();
		} else {
			path = null;
		}
		return path;
	}

	/**
	 * Generates a unique id in the temporary folder of the system according to the given Project. If the
	 * generated id already exists (cause a command already been called with the same Project), it is reused.
	 * 
	 * @param project
	 *            the given Project.
	 * @return the location of the workspace.
	 * @throws IOException
	 *             e
	 * @throws Die
	 *             e
	 */
	private String genWorkspacePath(Project project) throws IOException, Die {
		String id = generateIDForSetup(project.eResource().getURI().toFileString());
		File ws = createOrGetTempDir("emfcWs" + id); //$NON-NLS-1$
		return ws.getAbsolutePath();
	}

	/**
	 * Generates a unique id in the temporary folder of the system according to the given Project. If the
	 * generated id already exists (cause a command already been called with the same Project), it is reused.
	 * 
	 * @param project
	 *            the given Project.
	 * @return the location of the workspace.
	 * @throws IOException
	 *             e
	 * @throws Die
	 *             e
	 */
	private String genInstallationPath(Project project) throws IOException, Die {
		String id = generateIDForSetup(project.eResource().getURI().toFileString());
		File ws = createOrGetTempDir("emfcInstall" + id); //$NON-NLS-1$
		return ws.getAbsolutePath();
	}

	/**
	 * Creates a temporary directory in the system temp directory.
	 * 
	 * @param name
	 *            the name of the temp directory to create.
	 * @return the new created directory.
	 */
	private static File createOrGetTempDir(String name) {
		final File baseDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		final String baseName = name;

		for (int counter = 0; counter < 10000; counter++) {
			final File tempDir = new File(baseDir, baseName);
			if (tempDir.exists()) {
				return tempDir;
			}
			if (tempDir.mkdir()) {
				return tempDir;
			}
		}
		throw new IllegalStateException("Failed to create directory within " + 10000 + " attempts (tried "
				+ baseName + "0 to " + baseName + (10000 - 1) + ')');
	}

	/**
	 * Generate a unique ID for the a given file.
	 * 
	 * @param setupFilePath
	 *            the absolute path of the given file.
	 * @return a unique ID for the a given file.
	 * @throws IOException
	 *             e
	 * @throws Die
	 *             On any other program error.
	 */
	private static String generateIDForSetup(String setupFilePath) throws IOException, Die {
		File f = new File(setupFilePath);
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1"); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e) {
			throw new DiesOn(DeathType.ERROR).duedTo(e).ready();
		}
		FileInputStream inputStream = new FileInputStream(f);
		byte[] bytesBuffer = new byte[GEN_ID_BUFFER_SIZE];
		int bytesRead = -1;

		while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
			digest.update(bytesBuffer, 0, bytesRead);
		}
		inputStream.close();

		byte[] hashedBytes = digest.digest();

		return convertByteArrayToHexString(hashedBytes);
	}

	/**
	 * Converts an array of bytes into a string of hexadecimal values.
	 * 
	 * @param arrayBytes
	 *            the given array of bytes.
	 * @return a string of hexadecimal values.
	 */
	private static String convertByteArrayToHexString(byte[] arrayBytes) {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < arrayBytes.length; i++) {
			// CHECKSTYLE.OFF: MagicNumber
			stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
			// CHECKSTYLE.ON: MagicNumber
		}
		return stringBuffer.toString();
	}

	/**
	 * Gets the path to the Eclipse executable file.
	 * 
	 * @param setupFileAbsolutePath
	 *            path the setup file. This path is used to make the path the eclipse file absolute.
	 * @return the path to the Eclipse executable file.
	 */
	protected String getEclipsePath(String setupFileAbsolutePath) {
		String setupFileBasePath = Paths.get(setupFileAbsolutePath).getParent().toString();
		OS os = getPerformer().getOS();
		String eclipseDir = os.getEclipseDir();
		String eclipseExecutable = os.getEclipseExecutable();
		Path installationPath = Paths.get(getPerformer().getInstallationLocation().getPath(), eclipseDir,
				eclipseExecutable);
		File eclipseFile = toFileWithAbsolutePath(setupFileBasePath, installationPath.toString());
		return eclipseFile.toString();
	}
}

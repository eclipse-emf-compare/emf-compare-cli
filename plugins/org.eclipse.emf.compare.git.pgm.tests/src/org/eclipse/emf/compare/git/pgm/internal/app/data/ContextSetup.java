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
package org.eclipse.emf.compare.git.pgm.internal.app.data;

import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.SEP;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.compare.git.pgm.util.OomphUserModelBuilder;
import org.eclipse.emf.compare.git.pgm.util.ProjectBuilder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public class ContextSetup {

	//@formatter:off
	public final static String LYRICS_1 = "In the merry month of June, when first from home I started," + EOL
			+ "And left the girls alone, sad and broken-hearted." + EOL
			+ "Shook hands with father dear, kissed my darling mother," + EOL
			+ "Drank a pint of beer, my tears and grief to smother ;" + EOL
			+ "Then off to reap the corn, and leave where I was born." + EOL
			+ "I cut a stout black-thorn to banish ghost or goblin ;" + EOL
			+ "With a pair of bran new brogues, I rattled o'er the bogs —" + EOL
			+ "Sure I frightened all the dogs on the rocky road to Dublin." + EOL;

	public final static String LYRICS_2 = "For it is the rocky road, here's the road to Dublin;" + EOL
			+ "Here's the rocky road, now fire away to Dublin !" + EOL;

	public final static String LYRICS_3 = "The steam-coach was at hand, the driver said he'd cheap ones."+ EOL
			+ "But sure the luggage van was too much for my ha'pence." + EOL
			+ "For England I was bound, it would never do to balk it." + EOL
			+ "For every step of the road, bedad I says I, I'll walk it." + EOL
			+ "I did not sigh or moan until I saw Athlone." + EOL
			+ "A pain in my shin bone, it set my heart a-bubbling;" + EOL
			+ "And fearing the big cannon, looking o'er the Shannon," + EOL
			+ "I very quickly ran on the rocky road to Dublin." + EOL;
	//@formatter:on

	private File project;

	private File userSetupFile;

	private Path projectPath;

	private final Git git;

	private final Path tmpFolder;

	public ContextSetup(Git git, Path tmpFolder) {
		super();
		this.git = git;
		this.tmpFolder = tmpFolder;
	}

	public static File getWorkspaceLocation() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		return root.getLocation().toFile();
	}

	public File getProject() {
		return project;
	}

	public File getUserSetupFile() {
		return userSetupFile;
	}

	public Path getProjectPath() {
		return projectPath;
	}

	public Path getRepositoryPath() {
		return git.getRepository().getWorkTree().toPath();
	}

	private File createPapyrusUserOomphModel(Path setupFilePath, File... projects) throws IOException {
		OomphUserModelBuilder userModelBuilder = new OomphUserModelBuilder();
		Path oomphFolderPath = tmpFolder.resolve("oomphFolder");
		//@formatter:off
		File model = userModelBuilder
				.setInstallationLocation(oomphFolderPath.toString())
				.setWorkspaceLocation(getWorkspaceLocation().getAbsolutePath())
				.setProjectPaths(Arrays.stream(projects).map(p -> p.getAbsolutePath()).toArray(String[]::new))
				.setRepositories("http://download.eclipse.org/releases/mars/201505081000",
						"http://download.eclipse.org/modeling/emf/compare/updates/logical/emf.compare/nightly/latest/",
						"http://download.eclipse.org/modeling/mdt/papyrus/updates/nightly/mars")
				.setRequirements("org.eclipse.uml2.feature.group",
						"org.eclipse.papyrus.sdk.feature.feature.group",
						"org.eclipse.emf.compare.rcp.ui.feature.group",
						"org.eclipse.emf.compare.uml2.feature.group",
						"org.eclipse.emf.compare.diagram.gmf.feature.group",
						"org.eclipse.emf.compare.diagram.papyrus.feature.group")
				.saveTo(setupFilePath.toString());
		//@formatter:on
		return model;
	}

	private File createPapyrusUserOomphModel(File... projects) throws IOException {
		return createPapyrusUserOomphModel(tmpFolder.resolve("setup.setup"), projects); //$NON-NLS-1$
	}

	private RevCommit addAllAndCommit(String commitMessage) throws GitAPIException, NoFilepatternException,
			NoHeadException, NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException {
		DirCache dirChache = git.add().addFilepattern(".").call(); //$NON-NLS-1$
		// Assert there is something to commit
		assertTrue(dirChache.getEntriesWithin("").length > 0);
		RevCommit revCommit = git.commit().setAuthor("Logical test author", "logicaltest@obeo.fr")
				.setCommitter("Logical test author", "logicaltest@obeo.fr").setMessage(commitMessage).call();
		return revCommit;
	}

	private Ref createBranch(String branchName, String startingPoint) throws RefAlreadyExistsException,
			RefNotFoundException, InvalidRefNameException, GitAPIException {
		return git.branchCreate().setName(branchName).setStartPoint(startingPoint).call();
	}

	private Ref createBranchAndCheckout(String ref, String startingPoint) throws RefAlreadyExistsException,
			RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
		return git.checkout().setName(ref).setStartPoint(startingPoint).setCreateBranch(true).call();
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Adds Attr1 to C1 and adds Attr2 to C2 [branch_b, HEAD]
	 * |
	 * |
	 * | * Delete C2 [branch d]
	 * | |
	 * | * Delete C1 [branch_c]
	 * |/ 
	 * |  
	 * Initial commit - Create C1 and C2 [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupCHE002() throws Exception {
		projectPath = getRepositoryPath().resolve("CHE002");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/CHE002/branch_a/model.di")//
				.addContentToCopy("conflicts/CHE002/branch_a/model.uml") //
				.addContentToCopy("conflicts/CHE002/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit - Create C1 and C2");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/CHE002/branch_c/model.di")//
				.addContentToCopy("conflicts/CHE002/branch_c/model.uml") //
				.addContentToCopy("conflicts/CHE002/branch_c/model.notation") //
				.create(projectPath);

		addAllAndCommit("Delete C1");

		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/CHE002/branch_d/model.di")//
				.addContentToCopy("conflicts/CHE002/branch_d/model.uml") //
				.addContentToCopy("conflicts/CHE002/branch_d/model.notation") //
				.create(projectPath);

		addAllAndCommit("Delete C2");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/CHE002/branch_b/model.di")//
				.addContentToCopy("conflicts/CHE002/branch_b/model.uml") //
				.addContentToCopy("conflicts/CHE002/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds Attr1 to C1 and adds Attr2 to C2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History:</h3>
	 * 
	 * <pre>
	 * * Adds Class 1 [branch_b, HEAD]
	 * |
	 * |
	 * | * Adds Class 3 [branch d]
	 * | |
	 * | * Adds Class 2 [branch_c]
	 * |/ 
	 * |  
	 * Initial commit [branch_a]
	 * </pre>
	 * 
	 * @return
	 * @throws Exception
	 */
	public RevCommit setupCHE003() throws Exception {
		projectPath = getRepositoryPath().resolve("CHE003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/CHE003/branch_a/model.di")//
				.addContentToCopy("automerging/CHE003/branch_a/model.uml") //
				.addContentToCopy("automerging/CHE003/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/CHE003/branch_c/model.di")//
				.addContentToCopy("automerging/CHE003/branch_c/model.uml") //
				.addContentToCopy("automerging/CHE003/branch_c/model.notation") //
				.create(projectPath);

		RevCommit branchCLastCommit = addAllAndCommit("Adds class 2");

		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/CHE003/branch_d/model.di")//
				.addContentToCopy("automerging/CHE003/branch_d/model.uml") //
				.addContentToCopy("automerging/CHE003/branch_d/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds class 3");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/CHE003/branch_b/model.di")//
				.addContentToCopy("automerging/CHE003/branch_b/model.uml") //
				.addContentToCopy("automerging/CHE003/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds class 1");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

		return branchCLastCommit;
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 *  * [branch_c]
	 *  |     Add Class3 under Model1
	 *  |     Add Class4 under Model2
	 *  |    
	 *  | * [branch_b)]
	 *  |/  Add Class1 under Model1
	 *  |   Add Class2 under Model2
	 *  |  
	 * [branch_a]
	 *  Initial commit
	 *   - A project with 2 models, 2 diagrams
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupCHE004() throws Exception {
		projectPath = getRepositoryPath().resolve("MER004");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/MER004/branch_a/model.di")//
				.addContentToCopy("automerging/MER004/branch_a/model.uml") //
				.addContentToCopy("automerging/MER004/branch_a/model.notation") //
				.addContentToCopy("automerging/MER004/branch_a/model2.di")//
				.addContentToCopy("automerging/MER004/branch_a/model2.uml") //
				.addContentToCopy("automerging/MER004/branch_a/model2.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit" + EOL + "- A project with 2 models, 2 diagrams");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/MER004/branch_c/model.di")//
				.addContentToCopy("automerging/MER004/branch_c/model.uml") //
				.addContentToCopy("automerging/MER004/branch_c/model.notation") //
				.addContentToCopy("automerging/MER004/branch_c/model2.di")//
				.addContentToCopy("automerging/MER004/branch_c/model2.uml") //
				.addContentToCopy("automerging/MER004/branch_c/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add Class3 under Model1" + EOL + "Add Class4 under Model2");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/MER004/branch_b/model.di")//
				.addContentToCopy("automerging/MER004/branch_b/model.uml") //
				.addContentToCopy("automerging/MER004/branch_b/model.notation") //
				.addContentToCopy("automerging/MER004/branch_b/model2.di")//
				.addContentToCopy("automerging/MER004/branch_b/model2.uml") //
				.addContentToCopy("automerging/MER004/branch_b/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add Class1 under Model1" + EOL + "Add Class2 under Model2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

	}

	/**
	 * <h3>History:</h3>
	 * 
	 * <pre>
	 *  * [branch_d]
	 *  |     Delete Class2
	 *  |  
	 *  * [branch_c] Delete Class1
	 *  |    
	 *  | *  [branch_b]
	 *  |/   Add attribute1 under Class1
	 *  |    Add attribute2 under Class2
	 *  |   
	 *  [branch_a]
	 * Initial commit
	 *   - 1 project with 2 models, 2 diagrams
	 *   - add Class1 under Model1, Class2 under Model2
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupCHE006() throws Exception {
		projectPath = getRepositoryPath().resolve("MER006");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/MER006/branch_a/model.di")//
				.addContentToCopy("conflicts/MER006/branch_a/model.uml") // //$NON-NLS-1$
				.addContentToCopy("conflicts/MER006/branch_a/model.notation") //
				.addContentToCopy("conflicts/MER006/branch_a/model2.di")//
				.addContentToCopy("conflicts/MER006/branch_a/model2.uml") //
				.addContentToCopy("conflicts/MER006/branch_a/model2.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit" + EOL + "   - 1 project with 2 models, 2 diagrams" + EOL
				+ "   - add Class1 under Model1, Class2 under Model2");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER006/branch_c/model.di")//
				.addContentToCopy("conflicts/MER006/branch_c/model.uml") //
				.addContentToCopy("conflicts/MER006/branch_c/model.notation") //
				.addContentToCopy("conflicts/MER006/branch_c/model2.di")//
				.addContentToCopy("conflicts/MER006/branch_c/model2.uml") //
				.addContentToCopy("conflicts/MER006/branch_c/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Delete Class1");

		// Creates branch d
		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER006/branch_d/model.di")//
				.addContentToCopy("conflicts/MER006/branch_d/model.uml") //
				.addContentToCopy("conflicts/MER006/branch_d/model.notation") //
				.addContentToCopy("conflicts/MER006/branch_d/model2.di")//
				.addContentToCopy("conflicts/MER006/branch_d/model2.uml") //
				.addContentToCopy("conflicts/MER006/branch_d/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Delete Class2");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER006/branch_b/model.di")//
				.addContentToCopy("conflicts/MER006/branch_b/model.uml") //
				.addContentToCopy("conflicts/MER006/branch_b/model.notation") //
				.addContentToCopy("conflicts/MER006/branch_b/model2.di")//
				.addContentToCopy("conflicts/MER006/branch_b/model2.uml") //
				.addContentToCopy("conflicts/MER006/branch_b/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add attribute1 under Class1, attribute2 under Class2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	public void setupDIFnothingToDo() throws Exception {
		projectPath = getRepositoryPath().resolve("EmptyProject");
		project = new ProjectBuilder(this) //
				.create(projectPath);

		addAllAndCommit("First commit");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History:</h3>
	 * 
	 * <pre>
	 * * Delete Class1 [branch_c]
	 * |
	 * | * Add Attribute1 under Class1 [branch_b]
	 * |/  
	 * |   
	 * Initial Commit [branch_a]
	 *  -Add project PapyrusProject1
	 *  -Add ClassDiagram1
	 *  -Add Class1
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupMER001() throws Exception {

		projectPath = getRepositoryPath().resolve("MER001");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/MER001/branch_a/model.di")//
				.addContentToCopy("conflicts/MER001/branch_a/model.uml") //
				.addContentToCopy("conflicts/MER001/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit [PapyrusProject3]");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER001/branch_c/model.di")//
				.addContentToCopy("conflicts/MER001/branch_c/model.uml") //
				.addContentToCopy("conflicts/MER001/branch_c/model.notation") //
				.create(projectPath);

		addAllAndCommit("Deletes Class1");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER001/branch_b/model.di")//
				.addContentToCopy("conflicts/MER001/branch_b/model.uml") //
				.addContentToCopy("conflicts/MER001/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds attribute1 under Class1");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Delete Class2 [branch_d]
	 * | 
	 * |  
	 * Delete Class1 [branc_c]
	 * |     
	 * |    
	 * | * Add attribute1 under Class1, attribute2 under Class2 [branch_b]
	 * |/  
	 * |   
	 * |  
	 * Initial Commit [branch_a]
	 *  -Add project PapyrusProject2
	 *  -Add ClassDiagram1
	 *  -Add Class1, Class2
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupMER002() throws Exception {
		projectPath = getRepositoryPath().resolve("MER002");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/MER002/branch_a/model.di")//
				.addContentToCopy("conflicts/MER002/branch_a/model.uml") //
				.addContentToCopy("conflicts/MER002/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit [PapyrusProject3]");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER002/branch_c/model.di")//
				.addContentToCopy("conflicts/MER002/branch_c/model.uml") //
				.addContentToCopy("conflicts/MER002/branch_c/model.notation") //
				.create(projectPath);

		addAllAndCommit("Deletes Class1");

		// Creates branch d
		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER002/branch_c/model.di")//
				.addContentToCopy("conflicts/MER002/branch_c/model.uml") //
				.addContentToCopy("conflicts/MER002/branch_c/model.notation") //
				.create(projectPath);

		addAllAndCommit("Delete Class2");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER002/branch_b/model.di")//
				.addContentToCopy("conflicts/MER002/branch_b/model.uml") //
				.addContentToCopy("conflicts/MER002/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Add attribute1 under Class1, attribute2 under Class2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * Setup for testMER003_alreadyUpToDate0.
	 * 
	 * @throws Exception
	 */
	public void setupMER003_alreadyUpToDate0() throws Exception {
		projectPath = getRepositoryPath().resolve("MER003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/MER003/branch_a/model.di")//
				.addContentToCopy("automerging/MER003/branch_a/model.uml") //
				.addContentToCopy("automerging/MER003/branch_a/model.notation") //
				.create(projectPath);
		addAllAndCommit("Initial commit [PapyrusProject3]");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * Setup for testMER003_alreadyUpToDate0.
	 * 
	 * @throws Exception
	 */
	public void setupMER003_alreadyUpToDate1() throws Exception {
		projectPath = getRepositoryPath().resolve("PapyrusModel");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/MER003/branch_a/model.di")//
				.addContentToCopy("automerging/MER003/branch_a/model.uml") //
				.addContentToCopy("automerging/MER003/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit [PapyrusProject3]");
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/MER003/branch_b/model.di")//
				.addContentToCopy("automerging/MER003/branch_b/model.uml") //
				.addContentToCopy("automerging/MER003/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds class 1");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * Setup for testMER003_IncorrectProjectToImport_NotExistingProject.
	 * 
	 * @throws Exception
	 */
	public void setupMER003_IncorrectProjectToImport_NotExistingProject() throws Exception {
		projectPath = getRepositoryPath().resolve("MER003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/MER003/branch_a/model.di")//
				.addContentToCopy("automerging/MER003/branch_a/model.uml") //
				.addContentToCopy("automerging/MER003/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit [PapyrusProject3]");
		createBranch(branchA, "master");

		File notExistingProject = getRepositoryPath().resolve("GhostProject").toFile();

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project, notExistingProject);
	}

	/**
	 * Setup for testMER003_ProjectToImport_complexPath.
	 * 
	 * @throws Exception
	 */
	public void setupMER003_ProjectToImport_complexPath() throws Exception {
		Path folderWithComplexePath = getRepositoryPath().resolve("Folder with space & special char");
		folderWithComplexePath.toFile().mkdirs();
		projectPath = folderWithComplexePath.resolve("Project with path and spécial character");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/MER003/branch_a/model.di")//
				.addContentToCopy("automerging/MER003/branch_a/model.uml") //
				.addContentToCopy("automerging/MER003/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit [PapyrusProject3]");
		createBranch(branchA, "master");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * Setup for testMER003_SetupFile_complexPath.
	 * 
	 * @throws Exception
	 */
	public void setupMER003_SetupFile_complexPath() throws Exception {
		projectPath = getRepositoryPath().resolve("MER003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/MER003/branch_a/model.di")//
				.addContentToCopy("automerging/MER003/branch_a/model.uml") //
				.addContentToCopy("automerging/MER003/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit [PapyrusProject3]");
		createBranch(branchA, "master");

		Path folderWithComplexePath = getRepositoryPath().resolve("Folder with space & special char");
		folderWithComplexePath.toFile().mkdirs();

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(folderWithComplexePath
				.resolve("Setup file with spaces.setup"), project);
	}

	/**
	 * Setup for testMER003_RelativePaths.
	 * 
	 * @throws Exception
	 */
	public void setupMER003_RelativePaths() throws Exception {
		projectPath = getRepositoryPath().resolve("MER003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/MER003/branch_a/model.di")//
				.addContentToCopy("automerging/MER003/branch_a/model.uml") //
				.addContentToCopy("automerging/MER003/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit [PapyrusProject3]");
		createBranch(branchA, "master");

		Path folder = getRepositoryPath().resolve("a" + SEP + "b" + SEP + "c");
		folder.toFile().mkdirs();

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(folder.resolve("setup.setup"), project);
	}

	/**
	 * <h3>History:</h3>
	 * 
	 * <pre>
	 * * Adds Class 1 [branch_b]
	 * |    
	 * | * Adds Class 2 [branch_c]
	 * |/ 
	 * |  
	 * Initial commit (PapyrusProject3) [branch_a]
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupMER003() throws Exception {
		projectPath = getRepositoryPath().resolve("MER003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/MER003/branch_a/model.di")//
				.addContentToCopy("automerging/MER003/branch_a/model.uml") //
				.addContentToCopy("automerging/MER003/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit [PapyrusProject3]");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/MER003/branch_c/model.di")//
				.addContentToCopy("automerging/MER003/branch_c/model.uml") //
				.addContentToCopy("automerging/MER003/branch_c/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds class 2");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/MER003/branch_b/model.di")//
				.addContentToCopy("automerging/MER003/branch_b/model.uml") //
				.addContentToCopy("automerging/MER003/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds class 1");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 *  * [branch_c]
	 *  |     Add Class3 under Model1
	 *  |     Add Class4 under Model2
	 *  |    
	 *  | * [branch_b)]
	 *  |/  Add Class1 under Model1
	 *  |   Add Class2 under Model2
	 *  |  
	 * [branch_a]
	 *  Initial commit
	 *   - A project with 2 models, 2 diagrams
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupMER004() throws Exception {
		projectPath = getRepositoryPath().resolve("MER004");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/MER004/branch_a/model.di")//
				.addContentToCopy("automerging/MER004/branch_a/model.uml") //
				.addContentToCopy("automerging/MER004/branch_a/model.notation") //
				.addContentToCopy("automerging/MER004/branch_a/model2.di")//
				.addContentToCopy("automerging/MER004/branch_a/model2.uml") //
				.addContentToCopy("automerging/MER004/branch_a/model2.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit" + EOL + "- A project with 2 models, 2 diagrams");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/MER004/branch_c/model.di")//
				.addContentToCopy("automerging/MER004/branch_c/model.uml") //
				.addContentToCopy("automerging/MER004/branch_c/model.notation") //
				.addContentToCopy("automerging/MER004/branch_c/model2.di")//
				.addContentToCopy("automerging/MER004/branch_c/model2.uml") //
				.addContentToCopy("automerging/MER004/branch_c/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add Class3 under Model1" + EOL + "Add Class4 under Model2");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/MER004/branch_b/model.di")//
				.addContentToCopy("automerging/MER004/branch_b/model.uml") //
				.addContentToCopy("automerging/MER004/branch_b/model.notation") //
				.addContentToCopy("automerging/MER004/branch_b/model2.di")//
				.addContentToCopy("automerging/MER004/branch_b/model2.uml") //
				.addContentToCopy("automerging/MER004/branch_b/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add Class1 under Model1" + EOL + "Add Class2 under Model2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * [HEAD] [branch_c]
	 * |     Delete Class1 and Class2
	 * |    
	 * | * [branch_b]
	 * |/  Add attribute1 under Class1
	 * |   Add attribute2 under Class2
	 * |  
	 * [branch_a]
	 * Initial commit
	 *  - 1 project with 2 models, 2 diagrams
	 *  - add Class1 under Model1, Class2 under Model2
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupMER005() throws Exception {
		projectPath = getRepositoryPath().resolve("MER005");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/MER005/branch_a/model.di")//
				.addContentToCopy("conflicts/MER005/branch_a/model.uml") //
				.addContentToCopy("conflicts/MER005/branch_a/model.notation") //
				.addContentToCopy("conflicts/MER005/branch_a/model2.di")//
				.addContentToCopy("conflicts/MER005/branch_a/model2.uml") //
				.addContentToCopy("conflicts/MER005/branch_a/model2.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit" + EOL + " - 1 project with 2 models, 2 diagrams" + EOL
				+ " - add Class1 under Model1, Class2 under Model2");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER005/branch_c/model.di")//
				.addContentToCopy("conflicts/MER005/branch_c/model.uml") //
				.addContentToCopy("conflicts/MER005/branch_c/model.notation") //
				.addContentToCopy("conflicts/MER005/branch_c/model2.di")//
				.addContentToCopy("conflicts/MER005/branch_c/model2.uml") //
				.addContentToCopy("conflicts/MER005/branch_c/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Delete Class1 and Class2");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER005/branch_b/model.di")//
				.addContentToCopy("conflicts/MER005/branch_b/model.uml") //
				.addContentToCopy("conflicts/MER005/branch_b/model.notation") //
				.addContentToCopy("conflicts/MER005/branch_b/model2.di")//
				.addContentToCopy("conflicts/MER005/branch_b/model2.uml") //
				.addContentToCopy("conflicts/MER005/branch_b/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add attribute1 under Class1" + EOL + "Add attribute2 under Class2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History </p3>
	 * 
	 * <pre>
	 *  * [branch_d]
	 *  |     Delete Class2
	 *  |  
	 *  [branch_c] Delete Class1
	 *  |    
	 *  | *  [branch_b]
	 *  |/   Add attribute1 under Class1
	 *  |    Add attribute2 under Class2
	 *  |   
	 *  [branch_a]
	 * Initial commit
	 *   - 1 project with 2 models, 2 diagrams
	 *   - add Class1 under Model1, Class2 under Model2
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupMER006() throws Exception {
		projectPath = getRepositoryPath().resolve("MER006");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/MER006/branch_a/model.di")//
				.addContentToCopy("conflicts/MER006/branch_a/model.uml") //
				.addContentToCopy("conflicts/MER006/branch_a/model.notation") //
				.addContentToCopy("conflicts/MER006/branch_a/model2.di")//
				.addContentToCopy("conflicts/MER006/branch_a/model2.uml") //
				.addContentToCopy("conflicts/MER006/branch_a/model2.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit" + EOL + "   - 1 project with 2 models, 2 diagrams" + EOL
				+ "   - add Class1 under Model1, Class2 under Model2");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER006/branch_c/model.di")//
				.addContentToCopy("conflicts/MER006/branch_c/model.uml") //
				.addContentToCopy("conflicts/MER006/branch_c/model.notation") //
				.addContentToCopy("conflicts/MER006/branch_c/model2.di")//
				.addContentToCopy("conflicts/MER006/branch_c/model2.uml") //
				.addContentToCopy("conflicts/MER006/branch_c/model2.notation") //
				.create(projectPath);

		addAllAndCommit(" Delete Class1");

		// Creates branch d
		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER006/branch_d/model.di")//
				.addContentToCopy("conflicts/MER006/branch_d/model.uml") //
				.addContentToCopy("conflicts/MER006/branch_d/model.notation") //
				.addContentToCopy("conflicts/MER006/branch_d/model2.di")//
				.addContentToCopy("conflicts/MER006/branch_d/model2.uml") //
				.addContentToCopy("conflicts/MER006/branch_d/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Delete Class2");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/MER006/branch_b/model.di")//
				.addContentToCopy("conflicts/MER006/branch_b/model.uml") //
				.addContentToCopy("conflicts/MER006/branch_b/model.notation") //
				.addContentToCopy("conflicts/MER006/branch_b/model2.di")//
				.addContentToCopy("conflicts/MER006/branch_b/model2.uml") //
				.addContentToCopy("conflicts/MER006/branch_b/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add attribute1 under Class1, attribute2 under Class2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History:</h3>
	 * 
	 * <pre>
	 * * Delete Class1 [origin/master]
	 * |
	 * | * Add Attribute1 under Class1 [master] (local)
	 * |/  
	 * |   
	 * Initial Commit [branch_a]
	 *  -Add project PapyrusProjectPUL001
	 *  -Add ClassDiagram1
	 *  -Add Class1
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL001_Remote() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL001");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/PUL001/branch_a/model.di")//
				.addContentToCopy("conflicts/PUL001/branch_a/model.uml") //
				.addContentToCopy("conflicts/PUL001/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit [PapyrusProjectPUL001]");
		createBranch(branchA, "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/PUL001/master_remote/model.di")//
				.addContentToCopy("conflicts/PUL001/master_remote/model.uml") //
				.addContentToCopy("conflicts/PUL001/master_remote/model.notation") //
				.create(projectPath);

		addAllAndCommit("Delete Class1");
	}

	/**
	 * <h3>History:</h3>
	 * 
	 * <pre>
	 * * Delete Class1 [origin/master]
	 * |
	 * | * Add Attribute1 under Class1 [master] (local)
	 * |/  
	 * |   
	 * Initial Commit [branch_a]
	 *  -Add project PapyrusProjectPUL001
	 *  -Add ClassDiagram1
	 *  -Add Class1
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL001_Local() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL001");
		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/PUL001/master_local/model.di")//
				.addContentToCopy("conflicts/PUL001/master_local/model.uml") //
				.addContentToCopy("conflicts/PUL001/master_local/model.notation") //
				.create(projectPath);

		addAllAndCommit("Add Attribute1 under Class1");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	public void setupPUL001_Local_AlreadyUpToDate() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL001");
		project = new ProjectBuilder(this).create(projectPath);

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Delete Class2 [origin/master]
	 * | 
	 * |  
	 * Delete Class1 [branc_c]
	 * |     
	 * |    
	 * | * Add attribute1 under Class1, attribute2 under Class2 [master] (local)
	 * |/  
	 * |   
	 * |  
	 * Initial Commit [branch_a]
	 *  -Add project PapyrusProject2
	 *  -Add ClassDiagram1
	 *  -Add Class1, Class2
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL002_Remote() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL002");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/PUL002/branch_a/model.di")//
				.addContentToCopy("conflicts/PUL002/branch_a/model.uml") //
				.addContentToCopy("conflicts/PUL002/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit [PapyrusProjectPUL002]");
		createBranch(branchA, "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/PUL002/branch_c/model.di")//
				.addContentToCopy("conflicts/PUL002/branch_c/model.uml") //
				.addContentToCopy("conflicts/PUL002/branch_c/model.notation") //
				.create(projectPath);

		addAllAndCommit("Deletes Class1");
		String branchC = "branch_c";
		createBranch(branchC, "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/PUL002/master_remote/model.di")//
				.addContentToCopy("conflicts/PUL002/master_remote/model.uml") //
				.addContentToCopy("conflicts/PUL002/master_remote/model.notation") //
				.create(projectPath);

		addAllAndCommit("Delete Class2");
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Delete Class2 [origin/master]
	 * | 
	 * |  
	 * Delete Class1 [branc_c]
	 * |     
	 * |    
	 * | * Add attribute1 under Class1, attribute2 under Class2 [master] (local)
	 * |/  
	 * |   
	 * |  
	 * Initial Commit [branch_a]
	 *  -Add project PapyrusProject2
	 *  -Add ClassDiagram1
	 *  -Add Class1, Class2
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL002_Local() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL002");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/PUL002/master_local/model.di")//
				.addContentToCopy("conflicts/PUL002/master_local/model.uml") //
				.addContentToCopy("conflicts/PUL002/master_local/model.notation") //
				.create(projectPath);

		addAllAndCommit("Add attribute1 under Class1, attribute2 under Class2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History:</h3>
	 * 
	 * <pre>
	 * * Adds Class 1 [origin/master]
	 * |    
	 * | * Adds Class 2 [master] (local)
	 * |/ 
	 * |  
	 * Initial commit (PapyrusProjectPUL003) [branch_a]
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL003_Remote() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/PUL003/branch_a/model.di")//
				.addContentToCopy("automerging/PUL003/branch_a/model.uml") //
				.addContentToCopy("automerging/PUL003/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit [PapyrusProjectPUL003]");
		createBranch(branchA, "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/PUL003/master_remote/model.di")//
				.addContentToCopy("automerging/PUL003/master_remote/model.uml") //
				.addContentToCopy("automerging/PUL003/master_remote/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds class 1");
	}

	/**
	 * <h3>History:</h3>
	 * 
	 * <pre>
	 * * Adds Class 1 [origin/master]
	 * |    
	 * | * Adds Class 2 [master] (local)
	 * |/ 
	 * |  
	 * Initial commit (PapyrusProjectPUL003) [branch_a]
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL003_Local() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL003");
		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/PUL003/master_local/model.di")//
				.addContentToCopy("automerging/PUL003/master_local/model.uml") //
				.addContentToCopy("automerging/PUL003/master_local/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds class 2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 *  * [origin/master]
	 *  |     Add Class3 under Model1
	 *  |     Add Class4 under Model2
	 *  |    
	 *  | * [master] (local)
	 *  |/  Add Class1 under Model1
	 *  |   Add Class2 under Model2
	 *  |  
	 * [branch_a]
	 *  Initial commit
	 *   - A project with 2 models, 2 diagrams
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL004_Remote() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL004");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/PUL004/branch_a/model.di")//
				.addContentToCopy("automerging/PUL004/branch_a/model.uml") //
				.addContentToCopy("automerging/PUL004/branch_a/model.notation") //
				.addContentToCopy("automerging/PUL004/branch_a/model2.di")//
				.addContentToCopy("automerging/PUL004/branch_a/model2.uml") //
				.addContentToCopy("automerging/PUL004/branch_a/model2.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit" + EOL + "- A project with 2 models, 2 diagrams");
		createBranch(branchA, "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/PUL004/master_remote/model.di")//
				.addContentToCopy("automerging/PUL004/master_remote/model.uml") //
				.addContentToCopy("automerging/PUL004/master_remote/model.notation") //
				.addContentToCopy("automerging/PUL004/master_remote/model2.di")//
				.addContentToCopy("automerging/PUL004/master_remote/model2.uml") //
				.addContentToCopy("automerging/PUL004/master_remote/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add Class3 under Model1" + EOL + "Add Class4 under Model2");
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 *  * [origin/master]
	 *  |     Add Class3 under Model1
	 *  |     Add Class4 under Model2
	 *  |    
	 *  | * [master] (local)
	 *  |/  Add Class1 under Model1
	 *  |   Add Class2 under Model2
	 *  |  
	 * [branch_a]
	 *  Initial commit
	 *   - A project with 2 models, 2 diagrams
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL004_Local() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL004");
		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/PUL004/master_local/model.di")//
				.addContentToCopy("automerging/PUL004/master_local/model.uml") //
				.addContentToCopy("automerging/PUL004/master_local/model.notation") //
				.addContentToCopy("automerging/PUL004/master_local/model2.di")//
				.addContentToCopy("automerging/PUL004/master_local/model2.uml") //
				.addContentToCopy("automerging/PUL004/master_local/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add Class1 under Model1" + EOL + "Add Class2 under Model2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * [origin/master]
	 * |     Delete Class1 and Class2
	 * |    
	 * | * [master] (local)
	 * |/  Add attribute1 under Class1
	 * |   Add attribute2 under Class2
	 * |  
	 * [branch_a]
	 * Initial commit
	 *  - 1 project with 2 models, 2 diagrams
	 *  - add Class1 under Model1, Class2 under Model2
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL005_Remote() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL005");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/PUL005/branch_a/model.di")//
				.addContentToCopy("conflicts/PUL005/branch_a/model.uml") //
				.addContentToCopy("conflicts/PUL005/branch_a/model.notation") //
				.addContentToCopy("conflicts/PUL005/branch_a/model2.di")//
				.addContentToCopy("conflicts/PUL005/branch_a/model2.uml") //
				.addContentToCopy("conflicts/PUL005/branch_a/model2.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit" + EOL + " - 1 project with 2 models, 2 diagrams" + EOL
				+ " - add Class1 under Model1, Class2 under Model2");
		createBranch(branchA, "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/PUL005/master_remote/model.di")//
				.addContentToCopy("conflicts/PUL005/master_remote/model.uml") //
				.addContentToCopy("conflicts/PUL005/master_remote/model.notation") //
				.addContentToCopy("conflicts/PUL005/master_remote/model2.di")//
				.addContentToCopy("conflicts/PUL005/master_remote/model2.uml") //
				.addContentToCopy("conflicts/PUL005/master_remote/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Delete Class1 and Class2");
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * [origin/master]
	 * |     Delete Class1 and Class2
	 * |    
	 * | * [master] (local)
	 * |/  Add attribute1 under Class1
	 * |   Add attribute2 under Class2
	 * |  
	 * [branch_a]
	 * Initial commit
	 *  - 1 project with 2 models, 2 diagrams
	 *  - add Class1 under Model1, Class2 under Model2
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL005_Local() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL005");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/PUL005/master_local/model.di")//
				.addContentToCopy("conflicts/PUL005/master_local/model.uml") //
				.addContentToCopy("conflicts/PUL005/master_local/model.notation") //
				.addContentToCopy("conflicts/PUL005/master_local/model2.di")//
				.addContentToCopy("conflicts/PUL005/master_local/model2.uml") //
				.addContentToCopy("conflicts/PUL005/master_local/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add attribute1 under Class1" + EOL + "Add attribute2 under Class2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History </p3>
	 * 
	 * <pre>
	 *  * [origin/master]
	 *  |     Delete Class2
	 *  |  
	 *  [branch_c] Delete Class1
	 *  |    
	 *  | *  [master] (local)
	 *  |/   Add attribute1 under Class1
	 *  |    Add attribute2 under Class2
	 *  |   
	 *  [branch_a]
	 * Initial commit
	 *   - 1 project with 2 models, 2 diagrams
	 *   - add Class1 under Model1, Class2 under Model2
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL006_Remote() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL006");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/PUL006/branch_a/model.di")//
				.addContentToCopy("conflicts/PUL006/branch_a/model.uml") //
				.addContentToCopy("conflicts/PUL006/branch_a/model.notation") //
				.addContentToCopy("conflicts/PUL006/branch_a/model2.di")//
				.addContentToCopy("conflicts/PUL006/branch_a/model2.uml") //
				.addContentToCopy("conflicts/PUL006/branch_a/model2.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit" + EOL + "   - 1 project with 2 models, 2 diagrams" + EOL
				+ "   - add Class1 under Model1, Class2 under Model2");
		createBranch(branchA, "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/PUL006/branch_c/model.di")//
				.addContentToCopy("conflicts/PUL006/branch_c/model.uml") //
				.addContentToCopy("conflicts/PUL006/branch_c/model.notation") //
				.addContentToCopy("conflicts/PUL006/branch_c/model2.di")//
				.addContentToCopy("conflicts/PUL006/branch_c/model2.uml") //
				.addContentToCopy("conflicts/PUL006/branch_c/model2.notation") //
				.create(projectPath);

		String branchC = "branch_c";
		addAllAndCommit(" Delete Class1");
		createBranch(branchC, "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/PUL006/master_remote/model.di")//
				.addContentToCopy("conflicts/PUL006/master_remote/model.uml") //
				.addContentToCopy("conflicts/PUL006/master_remote/model.notation") //
				.addContentToCopy("conflicts/PUL006/master_remote/model2.di")//
				.addContentToCopy("conflicts/PUL006/master_remote/model2.uml") //
				.addContentToCopy("conflicts/PUL006/master_remote/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Delete Class2");
	}

	/**
	 * <h3>History </p3>
	 * 
	 * <pre>
	 *  * [origin/master]
	 *  |     Delete Class2
	 *  |  
	 *  [branch_c] Delete Class1
	 *  |    
	 *  | *  [master] (local)
	 *  |/   Add attribute1 under Class1
	 *  |    Add attribute2 under Class2
	 *  |   
	 *  [branch_a]
	 * Initial commit
	 *   - 1 project with 2 models, 2 diagrams
	 *   - add Class1 under Model1, Class2 under Model2
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL006_Local() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL006");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/PUL006/master_local/model.di")//
				.addContentToCopy("conflicts/PUL006/master_local/model.uml") //
				.addContentToCopy("conflicts/PUL006/master_local/model.notation") //
				.addContentToCopy("conflicts/PUL006/master_local/model2.di")//
				.addContentToCopy("conflicts/PUL006/master_local/model2.uml") //
				.addContentToCopy("conflicts/PUL006/master_local/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add attribute1 under Class1, attribute2 under Class2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Creates Attr2 in Class1.uml + Creates C3 in model.uml [origin/master]
	 * |
	 * | * Creates Attr1 in Class1.uml + Creates C2 in model.uml [master] (local)
	 * |/ 
	 * |  
	 * Creates C1 in Class1.uml [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL007_Remote() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL007");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/PUL007/branch_a/model.di")//
				.addContentToCopy("automerging/PUL007/branch_a/model.uml") //
				.addContentToCopy("automerging/PUL007/branch_a/model.notation") //
				.addContentToCopy("automerging/PUL007/branch_a/Class1.di")//
				.addContentToCopy("automerging/PUL007/branch_a/Class1.uml") //
				.addContentToCopy("automerging/PUL007/branch_a/Class1.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Creates C1 in Class1.uml");
		createBranch(branchA, "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/PUL007/master_remote/model.di")//
				.addContentToCopy("automerging/PUL007/master_remote/model.uml") //
				.addContentToCopy("automerging/PUL007/master_remote/model.notation") //
				.addContentToCopy("automerging/PUL007/master_remote/Class1.di")//
				.addContentToCopy("automerging/PUL007/master_remote/Class1.uml") //
				.addContentToCopy("automerging/PUL007/master_remote/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates Attr2 in Class1.uml + Creates C3 in model.uml");
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Creates Attr2 in Class1.uml + Creates C3 in model.uml [origin/master]
	 * |
	 * | * Creates Attr1 in Class1.uml + Creates C2 in model.uml [master] (local)
	 * |/ 
	 * |  
	 * Creates C1 in Class1.uml [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL007_Local() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL007");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/PUL007/master_local/model.di")//
				.addContentToCopy("automerging/PUL007/master_local/model.uml") //
				.addContentToCopy("automerging/PUL007/master_local/model.notation") //
				.addContentToCopy("automerging/PUL007/master_local/Class1.di")//
				.addContentToCopy("automerging/PUL007/master_local/Class1.uml") //
				.addContentToCopy("automerging/PUL007/master_local/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates Attr1 in Class1.uml + Creates C2 in model.uml");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Deletes C1 [origin/master]
	 * |
	 * | * Moves C1 to P2 [master] (local)
	 * |/ 
	 * |  
	 * Creates C1 in P1 & P2 [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL011_Remote() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL011");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/PUL011/branch_a/model.di")//
				.addContentToCopy("conflicts/PUL011/branch_a/model.uml") //
				.addContentToCopy("conflicts/PUL011/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Creates C1 in P1 & P2");
		createBranch(branchA, "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/PUL011/master_remote/model.di")//
				.addContentToCopy("conflicts/PUL011/master_remote/model.uml") //
				.addContentToCopy("conflicts/PUL011/master_remote/model.notation") //
				.create(projectPath);

		addAllAndCommit("Deletes C1");
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Deletes C1 [origin/master]
	 * |
	 * | * Moves C1 to P2 [master] (local)
	 * |/ 
	 * |  
	 * Creates C1 in P1 & P2 [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupPUL011_Local() throws Exception {
		projectPath = getRepositoryPath().resolve("PUL011");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/PUL011/master_local/model.di")//
				.addContentToCopy("conflicts/PUL011/master_local/model.uml") //
				.addContentToCopy("conflicts/PUL011/master_local/model.notation") //
				.create(projectPath);

		addAllAndCommit("Moves C1 to P2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Create Op1 in C1 [branch_b]
	 * |
	 * |
	 * | * Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt [branch_d,HEAD]
	 * | |
	 * | * Creates Int1 + Modifies in.txt & out.txt [branch_c]
	 * |/ 
	 * |  
	 * Creates C1 + Creates in.txt & out.txt [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupREB000() throws Exception {
		projectPath = getRepositoryPath().resolve("REB000");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/REB000/branch_a/model.di")//
				.addContentToCopy("automerging/REB000/branch_a/model.uml") //
				.addContentToCopy("automerging/REB000/branch_a/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1);

		addAllAndCommit("Creates C1 + Creates in.txt & out.txt");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/REB000/branch_b/model.di")//
				.addContentToCopy("automerging/REB000/branch_b/model.uml") //
				.addContentToCopy("automerging/REB000/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Creates Op1 in C1");

		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/REB000/branch_c/model.di")//
				.addContentToCopy("automerging/REB000/branch_c/model.uml") //
				.addContentToCopy("automerging/REB000/branch_c/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1 + LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2);

		addAllAndCommit("Creates Int1 + Modifies in.txt & out.txt");

		// Creates branch b
		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/REB000/branch_d/model.di")//
				.addContentToCopy("automerging/REB000/branch_d/model.uml") //
				.addContentToCopy("automerging/REB000/branch_d/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1 + LYRICS_2 + LYRICS_3) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3);

		addAllAndCommit("Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Creates ATTR1 in C1 + Modifies in.txt & out.txt [branch_b]
	 * |
	 * | * Deletes C1 + Modifies in.txt & out.txt [branch_c, HEAD]
	 * |/ 
	 * |  
	 * Creates C1 + Creates in.txt & out.txt [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupREB001() throws Exception {
		projectPath = getRepositoryPath().resolve("REB001");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/REB001/branch_a/model.di")//
				.addContentToCopy("conflicts/REB001/branch_a/model.uml") //
				.addContentToCopy("conflicts/REB001/branch_a/model.notation") //
				.addNewFileContent("in.txt", "") //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), "");

		addAllAndCommit("Creates C1 + Creates in.txt & out.txt");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB001/branch_b/model.di")//
				.addContentToCopy("conflicts/REB001/branch_b/model.uml") //
				.addContentToCopy("conflicts/REB001/branch_b/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1);

		addAllAndCommit("Creates ATTR1 in C1 + Modifies in.txt & out.txt");

		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB001/branch_c/model.di")//
				.addContentToCopy("conflicts/REB001/branch_c/model.uml") //
				.addContentToCopy("conflicts/REB001/branch_c/model.notation") //
				.addNewFileContent("in.txt", LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_2);

		addAllAndCommit("Deletes C1 + Modifies in.txt & out.txt");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Creates Attr1 in C1 & Attr2 in C2 + Modifies in.txt & out.txt [branch_b]
	 * |
	 * | * Deletes C2 + Modifies in.txt & out.txt [branch_c, HEAD]
	 * | |
	 * | * Deletes C1 + Modifies in.txt & out.txt [branch_c]
	 * |/ 
	 * |  
	 * Creates C1 & C2 + Creates in.txt & out.txt [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupREB002() throws Exception {
		projectPath = getRepositoryPath().resolve("REB002");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/REB002/branch_a/model.di")//
				.addContentToCopy("conflicts/REB002/branch_a/model.uml") //
				.addContentToCopy("conflicts/REB002/branch_a/model.notation") //
				.addNewFileContent("in.txt", "") //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), "");

		addAllAndCommit("Creates C1 & C2 + Creates in.txt & out.txt");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB002/branch_b/model.di")//
				.addContentToCopy("conflicts/REB002/branch_b/model.uml") //
				.addContentToCopy("conflicts/REB002/branch_b/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1);

		addAllAndCommit("Creates Attr1 in C1 & Attr2 in C2 + Modifies in.txt & out.txt");

		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB002/branch_c/model.di")//
				.addContentToCopy("conflicts/REB002/branch_c/model.uml") //
				.addContentToCopy("conflicts/REB002/branch_c/model.notation") //
				.addNewFileContent("in.txt", LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_2);

		addAllAndCommit("Deletes C1 + Modifies in.txt & out.txt");

		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB002/branch_d/model.di")//
				.addContentToCopy("conflicts/REB002/branch_d/model.uml") //
				.addContentToCopy("conflicts/REB002/branch_d/model.notation") //
				.addNewFileContent("in.txt", LYRICS_2 + LYRICS_3) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_2 + LYRICS_3);

		addAllAndCommit("Deletes C2 + Modifies in.txt & out.txt");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Creates C1 in P1 + adds content to in.txt & out.txt [branch_b] (no conflict with branch_c)
	 * |
	 * | * Creates C2 in P1 + adds content to in.txt & out.txt [branch_c,HEAD] (no conflict with branch_b)
	 * |/ 
	 * |  
	 * Creates P1 + Creates in.txt & out.txt [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupREB003() throws Exception {
		projectPath = getRepositoryPath().resolve("REB003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/REB003/branch_a/model.di")//
				.addContentToCopy("automerging/REB003/branch_a/model.uml") //
				.addContentToCopy("automerging/REB003/branch_a/model.notation") //
				.addNewFileContent("in.txt", LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_2);

		addAllAndCommit("Creates P1 + Creates in.txt & out.txt");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/REB003/branch_b/model.di")//
				.addContentToCopy("automerging/REB003/branch_b/model.uml") //
				.addContentToCopy("automerging/REB003/branch_b/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1 + LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2);

		addAllAndCommit("Creates C1 in P1 + adds content to in.txt & out.txt");

		// Creates branch_c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/REB003/branch_c/model.di")//
				.addContentToCopy("automerging/REB003/branch_c/model.uml") //
				.addContentToCopy("automerging/REB003/branch_c/model.notation") //
				.addNewFileContent("in.txt", LYRICS_2 + LYRICS_3) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_2 + LYRICS_3);

		addAllAndCommit("Creates C2 in P1 + adds content to in.txt & out.txt");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Creates Attr1 in Class1.uml + Creates C2 in model.uml [branch_b] (no conflict with branch_c)
	 * |
	 * | * Creates Attr2 in Class1.uml + Creates C3 in model.uml [branch_c,HEAD] (no conflict with branch_b)
	 * |/ 
	 * |  
	 * Creates C1 in Class1.uml [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupREB007() throws Exception {
		projectPath = getRepositoryPath().resolve("REB003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/REB007/branch_a/model.di")//
				.addContentToCopy("automerging/REB007/branch_a/model.uml") //
				.addContentToCopy("automerging/REB007/branch_a/model.notation") //
				.addContentToCopy("automerging/REB007/branch_a/Class1.di")//
				.addContentToCopy("automerging/REB007/branch_a/Class1.uml") //
				.addContentToCopy("automerging/REB007/branch_a/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates C1 in Class1.uml");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/REB007/branch_b/model.di")//
				.addContentToCopy("automerging/REB007/branch_b/model.uml") //
				.addContentToCopy("automerging/REB007/branch_b/model.notation") //
				.addContentToCopy("automerging/REB007/branch_b/Class1.di")//
				.addContentToCopy("automerging/REB007/branch_b/Class1.uml") //
				.addContentToCopy("automerging/REB007/branch_b/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates Attr1 in Class1.uml + Creates C2 in model.uml");

		// Creates branch_c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/REB007/branch_c/model.di")//
				.addContentToCopy("automerging/REB007/branch_c/model.uml") //
				.addContentToCopy("automerging/REB007/branch_c/model.notation") //
				.addContentToCopy("automerging/REB007/branch_c/Class1.di")//
				.addContentToCopy("automerging/REB007/branch_c/Class1.uml") //
				.addContentToCopy("automerging/REB007/branch_c/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates Attr2 in Class1.uml + Creates C3 in model.uml");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Deletes C2 [branch_d,HEAD]
	 * |
	 * * Deletes C1 (Deletes Class1.uml) [branch_c]
	 * |
	 * | * Creates Attr1 in C1 (Class1.uml) + Creates Attr2 in C2 (model.uml) [branch_b]
	 * |/ 
	 * |  
	 * Creates C1 in Class1.uml + Creates C2 in model.uml [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupREB009() throws Exception {
		projectPath = getRepositoryPath().resolve("REB009");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/REB009/branch_a/model.di")//
				.addContentToCopy("conflicts/REB009/branch_a/model.uml") //
				.addContentToCopy("conflicts/REB009/branch_a/model.notation") //
				.addContentToCopy("conflicts/REB009/branch_a/Class1.di")//
				.addContentToCopy("conflicts/REB009/branch_a/Class1.uml") //
				.addContentToCopy("conflicts/REB009/branch_a/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates C1 in Class1.uml + Creates C2 in model.uml");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB009/branch_b/model.di")//
				.addContentToCopy("conflicts/REB009/branch_b/model.uml") //
				.addContentToCopy("conflicts/REB009/branch_b/model.notation") //
				.addContentToCopy("conflicts/REB009/branch_b/Class1.di")//
				.addContentToCopy("conflicts/REB009/branch_b/Class1.uml") //
				.addContentToCopy("conflicts/REB009/branch_b/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates Attr1 in C1 (Class1.uml) + Creates Attr2 in C2 (model.uml)");

		// Creates branch_c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB009/branch_c/model.di")//
				.addContentToCopy("conflicts/REB009/branch_c/model.uml") //
				.addContentToCopy("conflicts/REB009/branch_c/model.notation") //
				.create(projectPath);

		git.rm()//
				.addFilepattern("REB009/Class1.di") //
				.addFilepattern("REB009/Class1.uml") //
				.addFilepattern("REB009/Class1.notation") //
				.call();
		addAllAndCommit("Deletes C1 (Deletes Class1.uml)");

		// Creates branch_d
		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB009/branch_d/model.di")//
				.addContentToCopy("conflicts/REB009/branch_d/model.uml") //
				.addContentToCopy("conflicts/REB009/branch_d/model.notation") //
				.create(projectPath);

		addAllAndCommit("Deletes C2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Deletes C1 [branch_c, HEAD]
	 * |
	 * | * Moves C1 to P2 [branch_b]
	 * |/ 
	 * |  
	 * Creates C1 in P1 & P2 [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupREB011() throws Exception {
		projectPath = getRepositoryPath().resolve("REB011");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/REB011/branch_a/model.di")//
				.addContentToCopy("conflicts/REB011/branch_a/model.uml") //
				.addContentToCopy("conflicts/REB011/branch_a/model.notation") //
				.create(projectPath);

		addAllAndCommit("Creates C1 in P1 & P2");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB011/branch_b/model.di")//
				.addContentToCopy("conflicts/REB011/branch_b/model.uml") //
				.addContentToCopy("conflicts/REB011/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Moves C1 to P2");

		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB011/branch_c/model.di")//
				.addContentToCopy("conflicts/REB011/branch_c/model.uml") //
				.addContentToCopy("conflicts/REB011/branch_c/model.notation") //
				.create(projectPath);

		addAllAndCommit("Deletes C1");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Deletes C2[branch_c, HEAD]
	 * |
	 * | * Creates association between C1 & C2 [branch_b]
	 * |/ 
	 * |  
	 * Creates C1 in P1 in P1.uml && C2 in P2 in P2.uml[branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupREB014() throws Exception {
		projectPath = getRepositoryPath().resolve("REB014");
		project = new ProjectBuilder(this) //
				.addContentToCopy("conflicts/REB014/branch_a/model.di")//
				.addContentToCopy("conflicts/REB014/branch_a/model.uml") //
				.addContentToCopy("conflicts/REB014/branch_a/model.notation") //
				.addContentToCopy("conflicts/REB014/branch_a/P1.di")//
				.addContentToCopy("conflicts/REB014/branch_a/P1.uml") //
				.addContentToCopy("conflicts/REB014/branch_a/P1.notation") //
				.addContentToCopy("conflicts/REB014/branch_a/P2.di")//
				.addContentToCopy("conflicts/REB014/branch_a/P2.uml") //
				.addContentToCopy("conflicts/REB014/branch_a/P2.notation") //
				.create(projectPath);

		addAllAndCommit("Creates C1 in P1 in P1.uml && C2 in P2 in P2.uml");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB014/branch_b/model.di")//
				.addContentToCopy("conflicts/REB014/branch_b/model.uml") //
				.addContentToCopy("conflicts/REB014/branch_b/model.notation") //
				.addContentToCopy("conflicts/REB014/branch_b/P1.di")//
				.addContentToCopy("conflicts/REB014/branch_b/P1.uml") //
				.addContentToCopy("conflicts/REB014/branch_b/P1.notation") //
				.addContentToCopy("conflicts/REB014/branch_b/P2.di")//
				.addContentToCopy("conflicts/REB014/branch_b/P2.uml") //
				.addContentToCopy("conflicts/REB014/branch_b/P2.notation") //
				.create(projectPath);

		addAllAndCommit("Creates association between C1 & C2");

		// Creates branch_c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("conflicts/REB014/branch_c/model.di")//
				.addContentToCopy("conflicts/REB014/branch_c/model.uml") //
				.addContentToCopy("conflicts/REB014/branch_c/model.notation") //
				.addContentToCopy("conflicts/REB014/branch_c/P1.di")//
				.addContentToCopy("conflicts/REB014/branch_c/P1.uml") //
				.addContentToCopy("conflicts/REB014/branch_c/P1.notation") //
				.addContentToCopy("conflicts/REB014/branch_c/P2.di")//
				.addContentToCopy("conflicts/REB014/branch_c/P2.uml") //
				.addContentToCopy("conflicts/REB014/branch_c/P2.notation") //
				.create(projectPath);

		addAllAndCommit("Deletes C2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}

	/**
	 * <h3>History</h3>
	 * 
	 * <pre>
	 * * Creates C1 in P1 [branch_b]
	 * |
	 * | * Adds in.txt && out.txt [branch_c,HEAD]
	 * |/ 
	 * |  
	 * Creates P1 [branch_a]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void setupREB016() throws Exception {
		projectPath = getRepositoryPath().resolve("REB016");
		project = new ProjectBuilder(this) //
				.addContentToCopy("automerging/REB016/branch_a/model.di")//
				.addContentToCopy("automerging/REB016/branch_a/model.uml") //
				.addContentToCopy("automerging/REB016/branch_a/model.notation") //
				.create(projectPath);

		addAllAndCommit("Creates P1");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/REB016/branch_b/model.di")//
				.addContentToCopy("automerging/REB016/branch_b/model.uml") //
				.addContentToCopy("automerging/REB016/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Creates C1 in P1");

		// Creates branch_c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("automerging/REB016/branch_a/model.di")//
				.addContentToCopy("automerging/REB016/branch_a/model.uml") //
				.addContentToCopy("automerging/REB016/branch_a/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1);

		addAllAndCommit("Adds in.txt && out.txt");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);
	}
}

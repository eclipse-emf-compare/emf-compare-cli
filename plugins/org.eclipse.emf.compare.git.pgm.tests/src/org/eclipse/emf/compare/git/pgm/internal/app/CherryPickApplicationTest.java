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

import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.util.ProjectBuilder;
import org.eclipse.equinox.app.IApplication;
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
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

/**
 * Test of {@link LogicalCherryPickApplication}.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
public class CherryPickApplicationTest extends AbstractLogicalCommandApplicationTest {

	private File project;

	private File userSetupFile;

	private Path projectPath;

	/**
	 * <p>
	 * This use case aims to test a logical cherry-pick on a model with successive conflicts.
	 * </p>
	 * <p>
	 * History see {@link #setupCHER002()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li> <code> logicalcherry-pick branch_c branch_d </code></li>
	 * <li> <code> logicalmergetool (mocks conflict resolution)</code></li>
	 * <li> <code> logicalcherry-pick --continue </code></li>
	 * <li> <code> logicalmergetool (mocks conflict resolution)</code></li>
	 * <li> <code> logicalcherry-pick --continue </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("nls")
	@Test
	public void testCHER002() throws Exception {
		setupCHER002();

		runCherryPick(Returns.ABORTED, "branch_c", "branch_d");

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_c") + "]... Delete C1"));

		assertNoConflitMarker(projectPath.resolve("model.uml"), projectPath.resolve("model.notation"));

		// Resolves conflits
		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.di")//
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.uml") //
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.notation") //
				.create(projectPath);

		getGit().add().addFilepattern(".").call(); //$NON-NLS-1$

		runContinue(Returns.ABORTED);

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_d") + "]... Delete C2",
				"[" + getShortId("HEAD") + "] Delete C1"));

		// Resolves conflicts
		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution2/model.di")//
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution2/model.uml") //
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution2/model.notation") //
				.create(projectPath);

		getGit().add().addFilepattern(".").call(); //$NON-NLS-1$

		runContinue(Returns.COMPLETE);

		assertOutputMessageEnd(getCompleteMessage("[" + getShortId("HEAD") + "] Delete C2"));

		List<RevCommit> revCommits = Lists.newArrayList(getGit().log().setMaxCount(3).add(getHeadCommit())
				.call());
		assertEquals("Delete C2", revCommits.get(0).getShortMessage());
		assertEquals("Delete C1", revCommits.get(1).getShortMessage());
		assertEquals("Adds Attr1 to C1 and adds Attr2 to C2", revCommits.get(2).getShortMessage());
	}

	/**
	 * <p>
	 * This use case aims to test a logical cherry-pick on a model with successive conflicts. The resolution
	 * of the second conflict finishes with the exact same model that was in the index before the cherry pick.
	 * In this case the user is expected to use the command --quit.
	 * </p>
	 * <p>
	 * History see {@link #setupCHER002()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li> <code> logicalcherry-pick branch_c branch_d </code></li>
	 * <li> <code> logicalmergetool (mocks conflict resolution)</code></li>
	 * <li> <code> logicalcherry-pick --continue </code></li>
	 * <li> <code> logicalmergetool (mocks conflict resolution)</code></li>
	 * <li> <code> logicalcherry-pick --quit </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("nls")
	@Test
	public void testCHER002_nothingToCommit() throws Exception {
		setupCHER002();

		runCherryPick(Returns.ABORTED, "branch_c", "branch_d");

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_c") + "]... Delete C1"));

		assertNoConflitMarker(projectPath.resolve("model.uml"), projectPath.resolve("model.notation"));

		// Resolve conflicts
		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.di")//
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.uml") //
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.notation") //
				.create(projectPath);

		getGit().add().addFilepattern(".").call(); //$NON-NLS-1$

		runContinue(Returns.ABORTED);

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_d") + "]... Delete C2",
				"[" + getShortId("HEAD") + "] Delete C1"));

		// Resolve conflicts by reverting changes to previsous version
		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.di")//
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.uml") //
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.notation") //
				.create(projectPath);

		getGit().add().addFilepattern(".").call(); //$NON-NLS-1$

		runContinue(Returns.ABORTED);

		String expected = "No changes detected" + EOL;
		expected += EOL;
		expected += "If there is nothing left to stage, chances are that something" + EOL;
		expected += "else already introduced the same changes; you might want to skip" + EOL;
		expected += "this patch using git logicalmerge --quit" + EOL + EOL;

		assertOutputMessageEnd(expected);

		runQuit(Returns.COMPLETE);

		assertOutputMessageEnd("Complete." + EOL);

		List<RevCommit> revCommits = Lists.newArrayList(getGit().log().setMaxCount(2).add(getHeadCommit())
				.call());
		assertEquals("Delete C1", revCommits.get(0).getShortMessage());
		assertEquals("Adds Attr1 to C1 and adds Attr2 to C2", revCommits.get(1).getShortMessage());
	}

	/**
	 * <p>
	 * This use case aims to test a logical cherry-pick on a model with successive conflicts. In this test the
	 * user use the --abort option.
	 * </p>
	 * <p>
	 * History see {@link #setupCHER002()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li> <code> logicalcherry-pick branch_c branch_d </code></li>
	 * <li> <code> logicalcherry-pick --abort </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("nls")
	@Test
	public void testCHER002_aborted() throws Exception {
		setupCHER002();

		String previousHead = getShortId("HEAD");

		runCherryPick(Returns.ABORTED, "branch_c", "branch_d");

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_c") + "]... Delete C1"));

		assertNoConflitMarker(projectPath.resolve("model.uml"), projectPath.resolve("model.notation"));

		runAbort(Returns.ABORTED);

		assertTrue("The repository should be clean", getGit().status().call().isClean());

		assertEquals(getShortId("HEAD"), previousHead);

		assertOutputMessageEnd("Aborted." + EOL);
	}

	/**
	 * <p>
	 * This use case aims to test a logical cherry-pick on a model with successive conflicts.In this test the
	 * user use the --quit option.
	 * </p>
	 * <p>
	 * History see {@link #setupCHER002()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li> <code> logicalcherry-pick branch_c branch_d </code></li>
	 * <li> <code> logicalcherry-pick --quit </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("nls")
	@Test
	public void testCHER002_quit() throws Exception {
		setupCHER002();

		runCherryPick(Returns.ABORTED, "branch_c", "branch_d");

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_c") + "]... Delete C1"));

		assertNoConflitMarker(projectPath.resolve("model.uml"), projectPath.resolve("model.notation"));

		// Resolve conflicts
		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.di")//
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.uml") //
				.addContentToCopy("data/conflicts/CHER002/conflict/resolution1/model.notation") //
				.create(projectPath);

		getGit().add().addFilepattern(".").call(); //$NON-NLS-1$

		runQuit(Returns.ABORTED);

		// Expects second conflict
		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_d") + "]... Delete C2"));
	}

	/**
	 * <p>
	 * This use case aims to test a logical cherry-pick on a model with no conflict (Auto merging should
	 * succeed). It also aims to test to cherry pick a commit using both its id and the name of the matching
	 * branch.
	 * </p>
	 * <p>
	 * History see {@link #setupCHER003()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li><code> logicalcherry-pick branch_c branch_d </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("nls")
	@Test
	public void testCHER003() throws Exception {
		RevCommit branchCLastCommit = setupCHER003();

		runCherryPick(Returns.COMPLETE, branchCLastCommit.getName()// uses commit id
				, "branch_d"/* uses branch name */);

		assertOutputMessageEnd(getCompleteMessage("[" + getShortId("HEAD") + "] Adds class 3",//
				"[" + getShortId("HEAD~1") + "] Adds class 2"));

		final String class1URIFragment = "_bB2fYC3HEeSN_5D5iyrZGQ";
		final String class2URIFragment = "_hfIr4C3HEeSN_5D5iyrZGQ";
		final String class3URIFragment = "_aDUsIGWiEeSuO4qBAOfkWA";
		assertExistInResource(project.toPath().resolve("model.uml"), class1URIFragment, class2URIFragment,
				class3URIFragment);

		final String class2ShapeURIFragment = "_hfJS8C3HEeSN_5D5iyrZGQ";
		final String class1ShapeURIFragment = "_bB3tgC3HEeSN_5D5iyrZGQ";
		final String class3ShapeURIFragement = "_aGWK8GWiEeSuO4qBAOfkWA";
		assertExistInResource(project.toPath().resolve("model.notation"), class1ShapeURIFragment,
				class2ShapeURIFragment, class3ShapeURIFragement);
	}

	/**
	 * <p>
	 * Tests the "up to date" use case.
	 * </p>
	 * <p>
	 * History see {@link #setupCHER003()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li><code> logicalcherry-pick branch_b </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("nls")
	@Test
	public void testCHER003_upToDate() throws Exception {
		setupCHER003();

		runCherryPick(Returns.COMPLETE, "branch_b");

		assertOutputMessageEnd("Fast forward." + EOL + EOL);

	}

	/**
	 * <p>
	 * This use case aims to test a logical cherry-pick on a model with fragment.
	 * </p>
	 * <p>
	 * History see {@link #setupCHER004()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li><code> logicalcherry-pick branch_c </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("nls")
	@Test
	public void testCHER004() throws Exception {
		setupCHER004();

		runCherryPick(Returns.COMPLETE, "branch_c");

		assertOutputMessageEnd(getCompleteMessage("[" + getShortId("HEAD")
				+ "] Add Class3 under Model1 Add Class4 under Model2"));

		final String class1URIFragment = "_adib0C9QEeShUolneTgohg";
		final String class3URIFragment = "_lztC0C9QEeShUolneTgohg";
		assertExistInResource(project.toPath().resolve("model.uml"), class1URIFragment, class3URIFragment);

		final String class2URIFragment = "_a7N2UC9QEeShUolneTgohg";
		final String class4URIFragment = "_m3mv0C9QEeShUolneTgohg";
		assertExistInResource(project.toPath().resolve("model2.uml"), class2URIFragment, class4URIFragment);

		final String class1ShapeURIFragment = "_adjp8C9QEeShUolneTgohg";
		final String class3ShapeURIFragment = "_lzuQ8C9QEeShUolneTgohg";
		assertExistInResource(project.toPath().resolve("model.notation"), class1ShapeURIFragment,
				class3ShapeURIFragment);

		final String class2ShapeURIFragment = "_a7PEcC9QEeShUolneTgohg";
		final String class4ShapeURIFragment = "_m3nW4C9QEeShUolneTgohg";
		assertExistInResource(project.toPath().resolve("model2.notation"), class2ShapeURIFragment,
				class4ShapeURIFragment);
	}

	/**
	 * <h3>Test CHER006</h3>
	 * <p>
	 * Successives conflicts on multiple models in multiple files (one file per model).
	 * </p>
	 * <p>
	 * History see {@link #setupCHER006()}
	 * </p>
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("nls")
	@Test
	public void testCHER006() throws Exception {
		setupCHER006();

		runCherryPick(Returns.ABORTED, "branch_c", "branch_d");

		assertNoConflitMarker(projectPath.resolve("model.uml"), projectPath.resolve("model.notation"),
				projectPath.resolve("model2.uml"), projectPath.resolve("model2.notation"));

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_c") + "]... Delete Class1"));

		Set<String> expectedConflictingFilePath = Sets
				.newHashSet("MER006/model.uml", "MER006/model.notation");
		assertEquals(expectedConflictingFilePath, getGit().status().call().getConflicting());

		// Resolve conflicts
		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/MER006/conflict/resolution1/model.di")//
				.addContentToCopy("data/conflicts/MER006/conflict/resolution1/model.uml") //
				.addContentToCopy("data/conflicts/MER006/conflict/resolution1/model.notation") //
				.create(projectPath);

		getGit().add().addFilepattern(".").call(); //$NON-NLS-1$

		runContinue(Returns.ABORTED);

		assertOutputMessageEnd(getExpectedConflictMessage(
				"[" + getShortId("branch_d") + "]... Delete Class2", //
				"[" + getShortId("HEAD") + "] Delete Class1"));

		expectedConflictingFilePath = Sets.newHashSet("MER006/model2.uml", "MER006/model2.notation");
		assertEquals(expectedConflictingFilePath, getGit().status().call().getConflicting());

		// Resolves conflicts
		getGit().add().addFilepattern(".").call(); //$NON-NLS-1$

		runContinue(Returns.COMPLETE);

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
	 * @throws IOException
	 * @throws GitAPIException
	 * @throws NoFilepatternException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathsException
	 * @throws ConcurrentRefUpdateException
	 * @throws WrongRepositoryStateException
	 * @throws RefAlreadyExistsException
	 * @throws RefNotFoundException
	 * @throws InvalidRefNameException
	 * @throws CheckoutConflictException
	 */
	@SuppressWarnings("nls")
	private void setupCHER006() throws IOException, GitAPIException, NoFilepatternException, NoHeadException,
			NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, RefAlreadyExistsException, RefNotFoundException,
			InvalidRefNameException, CheckoutConflictException {
		projectPath = getRepositoryPath().resolve("MER006");
		project = new ProjectBuilder(this) //
				.addContentToCopy("data/conflicts/MER006/branch_a/model.di")//
				.addContentToCopy("data/conflicts/MER006/branch_a/model.uml") // //$NON-NLS-1$
				.addContentToCopy("data/conflicts/MER006/branch_a/model.notation") //
				.addContentToCopy("data/conflicts/MER006/branch_a/model2.di")//
				.addContentToCopy("data/conflicts/MER006/branch_a/model2.uml") //
				.addContentToCopy("data/conflicts/MER006/branch_a/model2.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit" + EOL + "   - 1 project with 2 models, 2 diagrams" + EOL
				+ "   - add Class1 under Model1, Class2 under Model2");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/MER006/branch_c/model.di")//
				.addContentToCopy("data/conflicts/MER006/branch_c/model.uml") //
				.addContentToCopy("data/conflicts/MER006/branch_c/model.notation") //
				.addContentToCopy("data/conflicts/MER006/branch_c/model2.di")//
				.addContentToCopy("data/conflicts/MER006/branch_c/model2.uml") //
				.addContentToCopy("data/conflicts/MER006/branch_c/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Delete Class1");

		// Creates branch d
		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/MER006/branch_d/model.di")//
				.addContentToCopy("data/conflicts/MER006/branch_d/model.uml") //
				.addContentToCopy("data/conflicts/MER006/branch_d/model.notation") //
				.addContentToCopy("data/conflicts/MER006/branch_d/model2.di")//
				.addContentToCopy("data/conflicts/MER006/branch_d/model2.uml") //
				.addContentToCopy("data/conflicts/MER006/branch_d/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Delete Class2");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/MER006/branch_b/model.di")//
				.addContentToCopy("data/conflicts/MER006/branch_b/model.uml") //
				.addContentToCopy("data/conflicts/MER006/branch_b/model.notation") //
				.addContentToCopy("data/conflicts/MER006/branch_b/model2.di")//
				.addContentToCopy("data/conflicts/MER006/branch_b/model2.uml") //
				.addContentToCopy("data/conflicts/MER006/branch_b/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add attribute1 under Class1, attribute2 under Class2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

		// Mocks that the commands is lauched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());
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
	 * @throws IOException
	 * @throws NoFilepatternException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathsException
	 * @throws ConcurrentRefUpdateException
	 * @throws WrongRepositoryStateException
	 * @throws GitAPIException
	 */
	@SuppressWarnings("nls")
	private void setupCHER002() throws IOException, NoFilepatternException, NoHeadException,
			NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, GitAPIException {
		projectPath = getRepositoryPath().resolve("CHER002");
		project = new ProjectBuilder(this) //
				.addContentToCopy("data/conflicts/CHER002/branch_a/model.di")//
				.addContentToCopy("data/conflicts/CHER002/branch_a/model.uml") //
				.addContentToCopy("data/conflicts/CHER002/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit - Create C1 and C2");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHER002/branch_c/model.di")//
				.addContentToCopy("data/conflicts/CHER002/branch_c/model.uml") //
				.addContentToCopy("data/conflicts/CHER002/branch_c/model.notation") //
				.create(projectPath);

		addAllAndCommit("Delete C1");

		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHER002/branch_d/model.di")//
				.addContentToCopy("data/conflicts/CHER002/branch_d/model.uml") //
				.addContentToCopy("data/conflicts/CHER002/branch_d/model.notation") //
				.create(projectPath);

		addAllAndCommit("Delete C2");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHER002/branch_b/model.di")//
				.addContentToCopy("data/conflicts/CHER002/branch_b/model.uml") //
				.addContentToCopy("data/conflicts/CHER002/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds Attr1 to C1 and adds Attr2 to C2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

		// Mocks that the command is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());
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
	 * @throws IOException
	 * @throws GitAPIException
	 * @throws NoFilepatternException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathsException
	 * @throws ConcurrentRefUpdateException
	 * @throws WrongRepositoryStateException
	 * @throws RefAlreadyExistsException
	 * @throws RefNotFoundException
	 * @throws InvalidRefNameException
	 * @throws CheckoutConflictException
	 */
	@SuppressWarnings("nls")
	private void setupCHER004() throws IOException, GitAPIException, NoFilepatternException, NoHeadException,
			NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, RefAlreadyExistsException, RefNotFoundException,
			InvalidRefNameException, CheckoutConflictException {
		projectPath = getRepositoryPath().resolve("MER004");
		project = new ProjectBuilder(this) //
				.addContentToCopy("data/automerging/MER004/branch_a/model.di")//
				.addContentToCopy("data/automerging/MER004/branch_a/model.uml") //
				.addContentToCopy("data/automerging/MER004/branch_a/model.notation") //
				.addContentToCopy("data/automerging/MER004/branch_a/model2.di")//
				.addContentToCopy("data/automerging/MER004/branch_a/model2.uml") //
				.addContentToCopy("data/automerging/MER004/branch_a/model2.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit" + EOL + "- A project with 2 models, 2 diagrams");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/automerging/MER004/branch_c/model.di")//
				.addContentToCopy("data/automerging/MER004/branch_c/model.uml") //
				.addContentToCopy("data/automerging/MER004/branch_c/model.notation") //
				.addContentToCopy("data/automerging/MER004/branch_c/model2.di")//
				.addContentToCopy("data/automerging/MER004/branch_c/model2.uml") //
				.addContentToCopy("data/automerging/MER004/branch_c/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add Class3 under Model1" + EOL + "Add Class4 under Model2");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/automerging/MER004/branch_b/model.di")//
				.addContentToCopy("data/automerging/MER004/branch_b/model.uml") //
				.addContentToCopy("data/automerging/MER004/branch_b/model.notation") //
				.addContentToCopy("data/automerging/MER004/branch_b/model2.di")//
				.addContentToCopy("data/automerging/MER004/branch_b/model2.uml") //
				.addContentToCopy("data/automerging/MER004/branch_b/model2.notation") //
				.create(projectPath);

		addAllAndCommit("Add Class1 under Model1" + EOL + "Add Class2 under Model2");

		getGit().close();

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

		// Mocks that the command is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());
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
	 * @throws IOException
	 * @throws GitAPIException
	 * @throws NoFilepatternException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathsException
	 * @throws ConcurrentRefUpdateException
	 * @throws WrongRepositoryStateException
	 * @throws RefAlreadyExistsException
	 * @throws RefNotFoundException
	 * @throws InvalidRefNameException
	 * @throws CheckoutConflictException
	 */
	@SuppressWarnings("nls")
	private RevCommit setupCHER003() throws IOException, GitAPIException, NoFilepatternException,
			NoHeadException, NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, RefAlreadyExistsException, RefNotFoundException,
			InvalidRefNameException, CheckoutConflictException {
		projectPath = getRepositoryPath().resolve("CHER003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("data/automerging/CHER003/branch_a/model.di")//
				.addContentToCopy("data/automerging/CHER003/branch_a/model.uml") //
				.addContentToCopy("data/automerging/CHER003/branch_a/model.notation") //
				.create(projectPath);
		String branchA = "branch_a";
		addAllAndCommit("Initial commit");
		createBranch(branchA, "master");

		// Creates branch c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/automerging/CHER003/branch_c/model.di")//
				.addContentToCopy("data/automerging/CHER003/branch_c/model.uml") //
				.addContentToCopy("data/automerging/CHER003/branch_c/model.notation") //
				.create(projectPath);

		RevCommit branchCLastCommit = addAllAndCommit("Adds class 2");

		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/automerging/CHER003/branch_d/model.di")//
				.addContentToCopy("data/automerging/CHER003/branch_d/model.uml") //
				.addContentToCopy("data/automerging/CHER003/branch_d/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds class 3");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/automerging/CHER003/branch_b/model.di")//
				.addContentToCopy("data/automerging/CHER003/branch_b/model.uml") //
				.addContentToCopy("data/automerging/CHER003/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Adds class 1");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

		// Mocks that the command is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());
		return branchCLastCommit;
	}

	@Override
	protected IApplication buildApp() {
		return new LogicalCherryPickApplication();
	}

	@Override
	protected LogicalCherryPickApplication getApp() {
		return (LogicalCherryPickApplication)super.getApp();
	}

	private void runCommand(Returns expectedReturnCode) throws Exception {
		resetApp();
		// Runs command
		Object result = getApp().start(getContext());

		printOut();
		printErr();

		assertEquals(expectedReturnCode.code(), result);
		IProject[] projectInWorkspace = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		assertEquals(1, projectInWorkspace.length);
	}

	private void runCherryPick(Returns expectedReturnCode, String... commitsToCherryPick) throws Exception {
		resetContext();

		getContext().addArg(getRepositoryPath().resolve(".git").toString(), userSetupFile.getAbsolutePath(),
				"--show-stack-trace");
		getContext().addArg(commitsToCherryPick);
		runCommand(expectedReturnCode);
	}

	@SuppressWarnings("nls")
	private void runContinue(Returns expectedReturnCode) throws Exception {
		resetContext();
		getContext().addArg(getRepositoryPath().resolve(".git").toString(), userSetupFile.getAbsolutePath(),
				"--show-stack-trace", "--continue");
		runCommand(expectedReturnCode);

	}

	@SuppressWarnings("nls")
	private void runQuit(Returns expectedReturnCode) throws Exception {
		resetContext();
		getContext().addArg(getRepositoryPath().resolve(".git").toString(), userSetupFile.getAbsolutePath(),
				"--show-stack-trace", "--quit");
		runCommand(expectedReturnCode);
	}

	@SuppressWarnings("nls")
	private void runAbort(Returns expectedReturnCode) throws Exception {
		resetContext();
		getContext().addArg(getRepositoryPath().resolve(".git").toString(), userSetupFile.getAbsolutePath(),
				"--show-stack-trace", "--abort");
		runCommand(expectedReturnCode);
	}

	private String getShortId(String ref) throws RevisionSyntaxException, AmbiguousObjectException,
			IncorrectObjectTypeException, IOException {
		ObjectId resolved = getGit().getRepository().resolve(ref);
		return getShortId(resolved);
	}

	private String getShortId(ObjectId resolved) {
		return resolved.abbreviate(7).name();
	}

	@SuppressWarnings("nls")
	private String getExpectedConflictMessage(String conflictingCommitMessage,
			String... successfulCommitMessages) {
		String expected = "";
		if (successfulCommitMessages.length > 0) {
			expected += "The following revisions were successfully cherry-picked:" + EOL;
			for (String successfull : successfulCommitMessages) {
				expected += "	" + successfull + EOL;
			}
		}
		expected += "error: Could not apply " + conflictingCommitMessage + EOL;
		expected += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		expected += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		expected += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		expected += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		expected += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		expected += "hint:  git logical-cherrypick --continue : to continue the cherry pick" + EOL;
		expected += "hint:  git logical-cherrypick --abort : to abort the cherry pick" + EOL;
		expected += "hint:  git logical-cherrypick --quit : to skip this commit" + EOL + EOL;
		return expected;
	}

	@SuppressWarnings("nls")
	private String getCompleteMessage(String... successfulCommits) {
		String expected = "The following revisions were successfully cherry-picked:" + EOL;
		for (String success : successfulCommits) {
			expected += "	" + success + EOL;
		}
		expected += "Complete." + EOL;
		return expected;
	}

}

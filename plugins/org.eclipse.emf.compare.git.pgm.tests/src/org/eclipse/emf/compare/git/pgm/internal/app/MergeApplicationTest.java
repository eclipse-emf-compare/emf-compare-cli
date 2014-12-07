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

import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.CURRENT;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.PARENT;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.app.data.ContextSetup;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

/**
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public class MergeApplicationTest extends AbstractApplicationTest {

	private ContextSetup contextSetup;

	/**
	 * <h3>Use case MER001</h3>
	 * <p>
	 * This use case is used to achieve a conflicting merge
	 * </p>
	 * 
	 * @see ContextSetup#setupMER001()
	 * @throws Exception
	 */
	@Test
	public void testMER001() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupMER001();

		runMerge(Returns.ABORTED, getShortId("branch_c"));

		StringBuilder expectedOut = new StringBuilder();
		expectedOut.append("Auto-merging failed in ").append("MER001/model.notation").append(EOL);
		expectedOut.append("Auto-merging failed in ").append("MER001/model.uml").append(EOL);
		expectedOut.append("Automatic merge failed; fix conflicts and then commit the result.").append(EOL)
				.append(EOL);
		assertOutputMessageEnd(expectedOut.toString());

		assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"), contextSetup
				.getProjectPath().resolve("model.notation"));

		Set<String> expectedConflictingFilePath = Sets
				.newHashSet("MER001/model.uml", "MER001/model.notation");
		assertEquals(expectedConflictingFilePath, getGit().status().call().getConflicting());
	}

	/**
	 * <h3>Test use case MER002</h3>
	 * <p>
	 * This aims to test a merge between two branches with more than commit between them (see history)
	 * </p>
	 * 
	 * @see ContextSetup#setupMER002()
	 */
	@Test
	public void testMER002() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupMER002();

		runMerge(Returns.ABORTED, getShortId("branch_d"));

		StringBuilder expectedOut = new StringBuilder();
		expectedOut.append("Auto-merging failed in ").append("MER002/model.notation").append(EOL);
		expectedOut.append("Auto-merging failed in ").append("MER002/model.uml").append(EOL);
		expectedOut.append("Automatic merge failed; fix conflicts and then commit the result.").append(EOL)
				.append(EOL);
		assertOutputMessageEnd(expectedOut.toString());

		assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"), contextSetup
				.getProjectPath().resolve("model.notation"));

		Set<String> expectedConflictingFilePath = Sets
				.newHashSet("MER002/model.uml", "MER002/model.notation");
		assertEquals(expectedConflictingFilePath, getGit().status().call().getConflicting());

	}

	/**
	 * <h3>Test the logical merge application on the current branch</h3>
	 * <p>
	 * This use case aims to produce a "Already up to date message".
	 * </p>
	 * <h3>History:</h3>
	 * 
	 * <pre>
	 * Initial commit (PapyrusProject3) [Master]
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMER003_alreadyUpToDate0() throws Exception {
		// Mocks that the commands is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());

		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupMER003_alreadyUpToDate0();

		runMerge(Returns.COMPLETE, getShortId("master"));

		assertTrue(getGit().status().call().isClean());
		assertEquals(getGit().getRepository().resolve("HEAD").getName(), getLongId("master"));
		assertOutputMessageEnd("Already up to date." + EOL + EOL);
	}

	/**
	 * <h3>Test the logical application on previous commit of the current branch</h3> <h3>History:</h3>
	 * 
	 * <pre>
	 * * Adds Class 1 [branch_b]
	 * |    
	 * |  
	 * Initial commit (PapyrusProject3) [branch_a]
	 * 
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMER003_alreadyUpToDate1() throws Exception {
		// Mocks that the commands is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());

		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupMER003_alreadyUpToDate1();

		runMerge(Returns.COMPLETE, getShortId("branch_a"));

		assertOutputMessageEnd("Already up to date." + EOL + EOL);

		assertTrue(getGit().status().call().isClean());
		assertEquals(getGit().getRepository().resolve("HEAD").getName(), getLongId("branch_b"));
	}

	/**
	 * <h3>Test with a setup file that references incorrect projects.</h3>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMER003_IncorrectProjectToImport_NotExistingProject() throws Exception {
		// Mocks that the commands is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());

		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupMER003_IncorrectProjectToImport_NotExistingProject();

		// Sets args
		getContext().addArg(getRepositoryPath().resolve(".git").toString(),
				contextSetup.getUserSetupFile().getAbsolutePath(), getShortId("branch_a"));

		runCommand(Returns.ERROR);

		StringBuilder expectedOut = new StringBuilder();
		expectedOut.append("fatal: Projects Import Analysis Projects Import Analysis of '").append(
				getRepositoryPath().resolve("GhostProject")).append("'").append(EOL);
		expectedOut.append("  The root folder '").append(getRepositoryPath().resolve("GhostProject")).append(
				"' doesn't exist").append(EOL).append(EOL);

		assertOutputMessageEnd(expectedOut.toString());
	}

	/**
	 * Test importing a project with a real complexe path.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMER003_ProjectToImport_complexPath() throws Exception {
		// Mocks that the commands is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());

		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupMER003_ProjectToImport_complexPath();

		runMerge(Returns.COMPLETE, getShortId("branch_a"));

		StringBuilder expectedOut = new StringBuilder();
		expectedOut.append("Already up to date.").append(EOL).append(EOL);
		assertOutputMessageEnd(expectedOut.toString());
	}

	/**
	 * Test using a setup file using a complex path
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMER003_SetupFile_complexPath() throws Exception {
		// Mocks that the commands is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());

		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupMER003_SetupFile_complexPath();

		runMerge(Returns.COMPLETE, getShortId("branch_a"));

		StringBuilder expectedOut = new StringBuilder();
		expectedOut.append("Already up to date.").append(EOL).append(EOL);
		assertOutputMessageEnd(expectedOut.toString());

	}

	/**
	 * Test using a setup file using a complex path
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMER003_RelativePaths() throws Exception {
		// Mocks that the commands is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());

		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupMER003_RelativePaths();

		// Args : relative paths
		String repoPathLastSegment = getRepositoryPath().toString().substring(
				getRepositoryPath().toString().lastIndexOf(SEP) + 1);
		String gitRelativePath = PARENT + SEP + repoPathLastSegment + SEP + ".git";
		String setupRelativePath = CURRENT + SEP + "a" + SEP + "b" + SEP + "c" + SEP + "setup.setup";

		// Sets args
		getContext().addArg(gitRelativePath, setupRelativePath, getShortId("branch_a"));

		runCommand(Returns.COMPLETE);

		StringBuilder expectedOut = new StringBuilder();
		expectedOut.append("Already up to date.").append(EOL).append(EOL);
		assertOutputMessageEnd(expectedOut.toString());

	}

	/**
	 * <h3>Test the use case MER003</h3>
	 * <p>
	 * This use case aims to test a logical merge on a model with no conflict (Auto merging should succeed).
	 * </p>
	 * 
	 * @see ContextSetup#setupMER003()
	 * @throws Exception
	 */
	@Test
	public void testMER003() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupMER003();

		runMerge(Returns.COMPLETE, getShortId("branch_c"), "My message");

		assertOutputMessageEnd("Merge made by 'recursive' strategy." + EOL + EOL);

		final String class1URIFragment = "_bB2fYC3HEeSN_5D5iyrZGQ";
		final String class2URIFragment = "_hfIr4C3HEeSN_5D5iyrZGQ";
		assertExistInResource(contextSetup.getProjectPath().resolve("model.uml"), class1URIFragment,
				class2URIFragment);

		final String class2ShapeURIFragment = "_hfJS8C3HEeSN_5D5iyrZGQ";
		final String class1ShapeURIFragment = "_bB3tgC3HEeSN_5D5iyrZGQ";
		assertExistInResource(contextSetup.getProjectPath().resolve("model.notation"),
				class1ShapeURIFragment, class2ShapeURIFragment);

		Iterator<RevCommit> it = getGit().log().call().iterator();
		RevCommit newHead = it.next();
		assertEquals("My message", newHead.getFullMessage());

	}

	/**
	 * @see ContextSetup#setupMER004()
	 * @throws Exception
	 */
	@Test
	public void testMER004() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupCHE004();

		runMerge(Returns.COMPLETE, getShortId("branch_c"));

		assertOutputMessageEnd("Merge made by 'recursive' strategy." + EOL + EOL);

		final String class1URIFragment = "_adib0C9QEeShUolneTgohg";
		final String class3URIFragment = "_lztC0C9QEeShUolneTgohg";
		assertExistInResource(contextSetup.getProjectPath().resolve("model.uml"), class1URIFragment,
				class3URIFragment);

		final String class2URIFragment = "_a7N2UC9QEeShUolneTgohg";
		final String class4URIFragment = "_m3mv0C9QEeShUolneTgohg";
		assertExistInResource(contextSetup.getProjectPath().resolve("model2.uml"), class2URIFragment,
				class4URIFragment);

		final String class1ShapeURIFragment = "_adjp8C9QEeShUolneTgohg";
		final String class3ShapeURIFragment = "_lzuQ8C9QEeShUolneTgohg";
		assertExistInResource(contextSetup.getProjectPath().resolve("model.notation"),
				class1ShapeURIFragment, class3ShapeURIFragment);

		final String class2ShapeURIFragment = "_a7PEcC9QEeShUolneTgohg";
		final String class4ShapeURIFragment = "_m3nW4C9QEeShUolneTgohg";
		assertExistInResource(contextSetup.getProjectPath().resolve("model2.notation"),
				class2ShapeURIFragment, class4ShapeURIFragment);

	}

	/**
	 * @see ContextSetup#setupMER005()
	 * @throws Exception
	 */
	@Test
	public void testMER005() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupMER005();

		runMerge(Returns.ABORTED, getShortId("branch_c"));

		assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"),//
				contextSetup.getProjectPath().resolve("model.notation"),//
				contextSetup.getProjectPath().resolve("model2.uml"), //
				contextSetup.getProjectPath().resolve("model2.notation"));

		StringBuilder expectedOut = new StringBuilder();
		expectedOut.append("Auto-merging failed in ").append("MER005/model.notation").append(EOL);
		expectedOut.append("Auto-merging failed in ").append("MER005/model.uml").append(EOL);
		expectedOut.append("Auto-merging failed in ").append("MER005/model2.notation").append(EOL);
		expectedOut.append("Auto-merging failed in ").append("MER005/model2.uml").append(EOL);
		expectedOut.append("Automatic merge failed; fix conflicts and then commit the result.").append(EOL)
				.append(EOL);
		assertOutputMessageEnd(expectedOut.toString());

		Set<String> expectedConflictingFilePath = Sets.newHashSet("MER005/model.uml",
				"MER005/model.notation", "MER005/model2.uml", "MER005/model2.notation");
		assertEquals(expectedConflictingFilePath, getGit().status().call().getConflicting());

	}

	/**
	 * <h3>Test MER006</h3>
	 * <p>
	 * Successive conflicts on multiple models in multiple files (one file per model)
	 * </p>
	 * 
	 * @see ContextSetup#setupMER006()
	 * @throws Exception
	 */
	@Test
	public void testMER006() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupCHE006();

		runMerge(Returns.ABORTED, getShortId("branch_d"));

		assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"), //
				contextSetup.getProjectPath().resolve("model.notation"), //
				contextSetup.getProjectPath().resolve("model2.uml"), //
				contextSetup.getProjectPath().resolve("model2.notation"));

		StringBuilder expectedOut = new StringBuilder();
		expectedOut.append("Auto-merging failed in ").append("MER006/model.notation").append(EOL);
		expectedOut.append("Auto-merging failed in ").append("MER006/model.uml").append(EOL);
		expectedOut.append("Auto-merging failed in ").append("MER006/model2.notation").append(EOL);
		expectedOut.append("Auto-merging failed in ").append("MER006/model2.uml").append(EOL);
		expectedOut.append("Automatic merge failed; fix conflicts and then commit the result.").append(EOL)
				.append(EOL);
		assertOutputMessageEnd(expectedOut.toString());

		Set<String> expectedConflictingFilePath = Sets.newHashSet("MER006/model.uml",
				"MER006/model.notation", "MER006/model2.uml", "MER006/model2.notation");
		assertEquals(expectedConflictingFilePath, getGit().status().call().getConflicting());
	}

	/**
	 * <h3>Use case MER008</h3>
	 * <p>
	 * Single conflict on a fragmented model in multiple files (two files per model)
	 * </p>
	 * 
	 * @see ContextSetup#setupMER008()
	 * @throws Exception
	 */
	@Test
	public void testMER008() throws Exception {
		// implement this test once https://bugs.eclipse.org/bugs/show_bug.cgi?id=453709 resolved
	}

	/**
	 * <h3>Use case MER009</h3>
	 * <p>
	 * Single conflict on a fragmented model in multiple files (two files per model)
	 * </p>
	 * 
	 * @see ContextSetup#setupMER009()
	 * @throws Exception
	 */
	@Test
	public void testMER009() throws Exception {
		// implement this test once https://bugs.eclipse.org/bugs/show_bug.cgi?id=453709 resolved
	}

	/**
	 * Model conflict but no textual conflict.
	 * 
	 * @see ContextSetup#setupREB011()
	 * @throws Exception
	 */
	@Test
	public void testMER011() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB011();

		runMerge(Returns.ABORTED, "branch_b");

		StringBuilder expectedOut = new StringBuilder();
		expectedOut.append("Auto-merging failed in ").append("REB011/model.notation").append(EOL);
		expectedOut.append("Auto-merging failed in ").append("REB011/model.uml").append(EOL);
		expectedOut.append("Automatic merge failed; fix conflicts and then commit the result.").append(EOL)
				.append(EOL);
		assertOutputMessageEnd(expectedOut.toString());

		// Checks that the expected file are marked as conflicting
		assertEquals(Sets.newHashSet("REB011/model.notation", "REB011/model.uml"), getGit().status().call()
				.getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		Path projectPath = contextSetup.getProjectPath();
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"),//
				projectPath.resolve("model.di"));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.git.pgm.internal.app.AbstractApplicationTest#buildApp()
	 */
	@Override
	protected IApplication buildApp() {
		return new MergeApplication();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.git.pgm.internal.app.AbstractApplicationTest#getApp()
	 */
	@Override
	protected MergeApplication getApp() {
		return (MergeApplication)super.getApp();
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

	private void runMerge(Returns expectedReturnCode, String commit) throws Exception {
		runMerge(expectedReturnCode, commit, null);
	}

	private void runMerge(Returns expectedReturnCode, String commit, String message) throws Exception {
		resetContext();

		getContext().addArg(getGit().getRepository().getDirectory().getAbsolutePath(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "--show-stack-trace", commit);
		if (message != null) {
			getContext().addArg("-m", message);
		}

		runCommand(expectedReturnCode);
	}
}

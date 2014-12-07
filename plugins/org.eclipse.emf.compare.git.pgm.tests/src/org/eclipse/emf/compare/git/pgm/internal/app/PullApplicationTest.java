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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.nio.file.Path;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.app.data.ContextSetup;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.junit.Test;

/**
 * Test the {@link PullApplication}.
 * 
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 */
@SuppressWarnings("nls")
public class PullApplicationTest extends AbstractApplicationTest {

	private ContextSetup contextSetup;

	/**
	 * <h3>Use case PUL001</h3>
	 * <p>
	 * This use case is used to achieve a conflicting pull
	 * </p>
	 * 
	 * @see ContextSetup#setupPUL001_Remote()
	 * @see ContextSetup#setupPUL001_Local()
	 * @throws Exception
	 */
	@Test
	public void testPUL001() throws Exception {
		ContextSetup contextSetupRemote = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetupRemote.setupPUL001_Remote();

		Git clonedRepo = createClone(getGit(), Lists.newArrayList("branch_a")); //$NON-NLS-1$
		// Sets the clone repository as the main repo to launch/test the command
		Git oldRepo = setCurrentRepo(clonedRepo);
		// Back to branch_a
		clonedRepo.reset().setMode(ResetType.HARD).setRef("remotes/origin/branch_a").call();
		// Deletes remote master branch
		clonedRepo.branchDelete().setBranchNames("remotes/origin/master").setForce(true).call();
		try {
			contextSetup = new ContextSetup(clonedRepo, getTestTmpFolder());
			contextSetup.setupPUL001_Local();

			runPull(Returns.ABORTED);

			StringBuilder expectedOut = new StringBuilder();
			expectedOut.append("Auto-merging failed in ").append("PUL001/model.notation").append(EOL);
			expectedOut.append("Auto-merging failed in ").append("PUL001/model.uml").append(EOL);
			expectedOut.append("Automatic merge failed; fix conflicts and then commit the result.").append(
					EOL).append(EOL);
			assertOutputMessageEnd(expectedOut.toString());

			assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"), contextSetup
					.getProjectPath().resolve("model.notation"));

			Set<String> expectedConflictingFilePath = Sets.newHashSet("PUL001/model.uml",
					"PUL001/model.notation");
			assertEquals(expectedConflictingFilePath, getGit().status().call().getConflicting());

			// Sets back the current repo for dispo methods
			setCurrentRepo(oldRepo);
		} finally {
			clonedRepo.close();
		}
	}

	/**
	 * <h3>Use case PUL002</h3>
	 * <p>
	 * This aims to test a pull between two branches with more than commit between them (see history)
	 * </p>
	 * 
	 * @see ContextSetup#setupPUL002_Remote()
	 * @see ContextSetup#setupPUL002_Local()
	 * @throws Exception
	 */
	@Test
	public void testPUL002() throws Exception {
		ContextSetup contextSetupRemote = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetupRemote.setupPUL002_Remote();

		Git clonedRepo = createClone(getGit(), Lists.newArrayList("branch_a")); //$NON-NLS-1$
		// Sets the clone repository as the main repo to launch/test the command
		Git oldRepo = setCurrentRepo(clonedRepo);
		// Back to branch_a
		clonedRepo.reset().setMode(ResetType.HARD).setRef("remotes/origin/branch_a").call();
		// Deletes remote master branch
		clonedRepo.branchDelete().setBranchNames("remotes/origin/master").setForce(true).call();
		// Deletes remote branch_c branch
		clonedRepo.branchDelete().setBranchNames("remotes/origin/branch_c").setForce(true).call();
		try {
			contextSetup = new ContextSetup(clonedRepo, getTestTmpFolder());
			contextSetup.setupPUL002_Local();

			runPull(Returns.ABORTED);

			StringBuilder expectedOut = new StringBuilder();
			expectedOut.append("Auto-merging failed in ").append("PUL002/model.notation").append(EOL);
			expectedOut.append("Auto-merging failed in ").append("PUL002/model.uml").append(EOL);
			expectedOut.append("Automatic merge failed; fix conflicts and then commit the result.").append(
					EOL).append(EOL);
			assertOutputMessageEnd(expectedOut.toString());

			assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"), contextSetup
					.getProjectPath().resolve("model.notation"));

			Set<String> expectedConflictingFilePath = Sets.newHashSet("PUL002/model.uml",
					"PUL002/model.notation");
			assertEquals(expectedConflictingFilePath, getGit().status().call().getConflicting());

			// Sets back the current repo for dispo methods
			setCurrentRepo(oldRepo);
		} finally {
			clonedRepo.close();
		}
	}

	/**
	 * <h3>Use case PUL003</h3>
	 * <p>
	 * This use case aims to test a logical pull on a model with no conflict (Auto merging should succeed).
	 * </p>
	 * 
	 * @see ContextSetup#setupPUL003_Remote()
	 * @see ContextSetup#setupPUL003_Local()
	 * @throws Exception
	 */
	@Test
	public void testPUL003() throws Exception {
		ContextSetup contextSetupRemote = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetupRemote.setupPUL003_Remote();

		Git clonedRepo = createClone(getGit(), Lists.newArrayList("branch_a")); //$NON-NLS-1$
		// Sets the clone repository as the main repo to launch/test the command
		Git oldRepo = setCurrentRepo(clonedRepo);
		// Back to branch_a
		clonedRepo.reset().setMode(ResetType.HARD).setRef("remotes/origin/branch_a").call();
		// Deletes remote master branch
		clonedRepo.branchDelete().setBranchNames("remotes/origin/master").setForce(true).call();
		try {
			contextSetup = new ContextSetup(clonedRepo, getTestTmpFolder());
			contextSetup.setupPUL003_Local();

			runPull(Returns.COMPLETE);

			assertOutputMessageEnd("Merge made by 'recursive' strategy." + EOL + EOL);

			final String class1URIFragment = "_bB2fYC3HEeSN_5D5iyrZGQ";
			final String class2URIFragment = "_hfIr4C3HEeSN_5D5iyrZGQ";
			assertExistInResource(contextSetup.getProjectPath().resolve("model.uml"), class1URIFragment,
					class2URIFragment);

			final String class2ShapeURIFragment = "_hfJS8C3HEeSN_5D5iyrZGQ";
			final String class1ShapeURIFragment = "_bB3tgC3HEeSN_5D5iyrZGQ";
			assertExistInResource(contextSetup.getProjectPath().resolve("model.notation"),
					class1ShapeURIFragment, class2ShapeURIFragment);

			// Sets back the current repo for dispo methods
			setCurrentRepo(oldRepo);
		} finally {
			clonedRepo.close();
		}
	}

	/**
	 * <h3>Use case PUL004</h3>
	 * <p>
	 * This use case aims to test a logical pull on multiple models with no conflict (Auto merging should
	 * succeed).
	 * </p>
	 * 
	 * @see ContextSetup#setupPUL004_Remote()
	 * @see ContextSetup#setupPUL004_Local()
	 * @throws Exception
	 */
	@Test
	public void testPUL004() throws Exception {
		ContextSetup contextSetupRemote = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetupRemote.setupPUL004_Remote();

		Git clonedRepo = createClone(getGit(), Lists.newArrayList("branch_a")); //$NON-NLS-1$
		// Sets the clone repository as the main repo to launch/test the command
		Git oldRepo = setCurrentRepo(clonedRepo);
		// Back to branch_a
		clonedRepo.reset().setMode(ResetType.HARD).setRef("remotes/origin/branch_a").call();
		// Deletes remote master branch
		clonedRepo.branchDelete().setBranchNames("remotes/origin/master").setForce(true).call();
		try {
			contextSetup = new ContextSetup(clonedRepo, getTestTmpFolder());
			contextSetup.setupPUL004_Local();

			runPull(Returns.COMPLETE);

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

			// Sets back the current repo for dispo methods
			setCurrentRepo(oldRepo);
		} finally {
			clonedRepo.close();
		}
	}

	/**
	 * <h3>Use case PUL005</h3>
	 * <p>
	 * This use case aims to test a logical pull on multiple models with conflicts (Auto merging should
	 * failed).
	 * </p>
	 * 
	 * @see ContextSetup#setupPUL005_Remote()
	 * @see ContextSetup#setupPUL005_Local()
	 * @throws Exception
	 */
	@Test
	public void testPUL005() throws Exception {
		ContextSetup contextSetupRemote = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetupRemote.setupPUL005_Remote();

		Git clonedRepo = createClone(getGit(), Lists.newArrayList("branch_a")); //$NON-NLS-1$
		// Sets the clone repository as the main repo to launch/test the command
		Git oldRepo = setCurrentRepo(clonedRepo);
		// Back to branch_a
		clonedRepo.reset().setMode(ResetType.HARD).setRef("remotes/origin/branch_a").call();
		// Deletes remote master branch
		clonedRepo.branchDelete().setBranchNames("remotes/origin/master").setForce(true).call();
		try {
			contextSetup = new ContextSetup(clonedRepo, getTestTmpFolder());
			contextSetup.setupPUL005_Local();

			runPull(Returns.ABORTED);

			assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"),//
					contextSetup.getProjectPath().resolve("model.notation"),//
					contextSetup.getProjectPath().resolve("model2.uml"), //
					contextSetup.getProjectPath().resolve("model2.notation"));

			StringBuilder expectedOut = new StringBuilder();
			expectedOut.append("Auto-merging failed in ").append("PUL005/model.notation").append(EOL);
			expectedOut.append("Auto-merging failed in ").append("PUL005/model.uml").append(EOL);
			expectedOut.append("Auto-merging failed in ").append("PUL005/model2.notation").append(EOL);
			expectedOut.append("Auto-merging failed in ").append("PUL005/model2.uml").append(EOL);
			expectedOut.append("Automatic merge failed; fix conflicts and then commit the result.").append(
					EOL).append(EOL);
			assertOutputMessageEnd(expectedOut.toString());

			Set<String> expectedConflictingFilePath = Sets.newHashSet("PUL005/model.uml",
					"PUL005/model.notation", "PUL005/model2.uml", "PUL005/model2.notation");
			assertEquals(expectedConflictingFilePath, getGit().status().call().getConflicting());

			// Sets back the current repo for dispo methods
			setCurrentRepo(oldRepo);
		} finally {
			clonedRepo.close();
		}
	}

	/**
	 * <h3>Use case PUL006</h3>
	 * <p>
	 * Successive conflicts on multiple models in multiple files (one file per model)
	 * </p>
	 * 
	 * @see ContextSetup#setupPUL006_Remote()
	 * @see ContextSetup#setupPUL006_Local()
	 * @throws Exception
	 */
	@Test
	public void testPUL006() throws Exception {
		ContextSetup contextSetupRemote = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetupRemote.setupPUL006_Remote();

		Git clonedRepo = createClone(getGit(), Lists.newArrayList("branch_a")); //$NON-NLS-1$
		// Sets the clone repository as the main repo to launch/test the command
		Git oldRepo = setCurrentRepo(clonedRepo);
		// Back to branch_a
		clonedRepo.reset().setMode(ResetType.HARD).setRef("remotes/origin/branch_a").call();
		// Deletes remote master branch
		clonedRepo.branchDelete().setBranchNames("remotes/origin/master").setForce(true).call();
		try {
			contextSetup = new ContextSetup(clonedRepo, getTestTmpFolder());
			contextSetup.setupPUL006_Local();

			runPull(Returns.ABORTED);

			StringBuilder expectedOut = new StringBuilder();
			expectedOut.append("Auto-merging failed in ").append("PUL006/model.notation").append(EOL);
			expectedOut.append("Auto-merging failed in ").append("PUL006/model.uml").append(EOL);
			expectedOut.append("Auto-merging failed in ").append("PUL006/model2.notation").append(EOL);
			expectedOut.append("Auto-merging failed in ").append("PUL006/model2.uml").append(EOL);
			expectedOut.append("Automatic merge failed; fix conflicts and then commit the result.").append(
					EOL).append(EOL);
			assertOutputMessageEnd(expectedOut.toString());

			Set<String> expectedConflictingFilePath = Sets.newHashSet("PUL006/model.uml",
					"PUL006/model.notation", "PUL006/model2.uml", "PUL006/model2.notation");
			assertEquals(expectedConflictingFilePath, getGit().status().call().getConflicting());

			// Sets back the current repo for dispo methods
			setCurrentRepo(oldRepo);
		} finally {
			clonedRepo.close();
		}
	}

	/**
	 * <h3>Use case PUL007</h3>
	 * <p>
	 * Single difference on a fragmented model in multiple files (two files per model)
	 * </p>
	 * 
	 * @see ContextSetup#setupPUL007_Remote()
	 * @see ContextSetup#setupPUL007_Local()
	 * @throws Exception
	 */
	@Test
	public void testPUL007() throws Exception {
		ContextSetup contextSetupRemote = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetupRemote.setupPUL007_Remote();

		Git clonedRepo = createClone(getGit(), Lists.newArrayList("branch_a")); //$NON-NLS-1$
		// Sets the clone repository as the main repo to launch/test the command
		Git oldRepo = setCurrentRepo(clonedRepo);
		// Back to branch_a
		clonedRepo.reset().setMode(ResetType.HARD).setRef("remotes/origin/branch_a").call();
		// Deletes remote master branch
		clonedRepo.branchDelete().setBranchNames("remotes/origin/master").setForce(true).call();
		try {
			contextSetup = new ContextSetup(clonedRepo, getTestTmpFolder());
			contextSetup.setupPUL007_Local();

			runPull(Returns.COMPLETE);

			assertOutputMessageEnd("Merge made by 'recursive' strategy." + EOL + EOL);

			Path projectPath = contextSetup.getProjectPath();
			final String c2FragmentId = "_mq6J8HVUEeScI5AIfi-cqA";
			final String c3FragmentId = "_pYd8YHVUEeScI5AIfi-cqA";
			assertExistInResource(projectPath.resolve("model.uml"), //
					c3FragmentId, //
					c2FragmentId);
			final String c1FragmentId = "_mqPRAHVTEeScI5AIfi-cqA";
			final String attr1FragmentId = "_DIRX4HVUEeScI5AIfi-cqA";
			final String attr2FragmentId = "_M6nbsHVUEeScI5AIfi-cqA";
			assertExistInResource(projectPath.resolve("Class1.uml"), //
					c1FragmentId, //
					attr1FragmentId,//
					attr2FragmentId);

			final String c1ShapeFragmentId = "_mqRtQHVTEeScI5AIfi-cqA";
			final String c2ShapeFragmentId = "_mq-bYHVUEeScI5AIfi-cqA";
			final String c3ShapeFragmentId = "_pYgYoHVUEeScI5AIfi-cqA";
			final String attr1ShapeFragmentId = "_DIT0IHVUEeScI5AIfi-cqA";
			final String attr2ShapeFragmentId = "_M6rGEHVUEeScI5AIfi-cqA";
			assertExistInResource(projectPath.resolve("model.notation"), //
					c1ShapeFragmentId,//
					c2ShapeFragmentId,//
					c3ShapeFragmentId,//
					attr1ShapeFragmentId,//
					attr2ShapeFragmentId);

			// Sets back the current repo for dispo methods
			setCurrentRepo(oldRepo);
		} finally {
			clonedRepo.close();
		}
	}

	/**
	 * <h3>Use case PUL008</h3>
	 * <p>
	 * Single conflict on a fragmented model in multiple files (two files per model)
	 * </p>
	 * 
	 * @see ContextSetup#setupPUL008_Remote()
	 * @see ContextSetup#setupPUL008_Local()
	 * @throws Exception
	 */
	@Test
	public void testPUL008() throws Exception {
		// implement this test once https://bugs.eclipse.org/bugs/show_bug.cgi?id=453709 resolved
	}

	/**
	 * <h3>Use case PUL009</h3>
	 * <p>
	 * Single conflict on a fragmented model in multiple files (two files per model)
	 * </p>
	 * 
	 * @see ContextSetup#setupPUL009_Remote()
	 * @see ContextSetup#setupPUL009_Local()
	 * @throws Exception
	 */
	@Test
	public void testPUL009() throws Exception {
		// implement this test once https://bugs.eclipse.org/bugs/show_bug.cgi?id=453709 resolved
	}

	/**
	 * <h3>Use case PUL011</h3>
	 * <p>
	 * Model conflict but no textual conflict
	 * </p>
	 * 
	 * @see ContextSetup#setupPUL011_Remote()
	 * @see ContextSetup#setupPUL011_Local()
	 * @throws Exception
	 */
	@Test
	public void testPUL011() throws Exception {
		ContextSetup contextSetupRemote = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetupRemote.setupPUL011_Remote();

		Git clonedRepo = createClone(getGit(), Lists.newArrayList("branch_a")); //$NON-NLS-1$
		// Sets the clone repository as the main repo to launch/test the command
		Git oldRepo = setCurrentRepo(clonedRepo);
		// Back to branch_a
		clonedRepo.reset().setMode(ResetType.HARD).setRef("remotes/origin/branch_a").call();
		// Deletes remote master branch
		clonedRepo.branchDelete().setBranchNames("remotes/origin/master").setForce(true).call();
		try {
			contextSetup = new ContextSetup(clonedRepo, getTestTmpFolder());
			contextSetup.setupPUL011_Local();

			runPull(Returns.ABORTED);

			StringBuilder expectedOut = new StringBuilder();
			expectedOut.append("Auto-merging failed in ").append("PUL011/model.notation").append(EOL);
			expectedOut.append("Auto-merging failed in ").append("PUL011/model.uml").append(EOL);
			expectedOut.append("Automatic merge failed; fix conflicts and then commit the result.").append(
					EOL).append(EOL);
			assertOutputMessageEnd(expectedOut.toString());

			// Checks that the expected file are marked as conflicting
			assertEquals(Sets.newHashSet("PUL011/model.notation", "PUL011/model.uml"), getGit().status()
					.call().getConflicting());
			// Checks that the model files were not corrupted by <<< and >>> markers.
			Path projectPath = contextSetup.getProjectPath();
			assertNoConflitMarker(projectPath.resolve("model.uml"), //
					projectPath.resolve("model.notation"),//
					projectPath.resolve("model.di"));
			// Sets back the current repo for dispo methods
			setCurrentRepo(oldRepo);
		} finally {
			clonedRepo.close();
		}
	}

	/**
	 * Test branch with no tracking information.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNoTrackingInformation() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupMER001();

		runPull(Returns.ERROR);

		StringBuilder expectedOut = new StringBuilder();
		expectedOut.append("error: The current branch is not configured for pull").append(EOL);
		assertOutputMessageEnd(expectedOut.toString());
	}

	/**
	 * Test already up-to-date
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAlreadyUpToDate() throws Exception {
		ContextSetup contextSetupRemote = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetupRemote.setupPUL001_Remote();

		Git clonedRepo = createClone(getGit(), Lists.newArrayList("master", "branch_a")); //$NON-NLS-1$
		// Sets the clone repository as the main repo to launch/test the command
		Git oldRepo = setCurrentRepo(clonedRepo);

		try {
			contextSetup = new ContextSetup(clonedRepo, getTestTmpFolder());
			contextSetup.setupPUL001_Local_AlreadyUpToDate();

			runPull(Returns.COMPLETE);

			assertOutputMessageEnd("Already up to date." + EOL + EOL);

			// Sets back the current repo for dispo methods
			setCurrentRepo(oldRepo);
		} finally {
			clonedRepo.close();
		}
	}

	@Override
	protected IApplication buildApp() {
		return new PullApplication();
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

	private void runPull(Returns expectedReturnCode) throws Exception {
		resetContext();

		getContext().addArg(getGit().getRepository().getDirectory().getAbsolutePath(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "--show-stack-trace");

		runCommand(expectedReturnCode);
	}
}

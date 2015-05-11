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

import static org.eclipse.emf.compare.git.pgm.internal.app.data.ContextSetup.LYRICS_1;
import static org.eclipse.emf.compare.git.pgm.internal.app.data.ContextSetup.LYRICS_2;
import static org.eclipse.emf.compare.git.pgm.internal.app.data.ContextSetup.LYRICS_3;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Sets;

import java.nio.file.Path;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.app.data.ContextSetup;
import org.eclipse.emf.compare.git.pgm.util.ProjectBuilder;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.junit.Test;

/**
 * Test the {@link RebaseApplication}.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public class RebaseApplicationTest extends AbstractApplicationTest {

	private ContextSetup contextSetup;

	/**
	 * Basic use case: no conflict.
	 * 
	 * @see ContextSetup#setupREB000()
	 * @throws Exception
	 */
	@Test
	public void testREB000() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB000();

		runRebase(Returns.COMPLETE, "branch_b");

		//@formatter:off
		String expected = "Has rewinded head to replay your work on top of.." + EOL;
		expected += "Applied ["+getShortId("HEAD")+"] Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt" + EOL;
		expected += "Applied ["+getShortId("HEAD~1")+"] Creates Int1 + Modifies in.txt & out.txt" + EOL +EOL;
		//@formatter:on
		assertOutputMessageEnd(expected);

		assertLog("Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt",//
				"Creates Int1 + Modifies in.txt & out.txt",//
				"Creates Op1 in C1");

		// Checks that the Int1,C1,P1 and Op1 and their respective shapes are in the final model
		final String c1FragmentId = "_2VFN4HCgEeS1Cf2409Mk8g";
		final String int1FragmentId = "_-m_ZsHCgEeS1Cf2409Mk8g";
		final String p1FragmentId = "_Hd2FoHChEeS1Cf2409Mk8g";
		final String op1FragmentId = "_6n9_YHCgEeS1Cf2409Mk8g";
		assertExistInResource(contextSetup.getProjectPath().resolve("model.uml"), //
				c1FragmentId, //
				int1FragmentId, //
				p1FragmentId, //
				op1FragmentId);

		final String c1ShapeFragmentId = "_2VKGYHCgEeS1Cf2409Mk8g";
		final String int1ShapeFragmentId = "_-nGucHCgEeS1Cf2409Mk8g";
		final String p1ShapeFragmentId = "_Hd4h4HChEeS1Cf2409Mk8g";
		final String op1ShapeFragmentId = "_6oxQoHCgEeS1Cf2409Mk8g";
		assertExistInResource(contextSetup.getProjectPath().resolve("model.notation"), //
				c1ShapeFragmentId,//
				int1ShapeFragmentId,//
				p1ShapeFragmentId,//
				op1ShapeFragmentId);

		// Checks the content of the test file located in the workspace
		assertFileContent(contextSetup.getProjectPath().resolve("in.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3
				+ EOL);
		// Check the content of the test file located in the workspace
		assertFileContent(contextSetup.getProjectPath().resolve("../out.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3
				+ EOL);

	}

	/**
	 * Basic use case: no conflict but specifies an upstream branch.
	 * 
	 * @see ContextSetup#setupREB000()
	 * @throws Exception
	 */
	@Test
	public void testREB000_Upstream() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB000();

		// Sets the HEAD to branch_a
		getGit().checkout().setName("branch_a").call();

		runRebase(Returns.COMPLETE, "branch_b", "branch_d");

		//@formatter:off
		String expected ="Has rewinded head to replay your work on top of.." + EOL;
		expected += "Applied ["+getShortId("HEAD")+"] Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt" + EOL;
		expected += "Applied ["+getShortId("HEAD~1")+"] Creates Int1 + Modifies in.txt & out.txt" + EOL +EOL;
		//@formatter:on
		assertOutputMessageEnd(expected);

		assertLog("Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt",//
				"Creates Int1 + Modifies in.txt & out.txt",//
				"Creates Op1 in C1");

		Path projectPath = contextSetup.getProjectPath();
		// Checks that the Int1,C1,P1 and Op1 and their respective shapes are in the final model
		final String c1FragmentId = "_2VFN4HCgEeS1Cf2409Mk8g";
		final String int1FragmentId = "_-m_ZsHCgEeS1Cf2409Mk8g";
		final String p1FragmentId = "_Hd2FoHChEeS1Cf2409Mk8g";
		final String op1FragmentId = "_6n9_YHCgEeS1Cf2409Mk8g";
		assertExistInResource(projectPath.resolve("model.uml"), //
				c1FragmentId, //
				int1FragmentId, //
				p1FragmentId, //
				op1FragmentId);

		final String c1ShapeFragmentId = "_2VKGYHCgEeS1Cf2409Mk8g";
		final String int1ShapeFragmentId = "_-nGucHCgEeS1Cf2409Mk8g";
		final String p1ShapeFragmentId = "_Hd4h4HChEeS1Cf2409Mk8g";
		final String op1ShapeFragmentId = "_6oxQoHCgEeS1Cf2409Mk8g";
		assertExistInResource(projectPath.resolve("model.notation"), //
				c1ShapeFragmentId,//
				int1ShapeFragmentId,//
				p1ShapeFragmentId,//
				op1ShapeFragmentId);

		// Checks the content of the test file located in the workspace
		assertFileContent(projectPath.resolve("in.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);
		// Check the content of the test file located in the workspace
		assertFileContent(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);

	}

	/**
	 * Basic use case: no conflict but specifies an upstream branch (which is the current branch).
	 * 
	 * @see ContextSetup#setupREB000()
	 * @throws Exception
	 */
	@Test
	public void testREB000_Upstream2() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB000();

		runRebase(Returns.COMPLETE, "branch_b", "branch_d");

		//@formatter:off
		String expected ="Has rewinded head to replay your work on top of.." + EOL;
		expected += "Applied ["+getShortId("HEAD")+"] Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt" + EOL;
		expected += "Applied ["+getShortId("HEAD~1")+"] Creates Int1 + Modifies in.txt & out.txt" + EOL +EOL;
		//@formatter:on
		assertOutputMessageEnd(expected);

		assertLog("Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt",//
				"Creates Int1 + Modifies in.txt & out.txt",//
				"Creates Op1 in C1");

		Path projectPath = contextSetup.getProjectPath();
		// Checks that the Int1,C1,P1 and Op1 and their respective shapes are in the final model
		final String c1FragmentId = "_2VFN4HCgEeS1Cf2409Mk8g";
		final String int1FragmentId = "_-m_ZsHCgEeS1Cf2409Mk8g";
		final String p1FragmentId = "_Hd2FoHChEeS1Cf2409Mk8g";
		final String op1FragmentId = "_6n9_YHCgEeS1Cf2409Mk8g";

		assertExistInResource(projectPath.resolve("model.uml"), //
				c1FragmentId, //
				int1FragmentId, //
				p1FragmentId, //
				op1FragmentId);

		final String c1ShapeFragmentId = "_2VKGYHCgEeS1Cf2409Mk8g";
		final String int1ShapeFragmentId = "_-nGucHCgEeS1Cf2409Mk8g";
		final String p1ShapeFragmentId = "_Hd4h4HChEeS1Cf2409Mk8g";
		final String op1ShapeFragmentId = "_6oxQoHCgEeS1Cf2409Mk8g";
		assertExistInResource(projectPath.resolve("model.notation"), //
				c1ShapeFragmentId,//
				int1ShapeFragmentId,//
				p1ShapeFragmentId,//
				op1ShapeFragmentId);

		// Checks the content of the test file located in the workspace
		assertFileContent(projectPath.resolve("in.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);
		// Check the content of the test file located in the workspace
		assertFileContent(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);

	}

	/**
	 * Basic use case: no conflict.
	 * <p>
	 * Work on a cloned repository in order to have the remote tracking branch set up. This test run a logical
	 * rebase without any branch reference. In this case, the rebase should use the remote tacking branch
	 * </p>
	 * 
	 * @see ContextSetup#setupREB000()
	 * @throws Exception
	 */
	@Test
	public void testREB000_UpstreamFromConfig() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB000();

		Git clonedRepo = createClone(getGit());
		try {
			// Sets the clone repository as the main repo to launch/test the command
			Git oldRepo = setCurrentRepo(clonedRepo);
			getGit().branchCreate().setName("branch_a").setStartPoint("origin/branch_a").call();
			// Resets branch_d (current branch) to branch_a.
			getGit().reset().setMode(ResetType.HARD).setRef("branch_a").call();

			// Rebases branch_d againt refs/remotes/origin/branch_d
			runRebase(Returns.COMPLETE);

			String expected = "Fast forwarded 'refs/heads/branch_d' to 'refs/remotes/origin/branch_d'." + EOL
					+ EOL;
			assertOutputMessageEnd(expected);

			assertEquals("branch_d", getGit().getRepository().getBranch());

			assertLog("Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt",//
					"Creates Int1 + Modifies in.txt & out.txt",//
					"Creates C1 + Creates in.txt & out.txt");

			Path projectPath = contextSetup.getProjectPath();
			// Checks that the Int1,C1,P1 and their respective shapes are in the final model
			final String c1FragmentId = "_2VFN4HCgEeS1Cf2409Mk8g";
			final String int1FragmentId = "_-m_ZsHCgEeS1Cf2409Mk8g";
			final String p1FragmentId = "_Hd2FoHChEeS1Cf2409Mk8g";

			assertExistInResource(projectPath.resolve("model.uml"), //
					c1FragmentId, //
					int1FragmentId, //
					p1FragmentId);

			final String c1ShapeFragmentId = "_2VKGYHCgEeS1Cf2409Mk8g";
			final String int1ShapeFragmentId = "_-nGucHCgEeS1Cf2409Mk8g";
			final String p1ShapeFragmentId = "_Hd4h4HChEeS1Cf2409Mk8g";
			assertExistInResource(projectPath.resolve("model.notation"), //
					c1ShapeFragmentId,//
					int1ShapeFragmentId,//
					p1ShapeFragmentId);

			// Checks the content of the test file located in the workspace
			assertFileContent(projectPath.resolve("in.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);
			// Check the content of the test file located in the workspace
			assertFileContent(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);

			// Sets back the current repo for dispo methods
			setCurrentRepo(oldRepo);
		} finally {
			clonedRepo.close();
		}

	}

	/**
	 * Tests the up to date use case by rebasing branch_d against branchd
	 * 
	 * @see ContextSetup#setupREB000()
	 * @throws Exception
	 */
	@Test
	public void testREB000_UpToDate() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB000();

		// Rebases branch_d against branch_d
		runRebase(Returns.COMPLETE, "branch_d", "branch_d");

		String expected = "Current branch 'refs/heads/branch_d' is up to date." + EOL + EOL;
		assertOutputMessageEnd(expected);

		assertEquals("branch_d", getGit().getRepository().getBranch());

		assertLog("Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt",//
				"Creates Int1 + Modifies in.txt & out.txt",//
				"Creates C1 + Creates in.txt & out.txt");

		Path projectPath = contextSetup.getProjectPath();
		// Checks that the Int1,C1,P1 and their respective shapes are in the final model
		final String c1FragmentId = "_2VFN4HCgEeS1Cf2409Mk8g";
		final String int1FragmentId = "_-m_ZsHCgEeS1Cf2409Mk8g";
		final String p1FragmentId = "_Hd2FoHChEeS1Cf2409Mk8g";
		assertExistInResource(projectPath.resolve("model.uml"), //
				c1FragmentId, //
				int1FragmentId, //
				p1FragmentId);

		final String c1ShapeFragmentId = "_2VKGYHCgEeS1Cf2409Mk8g";
		final String int1ShapeFragmentId = "_-nGucHCgEeS1Cf2409Mk8g";
		final String p1ShapeFragmentId = "_Hd4h4HChEeS1Cf2409Mk8g";
		assertExistInResource(projectPath.resolve("model.notation"), //
				c1ShapeFragmentId,//
				int1ShapeFragmentId,//
				p1ShapeFragmentId);

		// Checks the content of the test file located in the workspace
		assertFileContent(projectPath.resolve("in.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);
		// Check the content of the test file located in the workspace
		assertFileContent(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);

	}

	/**
	 * Tests a simple conflict (text and model) between two branches.
	 * 
	 * @see ContextSetup#setupREB001()
	 * @throws Exception
	 */
	@Test
	public void testREB001() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB001();

		runRebase(Returns.ABORTED, "branch_b");

		String id = getShortId("branch_c");
		String msg = "Has rewinded head to replay your work on top of.." + EOL;
		msg += "error: Could not apply [" + id + "] Deletes C1 + Modifies in.txt & out.txt" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;

		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		assertEquals(
				Sets.newHashSet("out.txt", "REB001/in.txt", "REB001/model.uml", "REB001/model.notation"),
				getGit().status().call().getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		Path projectPath = contextSetup.getProjectPath();
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB001/branch_c/model.di")//
				.addContentToCopy("data/conflicts/REB001/branch_c/model.uml") //
				.addContentToCopy("data/conflicts/REB001/branch_c/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1 + LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2);

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.COMPLETE);

		msg = "Applied [" + getShortId("HEAD") + "] Deletes C1 + Modifies in.txt & out.txt" + EOL + EOL;

		assertOutputMessageEnd(msg);

		assertLog("Deletes C1 + Modifies in.txt & out.txt",//
				"Creates ATTR1 in C1 + Modifies in.txt & out.txt",//
				"Creates C1 + Creates in.txt & out.txt");

	}

	/**
	 * Tests the --abort option on a simple conflict (text and model) between two branches.
	 * 
	 * @see ContextSetup#setupREB001()
	 * @throws Exception
	 */
	@Test
	public void testREB001_abort() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB001();

		runRebase(Returns.ABORTED, "branch_b");

		String id = getShortId("branch_c");
		String msg = "Has rewinded head to replay your work on top of.." + EOL;
		msg += "error: Could not apply [" + id + "] Deletes C1 + Modifies in.txt & out.txt" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;

		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		assertEquals(
				Sets.newHashSet("out.txt", "REB001/in.txt", "REB001/model.uml", "REB001/model.notation"),
				getGit().status().call().getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		Path projectPath = contextSetup.getProjectPath();
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		runAbort(Returns.COMPLETE);

		msg = "Aborted." + EOL + EOL;

		assertOutputMessageEnd(msg);

		assertLog("Deletes C1 + Modifies in.txt & out.txt",//
				"Creates C1 + Creates in.txt & out.txt");

	}

	/**
	 * Tests the --skip option on a simple conflict (text and model) between two branches.
	 * 
	 * @see ContextSetup#setupREB001()
	 * @throws Exception
	 */
	@Test
	public void testREB001_skip() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB001();

		runRebase(Returns.ABORTED, "branch_b");

		String id = getShortId("branch_c");
		String msg = "Has rewinded head to replay your work on top of.." + EOL;
		msg += "error: Could not apply [" + id + "] Deletes C1 + Modifies in.txt & out.txt" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;

		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		assertEquals(
				Sets.newHashSet("out.txt", "REB001/in.txt", "REB001/model.uml", "REB001/model.notation"),
				getGit().status().call().getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		Path projectPath = contextSetup.getProjectPath();
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		runSkip(Returns.COMPLETE);

		assertLog("Creates ATTR1 in C1 + Modifies in.txt & out.txt",//
				"Creates C1 + Creates in.txt & out.txt");

	}

	/**
	 * Tests rebase with successive conflicts (model and text).
	 * 
	 * @see ContextSetup#setupREB002()
	 * @throws Exception
	 */
	@Test
	public void testREB002() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB002();

		runRebase(Returns.ABORTED, "branch_b");

		String id = getShortId("branch_c");
		String msg = "Has rewinded head to replay your work on top of.." + EOL;
		msg += "error: Could not apply [" + id + "] Deletes C1 + Modifies in.txt & out.txt" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;

		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		assertEquals(
				Sets.newHashSet("out.txt", "REB002/in.txt", "REB002/model.uml", "REB002/model.notation"),
				getGit().status().call().getConflicting());
		Path projectPath = contextSetup.getProjectPath();
		// Checks that the model files were not corrupted by <<< and >>> markers.
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		// Mock conflicts resolution
		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB002/branch_b/model.di")//
				.addContentToCopy("data/conflicts/REB002/branch_b/model.uml") //
				.addContentToCopy("data/conflicts/REB002/branch_b/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1 + LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2);

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.ABORTED);

		//@formatter:off
		msg = "Applied [" + getShortId("HEAD") + "] Deletes C1 + Modifies in.txt & out.txt" + EOL;
		msg += "error: Could not apply [" + getShortId("branch_d")+ "] Deletes C2 + Modifies in.txt & out.txt" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;
		//@formatter:on
		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		assertEquals(Sets.newHashSet("REB002/model.uml", "REB002/model.notation"), getGit().status().call()
				.getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB002/branch_d/model.di")//
				.addContentToCopy("data/conflicts/REB002/branch_d/model.uml") //
				.addContentToCopy("data/conflicts/REB002/branch_d/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1 + LYRICS_2 + LYRICS_3) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3);

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.COMPLETE);

		msg = "Importing project REB002" + EOL + EOL;
		msg += "Applied [" + getShortId("HEAD") + "] Deletes C2 + Modifies in.txt & out.txt" + EOL + EOL;

		assertLog("Deletes C2 + Modifies in.txt & out.txt",//
				"Deletes C1 + Modifies in.txt & out.txt",//
				"Creates Attr1 in C1 & Attr2 in C2 + Modifies in.txt & out.txt",//
				"Creates C1 & C2 + Creates in.txt & out.txt");

	}

	/**
	 * Tests rebase --abort with successive conflicts (model and text).
	 * 
	 * @see ContextSetup#setupREB002()
	 * @throws Exception
	 */
	@Test
	public void testREB002_abort() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB002();

		runRebase(Returns.ABORTED, "branch_b");

		String id = getShortId("branch_c");
		String msg = "Has rewinded head to replay your work on top of.." + EOL;
		msg += "error: Could not apply [" + id + "] Deletes C1 + Modifies in.txt & out.txt" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;

		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		assertEquals(
				Sets.newHashSet("out.txt", "REB002/in.txt", "REB002/model.uml", "REB002/model.notation"),
				getGit().status().call().getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		Path projectPath = contextSetup.getProjectPath();
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		runAbort(Returns.COMPLETE);

		msg = "Aborted." + EOL + EOL;

		assertOutputMessageEnd(msg);

		assertLog("Deletes C2 + Modifies in.txt & out.txt",//
				"Deletes C1 + Modifies in.txt & out.txt",//
				"Creates C1 & C2 + Creates in.txt & out.txt");

	}

	/**
	 * Tests rebase --skip with successive conflicts (model and text).
	 * 
	 * @see ContextSetup#setupREB002()
	 * @throws Exception
	 */
	@Test
	public void testREB002_skip() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB002();

		runRebase(Returns.ABORTED, "branch_b");

		String id = getShortId("branch_c");
		String msg = "Has rewinded head to replay your work on top of.." + EOL;
		msg += "error: Could not apply [" + id + "] Deletes C1 + Modifies in.txt & out.txt" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;

		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		assertEquals(
				Sets.newHashSet("out.txt", "REB002/in.txt", "REB002/model.uml", "REB002/model.notation"),
				getGit().status().call().getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		Path projectPath = contextSetup.getProjectPath();
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		runSkip(Returns.ABORTED);

		//@formatter:off
		msg = "error: Could not apply [" + getShortId("branch_d")+ "] Deletes C2 + Modifies in.txt & out.txt" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;
		//@formatter:on
		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		assertEquals(
				Sets.newHashSet("out.txt", "REB002/in.txt", "REB002/model.uml", "REB002/model.notation"),
				getGit().status().call().getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB002/branch_d/model.di")//
				.addContentToCopy("data/conflicts/REB002/branch_d/model.uml") //
				.addContentToCopy("data/conflicts/REB002/branch_d/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1 + LYRICS_2 + LYRICS_3) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3);

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.COMPLETE);

		msg = "Importing project REB002" + EOL + EOL;
		msg += "Applied [" + getShortId("HEAD") + "] Deletes C2 + Modifies in.txt & out.txt" + EOL + EOL;

		assertLog("Deletes C2 + Modifies in.txt & out.txt",//
				"Creates Attr1 in C1 & Attr2 in C2 + Modifies in.txt & out.txt",//
				"Creates C1 & C2 + Creates in.txt & out.txt");

	}

	/**
	 * Basic use case: rebase with no conflict.
	 * 
	 * @see ContextSetup#setupREB003()
	 * @throws Exception
	 */
	@Test
	public void testREB003() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB003();

		runRebase(Returns.COMPLETE, "branch_b");

		//@formatter:off
		String expected = "Has rewinded head to replay your work on top of.." + EOL;
		expected += "Applied ["+getShortId("HEAD")+"] Creates C2 in P1 + adds content to in.txt & out.txt" + EOL + EOL;
		//@formatter:on
		assertOutputMessageEnd(expected);

		assertLog("Creates C2 in P1 + adds content to in.txt & out.txt",//
				"Creates C1 in P1 + adds content to in.txt & out.txt",//
				"Creates P1 + Creates in.txt & out.txt");

		Path projectPath = contextSetup.getProjectPath();
		// Checks that all content has been merged
		final String c1FragmentId = "_Ko0m8HVOEeScI5AIfi-cqA";
		final String c2FragmentId = "_OI4NUHVOEeScI5AIfi-cqA";
		assertExistInResource(projectPath.resolve("model.uml"), //
				c1FragmentId, //
				c2FragmentId);

		final String c1ShapeFragmentId = "_Ko3qQHVOEeScI5AIfi-cqA";
		final String c2ShapeFragmentId = "_OI9F0HVOEeScI5AIfi-cqA";
		assertExistInResource(projectPath.resolve("model.notation"), //
				c1ShapeFragmentId,//
				c2ShapeFragmentId);

		// Checks the content of the test file located in the workspace
		assertFileContent(projectPath.resolve("in.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);
		// Checks the content of the test file located out of the workspace
		assertFileContent(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);
	}

	/**
	 * Test no conflicting rebase on fragmented model.
	 * 
	 * @see ContextSetup#setupREB007()
	 * @throws Exception
	 */
	@Test
	public void testREB007() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB007();

		runRebase(Returns.COMPLETE, "branch_b");

		//@formatter:off
		String expected = "Has rewinded head to replay your work on top of.." + EOL;
		expected += "Applied ["+getShortId("HEAD")+"] Creates Attr2 in Class1.uml + Creates C3 in model.uml" + EOL + EOL;
		//@formatter:on
		assertOutputMessageEnd(expected);

		assertLog("Creates Attr2 in Class1.uml + Creates C3 in model.uml",//
				"Creates Attr1 in Class1.uml + Creates C2 in model.uml",//
				"Creates C1 in Class1.uml");

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
	}

	/**
	 * <h3>Use case REB008</h3>
	 * <p>
	 * Single conflict on a fragmented model in multiple files (two files per model)
	 * </p>
	 * 
	 * @see ContextSetup#setupREB008()
	 * @throws Exception
	 */
	@Test
	public void testREB008() throws Exception {
		// implement this test once https://bugs.eclipse.org/bugs/show_bug.cgi?id=453709 resolved
	}

	/**
	 * Tests conflicting rebase on fragmented model.
	 * 
	 * @see ContextSetup#setupREB009()
	 * @throws Exception
	 */
	@Test
	public void testREB009() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB009();

		runRebase(Returns.ABORTED, "branch_b");

		String id = getShortId("branch_c");
		String msg = "Has rewinded head to replay your work on top of.." + EOL;
		msg += "error: Could not apply [" + id + "] Deletes C1 (Deletes Class1.uml)" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;

		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		// Switch the test when bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=453316 resolved
		// assertEquals(Sets.newHashSet("REB009/Class1.uml", "REB009/model.uml", "REB009/model.notation"),
		// getGit().status().call().getConflicting());
		assertEquals(Sets.newHashSet("REB009/model.uml", "REB009/model.notation"), getGit().status().call()
				.getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		Path projectPath = contextSetup.getProjectPath();
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"),//
				projectPath.resolve("Class1.uml"));

		// Mock conflicts resolution
		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB009/resolution/model.di")//
				.addContentToCopy("data/conflicts/REB009/resolution/model.uml") //
				.addContentToCopy("data/conflicts/REB009/resolution/model.notation") //
				.create(projectPath);

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.ABORTED);

		msg = "Applied [" + getShortId("HEAD") + "] Deletes C1 (Deletes Class1.uml)" + EOL;
		msg += "error: Could not apply [" + getShortId("branch_d") + "] Deletes C2" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;
		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		assertEquals(Sets.newHashSet("REB009/model.uml", "REB009/model.notation"), getGit().status().call()
				.getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB009/branch_d/model.di")//
				.addContentToCopy("data/conflicts/REB009/branch_d/model.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_d/model.notation") //
				.create(projectPath);

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.COMPLETE);

		msg = "Applied [" + getShortId("HEAD") + "] Deletes C2" + EOL + EOL;

		assertOutputMessageEnd(msg);

		assertLog("Deletes C2",//
				"Deletes C1 (Deletes Class1.uml)",//
				"Creates Attr1 in C1 (Class1.uml) + Creates Attr2 in C2 (model.uml)",//
				"Creates C1 in Class1.uml + Creates C2 in model.uml");
	}

	/**
	 * Test the NOTHING_TO_COMMIT use case. It happens when a conflict resolution leads to the initial state
	 * of HEAD.
	 * 
	 * @see ContextSetup#setupREB009()
	 * @throws Exception
	 */
	@Test
	public void testREB009_nothingToCommit() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB009();

		runRebase(Returns.ABORTED, "branch_b");

		String id = getShortId("branch_c");
		String msg = "Has rewinded head to replay your work on top of.." + EOL;
		msg += "error: Could not apply [" + id + "] Deletes C1 (Deletes Class1.uml)" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;

		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		// Switch the test when the bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=453316 is corrected
		// assertEquals(Sets.newHashSet("REB009/Class1.uml", "REB009/model.uml", "REB009/model.notation"),
		// getGit().status().call().getConflicting());
		assertEquals(Sets.newHashSet("REB009/model.uml", "REB009/model.notation"), getGit().status().call()
				.getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		Path projectPath = contextSetup.getProjectPath();
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"),//
				projectPath.resolve("Class1.uml"));

		// Mock conflicts resolution by reverting the changes
		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB009/branch_b/model.di")//
				.addContentToCopy("data/conflicts/REB009/branch_b/model.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_b/model.notation") //
				.addContentToCopy("data/conflicts/REB009/branch_b/Class1.di")//
				.addContentToCopy("data/conflicts/REB009/branch_b/Class1.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_b/Class1.notation") //
				.create(projectPath);

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.ABORTED);

		msg = "No changes - did you forget to use 'git add'?" + EOL;
		msg += "If there is nothing left to stage, chances are that something else" + EOL;
		msg += "already introduced the same changes; you might want to skip this patch." + EOL;
		msg += EOL;
		msg += "When you have resolved this problem, run \"git logicalrebase --continue\"." + EOL;
		msg += "If you prefer to skip this patch, run \"git logicalrebase --skip\" instead." + EOL;
		msg += "To check out the original branch and stop rebasing, run \"git logicalrebase --abort\"." + EOL
				+ EOL;

		assertOutputMessageEnd(msg);

		runSkip(Returns.ABORTED);

		msg = "error: Could not apply [" + getShortId("branch_d") + "] Deletes C2" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;
		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		assertEquals(Sets.newHashSet("REB009/model.uml", "REB009/model.notation", "REB009/Class1.notation",
				"REB009/Class1.di", "REB009/Class1.uml"), getGit().status().call().getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"),//
				projectPath.resolve("Class1.uml"), //
				projectPath.resolve("Class1.notation"));

		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB009/branch_d/model.di")//
				.addContentToCopy("data/conflicts/REB009/branch_d/model.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_d/model.notation") //
				.create(projectPath);

		getGit().rm().addFilepattern("REB009/Class1.uml").call();
		getGit().rm().addFilepattern("REB009/Class1.notation").call();
		getGit().rm().addFilepattern("REB009/Class1.di").call();

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.COMPLETE);

		msg = "Applied [" + getShortId("HEAD") + "] Deletes C2" + EOL + EOL;

		assertLog("Deletes C2",//
				"Creates Attr1 in C1 (Class1.uml) + Creates Attr2 in C2 (model.uml)",//
				"Creates C1 in Class1.uml + Creates C2 in model.uml");
	}

	/**
	 * Model conflict but no textual conflict.
	 * 
	 * @see ContextSetup#setupREB011()
	 * @throws Exception
	 */
	@Test
	public void testREB011() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB011();

		runRebase(Returns.ABORTED, "branch_b");

		String id = getShortId("branch_c");
		String msg = "Has rewinded head to replay your work on top of.." + EOL;
		msg += "error: Could not apply [" + id + "] Deletes C1" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;

		assertOutputMessageEnd(msg);

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
	 * @see ContextSetup#setupREB014()
	 * @throws Exception
	 */
	@Test
	public void testREB014() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB014();

		runRebase(Returns.ABORTED, "branch_b");

		String id = getShortId("branch_c");
		String msg = "Has rewinded head to replay your work on top of.." + EOL;
		msg += "error: Could not apply [" + id + "] Deletes C2" + EOL;
		msg += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		msg += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		msg += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		msg += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		msg += "hint: Do NOT commit, use one of the following commands instead" + EOL;
		msg += "hint:  git logicalrebase --continue : to continue the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --abort : to abort the rebase operation" + EOL;
		msg += "hint:  git logicalrebase --skip : to skip this commit" + EOL + EOL;

		assertOutputMessageEnd(msg);

		// Checks that the expected file are marked as conflicting
		HashSet<String> conflictingFile = Sets.newHashSet("REB014/model.notation",//
				"REB014/P1.uml",//
				"REB014/P2.uml");
		assertEquals(conflictingFile, getGit().status().call().getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		Path projectPath = contextSetup.getProjectPath();
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"),//
				projectPath.resolve("model.di"),//
				projectPath.resolve("P1.uml"),//
				projectPath.resolve("P1.notation"),//
				projectPath.resolve("P1.di"),//
				projectPath.resolve("P2.uml"),//
				projectPath.resolve("P2.notation"),//
				projectPath.resolve("P2.di"));
	}

	/**
	 * @see ContextSetup#setupREB016()
	 * @throws Exception
	 */
	@Test
	public void testREB016() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB016();

		runRebase(Returns.COMPLETE, "branch_b");

		//@formatter:off
		String expected = "Has rewinded head to replay your work on top of.." + EOL;
		expected += "Applied ["+getShortId("HEAD")+"] Adds in.txt && out.txt" + EOL + EOL;
		//@formatter:on
		assertOutputMessageEnd(expected);

		assertLog("Adds in.txt && out.txt",//
				"Creates C1 in P1",//
				"Creates P1");

		Path projectPath = contextSetup.getProjectPath();
		final String p1FragmentId = "_142C4HlpEeSjSr5E4B1VMw";
		final String c1FragmentId = "_Di70UHlqEeSjSr5E4B1VMw";
		assertExistInResource(projectPath.resolve("model.uml"), //
				p1FragmentId, //
				c1FragmentId);

		final String p1ShapeFragmentId = "_16b-UHlpEeSjSr5E4B1VMw";
		final String c1ShapeFragmentId = "_Di_esHlqEeSjSr5E4B1VMw";
		assertExistInResource(projectPath.resolve("model.notation"), //
				c1ShapeFragmentId,//
				p1ShapeFragmentId);

		// Checks the content of the test file located in the workspace
		assertFileContent(contextSetup.getProjectPath().resolve("in.txt"), LYRICS_1 + EOL);
		// Check the content of the test file located in the workspace
		assertFileContent(contextSetup.getProjectPath().resolve("../out.txt"), LYRICS_1 + EOL);
	}

	@Override
	protected IApplication buildApp() {
		return new RebaseApplication();
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

	private void runRebase(Returns expectedReturnCode, String upstream, String toRebase) throws Exception {
		resetContext();

		getContext().addArg(getGit().getRepository().getDirectory().getAbsolutePath(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "--show-stack-trace");
		if (upstream != null) {
			getContext().addArg(upstream);
		}
		if (toRebase != null) {
			getContext().addArg(toRebase);
		}

		runCommand(expectedReturnCode);
	}

	private void runRebase(Returns expectedReturnCode) throws Exception {
		runRebase(expectedReturnCode, null, null);
	}

	private void runRebase(Returns expectedReturnCode, String toRebase) throws Exception {
		runRebase(expectedReturnCode, null, toRebase);
	}

	private void runContinue(Returns expectedReturnCode) throws Exception {
		resetContext();
		getContext().addArg(getRepositoryPath().resolve(".git").toString(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "--show-stack-trace", "--continue");
		runCommand(expectedReturnCode);

	}

	private void runSkip(Returns expectedReturnCode) throws Exception {
		resetContext();
		getContext().addArg(getRepositoryPath().resolve(".git").toString(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "--show-stack-trace", "--skip");
		runCommand(expectedReturnCode);
	}

	private void runAbort(Returns expectedReturnCode) throws Exception {
		resetContext();
		getContext().addArg(getRepositoryPath().resolve(".git").toString(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "--show-stack-trace", "--abort");
		runCommand(expectedReturnCode);
	}

}

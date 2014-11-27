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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.util.ProjectBuilder;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

/**
 * Test the {@link LogicalCherryPickApplication}.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public class RebaseApplicationTest extends AbstractLogicalCommandApplicationTest {

	private File project;

	private File userSetupFile;

	private Path projectPath;

	//@formatter:off
	private final static String LYRICS_1 = "In the merry month of June, when first from home I started," + EOL
			+ "And left the girls alone, sad and broken-hearted." + EOL
			+ "Shook hands with father dear, kissed my darling mother," + EOL
			+ "Drank a pint of beer, my tears and grief to smother ;" + EOL
			+ "Then off to reap the corn, and leave where I was born." + EOL
			+ "I cut a stout black-thorn to banish ghost or goblin ;" + EOL
			+ "With a pair of bran new brogues, I rattled o'er the bogs —" + EOL
			+ "Sure I frightened all the dogs on the rocky road to Dublin." + EOL;

	private final static String LYRICS_2 = "For it is the rocky road, here's the road to Dublin;" + EOL
			+ "Here's the rocky road, now fire away to Dublin !" + EOL;

	private final static String LYRICS_3 = "The steam-coach was at hand, the driver said he'd cheap ones."+ EOL
			+ "But sure the luggage van was too much for my ha'pence." + EOL
			+ "For England I was bound, it would never do to balk it." + EOL
			+ "For every step of the road, bedad I says I, I'll walk it." + EOL
			+ "I did not sigh or moan until I saw Athlone." + EOL
			+ "A pain in my shin bone, it set my heart a-bubbling;" + EOL
			+ "And fearing the big cannon, looking o'er the Shannon," + EOL
			+ "I very quickly ran on the rocky road to Dublin." + EOL;
	//@formatter:on
	@Override
	protected IApplication buildApp() {
		return new LogicalRebaseApplication();
	}

	/**
	 * Basic use case: no conflict.
	 * 
	 * @see #setupREB000()
	 * @throws Exception
	 */
	@Test
	public void REB000() throws Exception {
		setupREB000();

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
		assertExistInResource(project.toPath().resolve("model.uml"), //
				c1FragmentId, //
				int1FragmentId, //
				p1FragmentId, //
				op1FragmentId);

		final String c1ShapeFragmentId = "_2VKGYHCgEeS1Cf2409Mk8g";
		final String int1ShapeFragmentId = "_-nGucHCgEeS1Cf2409Mk8g";
		final String p1ShapeFragmentId = "_Hd4h4HChEeS1Cf2409Mk8g";
		final String op1ShapeFragmentId = "_6oxQoHCgEeS1Cf2409Mk8g";
		assertExistInResource(project.toPath().resolve("model.notation"), //
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
	 * Basic use case: no conflict but specifies an upstream branch.
	 * 
	 * @see #setupREB000()
	 * @throws Exception
	 */
	@Test
	public void REB000_Upstream() throws Exception {
		setupREB000();

		// Sets the HEAD to branch_a
		getGit().checkout().setName("branch_a").call();

		runRebase(Returns.COMPLETE, "branch_b", "branch_d");

		//@formatter:off
		String expected = "Switched to branch 'refs/heads/branch_d'" + EOL;
		expected += EOL;
		expected +="Has rewinded head to replay your work on top of.." + EOL;
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
		assertExistInResource(project.toPath().resolve("model.uml"), //
				c1FragmentId, //
				int1FragmentId, //
				p1FragmentId, //
				op1FragmentId);

		final String c1ShapeFragmentId = "_2VKGYHCgEeS1Cf2409Mk8g";
		final String int1ShapeFragmentId = "_-nGucHCgEeS1Cf2409Mk8g";
		final String p1ShapeFragmentId = "_Hd4h4HChEeS1Cf2409Mk8g";
		final String op1ShapeFragmentId = "_6oxQoHCgEeS1Cf2409Mk8g";
		assertExistInResource(project.toPath().resolve("model.notation"), //
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
	 * @see #setupREB000()
	 * @throws Exception
	 */
	@Test
	public void REB000_Upstream2() throws Exception {
		setupREB000();

		runRebase(Returns.COMPLETE, "branch_b", "branch_d");

		//@formatter:off
		String expected = "Switched to branch 'refs/heads/branch_d'" + EOL;
		expected += EOL;
		expected +="Has rewinded head to replay your work on top of.." + EOL;
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
		assertExistInResource(project.toPath().resolve("model.uml"), //
				c1FragmentId, //
				int1FragmentId, //
				p1FragmentId, //
				op1FragmentId);

		final String c1ShapeFragmentId = "_2VKGYHCgEeS1Cf2409Mk8g";
		final String int1ShapeFragmentId = "_-nGucHCgEeS1Cf2409Mk8g";
		final String p1ShapeFragmentId = "_Hd4h4HChEeS1Cf2409Mk8g";
		final String op1ShapeFragmentId = "_6oxQoHCgEeS1Cf2409Mk8g";
		assertExistInResource(project.toPath().resolve("model.notation"), //
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
	 * @see #setupREB000()
	 * @throws Exception
	 */
	@Test
	public void REB000_UpstreamFromConfig() throws Exception {
		setupREB000();

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

			// Checks that the Int1,C1,P1 and their respective shapes are in the final model
			final String c1FragmentId = "_2VFN4HCgEeS1Cf2409Mk8g";
			final String int1FragmentId = "_-m_ZsHCgEeS1Cf2409Mk8g";
			final String p1FragmentId = "_Hd2FoHChEeS1Cf2409Mk8g";
			assertExistInResource(project.toPath().resolve("model.uml"), //
					c1FragmentId, //
					int1FragmentId, //
					p1FragmentId);

			final String c1ShapeFragmentId = "_2VKGYHCgEeS1Cf2409Mk8g";
			final String int1ShapeFragmentId = "_-nGucHCgEeS1Cf2409Mk8g";
			final String p1ShapeFragmentId = "_Hd4h4HChEeS1Cf2409Mk8g";
			assertExistInResource(project.toPath().resolve("model.notation"), //
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
	 * @throws Exception
	 */
	@Test
	public void REB000_UpToDate() throws Exception {
		setupREB000();

		// Rebases branch_d against branch_d
		runRebase(Returns.COMPLETE, "branch_d", "branch_d");

		String expected = "Current branch 'refs/heads/branch_d' is up to date." + EOL + EOL;
		assertOutputMessageEnd(expected);

		assertEquals("branch_d", getGit().getRepository().getBranch());

		assertLog("Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt",//
				"Creates Int1 + Modifies in.txt & out.txt",//
				"Creates C1 + Creates in.txt & out.txt");

		// Checks that the Int1,C1,P1 and their respective shapes are in the final model
		final String c1FragmentId = "_2VFN4HCgEeS1Cf2409Mk8g";
		final String int1FragmentId = "_-m_ZsHCgEeS1Cf2409Mk8g";
		final String p1FragmentId = "_Hd2FoHChEeS1Cf2409Mk8g";
		assertExistInResource(project.toPath().resolve("model.uml"), //
				c1FragmentId, //
				int1FragmentId, //
				p1FragmentId);

		final String c1ShapeFragmentId = "_2VKGYHCgEeS1Cf2409Mk8g";
		final String int1ShapeFragmentId = "_-nGucHCgEeS1Cf2409Mk8g";
		final String p1ShapeFragmentId = "_Hd4h4HChEeS1Cf2409Mk8g";
		assertExistInResource(project.toPath().resolve("model.notation"), //
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
	 * @see #setupREB001()
	 * @throws Exception
	 */
	@Test
	public void REB001() throws Exception {
		setupREB001();

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
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB001/branch_c/model.di")//
				.addContentToCopy("data/conflicts/REB001/branch_c/model.uml") //
				.addContentToCopy("data/conflicts/REB001/branch_c/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1 + LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2);

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.COMPLETE);

		msg = "Importing project REB001" + EOL;
		msg += "Applied [" + getShortId("HEAD") + "] Deletes C1 + Modifies in.txt & out.txt" + EOL + EOL;

		assertOutputMessageEnd(msg);

		assertLog("Deletes C1 + Modifies in.txt & out.txt",//
				"Creates ATTR1 in C1 + Modifies in.txt & out.txt",//
				"Creates C1 + Creates in.txt & out.txt");

	}

	/**
	 * Tests the --abort option on a simple conflict (text and model) between two branches.
	 * 
	 * @see #setupREB001()
	 * @throws Exception
	 */
	@Test
	public void REB001_abort() throws Exception {
		setupREB001();

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
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		runAbort(Returns.COMPLETE);

		msg = "Importing project REB001" + EOL;
		msg += "Aborted." + EOL + EOL;

		assertOutputMessageEnd(msg);

		assertLog("Deletes C1 + Modifies in.txt & out.txt",//
				"Creates C1 + Creates in.txt & out.txt");

	}

	/**
	 * Tests the --skip option on a simple conflict (text and model) between two branches.
	 * 
	 * @see #setupREB001()
	 * @throws Exception
	 */
	@Test
	public void REB001_skip() throws Exception {
		setupREB001();

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
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		runSkip(Returns.COMPLETE);

		msg = "Importing project REB001" + EOL + EOL;

		assertOutputMessageEnd(msg);

		assertLog("Creates ATTR1 in C1 + Modifies in.txt & out.txt",//
				"Creates C1 + Creates in.txt & out.txt");

	}

	/**
	 * Tests rebase with successive conflicts (model and text).
	 * 
	 * @see #setupREB002()
	 * @throws Exception
	 */
	@Test
	public void REB002() throws Exception {
		setupREB002();

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
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		// Mock conflicts resolution
		project = new ProjectBuilder(this) //
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
		msg = "Importing project REB002" + EOL;
		msg += "Applied [" + getShortId("HEAD") + "] Deletes C1 + Modifies in.txt & out.txt" + EOL;
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

		project = new ProjectBuilder(this) //
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
	 * @see #setupREB002()
	 * @throws Exception
	 */
	@Test
	public void REB002_abort() throws Exception {
		setupREB002();

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
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		runAbort(Returns.COMPLETE);

		msg = "Importing project REB002" + EOL;
		msg += "Aborted." + EOL + EOL;

		assertOutputMessageEnd(msg);

		assertLog("Deletes C2 + Modifies in.txt & out.txt",//
				"Deletes C1 + Modifies in.txt & out.txt",//
				"Creates C1 & C2 + Creates in.txt & out.txt");

	}

	/**
	 * Tests rebase --skip with successive conflicts (model and text).
	 * 
	 * @see #setupREB002()
	 * @throws Exception
	 */
	@Test
	public void REB002_skip() throws Exception {
		setupREB002();

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
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		runSkip(Returns.ABORTED);

		//@formatter:off
		msg = "Importing project REB002" + EOL;
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
		assertEquals(
				Sets.newHashSet("out.txt", "REB002/in.txt", "REB002/model.uml", "REB002/model.notation"),
				getGit().status().call().getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"));

		project = new ProjectBuilder(this) //
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
	 * @see #setupREB003()
	 * @throws Exception
	 */
	@Test
	public void REB003() throws Exception {
		setupREB003();

		runRebase(Returns.COMPLETE, "branch_b");

		//@formatter:off
		String expected = "Has rewinded head to replay your work on top of.." + EOL;
		expected += "Applied ["+getShortId("HEAD")+"] Creates C2 in P1 + adds content to in.txt & out.txt" + EOL + EOL;
		//@formatter:on
		assertOutputMessageEnd(expected);

		assertLog("Creates C2 in P1 + adds content to in.txt & out.txt",//
				"Creates C1 in P1 + adds content to in.txt & out.txt",//
				"Creates P1 + Creates in.txt & out.txt");

		// Checks that all content has been merged
		final String c1FragmentId = "_Ko0m8HVOEeScI5AIfi-cqA";
		final String c2FragmentId = "_OI4NUHVOEeScI5AIfi-cqA";
		assertExistInResource(project.toPath().resolve("model.uml"), //
				c1FragmentId, //
				c2FragmentId);

		final String c1ShapeFragmentId = "_Ko3qQHVOEeScI5AIfi-cqA";
		final String c2ShapeFragmentId = "_OI9F0HVOEeScI5AIfi-cqA";
		assertExistInResource(project.toPath().resolve("model.notation"), //
				c1ShapeFragmentId,//
				c2ShapeFragmentId);

		// Checks the content of the test file located in the workspace
		assertFileContent(projectPath.resolve("in.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);
		// Checks the content of the test file located out of the workspace
		assertFileContent(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3 + EOL);
	}

	/**
	 * Test no conflicting rebase on fragemented model.
	 * 
	 * @see #setupREB007()
	 * @throws Exception
	 */
	@Test
	public void REB007() throws Exception {
		setupREB007();

		runRebase(Returns.COMPLETE, "branch_b");

		//@formatter:off
				String expected = "Has rewinded head to replay your work on top of.." + EOL;
				expected += "Applied ["+getShortId("HEAD")+"] Creates Attr2 in Class1.uml + Creates C3 in model.uml" + EOL + EOL;
				//@formatter:on
		assertOutputMessageEnd(expected);

		assertLog("Creates Attr2 in Class1.uml + Creates C3 in model.uml",//
				"Creates Attr1 in Class1.uml + Creates C2 in model.uml",//
				"Creates C1 in Class1.uml");

		final String c2FragmentId = "_mq6J8HVUEeScI5AIfi-cqA";
		final String c3FragmentId = "_pYd8YHVUEeScI5AIfi-cqA";
		assertExistInResource(project.toPath().resolve("model.uml"), //
				c3FragmentId, //
				c2FragmentId);
		final String c1FragmentId = "_mqPRAHVTEeScI5AIfi-cqA";
		final String attr1FragmentId = "_DIRX4HVUEeScI5AIfi-cqA";
		final String attr2FragmentId = "_M6nbsHVUEeScI5AIfi-cqA";
		assertExistInResource(project.toPath().resolve("Class1.uml"), //
				c1FragmentId, //
				attr1FragmentId,//
				attr2FragmentId);

		final String c1ShapeFragmentId = "_mqRtQHVTEeScI5AIfi-cqA";
		final String c2ShapeFragmentId = "_mq-bYHVUEeScI5AIfi-cqA";
		final String c3ShapeFragmentId = "_pYgYoHVUEeScI5AIfi-cqA";
		final String attr1ShapeFragmentId = "_DIT0IHVUEeScI5AIfi-cqA";
		final String attr2ShapeFragmentId = "_M6rGEHVUEeScI5AIfi-cqA";
		assertExistInResource(project.toPath().resolve("model.notation"), //
				c1ShapeFragmentId,//
				c2ShapeFragmentId,//
				c3ShapeFragmentId,//
				attr1ShapeFragmentId,//
				attr2ShapeFragmentId);
	}

	/**
	 * Tests conflicting rebase on fragemented model.
	 * 
	 * @see #setupREB009()
	 * @throws Exception
	 */
	@Test
	public void REB009() throws Exception {
		setupREB009();

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
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"),//
				projectPath.resolve("Class1.uml"));

		// Mock conflicts resolution
		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB009/resolution/model.di")//
				.addContentToCopy("data/conflicts/REB009/resolution/model.uml") //
				.addContentToCopy("data/conflicts/REB009/resolution/model.notation") //
				.create(projectPath);

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.ABORTED);

		msg = "Importing project REB009" + EOL;
		msg += "Applied [" + getShortId("HEAD") + "] Deletes C1 (Deletes Class1.uml)" + EOL;
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

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB009/branch_d/model.di")//
				.addContentToCopy("data/conflicts/REB009/branch_d/model.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_d/model.notation") //
				.create(projectPath);

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.COMPLETE);

		msg = "Importing project REB009" + EOL + EOL;
		msg += "Applied [" + getShortId("HEAD") + "] Deletes C2" + EOL + EOL;

		assertLog("Deletes C2",//
				"Deletes C1 (Deletes Class1.uml)",//
				"Creates Attr1 in C1 (Class1.uml) + Creates Attr2 in C2 (model.uml)",//
				"Creates C1 in Class1.uml + Creates C2 in model.uml");
	}

	/**
	 * Test the NOTHING_TO_COMMIT use case. It happens when a conflict resolution leads to the initial state
	 * of HEAD.
	 * 
	 * @see #setupREB009()
	 * @throws Exception
	 */
	@Test
	public void REB009_nothingToCommit() throws Exception {
		setupREB009();

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
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"),//
				projectPath.resolve("Class1.uml"));

		// Mock conflicts resolution by reverting the changes
		project = new ProjectBuilder(this) //
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

		msg = "Importing project REB009" + EOL;
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
		assertEquals(Sets.newHashSet("REB009/model.uml", "REB009/model.notation", "REB009/Class1.uml"),
				getGit().status().call().getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"), projectPath.resolve("Class1.uml"), //
				projectPath.resolve("Class1.notation"));

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB009/branch_d/model.di")//
				.addContentToCopy("data/conflicts/REB009/branch_d/model.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_d/model.notation") //
				.create(projectPath);

		getGit().rm().addFilepattern("REB009/Class1.uml").call();

		getGit().add().addFilepattern(".").call();

		runContinue(Returns.COMPLETE);

		msg = "Importing project REB009" + EOL + EOL;
		msg += "Applied [" + getShortId("HEAD") + "] Deletes C2" + EOL + EOL;

		assertLog("Deletes C2",//
				"Creates Attr1 in C1 (Class1.uml) + Creates Attr2 in C2 (model.uml)",//
				"Creates C1 in Class1.uml + Creates C2 in model.uml");
	}

	private static void assertFileContent(Path pathfile, String expected) throws IOException {
		assertEquals(expected, new String(Files.readAllBytes(pathfile)));
	}

	private void assertLog(String... messages) throws NoHeadException, MissingObjectException,
			IncorrectObjectTypeException, GitAPIException, IOException {
		List<RevCommit> revCommits = Lists.newArrayList(getGit().log().setMaxCount(messages.length).add(
				getHeadCommit()).call());
		assertEquals(messages.length, revCommits.size());
		for (int i = 0; i < messages.length; i++) {
			assertEquals(messages[i], revCommits.get(i).getShortMessage());
		}
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
				userSetupFile.getAbsolutePath(), "--show-stack-trace");
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
		getContext().addArg(getRepositoryPath().resolve(".git").toString(), userSetupFile.getAbsolutePath(),
				"--show-stack-trace", "--continue");
		runCommand(expectedReturnCode);

	}

	private void runSkip(Returns expectedReturnCode) throws Exception {
		resetContext();
		getContext().addArg(getRepositoryPath().resolve(".git").toString(), userSetupFile.getAbsolutePath(),
				"--show-stack-trace", "--skip");
		runCommand(expectedReturnCode);
	}

	private void runAbort(Returns expectedReturnCode) throws Exception {
		resetContext();
		getContext().addArg(getRepositoryPath().resolve(".git").toString(), userSetupFile.getAbsolutePath(),
				"--show-stack-trace", "--abort");
		runCommand(expectedReturnCode);
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
	 * @throws IOException
	 * @throws NoFilepatternException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathsException
	 * @throws ConcurrentRefUpdateException
	 * @throws WrongRepositoryStateException
	 * @throws GitAPIException
	 */
	private void setupREB000() throws IOException, NoFilepatternException, NoHeadException,
			NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, GitAPIException {
		projectPath = getRepositoryPath().resolve("REB000");
		project = new ProjectBuilder(this) //
				.addContentToCopy("data/automerging/REB000/branch_a/model.di")//
				.addContentToCopy("data/automerging/REB000/branch_a/model.uml") //
				.addContentToCopy("data/automerging/REB000/branch_a/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1);

		addAllAndCommit("Creates C1 + Creates in.txt & out.txt");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/automerging/REB000/branch_b/model.di")//
				.addContentToCopy("data/automerging/REB000/branch_b/model.uml") //
				.addContentToCopy("data/automerging/REB000/branch_b/model.notation") //
				.create(projectPath);

		addAllAndCommit("Creates Op1 in C1");

		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/automerging/REB000/branch_c/model.di")//
				.addContentToCopy("data/automerging/REB000/branch_c/model.uml") //
				.addContentToCopy("data/automerging/REB000/branch_c/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1 + LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2);

		addAllAndCommit("Creates Int1 + Modifies in.txt & out.txt");

		// Creates branch b
		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/automerging/REB000/branch_d/model.di")//
				.addContentToCopy("data/automerging/REB000/branch_d/model.uml") //
				.addContentToCopy("data/automerging/REB000/branch_d/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1 + LYRICS_2 + LYRICS_3) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2 + LYRICS_3);

		addAllAndCommit("Creates P1. Moves C1 and Int1 to P1.+ Modifies in.txt & out.txt");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

		// Mocks that the command is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());

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
	 * @throws IOException
	 * @throws NoFilepatternException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathsException
	 * @throws ConcurrentRefUpdateException
	 * @throws WrongRepositoryStateException
	 * @throws GitAPIException
	 */
	private void setupREB001() throws IOException, NoFilepatternException, NoHeadException,
			NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, GitAPIException {
		projectPath = getRepositoryPath().resolve("REB001");
		project = new ProjectBuilder(this) //
				.addContentToCopy("data/conflicts/REB001/branch_a/model.di")//
				.addContentToCopy("data/conflicts/REB001/branch_a/model.uml") //
				.addContentToCopy("data/conflicts/REB001/branch_a/model.notation") //
				.addNewFileContent("in.txt", "") //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), "");

		addAllAndCommit("Creates C1 + Creates in.txt & out.txt");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB001/branch_b/model.di")//
				.addContentToCopy("data/conflicts/REB001/branch_b/model.uml") //
				.addContentToCopy("data/conflicts/REB001/branch_b/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1);

		addAllAndCommit("Creates ATTR1 in C1 + Modifies in.txt & out.txt");

		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB001/branch_c/model.di")//
				.addContentToCopy("data/conflicts/REB001/branch_c/model.uml") //
				.addContentToCopy("data/conflicts/REB001/branch_c/model.notation") //
				.addNewFileContent("in.txt", LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_2);

		addAllAndCommit("Deletes C1 + Modifies in.txt & out.txt");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

		// Mocks that the command is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());

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
	 * @throws IOException
	 * @throws NoFilepatternException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathsException
	 * @throws ConcurrentRefUpdateException
	 * @throws WrongRepositoryStateException
	 * @throws GitAPIException
	 */
	private void setupREB002() throws IOException, NoFilepatternException, NoHeadException,
			NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, GitAPIException {
		projectPath = getRepositoryPath().resolve("REB002");
		project = new ProjectBuilder(this) //
				.addContentToCopy("data/conflicts/REB002/branch_a/model.di")//
				.addContentToCopy("data/conflicts/REB002/branch_a/model.uml") //
				.addContentToCopy("data/conflicts/REB002/branch_a/model.notation") //
				.addNewFileContent("in.txt", "") //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), "");

		addAllAndCommit("Creates C1 & C2 + Creates in.txt & out.txt");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB002/branch_b/model.di")//
				.addContentToCopy("data/conflicts/REB002/branch_b/model.uml") //
				.addContentToCopy("data/conflicts/REB002/branch_b/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1);

		addAllAndCommit("Creates Attr1 in C1 & Attr2 in C2 + Modifies in.txt & out.txt");

		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB002/branch_c/model.di")//
				.addContentToCopy("data/conflicts/REB002/branch_c/model.uml") //
				.addContentToCopy("data/conflicts/REB002/branch_c/model.notation") //
				.addNewFileContent("in.txt", LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_2);

		addAllAndCommit("Deletes C1 + Modifies in.txt & out.txt");

		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB002/branch_d/model.di")//
				.addContentToCopy("data/conflicts/REB002/branch_d/model.uml") //
				.addContentToCopy("data/conflicts/REB002/branch_d/model.notation") //
				.addNewFileContent("in.txt", LYRICS_2 + LYRICS_3) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_2 + LYRICS_3);

		addAllAndCommit("Deletes C2 + Modifies in.txt & out.txt");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

		// Mocks that the command is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());
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
	 * @throws IOException
	 * @throws NoFilepatternException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathsException
	 * @throws ConcurrentRefUpdateException
	 * @throws WrongRepositoryStateException
	 * @throws GitAPIException
	 */
	private void setupREB003() throws IOException, NoFilepatternException, NoHeadException,
			NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, GitAPIException {
		projectPath = getRepositoryPath().resolve("REB003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("data/automerging/REB003/branch_a/model.di")//
				.addContentToCopy("data/automerging/REB003/branch_a/model.uml") //
				.addContentToCopy("data/automerging/REB003/branch_a/model.notation") //
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
				.addContentToCopy("data/automerging/REB003/branch_b/model.di")//
				.addContentToCopy("data/automerging/REB003/branch_b/model.uml") //
				.addContentToCopy("data/automerging/REB003/branch_b/model.notation") //
				.addNewFileContent("in.txt", LYRICS_1 + LYRICS_2) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_1 + LYRICS_2);

		addAllAndCommit("Creates C1 in P1 + adds content to in.txt & out.txt");

		// Creates branch_c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/automerging/REB003/branch_c/model.di")//
				.addContentToCopy("data/automerging/REB003/branch_c/model.uml") //
				.addContentToCopy("data/automerging/REB003/branch_c/model.notation") //
				.addNewFileContent("in.txt", LYRICS_2 + LYRICS_3) //
				.create(projectPath);

		ProjectBuilder.createFile(projectPath.resolve("../out.txt"), LYRICS_2 + LYRICS_3);

		addAllAndCommit("Creates C2 in P1 + adds content to in.txt & out.txt");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

		// Mocks that the command is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());

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
	 * @throws IOException
	 * @throws NoFilepatternException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathsException
	 * @throws ConcurrentRefUpdateException
	 * @throws WrongRepositoryStateException
	 * @throws GitAPIException
	 */
	private void setupREB007() throws IOException, NoFilepatternException, NoHeadException,
			NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, GitAPIException {
		projectPath = getRepositoryPath().resolve("REB003");
		project = new ProjectBuilder(this) //
				.addContentToCopy("data/automerging/REB007/branch_a/model.di")//
				.addContentToCopy("data/automerging/REB007/branch_a/model.uml") //
				.addContentToCopy("data/automerging/REB007/branch_a/model.notation") //
				.addContentToCopy("data/automerging/REB007/branch_a/Class1.di")//
				.addContentToCopy("data/automerging/REB007/branch_a/Class1.uml") //
				.addContentToCopy("data/automerging/REB007/branch_a/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates C1 in Class1.uml");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/automerging/REB007/branch_b/model.di")//
				.addContentToCopy("data/automerging/REB007/branch_b/model.uml") //
				.addContentToCopy("data/automerging/REB007/branch_b/model.notation") //
				.addContentToCopy("data/automerging/REB007/branch_b/Class1.di")//
				.addContentToCopy("data/automerging/REB007/branch_b/Class1.uml") //
				.addContentToCopy("data/automerging/REB007/branch_b/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates Attr1 in Class1.uml + Creates C2 in model.uml");

		// Creates branch_c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/automerging/REB007/branch_c/model.di")//
				.addContentToCopy("data/automerging/REB007/branch_c/model.uml") //
				.addContentToCopy("data/automerging/REB007/branch_c/model.notation") //
				.addContentToCopy("data/automerging/REB007/branch_c/Class1.di")//
				.addContentToCopy("data/automerging/REB007/branch_c/Class1.uml") //
				.addContentToCopy("data/automerging/REB007/branch_c/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates Attr2 in Class1.uml + Creates C3 in model.uml");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

		// Mocks that the command is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());

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
	 * @throws IOException
	 * @throws NoFilepatternException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathsException
	 * @throws ConcurrentRefUpdateException
	 * @throws WrongRepositoryStateException
	 * @throws GitAPIException
	 */
	private void setupREB009() throws IOException, NoFilepatternException, NoHeadException,
			NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, GitAPIException {
		projectPath = getRepositoryPath().resolve("REB009");
		project = new ProjectBuilder(this) //
				.addContentToCopy("data/conflicts/REB009/branch_a/model.di")//
				.addContentToCopy("data/conflicts/REB009/branch_a/model.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_a/model.notation") //
				.addContentToCopy("data/conflicts/REB009/branch_a/Class1.di")//
				.addContentToCopy("data/conflicts/REB009/branch_a/Class1.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_a/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates C1 in Class1.uml + Creates C2 in model.uml");

		String branchA = "branch_a";
		createBranch(branchA, "master");

		// Creates branch b
		String branchB = "branch_b";
		createBranchAndCheckout(branchB, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB009/branch_b/model.di")//
				.addContentToCopy("data/conflicts/REB009/branch_b/model.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_b/model.notation") //
				.addContentToCopy("data/conflicts/REB009/branch_b/Class1.di")//
				.addContentToCopy("data/conflicts/REB009/branch_b/Class1.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_b/Class1.notation") //
				.create(projectPath);

		addAllAndCommit("Creates Attr1 in C1 (Class1.uml) + Creates Attr2 in C2 (model.uml)");

		// Creates branch_c
		String branchC = "branch_c";
		createBranchAndCheckout(branchC, branchA);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB009/branch_c/model.di")//
				.addContentToCopy("data/conflicts/REB009/branch_c/model.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_c/model.notation") //
				.create(projectPath);

		getGit().rm()//
				.addFilepattern("REB009/Class1.di") //
				.addFilepattern("REB009/Class1.uml") //
				.addFilepattern("REB009/Class1.notation") //
				.call();
		addAllAndCommit("Deletes C1 (Deletes Class1.uml)");

		// Creates branch_d
		String branchD = "branch_d";
		createBranchAndCheckout(branchD, branchC);

		project = new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/REB009/branch_d/model.di")//
				.addContentToCopy("data/conflicts/REB009/branch_d/model.uml") //
				.addContentToCopy("data/conflicts/REB009/branch_d/model.notation") //
				.create(projectPath);

		addAllAndCommit("Deletes C2");

		// Creates Oomph model
		userSetupFile = createPapyrusUserOomphModel(project);

		// Mocks that the command is launched from the git repository folder.
		setCmdLocation(getRepositoryPath().toString());

	}

}

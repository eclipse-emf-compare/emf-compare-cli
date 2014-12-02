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

import static org.eclipse.emf.compare.git.pgm.internal.app.data.ContextSetup.LYRICS_1;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.app.data.ContextSetup;
import org.eclipse.emf.compare.git.pgm.util.ProjectBuilder;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

/**
 * Test of {@link CherryPickApplication}.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public class CherryPickApplicationTest extends AbstractLogicalCommandApplicationTest {

	private ContextSetup contextSetup;

	/**
	 * <p>
	 * This use case aims to test a logical cherry-pick on a model with successive conflicts.
	 * </p>
	 * <p>
	 * History see {@link ContextSetup#setupCHE002()}
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
	@Test
	public void testCHE002() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupCHE002();
		runCherryPick(Returns.ABORTED, "branch_c", "branch_d");

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_c") + "]... Delete C1"));

		assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"), contextSetup
				.getProjectPath().resolve("model.notation"));

		// Resolves conflits
		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.di")//
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.uml") //
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.notation") //
				.create(contextSetup.getProjectPath());

		getGit().add().addFilepattern(".").call(); //$NON-NLS-1$

		runContinue(Returns.ABORTED);

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_d") + "]... Delete C2",
				"[" + getShortId("HEAD") + "] Delete C1"));

		// Resolves conflicts
		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution2/model.di")//
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution2/model.uml") //
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution2/model.notation") //
				.create(contextSetup.getProjectPath());

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
	 * History see {@link ContextSetup#setupCHE002()}
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
	@Test
	public void testCHE002_nothingToCommit() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupCHE002();
		runCherryPick(Returns.ABORTED, "branch_c", "branch_d");

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_c") + "]... Delete C1"));

		assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"), contextSetup
				.getProjectPath().resolve("model.notation"));

		// Resolve conflicts
		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.di")//
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.uml") //
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.notation") //
				.create(contextSetup.getProjectPath());

		getGit().add().addFilepattern(".").call(); //$NON-NLS-1$

		runContinue(Returns.ABORTED);

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_d") + "]... Delete C2",
				"[" + getShortId("HEAD") + "] Delete C1"));

		// Resolve conflicts by reverting changes to previsous version
		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.di")//
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.uml") //
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.notation") //
				.create(contextSetup.getProjectPath());

		getGit().add().addFilepattern(".").call(); //$NON-NLS-1$

		runContinue(Returns.ABORTED);

		String expected = "No changes detected" + EOL;
		expected += EOL;
		expected += "If there is nothing left to stage, chances are that something" + EOL;
		expected += "else already introduced the same changes; you might want to skip" + EOL;
		expected += "this patch using git logicalcherry-pick --quit" + EOL + EOL;

		assertOutputMessageEnd(expected);

		runQuit(Returns.COMPLETE);

		assertOutputMessageEnd("Complete." + EOL);

		List<RevCommit> revCommits = Lists.newArrayList(getGit().log().setMaxCount(2).add(getHeadCommit())
				.call());
		assertEquals("Delete C1", revCommits.get(0).getShortMessage());
		assertEquals("Adds Attr1 to C1 and adds Attr2 to C2", revCommits.get(1).getShortMessage());
	}

	/**
	 * @see ContextSetup#setupREB016()
	 * @throws Exception
	 */
	@Test
	public void testCHE016() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB016();

		runCherryPick(Returns.COMPLETE, "branch_b");

		assertOutputMessageEnd(getCompleteMessage("[" + getShortId("HEAD") + "] Creates C1 in P1"));

		assertLog("Creates C1 in P1",//
				"Adds in.txt && out.txt",//
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

	/**
	 * <p>
	 * This use case aims to test a logical cherry-pick on a model with successive conflicts. In this test the
	 * user use the --abort option.
	 * </p>
	 * <p>
	 * History see {@link ContextSetup#setupCHE002()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li> <code> logicalcherry-pick branch_c branch_d </code></li>
	 * <li> <code> logicalcherry-pick --abort </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCHE002_aborted() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupCHE002();
		String previousHead = getShortId("HEAD");

		runCherryPick(Returns.ABORTED, "branch_c", "branch_d");

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_c") + "]... Delete C1"));

		assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"), contextSetup
				.getProjectPath().resolve("model.notation"));

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
	 * History see {@link ContextSetup#setupCHE002()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li> <code> logicalcherry-pick branch_c branch_d </code></li>
	 * <li> <code> logicalcherry-pick --quit </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCHE002_quit() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupCHE002();
		runCherryPick(Returns.ABORTED, "branch_c", "branch_d");

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_c") + "]... Delete C1"));

		assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"), contextSetup
				.getProjectPath().resolve("model.notation"));

		// Resolve conflicts
		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.di")//
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.uml") //
				.addContentToCopy("data/conflicts/CHE002/conflict/resolution1/model.notation") //
				.create(contextSetup.getProjectPath());

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
	 * History see {@link ContextSetup#setupCHE003()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li><code> logicalcherry-pick branch_c branch_d </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCHE003() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupCHE003();

		runCherryPick(Returns.COMPLETE, getShortId("branch_c")// uses commit id
				, "branch_d"/* uses branch name */);

		assertOutputMessageEnd(getCompleteMessage("[" + getShortId("HEAD") + "] Adds class 3",//
				"[" + getShortId("HEAD~1") + "] Adds class 2"));

		final String class1URIFragment = "_bB2fYC3HEeSN_5D5iyrZGQ";
		final String class2URIFragment = "_hfIr4C3HEeSN_5D5iyrZGQ";
		final String class3URIFragment = "_aDUsIGWiEeSuO4qBAOfkWA";
		assertExistInResource(contextSetup.getProjectPath().resolve("model.uml"), class1URIFragment,
				class2URIFragment, class3URIFragment);

		final String class2ShapeURIFragment = "_hfJS8C3HEeSN_5D5iyrZGQ";
		final String class1ShapeURIFragment = "_bB3tgC3HEeSN_5D5iyrZGQ";
		final String class3ShapeURIFragement = "_aGWK8GWiEeSuO4qBAOfkWA";
		assertExistInResource(contextSetup.getProjectPath().resolve("model.notation"),
				class1ShapeURIFragment, class2ShapeURIFragment, class3ShapeURIFragement);
	}

	/**
	 * <p>
	 * Tests the "up to date" use case.
	 * </p>
	 * <p>
	 * History see {@link ContextSetup#setupCHE003()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li><code> logicalcherry-pick branch_b </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCHE003_upToDate() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupCHE003();
		runCherryPick(Returns.COMPLETE, "branch_b");

		assertOutputMessageEnd("Fast forward." + EOL + EOL);

	}

	/**
	 * <p>
	 * This use case aims to test a logical cherry-pick on a model with fragment.
	 * </p>
	 * <p>
	 * History see {@link ContextSetup#setupCHE004()}
	 * </p>
	 * <h3>Operation</h3>
	 * <ul>
	 * <li><code> logicalcherry-pick branch_c </code></li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCHE004() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupCHE004();
		runCherryPick(Returns.COMPLETE, "branch_c");

		assertOutputMessageEnd(getCompleteMessage("[" + getShortId("HEAD")
				+ "] Add Class3 under Model1 Add Class4 under Model2"));

		final String class1URIFragment = "_adib0C9QEeShUolneTgohg";
		final String class3URIFragment = "_lztC0C9QEeShUolneTgohg";
		Path projectPath = contextSetup.getProjectPath();
		assertExistInResource(projectPath.resolve("model.uml"), class1URIFragment, class3URIFragment);

		final String class2URIFragment = "_a7N2UC9QEeShUolneTgohg";
		final String class4URIFragment = "_m3mv0C9QEeShUolneTgohg";
		assertExistInResource(projectPath.resolve("model2.uml"), class2URIFragment, class4URIFragment);

		final String class1ShapeURIFragment = "_adjp8C9QEeShUolneTgohg";
		final String class3ShapeURIFragment = "_lzuQ8C9QEeShUolneTgohg";
		assertExistInResource(projectPath.resolve("model.notation"), class1ShapeURIFragment,
				class3ShapeURIFragment);

		final String class2ShapeURIFragment = "_a7PEcC9QEeShUolneTgohg";
		final String class4ShapeURIFragment = "_m3nW4C9QEeShUolneTgohg";
		assertExistInResource(projectPath.resolve("model2.notation"), class2ShapeURIFragment,
				class4ShapeURIFragment);
	}

	/**
	 * <h3>Test CHER006</h3>
	 * <p>
	 * Successives conflicts on multiple models in multiple files (one file per model).
	 * </p>
	 * <p>
	 * History see {@link ContextSetup#setupCHE007()}
	 * </p>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCHE007() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB007();
		runCherryPick(Returns.COMPLETE, "branch_b");

		assertOutputMessageEnd(getCompleteMessage("[" + getShortId("HEAD")
				+ "] Creates Attr1 in Class1.uml + Creates C2 in model.uml"));

		assertLog("Creates Attr1 in Class1.uml + Creates C2 in model.uml",//
				"Creates Attr2 in Class1.uml + Creates C3 in model.uml",//
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
	 * <h3>Test CHER007</h3>
	 * <p>
	 * Test no conflicting cherry-pick on fragmented model.
	 * </p>
	 * <p>
	 * History see {@link ContextSetup#setupCHE006()}
	 * </p>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCHE006() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupCHE006();
		runCherryPick(Returns.ABORTED, "branch_c", "branch_d");

		assertNoConflitMarker(contextSetup.getProjectPath().resolve("model.uml"), contextSetup
				.getProjectPath().resolve("model.notation"), contextSetup.getProjectPath().resolve(
				"model2.uml"), contextSetup.getProjectPath().resolve("model2.notation"));

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_c") + "]... Delete Class1"));

		Set<String> expectedConflictingFilePath = Sets
				.newHashSet("MER006/model.uml", "MER006/model.notation");
		assertEquals(expectedConflictingFilePath, getGit().status().call().getConflicting());

		// Resolve conflicts
		new ProjectBuilder(this) //
				.clean(true) //
				.addContentToCopy("data/conflicts/MER006/conflict/resolution1/model.di")//
				.addContentToCopy("data/conflicts/MER006/conflict/resolution1/model.uml") //
				.addContentToCopy("data/conflicts/MER006/conflict/resolution1/model.notation") //
				.create(contextSetup.getProjectPath());

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
	 * @see ContextSetup#setupREB011()
	 * @throws Exception
	 */
	@Test
	public void testCHE011() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB011();

		runCherryPick(Returns.ABORTED, "branch_b");

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_b")
				+ "]... Moves C1 to P2"));

		// Checks that the expected file are marked as conflicting
		assertEquals(Sets.newHashSet("REB011/model.notation", "REB011/model.uml"), getGit().status().call()
				.getConflicting());
		// Checks that the model files were not corrupted by <<< and >>> markers.
		Path projectPath = contextSetup.getProjectPath();
		assertNoConflitMarker(projectPath.resolve("model.uml"), //
				projectPath.resolve("model.notation"),//
				projectPath.resolve("model.di"));

	}

	@Override
	protected IApplication buildApp() {
		return new CherryPickApplication();
	}

	@Override
	protected CherryPickApplication getApp() {
		return (CherryPickApplication)super.getApp();
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

		getContext().addArg(getRepositoryPath().resolve(".git").toString(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "--show-stack-trace");
		getContext().addArg(commitsToCherryPick);
		runCommand(expectedReturnCode);
	}

	private void runContinue(Returns expectedReturnCode) throws Exception {
		resetContext();
		getContext().addArg(getRepositoryPath().resolve(".git").toString(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "--show-stack-trace", "--continue");
		runCommand(expectedReturnCode);

	}

	private void runQuit(Returns expectedReturnCode) throws Exception {
		resetContext();
		getContext().addArg(getRepositoryPath().resolve(".git").toString(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "--show-stack-trace", "--quit");
		runCommand(expectedReturnCode);
	}

	private void runAbort(Returns expectedReturnCode) throws Exception {
		resetContext();
		getContext().addArg(getRepositoryPath().resolve(".git").toString(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "--show-stack-trace", "--abort");
		runCommand(expectedReturnCode);
	}

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
		expected += "hint:  git logicalcherry-pick --continue : to continue the cherry pick" + EOL;
		expected += "hint:  git logicalcherry-pick --abort : to abort the cherry pick" + EOL;
		expected += "hint:  git logicalcherry-pick --quit : to skip this commit" + EOL + EOL;
		return expected;
	}

	private String getCompleteMessage(String... successfulCommits) {
		String expected = "The following revisions were successfully cherry-picked:" + EOL;
		for (String success : successfulCommits) {
			expected += "	" + success + EOL;
		}
		expected += "Complete." + EOL;
		return expected;
	}

}

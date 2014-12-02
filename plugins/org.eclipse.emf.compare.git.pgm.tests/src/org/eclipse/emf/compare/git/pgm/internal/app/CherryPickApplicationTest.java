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
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Sets;

import java.nio.file.Path;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.app.data.ContextSetup;
import org.eclipse.equinox.app.IApplication;
import org.junit.Test;

/**
 * Test of {@link CherryPickApplication}.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public class CherryPickApplicationTest extends AbstractApplicationTest {

	private ContextSetup contextSetup;

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

		assertOutputMessageEnd("Complete." + EOL);
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
	 * <h3>Test CHE007</h3>
	 * <p>
	 * Successive conflicts on multiple models in multiple files (one file per model).
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
	 * <h3>Test CHER008</h3>
	 * <p>
	 * Single conflict on a fragmented model in multiple files (two files per model)
	 * </p>
	 * 
	 * @see ContextSetup#setupCHE008()
	 * @throws Exception
	 */
	@Test
	public void testCHE008() throws Exception {
		// This test has no more sense since it is now impossible to cherry pick several commits with EGit
		// (see
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=453709)
	}

	/**
	 * <h3>Test CHE009</h3>
	 * <p>
	 * Single conflict on a fragmented model in multiple files (two files per model)
	 * </p>
	 * 
	 * @see ContextSetup#setupCHE009()
	 * @throws Exception
	 */
	@Test
	public void testCHE009() throws Exception {
		// This test has no more sense since it is now impossible to cherry pick several commits with EGit
		// (see
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=453709)
	}

	/**
	 * Model conflict but no textual conflict.
	 * 
	 * @see ContextSetup#setupREB011()
	 * @throws Exception
	 */
	@Test
	public void testCHE011() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB011();

		runCherryPick(Returns.ABORTED, "branch_b");

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_b")
				+ "]... Moves C1 to P2", null)
				+ EOL);

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
	public void testCHE014() throws Exception {
		contextSetup = new ContextSetup(getGit(), getTestTmpFolder());
		contextSetup.setupREB014();

		runCherryPick(Returns.ABORTED, "branch_b");

		assertOutputMessageEnd(getExpectedConflictMessage("[" + getShortId("branch_b")
				+ "]... Creates association between C1 & C2", null)
				+ EOL);

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

	private void runCherryPick(Returns expectedReturnCode, String commitToCherryPick) throws Exception {
		resetContext();

		getContext().addArg(getRepositoryPath().resolve(".git").toString(),
				contextSetup.getUserSetupFile().getAbsolutePath(), "--show-stack-trace");
		getContext().addArg(commitToCherryPick);
		runCommand(expectedReturnCode);
	}

	private String getExpectedConflictMessage(String conflictingCommitMessage, String successfulCommitMessage) {
		String expected = "";
		if (successfulCommitMessage != null) {
			expected += "The following revision was successfully cherry-picked:" + EOL;
			expected += "	" + successfulCommitMessage + EOL;
		}
		expected += "error: Could not apply " + conflictingCommitMessage + EOL;
		expected += "hint: to resolve the conflict use git logicalmergetool command." + EOL;
		expected += "hint: After resolving the conflicts, mark the corrected paths" + EOL;
		expected += "hint: by adding them to the index (Team > Add to index) or" + EOL;
		expected += "hint: by removing them from the index (Team > Remove from index)." + EOL;
		expected += "hint: Then DO commit." + EOL;
		return expected;
	}

	private String getCompleteMessage(String successfulCommit) {
		String expected = "The following revision was successfully cherry-picked:" + EOL;
		expected += "	" + successfulCommit + EOL;
		expected += "Complete." + EOL;
		return expected;
	}

}

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

import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.TAB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;

import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.util.OomphUserModelBuilder;
import org.eclipse.emf.compare.git.pgm.util.ProjectBuilder;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

/**
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public class RebaseArgumentsTest extends AbstractCommandTest {

	@Override
	protected String getCommandName() {
		return RebaseCommand.LOGICAL_REBASE_CMD_NAME;
	}

	@Override
	protected String getExpectedUsage() {
		//@formatter:off
		return "logicalrebase <setup> [<upstream>] [<branch>] [--abort] [--continue] [--git-dir gitFolderPath] [--help (-h)] [--show-stack-trace] [--skip]" + EOL
				+ EOL
				+" <setup>                 : Path to the setup file. The setup file is a Oomph" + EOL
				+"                           model." + EOL
				+" <upstream>              : Upstream reference on top of which commits will be" + EOL
				+"                           rebased." + EOL
				+" <branch>                : Branch to rebase." + EOL
				+" --abort                 : Use this option to abort an in going rebase" + EOL
				+"                           operation." + EOL
				+" --continue              : Use this option to continue an in going rebase" + EOL
				+"                           operation." + EOL
				+" --git-dir gitFolderPath : Path to the .git folder of your repository." + EOL
				+" --help (-h)             : Dispays help for this command." + EOL
				+" --show-stack-trace      : Use this option to display java stack trace in" + EOL
				+"                           console on error." + EOL
				+" --skip                  : Use this option to skip the current commit being" + EOL
				+"                           rebased." + EOL +EOL;
		//@formatter:on

	}

	@Test
	public void testProcessOptions() throws Exception {
		File newSetupFile = setUp();

		// Creates some content for the first commit.
		new ProjectBuilder(this) //
				.create(getRepositoryPath().resolve("EmptyProject"));

		RevCommit initialCommit = addAllAndCommit("First commit");

		// Tests each of the three options --continue --abort --skip while the repository is not curently
		// rebasing
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--continue");

		Object result = getApp().start(getContext());
		assertOutput("fatal: No rebase in progress." + EOL);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

		resetApp();
		resetContext();
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--abort");

		result = getApp().start(getContext());
		assertOutput("fatal: No rebase in progress." + EOL);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

		resetApp();
		resetContext();
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--skip");

		result = getApp().start(getContext());
		assertOutput("fatal: No rebase in progress." + EOL);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

		// Makes the repository in a rebasing state
		getGit().rebase().setOperation(Operation.BEGIN).setUpstream("HEAD").runInteractively(
				new InteractiveHandler() {

					public void prepareSteps(List<RebaseTodoLine> steps) {
					}

					public String modifyCommitMessage(String commit) {
						return commit;
					}
				}, true).call();

		// Tests one option plus the commit parameter
		resetApp();
		resetContext();
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--skip",
				initialCommit.getId().name());

		result = getApp().start(getContext());
		String expected = "fatal: We are currently rebasing. Please use one of the following options:" + EOL;
		expected += "\t--continue" + EOL;
		expected += "\t--abort" + EOL;
		expected += "\t--skip" + EOL + EOL;
		assertOutput(expected);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

		// Tests using two options
		resetApp();
		resetContext();
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--skip", "--continue");
		result = getApp().start(getContext());

		assertOutput("fatal: logical rebase: --skip cannot be used with --continue" + EOL);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

		resetApp();
		resetContext();
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--skip", "--abort");
		result = getApp().start(getContext());

		assertOutput("fatal: logical rebase: --skip cannot be used with --abort" + EOL);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

		resetApp();
		resetContext();
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--abort", "--continue");
		result = getApp().start(getContext());

		assertOutput("fatal: logical rebase: --continue cannot be used with --abort" + EOL);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);
	}

	@Test
	public void isNotAGitRepoTest() throws Exception {
		Path myTmpDir = Files.createTempDirectory(getTestTmpFolder(), "NotARepo", new FileAttribute<?>[] {});
		// Launches command from directory that is not contained by a git repository
		setCmdLocation(myTmpDir.toString());

		File setupFile = new OomphUserModelBuilder()//
				.saveTo(getTestTmpFolder().resolve("setup.setup").toString());

		getContext().addArg(getCommandName(), setupFile.getAbsolutePath(), "master");

		Object result = getApp().start(getContext());
		assertOutput("fatal: Can't find git repository" + EOL);
		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);
	}

	@Test
	public void emptyRepoTest() throws Exception {
		File setupFile = setUp();

		// Tests command on an empty repo (not commit, no branch)
		getContext().addArg(getCommandName(), setupFile.getAbsolutePath(), "master");
		Object result = getApp().start(getContext());
		assertOutput("fatal: bad revision 'master'." + EOL);
		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);
	}

	@Test
	public void corruptSetupModelTest() throws Exception {
		setCmdLocation(getRepositoryPath().toString());

		// Creates some content for the first commit.
		File project = new ProjectBuilder(this) //
				.addNewFileContent("aFile.txt", "Some content") //
				.create(getRepositoryPath().resolve("EmptyProject"));

		addAllAndCommit("First commit");

		Path corruptedSetupFilePath = project.toPath().resolve("aFile.txt");
		getContext().addArg(getCommandName(), corruptedSetupFilePath.toString(), "master");
		Object result = getApp().start(getContext());
		assertOutput("fatal: " + corruptedSetupFilePath.toString() + " is not a valid setup file" + EOL);
		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);
	}

	@Test
	public void validRepoNoCommitIDNoRemoteTrackingTest() throws Exception {
		File newSetupFile = setUp();

		// Creates some content for the first commit.
		new ProjectBuilder(this) //
				.create(getRepositoryPath().resolve("EmptyProject"));

		addAllAndCommit("First commit");

		// No reference
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath());
		Object result = getApp().start(getContext());

		String expectedMessage = "fatal: There is no tracking information for the current branch." + EOL;
		expectedMessage += "Please specify which branch you want to rebase against." + EOL;
		expectedMessage += "    git logicalrebase <setup> <branch>" + EOL;
		expectedMessage += "Use 'git logicalrebase --help' command for details." + EOL;
		expectedMessage += "If you wish to set tracking information for this branch you can do so with:"
				+ EOL;
		expectedMessage += "    git branch --set-upstream-to=<remote>/<branch> <currentBranchName>" + EOL
				+ EOL;

		assertOutput(expectedMessage);
		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);
	}

	@Test
	public void dirtyRepo() throws Exception {
		File newSetupFile = setUp();

		// Creates some content for the first commit.
		Path projectPath = getRepositoryPath().resolve("SomeProject");
		new ProjectBuilder(this) //
				.create(projectPath);

		RevCommit c1 = addAllAndCommit("First commit");

		new ProjectBuilder(this) //
				.clean(true)//
				.addNewFileContent("aFile.txt", "Some content")//
				.create(projectPath);

		addAllAndCommit("Second commit");

		// Makes the repository dirty
		new ProjectBuilder(this) //
				.clean(true)//
				.addNewFileContent("aFile.txt", "Some content 2")//
				.create(projectPath);

		// No reference
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), c1.name());

		Object result = getApp().start(getContext());

		String expectedMessage = "fatal: Your local changes would be overwritten by rebase." + EOL;
		expectedMessage += "hint: Please commit or stash the following files before rebasing:" + EOL;
		expectedMessage += "	" + "SomeProject/aFile.txt" + EOL;
		expectedMessage += EOL;

		assertOutput(expectedMessage);
		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);
	}

	@Test
	public void continueWithUnresolvedConflict() throws Exception {
		File newSetupFile = setUp();

		// Creates some content for the first commit.
		Path projectPath = getRepositoryPath().resolve("SomeProject");
		new ProjectBuilder(this) //
				.addNewFileContent("Conflicting.txt", "") //
				.create(projectPath);

		addAllAndCommit("First commit");

		createBranchAndCheckout("branch_a", "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addNewFileContent("Conflicting.txt", "Some content") //
				.create(projectPath);

		addAllAndCommit("Adds some content to Conflicting.txt");

		createBranchAndCheckout("branch_b", "master");

		new ProjectBuilder(this) //
				.clean(true) //
				.addNewFileContent("Conflicting.txt", "Conflicting") //
				.create(projectPath);

		addAllAndCommit("Adds some conflicting content to Conflicting.txt");

		// Mocks a logical rebase with normal rebase to avoid launching the second platform
		getGit().rebase() //
				.setUpstream("branch_a")//
				.setStrategy(MergeStrategy.RECURSIVE) //
				.call();

		assertTrue(!getGit().status().call().getConflicting().isEmpty());

		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--continue");
		Object result = getApp().start(getContext());

		String expectedMessage = "fatal: Some files are in conflict:" + EOL;
		expectedMessage += TAB + "SomeProject/Conflicting.txt" + EOL;
		expectedMessage += "hint: You must edit all merge conflicts and then" + EOL;
		expectedMessage += "hint: mark them as resolved using git add." + EOL;
		expectedMessage += EOL;

		assertOutput(expectedMessage);
		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

	}

	@Test
	public void validRepoIncorrectIDTest() throws Exception {
		File newSetupFile = setUp();

		// Creates some content for the first commit.
		new ProjectBuilder(this) //
				.create(getRepositoryPath().resolve("EmptyProject"));

		// Gives an incorrect ref
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "incorrectId");
		Object result = getApp().start(getContext());
		assertOutput("fatal: bad revision 'incorrectId'." + EOL);
		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);
	}

	@Test
	public void tooManyArgsTest() throws Exception {
		File newSetupFile = setUp();

		// Creates some content for the first commit.
		new ProjectBuilder(this) //
				.create(getRepositoryPath().resolve("EmptyProject"));

		addAllAndCommit("First commit");

		// Tests referencing a commit using the name of a branch
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "master", "extraArg");
		Object result = getApp().start(getContext());
		String expectedOut = "fatal: bad revision 'extraArg'." + EOL; //
		assertOutput(expectedOut);
		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);
	}

	@Test
	public void validRepoTestIncorrectGitDirArg() throws Exception {
		// Sets context to be sure were are not in a git repository
		setCmdLocation(getTestTmpFolder().toString());

		Path oomphFolderPath = getTestTmpFolder().resolve("oomphFolder");
		File newSetupFile = new OomphUserModelBuilder() //
				.setInstallationLocation(oomphFolderPath.toString()) //
				.setWorkspaceLocation(oomphFolderPath.resolve("ws").toString()) //
				.saveTo(getTestTmpFolder().resolve("setup.setup").toString());

		// Creates some content for the first commit.
		new ProjectBuilder(this) //
				.create(getRepositoryPath().resolve("EmptyProject"));

		addAllAndCommit("First commit");

		// Provides the repository using parameter
		getContext().addArg(getCommandName(), "--git-dir", "x/x/x/incorrectPathToGitDir",
				newSetupFile.getAbsolutePath(), "master");

		Object result = getApp().start(getContext());
		assertOutput("fatal: Can't find git repository" + EOL);
		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);
	}

	// test --continue with conflict

	/**
	 * Tests the following assertion.
	 * <p>
	 * If &lt;upstream&gt; is not specified, the upstream configured in branch.&lt;name&gt.remote and
	 * branch.&lt;name&gt.merge options will be used; see git-config(1) for details. If you are currently not
	 * on any branch or if the current branch does not have a configured upstream, the rebase will abort. <i>
	 * (from git rebase documentation) </i>
	 * </p>
	 */
	@Test
	public void testUpstream_NoSpecified() {

	}

	private File setUp() throws IOException {
		setCmdLocation(getRepositoryPath().toString());

		Path oomphFolderPath = getTestTmpFolder().resolve("oomphFolder");
		File newSetupFile = new OomphUserModelBuilder() //
				.setInstallationLocation(oomphFolderPath.toString()) //
				.setWorkspaceLocation(oomphFolderPath.resolve("ws").toString()) //
				.saveTo(getTestTmpFolder().resolve("setup.setup").toString());
		return newSetupFile;
	}

}

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
import static org.junit.Assert.assertEquals;

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
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

/**
 * Test of {@link LogicalDiffCommand}.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public class CherryPickArgumentsTest extends AbstractLogicalCommandTest {

	@Override
	protected String getCommandName() {
		return LogicalCherryPickCommand.LOGICAL_CHERRY_PICK_CMD_NAME;
	}

	@Override
	protected String getExpectedUsage() {
		//@formatter:off
		
		String usage = "logicalcherry-pick <setup> [<commit> ...] [--abort] [--continue] [--debug (-d)] [--git-dir gitFolderPath] [--help (-h)] [--quit] [--show-stack-trace]" + EOL;
		usage += EOL;
		usage += " <setup>                 : Path to the setup file. The setup file is a Oomph" + EOL;
		usage += "                           model." +EOL;
		usage += " <commit>                : Commit IDs to cherry pick." + EOL;
		usage += " --abort                 : Use this option to abort a in going cherry-pick" + EOL;
		usage += " --continue              : Use this option to continue a in going cherry-pick" + EOL;
		usage += " --debug (-d)            : Launched the provisionned eclipse in debug mode." + EOL;
		usage += " --git-dir gitFolderPath : Path to the .git folder of your repository."+ EOL;
		usage += " --help (-h)             : Dispays help for this command." + EOL;
		usage += " --quit                  : Use this option to quit a in going cherry-pick" + EOL;
		usage += " --show-stack-trace      : Use this option to display java stack trace in" + EOL;
		usage += "                           console on error." + EOL;
		usage += EOL;
		
		return usage;
		//@formatter:on
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
	public void validRepoNoCommitIDTest() throws Exception {
		File newSetupFile = setUp();

		// Creates some content for the first commit.
		new ProjectBuilder(this) //
				.create(getRepositoryPath().resolve("EmptyProject"));

		addAllAndCommit("First commit");

		// No reference
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath());
		Object result = getApp().start(getContext());

		String expectedMessage = "fatal: Argument \"<commit>\" is required in:" + EOL;
		expectedMessage += getExpectedUsage();
		expectedMessage += EOL;

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

		String expectedMessage = "fatal: Your local changes would be overwritten by cherry-pick." + EOL;
		expectedMessage += "hint: Please commit or stash the following file before cherry-picking:" + EOL;
		expectedMessage += "	" + "SomeProject/aFile.txt" + EOL;
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
	public void testProcessOptions() throws Exception {
		File newSetupFile = setUp();

		// Creates some content for the first commit.
		new ProjectBuilder(this) //
				.create(getRepositoryPath().resolve("EmptyProject"));

		RevCommit initialCommit = addAllAndCommit("First commit");

		// Tests each of the three options --continue --abort --quit while the repository is not curently
		// rebasing
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--continue");

		Object result = getApp().start(getContext());
		assertOutput("fatal: No current cherry-picking." + EOL);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

		resetApp();
		resetContext();
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--abort");

		result = getApp().start(getContext());
		assertOutput("fatal: No current cherry-picking." + EOL);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

		resetApp();
		resetContext();
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--quit");

		result = getApp().start(getContext());
		assertOutput("fatal: No current cherry-picking." + EOL);

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
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--quit",
				initialCommit.getId().name());

		result = getApp().start(getContext());
		String expected = "fatal: We are currently cherry-picking commits. Please use one of the following options:"
				+ EOL;
		expected += "\t--continue" + EOL;
		expected += "\t--abort" + EOL;
		expected += "\t--quit" + EOL + EOL;
		assertOutput(expected);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

		// Tests using two options
		resetApp();
		resetContext();
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--quit", "--continue");
		result = getApp().start(getContext());

		assertOutput("fatal: logical cherry-pick: --quit cannot be used with --continue" + EOL);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

		resetApp();
		resetContext();
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--quit", "--abort");
		result = getApp().start(getContext());

		assertOutput("fatal: logical cherry-pick: --quit cannot be used with --abort" + EOL);

		assertEmptyErrorMessage();
		assertEquals(Returns.ERROR.code(), result);

		resetApp();
		resetContext();
		getContext().addArg(getCommandName(), newSetupFile.getAbsolutePath(), "--abort", "--continue");
		result = getApp().start(getContext());

		assertOutput("fatal: logical cherry-pick: --continue cannot be used with --abort" + EOL);

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

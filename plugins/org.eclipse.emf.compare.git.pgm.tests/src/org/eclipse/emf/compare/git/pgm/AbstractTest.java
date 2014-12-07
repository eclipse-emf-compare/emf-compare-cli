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
package org.eclipse.emf.compare.git.pgm;

import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EMPTY_STRING;
import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.compare.git.pgm.util.MockedApplicationContext;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.After;
import org.junit.Before;

/**
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public abstract class AbstractTest {
	private static final String TMP_DIRECTORY_PREFIX = "emfcompare-git-pgm"; //$NON-NLS-1$

	protected static final String REPO_PREFIX = "Repo_"; //$NON-NLS-1$

	private Path testTmpFolder;

	private IApplication app;

	private MockedApplicationContext context;

	private ByteArrayOutputStream outputStream;

	private ByteArrayOutputStream errStream;

	private Git git;

	private String userDir;

	private PrintStream sysout;

	private PrintStream syserr;

	@Before
	public void before() throws Exception {
		// Creates a local git repository for test purpose
		testTmpFolder = Files.createTempDirectory(TMP_DIRECTORY_PREFIX, new FileAttribute<?>[] {});
		outputStream = new ByteArrayOutputStream();
		errStream = new ByteArrayOutputStream();
		sysout = System.out;
		syserr = System.err;

		// Redirects out and err in order to test outputs.
		System.setOut(new PrintStream(outputStream));
		System.setErr(new PrintStream(errStream));

		app = buildApp();
		setContext(new MockedApplicationContext());

		Path newRepoFile = Files.createTempDirectory(testTmpFolder, REPO_PREFIX, new FileAttribute<?>[] {});
		git = Git.init().setDirectory(newRepoFile.toFile()).call();
		// Saves the user.dire property to be able to restore it.( some tests can modify it)
		userDir = System.getProperty("user.dir"); //$NON-NLS-1$

	}

	@After
	public void tearDown() throws Exception {
		// repository.dispose();
		git.close();
		// Restores system properties
		setCmdLocation(userDir);

		deleteRecursively(testTmpFolder.toFile());

		System.setOut(sysout);
		outputStream.close();

		System.setErr(syserr);
		errStream.close();

	}

	public MockedApplicationContext getContext() {
		return context;
	}

	protected void resetContext() {
		context = new MockedApplicationContext();
	}

	protected void resetApp() {
		app = buildApp();
	}

	protected RevCommit getHeadCommit() throws MissingObjectException, IncorrectObjectTypeException,
			IOException, GitAPIException {
		Ref headRef = getGit().getRepository().getRef(Constants.HEAD);
		RevWalk walk = new RevWalk(git.getRepository());
		return walk.parseCommit(headRef.getObjectId());
	}

	public void setContext(MockedApplicationContext context) {
		this.context = context;
	}

	public Path getRepositoryPath() {
		return git.getRepository().getWorkTree().toPath();
	}

	public File getGitFolderPath() {
		return git.getRepository().getDirectory();
	}

	public void deleteRecursively(File f) {
		if (!f.exists()) {
			return;
		}

		if (f.isDirectory()) {
			for (File content : f.listFiles()) {
				deleteRecursively(content);
			}
		}
		f.delete();
	}

	protected abstract IApplication buildApp();

	protected File getWorkspaceLocation() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		return root.getLocation().toFile();
	}

	protected Path getTestTmpFolder() {
		return testTmpFolder;
	}

	protected IApplication getApp() {
		return app;
	}

	protected Git getGit() {
		return git;
	}

	protected void printOut() {
		sysout.println(outputStream.toString());
	}

	protected void printErr() {
		syserr.println(errStream.toString());
	}

	protected void setCmdLocation(String path) {
		System.setProperty("user.dir", path); //$NON-NLS-1$
	}

	/**
	 * Sets the current repo for command and assert methods.
	 * 
	 * @param currentRepo
	 *            new repo
	 * @return the old {@link Git}.
	 */
	protected Git setCurrentRepo(Git currentRepo) {
		Git oldGit = git;
		git = currentRepo;
		return oldGit;
	}

	protected void assertOutputMessageEnd(String expected) throws IOException {
		String outputStreamContent = outputStream.toString();
		assertTrue(getAssertOutputMessageEnd(expected, outputStreamContent), outputStreamContent
				.endsWith(expected));
		// Resets the outputstream
		outputStream.close();
		outputStream = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outputStream));
	}

	private String getAssertOutputMessageEnd(String expected, String actual) {
		StringBuilder builder = new StringBuilder();
		builder.append("Expected end:").append(EOL);
		builder.append(expected).append(EOL);
		builder.append("but was:").append(EOL);
		ArrayList<String> actualLines = Lists.newArrayList(actual.split(EOL, -1));
		List<String> expectedLines = Lists.newArrayList(expected.split(EOL, -1));
		int expectedLineSize = expectedLines.size();
		int actualLineSize = actualLines.size();
		if (expectedLineSize > actualLineSize) {
			builder.append(actual);
		} else {
			builder.append("...");
			// Tries to display 5 extra line from the actual message to understand the context
			int numberOfLineToDisplay = Math.min(expectedLineSize + 5, actualLineSize);
			for (int i = actualLineSize - numberOfLineToDisplay; i < actualLineSize; i++) {
				builder.append(actualLines.get(i)).append(EOL);
			}
		}
		return builder.toString();
	}

	protected void assertEmptyErrorMessage() {
		assertEquals(EMPTY_STRING, errStream.toString());
	}

	protected void assertOutput(String message) throws IOException {
		assertEquals(message, outputStream.toString());

		// Reset the outputstream
		outputStream.close();
		outputStream = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outputStream));
	}

	protected RevCommit addAllAndCommit(String commitMessage) throws Exception {
		DirCache dirChache = getGit().add().addFilepattern(".").call(); //$NON-NLS-1$
		// Assert there is something to commit
		assertTrue(dirChache.getEntriesWithin("").length > 0);
		RevCommit revCommit = getGit().commit().setAuthor("Logical test author", "logicaltest@obeo.fr")
				.setCommitter("Logical test author", "logicaltest@obeo.fr").setMessage(commitMessage).call();
		return revCommit;
	}

	protected Ref createBranchAndCheckout(String ref, String startingPoint) throws RefAlreadyExistsException,
			RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
		return getGit().checkout().setName(ref).setStartPoint(startingPoint).setCreateBranch(true).call();
	}

	protected String getShortId(String ref) throws RevisionSyntaxException, AmbiguousObjectException,
			IncorrectObjectTypeException, IOException {
		ObjectId resolved = getGit().getRepository().resolve(ref);
		return getShortId(resolved);
	}

	protected String getShortId(ObjectId resolved) {
		return resolved.abbreviate(7).name();
	}

	protected String getLongId(String ref) throws RevisionSyntaxException, AmbiguousObjectException,
			IncorrectObjectTypeException, IOException {
		ObjectId resolved = getGit().getRepository().resolve(ref);
		return getLongId(resolved);
	}

	protected String getLongId(ObjectId resolved) {
		return resolved.name();
	}
}

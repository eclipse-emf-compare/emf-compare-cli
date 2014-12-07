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
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.git.pgm.AbstractTest;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public abstract class AbstractApplicationTest extends AbstractTest {

	/**
	 * Assert that there is no conflict marker in the file (( <<<<<<<<<< or ========= or >>>>>>>>>>>).
	 * <p>
	 * In fact this test tries to load the resource.
	 * </p>
	 * 
	 * @param paths
	 * @throws IOException
	 * @throws AssertionError
	 */
	protected void assertNoConflitMarker(Path... paths) throws AssertionError, IOException {
		ResourceSet resourceSet = new ResourceSetImpl();
		for (Path p : paths) {
			try {
				if (p.toFile().exists()) {
					Resource resource = resourceSet.getResource(URI.createFileURI(p.toString()), true);
					assertNotNull(resource);
				} else {
					throw new AssertionError("The file " + p.toString() + " does not exist.");
				}
			} catch (Exception e) {
				throw new AssertionError("Error wile parsing resource " + p.toString() + EOL
						+ getConfigurationMessage(), e);
			}
		}
	}

	protected void assertExistInResource(Path resourcePath, String... fragments) throws IOException {
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource resource = resourceSet.getResource(URI.createFileURI(resourcePath.toString()), true);
		assertNotNull(resource);
		for (String fragment : fragments) {
			EObject eObject = resource.getEObject(fragment);
			assertNotNull("Element with framgment " + fragment + " does not exist" + EOL
					+ getConfigurationMessage(), eObject);
		}
	}

	protected void assertFileContent(Path pathfile, String expected) throws IOException {
		assertEquals(expected, new String(Files.readAllBytes(pathfile)));
	}

	protected void assertLog(String... messages) throws NoHeadException, MissingObjectException,
			IncorrectObjectTypeException, GitAPIException, IOException {
		List<RevCommit> revCommits = Lists.newArrayList(getGit().log().setMaxCount(messages.length).add(
				getHeadCommit()).call());
		assertEquals(messages.length, revCommits.size());
		for (int i = 0; i < messages.length; i++) {
			assertEquals(messages[i], revCommits.get(i).getShortMessage());
		}
	}

	protected String getConfigurationMessage() throws IOException {
		final StringBuilder builder = new StringBuilder();
		builder.append("Configuration:").append(EOL);
		builder.append("\t").append("Tmp folder: ").append(getTestTmpFolder().toString()).append(EOL);
		builder.append("\t").append("Git folder: ").append(getGitFolderPath().getAbsolutePath()).append(EOL);
		builder.append("\t").append("Git content:").append(EOL);
		Files.walkFileTree(getRepositoryPath(), new FileVisitor<Path>() {

			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.endsWith(".git")) {
					return FileVisitResult.SKIP_SUBTREE;
				} else {
					builder.append("\t\t").append(dir.toString()).append(EOL);
					return FileVisitResult.CONTINUE;
				}
			}

			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});

		return builder.toString();
	}

	protected Git createClone(Git gitToClone) throws InvalidRemoteException, TransportException,
			GitAPIException, IOException {
		Path newRepoFile = Files.createTempDirectory(getTestTmpFolder(), REPO_PREFIX,
				new FileAttribute<?>[] {});
		return Git.cloneRepository().setDirectory(newRepoFile.toFile()) //
				.setURI(gitToClone.getRepository().getDirectory().getAbsolutePath()) //
				.setBare(false) //
				.setCloneAllBranches(true) //
				.call();
	}

	protected Git createClone(Git gitToClone, Collection<String> branchesToClone)
			throws InvalidRemoteException, TransportException, GitAPIException, IOException {
		Path newRepoFile = Files.createTempDirectory(getTestTmpFolder(), REPO_PREFIX,
				new FileAttribute<?>[] {});
		return Git.cloneRepository().setDirectory(newRepoFile.toFile()) //
				.setURI(gitToClone.getRepository().getDirectory().getAbsolutePath()) //
				.setBare(false) //
				.setBranchesToClone(branchesToClone) //
				.call();
	}
}

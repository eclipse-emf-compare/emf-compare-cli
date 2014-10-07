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
package org.eclipse.emf.compare.git.pgm.internal.args;

import static org.eclipse.emf.compare.git.pgm.internal.Messages.CAN_T_FIND_GIT_REPOSITORY_MESSAGE;
import static org.eclipse.emf.compare.git.pgm.internal.exception.Die.DeathType.FATAL;

import com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;

import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DeathType;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DiesOn;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.kohsuke.args4j.CmdLineParser;

/**
 * CmdLineParser that is aware of a git repository. Some {@link org.kohsuke.args4j.spi.OptionHandler}s might
 * need it for validation.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
public final class CmdLineParserRepositoryBuilder extends CmdLineParser {

	/**
	 * Functional interface used to build a {@link Repository}.
	 * 
	 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
	 */
	public interface RepoBuilder {

		/**
		 * Build the repository represented by the given directory.
		 * 
		 * @param gitDir
		 *            the repository to build.
		 * @return a {@link Repository}.
		 * @throws Die
		 *             e
		 */
		Repository buildRepository(String gitDir) throws Die;
	}

	/**
	 * {@link Repository} builder that uses pure JGit code.
	 */
	private static RepoBuilder jgitRepoBuilder = new RepoBuilder() {
		/**
		 * {@inheritDoc}.
		 */
		public Repository buildRepository(String aGitdir) throws Die {
			/** The current repository directory. */
			final File myGitDir;
			/** The current repository. */
			final Repository myRepo;
			if (aGitdir == null) {
				myGitDir = null;
			} else {
				myGitDir = new File(aGitdir);
			}
			RepositoryBuilder rb = new RepositoryBuilder().setGitDir(myGitDir).readEnvironment()
					.setMustExist(true).findGitDir();
			if (rb.getGitDir() == null) {
				throw new DiesOn(FATAL).displaying(CAN_T_FIND_GIT_REPOSITORY_MESSAGE).ready();
			}
			try {
				myRepo = rb.build();
			} catch (RepositoryNotFoundException e) {
				throw new DiesOn(FATAL).displaying(CAN_T_FIND_GIT_REPOSITORY_MESSAGE).ready();
			} catch (IOException e) {
				throw new DiesOn(FATAL).duedTo(e).displaying("Cannot build the git repository").ready();
			}
			return myRepo;
		}
	};

	/**
	 * {@link Repository} builder that uses pure EGit code.
	 * <p>
	 * This builder creates the repository and add it the RepositoryCache of EGit
	 * </p>
	 */
	private static RepoBuilder egitRepoBuilder = new RepoBuilder() {
		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.git.pgm.internal.args.CmdLineParserRepositoryBuilder.RepoBuilder#buildRepository(java.lang.String)
		 */
		public Repository buildRepository(String aGitdir) throws Die {
			Preconditions.checkNotNull(aGitdir);
			File myGitDir = new File(aGitdir);
			if (!myGitDir.exists()) {
				throw new DiesOn(DeathType.FATAL).displaying(
						"Can't build git repository: " + aGitdir + "does not exist").ready();
			}
			try {
				Repository myRepo = org.eclipse.egit.core.Activator.getDefault().getRepositoryCache()
						.lookupRepository(myGitDir);
				if (myRepo == null) {
					throw new DiesOn(DeathType.FATAL).displaying("Can't build repository " + aGitdir).ready();
				}
				return myRepo;
			} catch (IOException e1) {
				throw new DiesOn(DeathType.FATAL).duedTo(e1).displaying("Can't build repository " + aGitdir)
						.ready();
			}
		}
	};

	/**
	 * Git directory.
	 */
	private String gitDir;

	/**
	 * Git repository.
	 */
	private Repository repository;

	/**
	 * {@link Repository} builder.
	 */
	private final RepoBuilder builder;

	/**
	 * Constructor.
	 * <p>
	 * The git repository will be buit during argument parsing
	 * </p>
	 * 
	 * @param bean
	 *            instance of a class annotated by Option and Argument.
	 * @param builder
	 *            the Repository builder.
	 */
	private CmdLineParserRepositoryBuilder(Object bean, RepoBuilder builder) {
		super(bean);
		this.builder = builder;
	}

	/**
	 * Creates a new {@link CmdLineParser} that will build the {@link Repository} using pure JGit methods.
	 * <p>
	 * Using this method calling {@link #setGitDir(String)} is optional.
	 * </p>
	 * 
	 * @param bean
	 *            instance of a class annotated by Option and Argument.
	 * @return a new {@link CmdLineParser}.
	 */
	public static CmdLineParserRepositoryBuilder newJGitRepoBuilderCmdParser(Object bean) {
		return new CmdLineParserRepositoryBuilder(bean, jgitRepoBuilder);
	}

	/**
	 * Creates a new {@link CmdLineParser} that will build the {@link Repository} using EGit methods (creates
	 * the repository and add it to the cache).
	 * <p>
	 * With this implementation the method {@link #setGitDir(String)} needs to be called before the  
	 * {@link #getRepo()} since EGit needs the path to the git dir to build the repository
	 * </p>
	 * 
	 * @param bean
	 *            instance of a class annotated by Option and Argument.
	 * @return a new {@link CmdLineParser}.
	 */
	public static CmdLineParserRepositoryBuilder newEGitRepoBuilderCmdParser(Object bean) {
		return new CmdLineParserRepositoryBuilder(bean, egitRepoBuilder);
	}

	/**
	 * Get the git {@link Repository}.
	 * <p>
	 * The first time this method is called it may build the repository and so laucn a {@link Die} exception
	 * <p>
	 * 
	 * @return the git {@link Repository}.
	 * @throws Die
	 *             e
	 */
	public Repository getRepo() throws Die {
		if (repository == null) {
			repository = builder.buildRepository(gitDir);
		}
		return repository;
	}

	/**
	 * Sets the path to the git dir repository.
	 * <p>
	 * This method does not need to be called if the command has been run inside the git repository. However
	 * if it's used it needs to be called before the first call of the {@link #getRepo()} method.
	 * </p>
	 * 
	 * @param gitDir
	 *            the path to the git dir repository.
	 */
	public void setGitDir(String gitDir) {
		// The gitDir argument should be provided before the repository is built
		Preconditions.checkArgument(repository == null);
		this.gitDir = gitDir;
	}
}

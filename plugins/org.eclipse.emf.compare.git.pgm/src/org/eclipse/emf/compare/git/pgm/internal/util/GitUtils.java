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
package org.eclipse.emf.compare.git.pgm.internal.util;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;

/**
 * Util class for JGit/EGit.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
public final class GitUtils {

	/** Short commit ID length. */
	public static final int SHORT_REV_COMMIT_ID_LENGTH = 7;

	/**
	 * Private constructor.
	 */
	private GitUtils() {
	}

	/**
	 * Gets the list of {@link RevCommit}s between two {@link RevCommit}s (the starting commit is excluded).
	 * 
	 * @param repo
	 *            Git repository of the selected commits.
	 * @param from
	 *            Starting commit (newest in the history).
	 * @param to
	 *            Ending commit.
	 * @return the list of {@link RevCommit}s between two {@link RevCommit}s
	 * @throws IOException
	 *             propagates JGit exceptions.
	 */
	public static List<RevCommit> getCommitsBetween(Repository repo, ObjectId from, ObjectId to)
			throws IOException {

		try (RevWalk revWak = new RevWalk(repo)) {
			return RevWalkUtils.find(revWak, revWak.parseCommit(from), revWak.parseCommit(to));
		}
	}

	/**
	 * Creates a one line description of a commit.
	 * <p>
	 * <i> [CommitID] Short commit message EOL </i>
	 * </p>
	 * 
	 * @param revCommit
	 *            commit to describe.
	 * @return a one line description of the commit.
	 */
	public static String getOneLineCommitMsg(RevCommit revCommit) {
		String id = revCommit.abbreviate(SHORT_REV_COMMIT_ID_LENGTH).name();
		String message = revCommit.getShortMessage();
		return "[" + id + "] " + message; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Gets the remote tracking branch of the current branch of a repository.
	 * 
	 * @param repo
	 *            {@link Repository} (Should not be <code>null</code>).
	 * @return a {@link Ref} of the remote tracking branch.
	 */
	public static Ref getCurrentBranchRemoteTrackingRef(Repository repo) {
		StoredConfig config = repo.getConfig();
		if (config != null) {
			try {
				BranchConfig branchConfig = new BranchConfig(config, repo.getBranch());
				String remoteTrackingBranch = branchConfig.getRemoteTrackingBranch();
				if (remoteTrackingBranch != null) {
					return repo.getRef(remoteTrackingBranch);
				}
			} catch (IOException e) {
				// Does nothing, fallbact to return null
			}
		}
		return null;
	}

}

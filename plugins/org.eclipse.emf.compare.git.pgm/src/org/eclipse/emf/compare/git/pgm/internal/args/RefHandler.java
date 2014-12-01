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

import com.google.common.base.Preconditions;

import java.io.IOException;

import org.eclipse.emf.compare.git.pgm.internal.exception.ArgumentValidationError;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

/**
 * {@link OptionHandler} that converts string into a {@link Ref} targeting a {@link RevCommit}.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
public class RefHandler extends OptionHandler<Ref> {

	/**
	 * Constructor.
	 *
	 * @param parser
	 *            {@link OptionHandler#owner}.
	 * @param option
	 *            {@link OptionHandler#option}
	 * @param setter
	 *            {@link OptionHandler#setter}
	 */
	public RefHandler(CmdLineParser parser, OptionDef option, Setter<? super Ref> setter) {
		super(parser, option, setter);
		Preconditions.checkArgument(parser instanceof CmdLineParserRepositoryBuilder);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.kohsuke.args4j.spi.OptionHandler#getDefaultMetaVariable()
	 */
	@Override
	public String getDefaultMetaVariable() {
		return "<ref>"; //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.kohsuke.args4j.spi.OptionHandler#parseArguments(org.kohsuke.args4j.spi.Parameters)
	 */
	@Override
	public int parseArguments(Parameters params) throws CmdLineException {
		String refName = params.getParameter(0);
		try {
			Repository repo = ((CmdLineParserRepositoryBuilder)owner).getRepo();
			Ref ref = repo.getRef(refName);
			if (ref == null) {
				// The refName is not the name of branch. It might be an id of a commit
				ObjectId objectID;
				try {
					objectID = repo.resolve(refName);
				} catch (RevisionSyntaxException | IOException e) {
					throw new ArgumentValidationError(owner, e);
				}

				if (objectID == null) {
					throw new ArgumentValidationError(owner, "bad revision '" + refName + "'.");
				}
				// Checks that the resolved object is a RevCommit
				RevWalk revWalk = new RevWalk(repo);
				try {
					RevCommit commitId = revWalk.parseCommit(objectID);
					ref = new ObjectIdRef.Unpeeled(Storage.LOOSE, commitId.getName(), commitId);
				} catch (IOException e) {
					throw new ArgumentValidationError(owner, "bad revision '" + refName + "'.");
				} finally {
					revWalk.release();
				}
			}

			setter.addValue(ref);
		} catch (Die | IOException e) {
			throw new ArgumentValidationError(owner, e);
		}
		return 1;
	}

}

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
package org.eclipse.emf.compare.git.pgm.internal.args;

import com.google.common.base.Preconditions;

import java.io.IOException;

import org.eclipse.emf.compare.git.pgm.internal.exception.ArgumentValidationError;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
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
 * {@link OptionHandler} that converts string into {@link RevCommit}.
 *
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
public class RevCommitHandler extends OptionHandler<RevCommit> {

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
	public RevCommitHandler(CmdLineParser parser, OptionDef option, Setter<? super ObjectId> setter) {
		super(parser, option, setter);
		Preconditions.checkArgument(parser instanceof CmdLineParserRepositoryBuilder);
	}

	@Override
	public int parseArguments(Parameters params) throws CmdLineException {
		String ref = params.getParameter(0);

		ObjectId objectID;
		Repository repo;
		try {
			repo = ((CmdLineParserRepositoryBuilder)owner).getRepo();
			try {
				objectID = repo.resolve(ref);
			} catch (RevisionSyntaxException | IOException e) {
				throw new ArgumentValidationError(owner, e);
			}

			if (objectID == null) {
				throw new ArgumentValidationError(owner, "bad revision '" + ref + "'.");
			}

			try (RevWalk revWalk = new RevWalk(repo)) {
				RevCommit commitId = revWalk.parseCommit(objectID);
				setter.addValue(commitId);
			} catch (IOException e) {
				throw new ArgumentValidationError(owner, "bad revision '" + ref + "'.");
			}

		} catch (Die e) {
			throw new ArgumentValidationError(owner, e);
		}
		return 1;
	}

	@Override
	public String getDefaultMetaVariable() {
		return "commit"; //$NON-NLS-1$
	}

}

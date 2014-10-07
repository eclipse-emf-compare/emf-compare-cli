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
package org.eclipse.emf.compare.git.pgm.internal.util;

import com.google.common.base.Function;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.oomph.resources.SourceLocator;

/**
 * A catalog of helpfull {@link Function}.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
public final class FunctionCatalog {

	/**
	 * Get the absolute path of a given File.
	 */
	public static final Function<File, String> FILE_TO_PATH = new Function<File, String>() {

		public String apply(File input) {
			return input.getAbsolutePath();
		}
	};

	/**
	 * Get the File for a given IProject.
	 */
	public static final Function<IProject, File> IPROJECT_TO_FILE = new Function<IProject, File>() {
		public File apply(IProject input) {
			return new File(input.getLocation().toString());
		}
	};

	/**
	 * Get the File for a given SourceLocator.
	 */
	public static final Function<SourceLocator, File> SOURCELOCATOR_TO_FILE = new Function<SourceLocator, File>() {
		public File apply(SourceLocator input) {
			return new File(input.getRootFolder());
		}
	};

	/**
	 * Internal constructor.
	 */
	private FunctionCatalog() {
	}
}

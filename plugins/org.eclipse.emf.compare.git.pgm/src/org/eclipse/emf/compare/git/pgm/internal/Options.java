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
package org.eclipse.emf.compare.git.pgm.internal;

/**
 * Options available from commands.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
public final class Options {

	/** Display help for the command. */
	public static final String HELP_OPT = "--help"; //$NON-NLS-1$

	/** Show stack trace. */
	public static final String SHOW_STACK_TRACE_OPT = "--show-stack-trace"; //$NON-NLS-1$

	/** Apply the command on a specific git repository. */
	public static final String GIT_DIR_OPT = "--git-dir"; //$NON-NLS-1$

	/**
	 * Internal constructor.
	 */
	private Options() {
	}
}

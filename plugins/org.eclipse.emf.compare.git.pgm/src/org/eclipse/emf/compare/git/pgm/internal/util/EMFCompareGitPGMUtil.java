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

import com.google.common.base.Strings;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;

/**
 * Utils methods.
 * 
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 */
public final class EMFCompareGitPGMUtil {

	/**
	 * File separator.
	 */
	public static final String SEP = File.separator;

	/**
	 * End of line.
	 */
	public static final String EOL = System.getProperty("line.separator"); //$NON-NLS-1$

	/**
	 * Empty string.
	 */
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * Current Folder.
	 */
	public static final String CURRENT = "."; //$NON-NLS-1$

	/**
	 * Parent Folder.
	 */
	public static final String PARENT = ".."; //$NON-NLS-1$

	/**
	 * Internal constructor.
	 */
	private EMFCompareGitPGMUtil() {
	}

	/**
	 * Displays the error message to the user and return matching {@link Returns}.
	 * 
	 * @param error
	 *            Error to handle.
	 * @param showStackTrace
	 *            Set to <code>true</code> if the stack trace should be display in the console or
	 *            <code>false</code> otherwise.
	 * @return a {@link Returns}
	 */
	public static Integer handleDieError(Die error, boolean showStackTrace) {
		final PrintStream stream;
		final Integer returnCode = Returns.ERROR.code();
		final String prefix;
		switch (error.getType()) {
			case ERROR:
				prefix = "error: ";
				stream = System.out;
				break;
			case FATAL:
				prefix = "fatal: ";
				stream = System.out;
				break;
			case SOFTWARE_ERROR:
			default:
				prefix = "software error: ";
				stream = System.err;
				break;
		}
		if (error.getMessage() != null) {
			stream.println(prefix + error.getMessage());
		}
		if (showStackTrace && error.getCause() != null) {
			error.getCause().printStackTrace(stream);
		}

		return returnCode;
	}

	/**
	 * Returns, from a relative path, the corresponding file with an absolute path. This absolute path is
	 * computed against 'user.dir' system property.
	 * 
	 * @param relativePath
	 *            the relative path for which we want the corresponding file.
	 * @return the corresponding file with an absolute path.
	 */
	public static File toFileWithAbsolutePath(String relativePath) {
		return toFileWithAbsolutePath(System.getProperty("user.dir"), relativePath); //$NON-NLS-1$
	}

	/**
	 * Returns, from a relative path, the corresponding file with an absolute path. This absolute path is
	 * computed against the given base path.
	 * 
	 * @param basePath
	 *            teh basePath to construct the full path.
	 * @param relativePath
	 *            the relative path for which we want the corresponding file. the relative path for which we
	 *            want the corresponding file.
	 * @return the corresponding file with an absolute path.
	 */
	public static File toFileWithAbsolutePath(String basePath, String relativePath) {
		File file = new File(relativePath);
		if (!file.isAbsolute()) {
			Path base = FileSystems.getDefault().getPath(basePath);
			Path resolvedPath = base.resolve(file.toPath());
			Path absolutePath = resolvedPath.normalize();
			file = absolutePath.toFile();
		}
		return file;
	}

	/**
	 * Get a nice message from a IStatus.
	 * 
	 * @param status
	 *            the IStatus.
	 * @return a nice message from a IStatus.
	 */
	public static String getStatusMessage(IStatus status) {
		StringBuilder statusMessage = new StringBuilder(status.getMessage());
		if (status.isMultiStatus()) {
			appendChildrenStatus(status, statusMessage, 1);
		}
		return statusMessage.toString();
	}

	/**
	 * Append a new message to the status.
	 * 
	 * @param status
	 *            the IStatus.
	 * @param builder
	 *            the message.
	 * @param depth
	 *            the depth of the new message.
	 */
	private static void appendChildrenStatus(IStatus status, StringBuilder builder, int depth) {
		for (IStatus child : status.getChildren()) {
			builder.append(Strings.repeat(" ", depth)).append(child.getMessage()).append(EOL); //$NON-NLS-1$
			if (child.isMultiStatus()) {
				appendChildrenStatus(child, builder, depth + 1);
			}
		}
	}

	/**
	 * Forces to wait for all EGit operation to terminate.
	 * <p>
	 * If this is not done then it might happen that some projects are not connected yet whereas the git
	 * command is being performed.
	 * </p>
	 */
	public static void waitEgitJobs() {
		waitForScope(JobFamilies.AUTO_SHARE);
		waitForScope(JobFamilies.AUTO_IGNORE);
		waitForScope(JobFamilies.REPOSITORY_CHANGED);
		waitForScope(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
	}

	/**
	 * Waits for a Job to terminate.
	 * 
	 * @param context
	 *            familly og the job
	 */
	private static void waitForScope(Object context) {
		// The UILockListener might prevent us from properly joining.
		boolean joined = false;
		while (!joined) {
			try {
				Job.getJobManager().join(context, new NullProgressMonitor());
				joined = true;
			} catch (InterruptedException e) {
				// Some other UI threads were trying to run. Let the syncExecs
				// do their jobs and re-try to join on ours.
			}
		}
	}

}

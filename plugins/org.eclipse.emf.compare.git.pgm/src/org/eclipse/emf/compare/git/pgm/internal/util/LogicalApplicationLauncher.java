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

import static org.eclipse.emf.compare.git.pgm.internal.Options.SHOW_STACK_TRACE_OPT;

import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.compare.git.pgm.Returns;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DeathType;
import org.eclipse.emf.compare.git.pgm.internal.exception.Die.DiesOn;
import org.eclipse.jgit.util.io.ThrowingPrintWriter;

/**
 * Util class to launch an application.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
public class LogicalApplicationLauncher {

	/** VM Args option. */
	private static final String VMARGS_OPTION = "-D"; //$NON-NLS-1$

	/** Oomph option. */
	private static final String PROP_SETUP_CONFIRM_SKIP = "oomph.setup.confirm.skip"; //$NON-NLS-1$

	/** Oomph option. */
	private static final String PROP_SETUP_OFFLINE_STARTUP = "oomph.setup.offline.startup"; //$NON-NLS-1$

	/** Oomph option. */
	private static final String PROP_SETUP_MIRRORS_STARTUP = "oomph.setup.mirrors.startup"; //$NON-NLS-1$

	/** Application attributes. */
	private List<String> attributes = new ArrayList<>();

	/** Output {@link java.io.Writer}. */
	private ThrowingPrintWriter out;

	/** Path to the eclipse installation. */
	private String eclipseFilePath;

	/** Show stack trace option. */
	private boolean showStackTrace;

	/** Path of the repository. */
	private String repositoryPath;

	/** Oomph setup file path. */
	private String setupFilePath;

	/** Name of the application to launch. */
	private String applicationName;

	/** Path to the workspace. */
	private String workspaceLocation;

	/** Debug option. */
	private boolean debug;

	/**
	 * Constructor.
	 * 
	 * @param out
	 *            {@link #out}
	 */
	public LogicalApplicationLauncher(ThrowingPrintWriter out) {
		Preconditions.checkNotNull(out);
		this.out = out;
	}

	/**
	 * {@link #debug}.
	 * 
	 * @param value
	 *            {@link #debug}
	 * @return this
	 */
	public LogicalApplicationLauncher debug(boolean value) {
		this.debug = value;
		return this;
	}

	/**
	 * {@link #applicationName}.
	 * 
	 * @param name
	 *            {@link #applicationName}
	 * @return this
	 */
	public LogicalApplicationLauncher setApplicationName(String name) {
		this.applicationName = name;
		return this;
	}

	/**
	 * {@link #repositoryPath}.
	 * 
	 * @param path
	 *            {@link #repositoryPath}.
	 * @return this
	 */
	public LogicalApplicationLauncher setRepositoryPath(String path) {
		this.repositoryPath = path;
		return this;
	}

	/**
	 * {@link #workspaceLocation}.
	 * 
	 * @param location
	 *            {@link #workspaceLocation}
	 * @return this
	 */
	public LogicalApplicationLauncher setWorkspaceLocation(String location) {
		this.workspaceLocation = location;
		return this;
	}

	/**
	 * {@link #setSetupFilePath(String)}.
	 * 
	 * @param path
	 *            {@link #setSetupFilePath(String)}
	 * @return this
	 */
	public LogicalApplicationLauncher setSetupFilePath(String path) {
		this.setupFilePath = path;
		return this;
	}

	/**
	 * {@link #eclipseFilePath}.
	 * 
	 * @param eclipsePath
	 *            {@link #eclipseFilePath}
	 * @return this
	 */
	public LogicalApplicationLauncher setEclipsePath(String eclipsePath) {
		this.eclipseFilePath = eclipsePath;
		return this;
	}

	/**
	 * {@link #showStackTrace}.
	 * 
	 * @param showStackTraceValue
	 *            {@link #showStackTrace}
	 * @return this
	 */
	public LogicalApplicationLauncher showStackTrace(boolean showStackTraceValue) {
		this.showStackTrace = showStackTraceValue;
		return this;
	}

	/**
	 * Adds a new attribute to {@link #attributes}.
	 * 
	 * @param attr
	 *            new attribute.
	 * @return this
	 */
	public LogicalApplicationLauncher addAttribute(String attr) {
		this.attributes.add(attr);
		return this;
	}

	/**
	 * Launch the application with the specified parameters.
	 * 
	 * @return the return code of the application.
	 * @throws Die
	 *             if the process fail to start or is interrupted.
	 */
	public Integer launch() throws Die {
		Preconditions.checkNotNull(workspaceLocation);
		Preconditions.checkNotNull(setupFilePath);
		Preconditions.checkNotNull(eclipseFilePath);

		try {
			out.println("Launching the installed product...");
		} catch (IOException e) {
			throw new DiesOn(DeathType.FATAL).duedTo(e).ready();
		}

		List<String> command = new ArrayList<String>();
		command.add(eclipseFilePath);
		command.add("-nosplash"); //$NON-NLS-1$
		command.add("--launcher.suppressErrors"); //$NON-NLS-1$
		if (applicationName != null) {
			command.add("-application"); //$NON-NLS-1$
			command.add(applicationName);
		}

		// Propagates the show stack trace option to the application.
		if (showStackTrace) {
			command.add(SHOW_STACK_TRACE_OPT);
		}

		command.add(repositoryPath);

		command.add(setupFilePath);

		for (String attr : attributes) {
			command.add(attr);
		}

		command.add("-data"); //$NON-NLS-1$
		command.add(workspaceLocation);

		command.add("-vmargs"); //$NON-NLS-1$
		command.add(VMARGS_OPTION + PROP_SETUP_CONFIRM_SKIP + "=true"); //$NON-NLS-1$ 
		command.add(VMARGS_OPTION + PROP_SETUP_OFFLINE_STARTUP + "=" + false); //$NON-NLS-1$ 
		command.add(VMARGS_OPTION + PROP_SETUP_MIRRORS_STARTUP + "=" + true); //$NON-NLS-1$ 

		if (debug) {
			command.add("-Xdebug"); //$NON-NLS-1$
			command.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8123"); //$NON-NLS-1$
		}

		ProcessBuilder builder = new ProcessBuilder(command);
		Process process;
		try {
			process = builder.start();
		} catch (IOException e) {
			throw new DiesOn(DeathType.FATAL).duedTo(e).ready();
		}

		// output both stdout and stderr data from proc to stdout of this
		// process
		StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), out);
		StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), out);
		new Thread(errorGobbler).start();
		new Thread(outputGobbler).start();

		int returnValue;
		try {
			returnValue = process.waitFor();
		} catch (InterruptedException e) {
			throw new DiesOn(DeathType.FATAL).duedTo(e).ready();
		}

		return Returns.valueOf(returnValue).code();
	}

	/**
	 * Stream gobbler.
	 * 
	 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
	 */
	static class StreamGobbler implements Runnable {
		/** The stream. */
		private InputStream is;

		/** Output {@link java.io.Writer}. */
		private ThrowingPrintWriter out;

		/**
		 * Reads everything from is until empty.
		 * 
		 * @param is
		 *            the stream to read.
		 * @param out
		 *            the writer to write.
		 */
		StreamGobbler(InputStream is, ThrowingPrintWriter out) {
			this.is = is;
			this.out = out;
		}

		/**
		 * {@inheritDoc}.
		 */
		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					out.println(line);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

}

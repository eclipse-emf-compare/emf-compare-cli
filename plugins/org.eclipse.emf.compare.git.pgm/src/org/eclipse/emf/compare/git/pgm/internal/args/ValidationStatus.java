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

/**
 * Status used for validating command arguments.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
public final class ValidationStatus {

	/** Unique instance of the valid status. */
	public static final ValidationStatus OK_STATUS = new ValidationStatus(true, false, null);

	/**
	 * Holds <code>true</code> if the usage of the command should be printed.
	 */
	private final boolean printUsage;

	/**
	 * Holds the message to print or <code>null</code> otherwise.
	 */
	private final String message;

	/**
	 * Holds <code>true</code> if the status is valid.
	 */
	private final boolean isValid;

	/**
	 * Constructor.
	 * 
	 * @param isValid
	 *            {@link #isValid}
	 * @param printUsage
	 *            {@link #printUsage}
	 * @param message
	 *            {@link #message} or <code>null</code>.
	 */
	private ValidationStatus(boolean isValid, boolean printUsage, String message) {
		super();
		this.isValid = isValid;
		this.printUsage = printUsage;
		this.message = message;
	}

	/**
	 * Creates an error status that will not display the usage of the command.
	 * 
	 * @param message
	 *            to display.
	 * @return error status.
	 */
	public static ValidationStatus createErrorStatus(String message) {
		return new ValidationStatus(false, false, message);
	}

	/**
	 * Creates an error status that will display the usage of the command.
	 * 
	 * @param message
	 *            to display.
	 * @return error status.
	 */
	public static ValidationStatus createErrorStatusWithUsage(String message) {
		return new ValidationStatus(false, true, message);
	}

	/**
	 * Creates an error status with no message that will simply display the usage.
	 * 
	 * @return error status.
	 */
	public static ValidationStatus createErrorStatusWithUsage() {
		return new ValidationStatus(false, true, null);
	}

	public boolean isPrintUsage() {
		return printUsage;
	}

	public String getMessage() {
		return message;
	}

	public boolean isValid() {
		return isValid;
	}

}

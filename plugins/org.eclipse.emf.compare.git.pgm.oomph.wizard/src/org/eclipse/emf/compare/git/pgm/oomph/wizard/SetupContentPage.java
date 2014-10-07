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
package org.eclipse.emf.compare.git.pgm.oomph.wizard;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.File;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This page handles the content of the model created by this wizard.
 * 
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 *
 */
public class SetupContentPage extends WizardPage {

	/** Vertical spacing between widgets. */
	private static final int VERTICAL_SPACING = 9;

	/** The root object name typed by the user. */
	private String rootObjectName;
	/** The selected state of the default workspace checkbox. */
	private boolean useDefaultWsPath;
	/** The workspace path typed by the user. */
	private String wsPath;
	/** The selected state of the default installation checkbox. */
	private boolean useDefaultInstallPath;
	/** The installation path typed by the user. */
	private String installPath;
	/** The selected state of the import all projects checkbox. */
	private boolean importAll;

	/** Widget to capture the root object name typed by the user. */
	private Text rootObjectNameText;
	/** Widget to capture the workspace path typed by the user. */
	private Text workspacePathText;
	/** Widget to capture the installation path typed by the user. */
	private Text installationPathText;

	/** Handles the Text Widgets changes. */
	private ModifyListener modifyTextListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			dialogChanged();
		}
	};

	/**
	 * Default constructor.
	 */
	public SetupContentPage() {
		super("wizardPage");
		setTitle("Setup for EMF Compare Git PGM");
		setDescription("This wizard creates a new setup model for EMF Compare Git PGM.");
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				Activator.PLUGIN_ID, "icons/emfcompare-logo-wiz.png"));
	}

	/**
	 * Creates control.
	 * 
	 * @param parent
	 *            the parent widget.
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = VERTICAL_SPACING;
		container.setLayout(layout);

		createRootObjectNamePart(container);
		createWorkspacePathPart(container);
		createInstallationPathPart(container);
		createImportProjectsPart(container);

		// Add listeners to Text Widgets
		rootObjectNameText.addModifyListener(modifyTextListener);
		workspacePathText.addModifyListener(modifyTextListener);
		installationPathText.addModifyListener(modifyTextListener);

		// Set default values
		rootObjectNameText.setText("");
		workspacePathText.setText("");
		installationPathText.setText("");

		setControl(container);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#canFlipToNextPage()
	 */
	@Override
	public boolean canFlipToNextPage() {
		return false;
	}

	/**
	 * Get the root object name typed by the user.
	 * 
	 * @return the root object name typed by the user.
	 */
	public String getRootObjectName() {
		return rootObjectName;
	}

	/**
	 * Get the selected state of the default workspace checkbox.
	 * 
	 * @return the selected state of the default workspace checkbox.
	 */
	public boolean useDefaultWorkspacePath() {
		return useDefaultWsPath;
	}

	/**
	 * Get the workspace path typed by the user.
	 * 
	 * @return the workspace path typed by the user.
	 */
	public String getWorkspacePath() {
		return wsPath;
	}

	/**
	 * Get the selected state of the default installation checkbox.
	 * 
	 * @return the selected state of the default installation checkbox.
	 */
	public boolean useDefaultInstallationPath() {
		return useDefaultInstallPath;
	}

	/**
	 * Get the installation path typed by the user.
	 * 
	 * @return the installation path typed by the user.
	 */
	public String getInstallationPath() {
		return installPath;
	}

	/**
	 * Get the selected state of the import all projects checkbox.
	 * 
	 * @return the selected state of the import all projects checkbox.
	 */
	public boolean importAll() {
		return importAll;
	}

	/**
	 * Creates widgets related to the root object's name.
	 * 
	 * @param container
	 *            the parent widget.
	 */
	private void createRootObjectNamePart(Composite container) {
		Label rootObjectNameLabel = new Label(container, SWT.NULL);
		rootObjectNameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
				false, false));
		rootObjectNameLabel.setText("Model's Root Object Name:");

		rootObjectNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
		rootObjectNameText
				.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		rootObjectNameText.setToolTipText("The model's root object name.");
	}

	/**
	 * Creates widgets related to the workspace path.
	 * 
	 * @param container
	 *            the parent widget.
	 */
	private void createWorkspacePathPart(Composite container) {
		Label wsLabel = new Label(container, SWT.NULL);
		wsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		wsLabel.setText("Workspace Location:");

		Composite wsGroup = new Composite(container, SWT.NONE);
		wsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		GridLayout wsLayout = new GridLayout(3, false);
		wsLayout.marginWidth = 0;
		wsLayout.marginHeight = 0;
		wsLayout.horizontalSpacing = 10;
		wsGroup.setLayout(wsLayout);

		final Button defaultWsCheckBox = new Button(wsGroup, SWT.CHECK);
		defaultWsCheckBox.setText("Default");
		defaultWsCheckBox
				.setToolTipText("A default workspace path will be used.");
		defaultWsCheckBox.setSelection(false);
		useDefaultWsPath = false;

		workspacePathText = new Text(wsGroup, SWT.BORDER | SWT.SINGLE);
		workspacePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
				true, false));
		workspacePathText.setToolTipText("The path to use for the workspace.");
		workspacePathText.setEnabled(true);

		final Button browseWorkspace = new Button(wsGroup, SWT.PUSH);
		browseWorkspace.setText("Browse...");
		browseWorkspace.setEnabled(true);

		defaultWsCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean checked = defaultWsCheckBox.getSelection();
				if (checked) {
					browseWorkspace.setEnabled(false);
					workspacePathText.setEnabled(false);
					useDefaultWsPath = true;
				} else {
					browseWorkspace.setEnabled(true);
					workspacePathText.setEnabled(true);
					useDefaultWsPath = false;
				}
				dialogChanged();
			}
		});

		browseWorkspace.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String previous = workspacePathText.getText();
				File previousFile = new File(previous);
				String result;
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				if (previousFile.exists() && previousFile.isDirectory()) {
					dialog.setFilterPath(previousFile.getPath());
				}
				result = dialog.open();
				if (result != null) {
					workspacePathText.setText(result);
					wsPath = result;
				}
			}
		});
	}

	/**
	 * Creates widgets related to the installation path.
	 * 
	 * @param container
	 *            the parent widget.
	 */
	private void createInstallationPathPart(Composite container) {
		Label installLabel = new Label(container, SWT.NULL);
		installLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
				false));
		installLabel.setText("Installation Location:");

		Composite installGroup = new Composite(container, SWT.NONE);
		installGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));
		GridLayout installLayout = new GridLayout(3, false);
		installLayout.marginWidth = 0;
		installLayout.marginHeight = 0;
		installLayout.horizontalSpacing = 10;
		installGroup.setLayout(installLayout);

		final Button defaultInstallCheckBox = new Button(installGroup,
				SWT.CHECK);
		defaultInstallCheckBox.setText("Default");
		defaultInstallCheckBox
				.setToolTipText("A default instllation path will be used.");
		defaultInstallCheckBox.setSelection(false);
		useDefaultInstallPath = false;

		installationPathText = new Text(installGroup, SWT.BORDER | SWT.SINGLE);
		installationPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
				true, false));
		installationPathText
				.setToolTipText("The path to use for the installation.");
		installationPathText.setEnabled(true);

		final Button browseInstallation = new Button(installGroup, SWT.PUSH);
		browseInstallation.setText("Browse...");
		browseInstallation.setEnabled(true);

		defaultInstallCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean checked = defaultInstallCheckBox.getSelection();
				if (checked) {
					browseInstallation.setEnabled(false);
					installationPathText.setEnabled(false);
					useDefaultInstallPath = true;
				} else {
					browseInstallation.setEnabled(true);
					installationPathText.setEnabled(true);
					useDefaultInstallPath = false;
				}
				dialogChanged();
			}
		});

		browseInstallation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String previous = installationPathText.getText();
				File previousFile = new File(previous);
				String result;
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				if (previousFile.exists() && previousFile.isDirectory()) {
					dialog.setFilterPath(previousFile.getPath());
				}
				result = dialog.open();
				if (result != null) {
					installationPathText.setText(result);
					installPath = result;
				}
			}
		});
	}

	/**
	 * Creates widgets related to the import of projects.
	 * 
	 * @param container
	 *            the parent widget.
	 */
	private void createImportProjectsPart(Composite container) {
		final Button importAllCheckBox = new Button(container, SWT.CHECK);
		importAllCheckBox
				.setText("Import All Projects Found In Current Repository.");
		importAllCheckBox
				.setToolTipText("Import all projects found in the current repository.");
		importAllCheckBox.setSelection(true);
		importAll = true;

		importAllCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean checked = importAllCheckBox.getSelection();
				if (checked) {
					importAll = true;
				} else {
					importAll = false;
				}
			}
		});

		final Label importProjectsLabel = new Label(container, SWT.NULL);
		importProjectsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
				true, false));
		importProjectsLabel
				.setText("If you want to specify which projets to import, you will have to set the paths in the properties\n"
						+ "of the Source Locator element of the setup model that will be created.");
		importProjectsLabel.setEnabled(true);
	}

	/**
	 * Handles dialog changes.
	 */
	private void dialogChanged() {
		final String rootText = rootObjectNameText.getText();
		final String wsText = workspacePathText.getText();
		final String installText = installationPathText.getText();
		String message = null;

		if (isNullOrEmpty(installText) && !useDefaultInstallPath) {
			message = "Add an installation path or select default.";
		} else {
			installPath = installText;
		}
		if (isNullOrEmpty(wsText) && !useDefaultWsPath) {
			message = "Add a workspace path or select default.";
		} else {
			wsPath = wsText;
		}
		if (isNullOrEmpty(rootText)) {
			message = "Model's root object name cannot be empty.";
		} else {
			rootObjectName = rootText;
		}

		setErrorMessage(message);
		setPageComplete(message == null);
	}
}

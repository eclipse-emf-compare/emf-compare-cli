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
package org.eclipse.emf.compare.git.pgm.internal.app;

import static org.eclipse.emf.compare.git.pgm.internal.util.EMFCompareGitPGMUtil.EOL;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.git.pgm.AbstractApplicationTest;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

/**
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 */
@SuppressWarnings("nls")
public abstract class AbstractLogicalCommandApplicationTest extends AbstractApplicationTest {

	/**
	 * Assert that there is no conflict marker in the file (( <<<<<<<<<< or ========= or >>>>>>>>>>>).
	 * <p>
	 * In fact this test tries to load the resource.
	 * </p>
	 * 
	 * @param paths
	 * @throws IOException
	 * @throws AssertionError
	 */
	protected void assertNoConflitMarker(Path... paths) throws AssertionError, IOException {
		ResourceSet resourceSet = new ResourceSetImpl();
		for (Path p : paths) {
			try {
				if (p.toFile().exists()) {
					Resource resource = resourceSet.getResource(URI.createFileURI(p.toString()), true);
					assertNotNull(resource);
				} else {
					throw new AssertionError("The file " + p.toString() + " does not exist.");
				}
			} catch (Exception e) {
				throw new AssertionError("Error wile parsing resource " + p.toString() + EOL
						+ getConfigurationMessage(), e);
			}
		}
	}

	protected void assertExistInResource(Path resourcePath, String... fragments) throws IOException {
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource resource = resourceSet.getResource(URI.createFileURI(resourcePath.toString()), true);
		assertNotNull(resource);
		for (String fragment : fragments) {
			EObject eObject = resource.getEObject(fragment);
			assertNotNull("Element with framgment " + fragment + " does not exist" + EOL
					+ getConfigurationMessage(), eObject);
		}
	}

}

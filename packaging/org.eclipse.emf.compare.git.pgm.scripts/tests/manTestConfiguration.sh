#!/bin/bash
# ====================================================================
# Copyright (c) 2014 Obeo
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#    Obeo - initial API and implementation
# ====================================================================

# Running source manTestConfiguation.sh will configure your testing environment.
# It will:
#	Exports the EMF_COMPARE_GIT_PGM_PATH variable
#	Adds EMF Compare CLI scripts to the path

#Location of the current script
scriptLocation="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
OS=""
FILE_EXT=""
if [[ "${OSTYPE}" == "linux"* || "${OSTYPE}" == "freebsd"* ]]; then
	OS="linux/gtk"
elif [[ "${OSTYPE}" == "cygwin"* ]]; then
	OS="win32"
	FILE_EXT=".exe"
elif [[ "${OSTYPE}" == "darwin"* ]]; then
	OS="macosx"
else
	LSCRITICAL "Unknown 'OSTYPE'=${OSTYPE}."
	exit -1
fi

if [[ $(uname -m) == *"64"* ]]; then
	ARCH="x86_64"
else
	ARCH="x86"
fi

PLATFORM_SPECIFIER="${OSWS}.${ARCH}"

echo "Exporting application variables EMF_COMPARE_GIT_PGM_PATH"
export EMF_COMPARE_GIT_PGM_PATH="$scriptLocation/../../org.eclipse.emf.compare.git.pgm.product/target/products/org.eclipse.emf.compare.git.pgm.product/${OS}/${ARCH}/emfcompare-git-pgm${FILE_EXT}"

echo "Adding EMF Compare CLI scripts to the PATH"
# Adds the location of the folder holding git scripts to the path. This way the command "git SCRIPT_NAME ARGS" will work.
PATH=$scriptLocation/../scripts:$PATH

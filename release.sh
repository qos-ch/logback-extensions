#!/bin/sh -e
#
# Copyright (C) 2014 The logback-extensions developers (logback-user@qos.ch)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# -----------------------------------------------------------------------------
# This script performs a logback-extensions release:
#  * sets the version (removes SNAPSHOT suffix from current version, and bumps
#    to the next SNAPSHOT version when release is finished)
#  * tags the source in GitHub (e.g., with "v_1.0.2")
#  * deploys the signed jars to Sonatype
#  * updates the README.md with the current version info
#
# Usage:
#   ./release.sh [--dryrun]
#
# -----------------------------------------------------------------------------
version=$(mvn help:evaluate -Dexpression=project.version | grep '^[^[]')
version=${version%-SNAPSHOT}
outdir=$PWD/target
readme=$PWD/README.md

if [ "x$1" == "x--dryrun" ]; then
  echo "[dryrun] just a test!"
  dryrun=true
fi
echo "Starting release process for logback-extensions ${version}..."

#
# Build the JAR and print its SHA1. The last line uses GNU sed (gsed)
# to update the README with the current release version.
#
mvnDryRun=
if [ ! ${dryrun} ]; then
  mvnDryRun=-DdryRun
fi

mvn release:clean
mvn -Dtag=v_${version} $mvnDryRun release:prepare
mvn -Dtag=v_${version} $mvnDryRun release:perform

# Update the version number in the README
echo "Updating README.md..."
gsed -i -e "s/\\d\+\.\\d\+\.\\d\+/${version}/" ${readme}

if [ ! ${dryrun} ]; then
  git add ${readme}
  git commit -m "Update README for release ${version}"
else
  echo '[dryrun] skip commit README...'
fi

if [ ! ${dryrun} ]; then
  echo Done. Push changes to GitHub!!
else
  echo Done...just a dryrun!!
  mvn release:clean
fi

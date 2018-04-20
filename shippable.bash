#!/bin/bash

# This script contains the CI build workflow for this repository
# Author: Roland Kluge
# Date: 2018-04-20
#

# Change into Tycho root parent directory
cd ./src/

echo "Run Tycho (clean compile)"
mvn -q clean compile integration-test || exit -1

echo "Publish JUnit test results"
find . -path "*/target/*/TEST*.xml" -exec cp {} /root/src/github.com/eMoflon/cmoflon/shippable/testresults/ \;

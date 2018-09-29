import sys
import pathlib

# This script serves to check the

if len(sys.argv) != 2:
    print("Expecting one command-line argument")
    sys.exit(1)

ktcParameterValue=1.2
resultsFolder = pathlib.Path(sys.argv[1])
outputFolder = resultsFolder / "output"
topologyBeforeFile = outputFolder / "01_TopologyBefore.txt"
topologyAfterFile = outputFolder / "02_TopologyAfter.txt"

with open(topologyBeforeFile, "r") as f:
    content = f.readlines()

content = [x.strip() for x in content]


# Documentation for cMoflon (UNDER CONSTRUCTION)
Documentation for cMoflon, the eMoflon derivate for generating Contiki code
----

## How to set up a small case study (walkthrough)

### Setup: How to install cMoflon and all required software
The currently requires a mixed environment with a Windows system (for cMoflon/eMoflon and Enterprise Architect) and a Linux VM (provided as Instant Contiki).
This version of cMoflon has been tested with Eclipse Neon.2 (4.6.2) and eMoflon 2.26.0 and Enterprise Architect 12

1. **Install Eclipse with Modeling Components Neon.2 (or newer)**
  * All Eclipse packages are available here: https://eclipse.org/downloads/
1. **Install Enterprise Architect 12 (or newer)**
  * A 30-days trial version of Enterprise Architect is available here: https://www.sparxsystems.de/uml/download-trial/
1. **Install eMoflon 2.26.0**
  * To install eMoflon in Eclipse use the following update site: http://emoflon.github.io/eclipse-plugin/emoflon_2.26.0/update-site2/
  * You only need to install the feature *eMoflon Core*
  * Additionally, download, unpack and install the eMoflon addin for Enterprice Architect from here: https://emoflon.github.io/eclipse-plugin/emoflon_2.26.0/update-site2/ea-ecore-addin.zip
1. **Install cMoflon 0.0.1**
  * Update Site: http://emoflon.github.io/cmoflon/update-site/
  * You may also use the following: <a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=3266408" class="drag" title="Drag to your running Eclipse workspace."><img class="img-responsive" src="https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png" alt="Drag to your running Eclipse workspace." /></a> from Eclipse Marketplace via drap and drop.
1. **Get Contiki**
 * Download the *Instant Contiki 3.0* VM from here: https://sourceforge.net/projects/contiki/files/Instant%20Contiki/Instant%20Contiki%203.0/InstantContiki3.0.zip/download 
 * You will need Virtual Box to run the VM (https://www.virtualbox.org/)
1. **Get ConMEF framework**
 * A snapshot of the proper version will be made available later.
 * Clone the Contiki evaluation framework from here (non-public): https://git.tk.informatik.tu-darmstadt.de/michael.stein/topology-control-evaluation
 * The root folder of the working copy will be refered to as *TCEval* in the following.
 
## How to specify a sample TC algorithm/test the existing TC algorithms
1. Open a fresh workspace in Eclipse.
1. Navigate to *File->New->Other->eMoflon*, select *New cMoflon Metamodel Wizard* add a name (e.g., *cMoflonTestSpecification*), and press *Finish*.
1. This will create a new project, a so-called metamodel project. Open it and double-click the .eap file
1. Specify your TC algorithm or algorithms according to the pre-implemented ones.
1. Hit the *Validate* button and select your project in eclipse and refresh it. This will create two new projects, named after the corresponding ECore Package in Enterprise Architect.
 * The project ending with _C contains the Contiki code
 * The other project contains regular EMF code (e.g., for unit testing or Java-based debugging).
1. Inside the Contiki project (the one ending with _C), the two *.properties files serve for customizing the generated code. Fill those according to the instructions
1. Select the Contiki project and hit the blue build button, the generated code is placed in the /gen folder.

### How to test a TC algorithm generated by cMoflon in Cooja Simulator
Cooja is the network simulator that ships with Contiki.
It is quite a powerful simulator for a first test of your code.

1. Store the *TCEval/src* folder of the evaluation framework on a portable drive to be able to access it from the VM later.
1. Copy both generated files into the folder *TCEval/src/components/topologycontrol/*
1. Add a line for the .c file to *TCEval/src/components/topologycontrol/Makefile*
1. Add the new constant (found in the .c file in the PROCESS section) to *TCEval/src/app-conf-constants.h*
1. Copy *TCEval/src/app-conf-default*, remove the *_default* suffix and adjust the settings within to your wishes
1. Copy *TCEval/src/Makefile-conf-default.include*, remove the *_default*, link to the contiki path in the vm, and **do not use** IPv6
1. Alternatively you can use the files provided inside the /resources folder.
1. Start the VM, and mount the portable drive.
1. Open a terminal and navigate to */contiki/tools/cooja* and use the command ```ant run``` to start the Cooja Simulator.
1. Hit *CTRL+N* to create a new simulation
1. Navigate to *Motes->Add motes->Create New Mote Type -> Sky mote*, browse to *TCEval/src/app.c*, and hit compile.
1. After compiling is done, create the motes and run the simulation.

### How to test a TC algorithm generated by cMoflon using FlockLab
FlockLab (https://www.flocklab.ethz.ch/) is a wireless sensor testbed at ETH Zurich.

1. You will need a Flocklab account for testing so create one here: https://www.flocklab.ethz.ch/user/login.php
1. Follow all the Steps for Cooja until compilation has terminated.
1. Then, unmount the drive from the VM
1. Go to the test image section (https://www.flocklab.ethz.ch/user/images.php) and upload the compiled *app.sky* file, located in *TCEval/src*.
1. Copy the ImageID into the xml configuration provided in the */resources* folder in the Eclipse Project and then upload it to the test section
1. After the Test is run, you will receive an e-mail with the results in a zip folder.

### How to use the evaluation tools

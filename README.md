# Documentation for cMoflon
cMoflon is a code generator for topology control algorithms based on the wireless sensor operating system Contiki.
cMoflon is a variant of [eMoflon](https://emoflon.github.io).
In cMoflon, topology control algorithms are specified using Ecore and Story-Driven Modeling, a programmed graph transformation dialect.
The target programmling language is (embedded) C.

[![Project Stats](https://www.openhub.net/p/cmoflon/widgets/project_thin_badge.gif)](https://www.openhub.net/p/cmoflon)

## Supplementary material for ECMFA 2017
We have set up a separate document that links to supplementary material (e.g, a SHARE VM) related to the cMoflon paper at ECMFA 2017:
https://github.com/eMoflon/cmoflon/blob/master/ECMFA2017/README_ECMFA2017.md

## Troubleshooting
If you encounter any problems, please post an issue (https://github.com/eMoflon/cmoflon/issues) or send me (Roland Kluge) a message

## Short walk through
The following steps will enable you to generate code for the specifications shown in ECMFA'17 (under submission).
If you want to run the generated code in the testbed, follow the steps in the *Complete walkthrough* section.

1. **Install Eclipse with Modeling Components Neon.2 (or newer)**
   * All Eclipse packages are available here: https://eclipse.org/downloads/
1. **Install Enterprise Architect 12 (or later)**
   * A 30-days trial version of Enterprise Architect is available here: https://www.sparxsystems.de/uml/download-trial/
1. **Install eMoflon 3.4.0**
   * Follow the installation instructions from here:  https://github.com/eMoflon/emoflon-tool/releases/tag/emoflon-tie_3.4.0
   * From eMoflon Tool you only need the basic eMoflon Tool feature
1. **Install cMoflon 1.0.0**
   * Stable update site:
   * Unstable update Site: http://emoflon.github.io/cmoflon/updatesite/
   * You may also use the following: <a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=3266408" class="drag" title="Drag to your running Eclipse workspace."><img class="img-responsive" src="https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png" alt="Drag to your running Eclipse workspace." /></a> from Eclipse Marketplace via drap and drop.
1. **Demo specification**
   1. Download and extract the following archive: https://github.com/eMoflon/cmoflon/releases/download/cmoflon_1.0.0/cMoflonDemo.zip
   1. The three contained projects can be imported into your Eclipse as is. 
   1. Select the projects and perform a full build (e.g., via the *context menu -> eMoflon -> Build selected projects fully*).
   1. After the build was successful the project *cMoflonDemoLanguage* contains the generated C code (/gen folder)

## Complete walkthrough

### Setup: How to install cMoflon and all required software
The currently requires a mixed environment with a Windows system (for cMoflon/eMoflon and Enterprise Architect) and a Linux VM (provided as Instant Contiki).
This version of cMoflon has been tested with Eclipse Neon.2 (4.6.2) and eMoflon 2.26.0 and Enterprise Architect 12

1. **Install Eclipse with Modeling Components Neon.2 (or newer)**
   * All Eclipse packages are available here: https://eclipse.org/downloads/
1. **Install Enterprise Architect 12 (or later)**
   * A 30-days trial version of Enterprise Architect is available here: https://www.sparxsystems.de/uml/download-trial/
1. **Install eMoflon 3.4.0**
   * Follow the installation instructions from here:  https://github.com/eMoflon/emoflon-tool/releases/tag/emoflon-tie_3.4.0
   * From eMoflon Tool you only need the basic eMoflon Tool feature, do also install the EA add-in
1. **Install cMoflon 1.0.0**
   * Stable update site:
   * Unstable update Site: http://emoflon.github.io/cmoflon/updatesite/
   <!--* You may also use the following: <a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=3266408" class="drag" title="Drag to your running Eclipse workspace."><img class="img-responsive" src="https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png" alt="Drag to your running Eclipse workspace." /></a> from Eclipse Marketplace via drap and drop.-->
1. **Get Contiki 3.0**
   * Download the *Instant Contiki 3.0* VM from here: https://sourceforge.net/projects/contiki/files/Instant%20Contiki/Instant%20Contiki%203.0/InstantContiki3.0.zip/download 
   * You will need Virtual Box to run the VM (https://www.virtualbox.org/)
   * Detailed setup instructions can be found here: http://www.contiki-os.org/start.html
      * Instant Contiki fix: To run the sample simulation, you will need to call ```git submodule update --init```. 
     Otherwise, starting Cooja will fail.
1. **Get ToCoCo 2.0.0**
   * We suggest to checkout the ToCoCo framework from inside the Instant Contiki VM
   * For cMoflon developers: the Contiki evaluation framework repository is available from here: 
      * URL: https://github.com/eMoflon/ToCoCo
   * The original ToCoCo base repository without the cMoflon specific scripts and tools is available here: https://github.com/steinmic/ToCoCo/releases/tag/tococo_2.0
   * The root folder of the working copy will be refered to as *$ToCoCo* in the following.
 
## How to specify a sample TC algorithm/test the existing TC algorithms
1. Open a fresh workspace in Eclipse.
1. Navigate to *File->New->Other->eMoflon*, select *New cMoflon Metamodel Wizard* add a name (e.g., *cMoflonTestSpecification*), and press *Finish*.
1. This will create a new project, a so-called metamodel project. Open it and double-click the .eap file
1. Specify your TC algorithm or algorithms according to the pre-implemented ones.
1. Hit the *Validate* button and select your project in eclipse and refresh it. This will create a new project, named after the corresponding ECore Package in Enterprise Architect.
1. Inside the newly created project, edit the *cMoflon.properties* for customizing the generated code. Fill the property file according to the instructions here: https://github.com/eMoflon/cmoflon/wiki/cMoflon-Properties
1. Select the Contiki project and hit the black-hammer button, the generated code is placed in the */gen* folder.
      * the black-hammer button is only visible in the eMoflon perspective, to open the perspective navigate to *Window->Perspective->Open Perspective->Other->eMoflon*

### How to test a TC algorithm generated by cMoflon in Cooja Simulator
Cooja is the network simulator that ships with Contiki.
It is quite a powerful simulator for a first test of your code.
We suggest to mount a [shared folder](https://help.ubuntu.com/community/VirtualBox/SharedFolders) to easily copy cMoflon-generated files to the Instant Contiki VM.

1. Copy both generated files (.c and .h) from the */gen* folder of your cMoflon workspace into the folder *$ToCoCo/src/components/topologycontrol/*
1. Add a line for the .c file to *$ToCoCo/src/components/topologycontrol/Makefile*
1. Add the new constant (found in the .c file in the PROCESS section) to *$ToCoCo/src/app-conf-constants.h*
1. Copy *$ToCoCo/src/app-conf-default*, and remove the *_default* suffix
   * Add the following lines
      * ```#define TOPOLOGYCONTROL_LINKS_HAVE_STATES``` (This ensures that the type ```neighbor_t``` has a member called ```state```)
   * Adjust the settings to your wishes
1. Copy *$ToCoCo/src/Makefile-conf-default.include*, and remove the *_default*, 
   * Set the *Contiki* property to the contiki path in the VM (e.g., ```Contiki=/home/user/contiki``` in Instant Contiki)
   * **Do not use IPv6** (the resulting image would be too large for Sky motes): ```NETWORK_IPV6 = 0```
   * Alternatively, you can use the files provided inside the /resources folder.
1. Open a terminal, navigate to */contiki/tools/cooja*, and use the command ```ant run``` to start the Cooja Simulator.
1. Hit *CTRL+N* to create a new simulation
1. Navigate to *Motes->Add motes->Create New Mote Type->Sky mote*, browse to *$ToCoCo/src/app.c*, and hit *Compile*.
1. After compiling is done, create the motes, and run the simulation.

### How to test a TC algorithm generated by cMoflon using FlockLab
FlockLab (https://www.flocklab.ethz.ch/) is a wireless sensor testbed at ETH Zurich.

1. You will need a Flocklab account for testing so create one here: https://www.flocklab.ethz.ch/user/login.php
1. Follow all the Steps for Cooja until compilation has terminated.
1. Go to the test image section (https://www.flocklab.ethz.ch/user/images.php) and upload the compiled *app.sky* file, located in *$ToCoCo/src*.
   * As *OS*, choose *TinyOS*.
   * As *Platform*, choose *Tmote*.
1. Create a copy of the xml configuration file *org.cmoflon.runtime/resources/Evaluation/flocklabConfig.xml*.
   * Copy the image ID into the ```<dbImageId/>``` element
   * (optional) Adjust the duration of the test (```<durationSecs/>```, e.g., 960).
1. Create a new test (https://www.flocklab.ethz.ch/user/index.php) by uploading the adjusted xml configuration file.
   * The image ID can be modified as long as the test has not started yet.
1. After the test is run, you will receive an e-mail with the results in a zip folder.
   * The contents of the *serial.csv* file allow you to debug/analyze the topology control behavior (Watch out for lines like ```[topologycontrol-*]```).

### How to use the evaluation tools (excerpt)
The ToCoCo evaluation tools help you to analyze the serial logging output of the FlockLab sensor nodes.

#### Batch compilation
If you are interested in compiling all of the considered topology control algorithms as a batch, we recommend to use the Bash script that can be found at *$ToCoCo/evaluation/src/BatchCompilation*.
This script is tailored to the topology control algorithms of the ECMFA evaluation.

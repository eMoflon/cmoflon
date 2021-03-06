# Documentation for cMoflon
cMoflon is a code generator for topology control algorithms based on the wireless sensor operating system Contiki.
cMoflon is a variant of [eMoflon](https://emoflon.github.io).
In cMoflon, topology control algorithms are specified using Ecore and Story-Driven Modeling, a programmed graph transformation dialect.
The target programmling language is (embedded) C.

[![Project Stats](https://www.openhub.net/p/cmoflon/widgets/project_thin_badge.gif)](https://www.openhub.net/p/cmoflon)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/59c4befefa994cea973be2faba81c01f)](https://www.codacy.com/app/RolandKluge/cmoflon?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=eMoflon/cmoflon&amp;utm_campaign=Badge_Grade)

If you encounter any problems during the installation or usage of cMoflon, please post an issue: https://github.com/eMoflon/cmoflon/issues .

## Supplementary material for ECMFA 2017
We have set up a separate document that links to supplementary material (e.g, a SHARE VM) related to the cMoflon paper at ECMFA 2017:
https://github.com/eMoflon/cmoflon-examples/blob/master/Talks/ECMFA2017/README_ECMFA2017.md

## cMoflon user setup
If you want to run the generated code in the testbed, follow the steps in the *Complete walkthrough* section.
This version of cMoflon has been tested with Eclipse Oxygen and eMoflon 3.4.0 and Enterprise Architect 12.

1. **Install Eclipse 2018-09 R with Modeling Components Oxygen (or newer)**
   * All Eclipse packages are available here: https://eclipse.org/downloads/
1. **Install Enterprise Architect 12 (or later)**
   * A 30-days trial version of Enterprise Architect is available here: https://www.sparxsystems.de/uml/download-trial/
1. **Install cMoflon**
   1. In Eclipse, navigate to the *Install* dialog (*Help &rarr; Install New Software...*).
   1. First, install only *PlantUML 1.1.21* (or above)&ndash;no need to restart Eclipse afterwards: https://hallvard.github.io/plantuml/
   1. Again, open the *Install* dialog.
   1. Paste the following Eclipse update site: https://raw.githubusercontent.com/eMoflon/cmoflon-updatesite/cmoflon_2.0.0/updatesite
       * Note: The most recent (maybe unstable) Eclipse update site is located here: https://raw.githubusercontent.com/eMoflon/cmoflon-updatesite/master/updatesite
   1. Select *Manage...*.
   1. Enable at least the following update sites:
        * https://emoflon.org/eclipse-plugin/beta/updatesite/ (aka. https://emoflon.github.io/eclipse-plugin/beta/updatesite )
        * http://emoflon.org/emoflon-core-updatesite/stable/updatesite/
        * "Latest Eclipse release"
   1. Go back via *Apply and Close*.
   1. Make sure that the option *Contact all update sites during install to find required software* is enabled.
   1. Select *cMoflon* and complete the installation via *Next* etc.
        * Note: The dependency resolution may take some time...
   1. Restart Eclipse, open a fresh workspace, and switch to the *eMoflon* perspective.
1. **Demo specification**
   1. Import the sample projects into your workspace as follows: 
       * Go *Right-click &rarr; Import... &rarr; Git/Projects from Git*.
       * Choose the following URL: https://github.com/eMoflon/cmoflon-examples.git 
       * Follow the displayed instructions
       * When asked, choose the projects 
           * *CMoflonDemoEASpecification*
           * *CMoflonDemoLanguage*.
   1. Select the projects and perform a full build (e.g., via the *Right-click &rarr; eMoflon &rarr; Build selected projects fully*).
   1. After the build was successful, the project *CMoflonDemoLanguage* contains the generated C code (*/gen* folder).
1. **Get Contiki 3.0**
   * Download the *Instant Contiki 3.0* Virtual Box VM from here: https://sourceforge.net/projects/contiki/files/Instant%20Contiki/Instant%20Contiki%203.0/InstantContiki3.0.zip/download 
       * Get Virtual Box here: https://www.virtualbox.org/
   * Detailed setup instructions can be found here: http://www.contiki-os.org/start.html
   * If you use Instant Contiki, you need to call ```git submodule update --init```. 
     Otherwise, starting Cooja will fail.
1. **Get ToCoco 2.0.0**
   * Checkout the ToCoCo framework from inside the Instant Contiki VM: `git clone https://github.com/eMoflon/ToCoCo.git`
   * The root folder of the working copy is called *$ToCoCo* in the following.
 
## How to specify a sample TC algorithm/test the existing TC algorithms
1. Open a fresh workspace in Eclipse.
1. Navigate to *File->New->Other->cMoflon*, select *New cMoflon Metamodel Wizard* add a name (e.g., *CMoflonTestSpecification*), and press *Finish*.
1. This will create a new project that contains the visual specification of your algorithm.
1. Double-click the .eap file in the created project.
1. Optinally, you can inspect the specification of the Maxpower topology control algorithm as follows:
    1. In the Project Explorer (usually on the right-hand side), navigate to *cMoflon/«EPackage» CMoflonTopologyControl/CMoflonToplogyControl*
    1. In the diagram that opens up, first click and then double-click *MaxpowerTopologyControl::run*.
1. To trigger the export of your specification from Enterprise Architect, hit the *Validate* button.
1. Switch to Eclipse, select the project *CMoflonTestSpecification*, and refresh it via the context menu.
1. A new project *CMoflonTopologyControl*, named after the EPackage in Enterprise Architect, appears.
1. After the Eclipse build is complete, you need to adjust the file *cMoflon.properties* of *CMoflonTopologyControl* as follows: Add the value *MaxpowerTopologyControl* to the property *tc.algorithms*.
1. The Eclipse build should restart (otherwise, trigger it manually) and you should find generated .c and .h files in */gen*.

### How to test a TC algorithm generated by cMoflon in Cooja Simulator
Cooja is the network simulator that ships with Contiki.
It is quite a powerful simulator for a first test of your code.
We suggest to mount a [shared folder](https://help.ubuntu.com/community/VirtualBox/SharedFolders) to copy cMoflon-generated files to the Instant Contiki VM.

1. Copy both all .c and .h files from the */gen* folder of your cMoflon project to the folder *$ToCoCo/src/components/topologycontrol/*
1. Insert the constants from the cMoflon-generated file *app-conf-constants.h.sample* into *$ToCoCo/src/app-conf-constants.h*. These constants 
   * assign to each generated algorithm a unique ID (e.g., `COMPONENT_TOPOLOGYCONTROL_CMOFLONTOPOLOGYCONTROL_MAXPOWERTOPOLOGYCONTROL`),
   * indicate which .c file implements which TC algorithm (e.g., `COMPONENT_TOPOLOGYCONTROL_IMPL_FILE_CMOFLONTOPOLOGYCONTROL_MAXPOWERTOPOLOGYCONTROL`), and 
   * signal ToCoCo to use the link-state attribute (`TOPOLOGYCONTROL_LINKS_HAVE_STATES`). The latter constant enables that the type ```neighbor_t``` has a member called ```state```.
1. Copy *$ToCoCo/src/app-conf-default.h* to *$ToCoCo/src/app-conf.h*. The file contains some sensible default settings for the FlockLab testbed and the Cooja simulator.
   * To change the waiting time until topology control is executed, insert or update the corresponding preprocessor constants that end with `_UPDATEINTERVAL` (e.g., for the algorithm with ID `COMPONENT_TOPOLOGYCONTROL_KTCALGORITHM`, the corresponding definition is `#define COMPONENT_TOPOLOGYCONTROL_KTCALGORITHM_UPDATEINTERVAL 660`).
1. Copy *$ToCoCo/src/Makefile-conf-default.include* to *$ToCoCo/src/Makefile-conf.include* 
   * The `Contiki` property points to the Contiki distribution. In the Instant Contiki Vm, choose ```Contiki=/home/user/contiki```.
   * Disable `NETWORK_IPV6`: ```NETWORK_IPV6 = 0``` (With IPv6, the resulting image would be too large for Sky motes).
   * To select the active .c file that contains the implementation of the generated algorithm, update the assignment of `TOPOLOGYCONTROL_PREDEFINED_IMPL_FILE` in *Makefile-conf-default.include*  according to the file with the same name in the */gen* folder of your cMoflon project (e.g., `TOPOLOGYCONTROL_PREDEFINED_IMPL_FILE=topologycontrol-CMoflonTopologyControl-MaxpowerTopologyControl.c`).
1. Open a terminal and navigate to */home/user/contiki/tools/cooja* (for Instant Contiki).
1. Use the command ```ant run``` to start the Cooja Simulator.
1. Create a new simulation (*File &rarr; New Simulation* or *CTRL+N*)
1. Navigate to *Motes &rarr; Add motes &rarr; Create New Mote Type &rarr; Sky mote*, browse to *$ToCoCo/src/app.c*, and hit *Compile*.
1. After compiling is done, create the motes, and run the simulation.


### How to test a TC algorithm generated by cMoflon using FlockLab
FlockLab (https://www.flocklab.ethz.ch/) is a wireless sensor testbed at ETH Zurich.

1. You will need a Flocklab account for testing so create one here: https://www.flocklab.ethz.ch/user/login.php
1. Follow all the steps for Cooja until compilation has terminated.
1. Go to the test image section (https://www.flocklab.ethz.ch/user/images.php) and upload the compiled *app.sky* file, located in *$ToCoCo/src*.
   * As *OS*, choose *TinyOS*.
   * As *Platform*, choose *Tmote*.
1. Create a copy of the FlockLab configuration file *https://github.com/eMoflon/cmoflon/blob/master/org.cmoflon.evaluation.flocklab/config/flocklabConfig.xml*.
   * Copy the image ID that you received in the following step into the ```<dbImageId/>``` element
   * (optional) Adjust the duration of the test in seconds (e.g., ```<durationSecs>960</durationSecs>```).
1. Create a new test (https://www.flocklab.ethz.ch/user/index.php) by uploading the adjusted xml configuration file.
   * The configuration can be updated as long as the test has not started yet.
1. After the test, you will receive an e-mail with the results in a zip folder.
   * The contents of the *serial.csv* file allow you to debug/analyze the topology control behavior (Watch out for lines like ```[topologycontrol-*]```).

### How to use the evaluation tools (excerpt)
The ToCoCo evaluation tools help you to analyze the serial logging output of the FlockLab sensor nodes.


#### Batch compilation
If you are interested in compiling all of the considered topology control algorithms as a batch, we recommend to use the Bash script that can be found at *$ToCoCo/evaluation/src/BatchCompilation*.
This script is tailored to the topology control algorithms of the ECMFA evaluation.

### Developer setup

The following steps describe how to build cMoflon from source.

1. Create a fresh Eclipse workspace
1. Checkout all projects from the Git repository https://github.com/eMoflon/cmoflon.git
   * You will not need *org.cmoflon.evaluation.flocklab*
1. In the project *org.cmoflon.updatesite*, open *site.xml*, and select *Build all*.
1. After the build is complete, reset *site.xml* because the build process for update sites modifies *site.xml*.
1. You find launch configurations for packaging and publishing the created update site in *org.cmoflon.updatesite/.launch*
   * *... createArchive.launch* packages the update site as ZIP archive
   * *... releaseCMoflon.launch* copies the update site to the project *cmoflon-updatesite*, which can be checked out from: https://github.com/eMoflon/cmoflon-updatesite

# Documentation for cMoflon
cMoflon is a code generator for topology control algorithms based on the wireless sensor operating system Contiki.
cMoflon is a variant of [eMoflon](https://emoflon.github.io).
In cMoflon, topology control algorithms are specified using Ecore and Story-Driven Modeling, a programmed graph transformation dialect.
The target programmling language is (embedded) C.

[![Project Stats](https://www.openhub.net/p/cmoflon/widgets/project_thin_badge.gif)](https://www.openhub.net/p/cmoflon)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/59c4befefa994cea973be2faba81c01f)](https://www.codacy.com/app/RolandKluge/cmoflon?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=eMoflon/cmoflon&amp;utm_campaign=Badge_Grade)

## Supplementary material for ECMFA 2017
We have set up a separate document that links to supplementary material (e.g, a SHARE VM) related to the cMoflon paper at ECMFA 2017:
https://github.com/eMoflon/cmoflon-examples/blob/master/Talks/ECMFA2017/README_ECMFA2017.md

## Troubleshooting
If you encounter any problems or need support, please 
* post an issue (https://github.com/eMoflon/cmoflon/issues) or
* send me (Roland Kluge) a message

## cMoflon user setup
If you want to run the generated code in the testbed, follow the steps in the *Complete walkthrough* section.
This version of cMoflon has been tested with Eclipse Oxygen and eMoflon 3.4.0 and Enterprise Architect 12

1. **Install Eclipse with Modeling Components Oxygen (or newer)**
   * All Eclipse packages are available here: https://eclipse.org/downloads/
1. **Install Enterprise Architect 12 (or later)**
   * A 30-days trial version of Enterprise Architect is available here: https://www.sparxsystems.de/uml/download-trial/
1. **Install eMoflon 3.4.0**
   * Follow the installation instructions from here:  https://github.com/eMoflon/emoflon-tool/releases/tag/emoflon-tie_3.4.0
   * From eMoflon Tool you only need the basic eMoflon Tool feature
   * If not done automatically, you will have to enable the update site https://emoflon.org/emoflon-core-updatesite/stable/updatesite/ in the Software Site dialog of the Eclipse preferences.
   * Additionally, download, unpack and install the eMoflon addin for Enterprice Architect from here: https://emoflon.org/eclipse-plugin/beta/updatesite/ea-ecore-addin.zip
1. **Install cMoflon 1.0.0**
   * Stable update site:
   * Unstable update Site: http://emoflon.github.io/cmoflon/updatesite/
   * You may also use the following: <a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=3266408" class="drag" title="Drag to your running Eclipse workspace."><img class="img-responsive" src="https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png" alt="Drag to your running Eclipse workspace." /></a> from Eclipse Marketplace via drap and drop.
1. **Demo specification**
   1. Import the sample projects into your workspace as follows: 
       * *Right-click &rarr; Import... &rarr; Git/Projects from Git*
       * Choose to clone the following repository: https://github.com/eMoflon/cmoflon-examples.git
       * Choose the projects *CMoflonDemoEASpecification* and *CMoflonDemoLanguage*.
   1. Select the projects and perform a full build (e.g., via the *Right-click &rarr; eMoflon &rarr; Build selected projects fully*).
   1. After the build was successful, the project *CMoflonDemoLanguage* contains the generated C code (*/gen* folder).

## Complete walkthrough
cMoflon requires a mixed environment with a Windows system (for Enterprise Architect) and a Linux/Instant Contiki VM.

1. **User setup:** Follow the steps above to set up your development workspace.
1. **Get Contiki 3.0**
   * Download the *Instant Contiki 3.0* Virtual Box VM from here: https://sourceforge.net/projects/contiki/files/Instant%20Contiki/Instant%20Contiki%203.0/InstantContiki3.0.zip/download (Get Virtual Box here: https://www.virtualbox.org/)
   * Detailed setup instructions can be found here: http://www.contiki-os.org/start.html
   * If you use Instant Contiki, you need to call ```git submodule update --init```. 
     Otherwise, starting Cooja will fail.
1. **Get ToCoco 2.0.0**
   * Checkout the ToCoCo framework from inside the Instant Contiki VM: `git clone -b tococo_2.0 https://github.com/eMoflon/ToCoCo.git`
   * The root folder of the working copy will be refered to as *$ToCoCo* in the following.
 
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

1. Copy both generated files (.c and .h) from the */gen* folder of your cMoflon workspace into the folder *$ToCoCo/src/components/topologycontrol/*
1. Add a line for the .c file to *$ToCoCo/src/components/topologycontrol/Makefile*
   * Example: `PROJECT_SOURCEFILES += topologycontrol-CMoflonTopologyControl-MaxpowerTopologyControl.c`
1. Insert the constants from the cMoflon-generated file *app-conf-constants.h.sample* into *$ToCoCo/src/app-conf-constants.h*. These constants assign to each generated algorithm a unique ID and signal ToCoCo to use the link-state attribute (`TOPOLOGYCONTROL_LINKS_HAVE_STATES`). The latter constant enables that the type ```neighbor_t``` has a member called ```state```.
1. Copy *$ToCoCo/src/app-conf-default* to *$ToCoCo/src/app-conf*. The file contains some sensible default settings for the FlockLab testbed and the Cooja simulator.
1. Copy *$ToCoCo/src/Makefile-conf-default.include* to *$ToCoCo/src/Makefile-conf.include* 
   * The `Contiki` property points to the Contiki distribution. In the Instant Contiki Vm, choose ```Contiki=/home/user/contiki```.
   * Disable `NETWORK_IPV6`: ```NETWORK_IPV6 = 0``` (With IPv6, the resulting image would be too large for Sky motes).
1. Open a terminal, navigate to */home/user/contiki/tools/cooja* (for Instant Contiki).
1. Use the command ```ant run``` to start the Cooja Simulator.
1. Create a new simulation (*File &rarr; New Simulation* or *CTRL+N*)
1. Navigate to *Motes &rarr; Add motes &rarr; Create New Mote Type &rarr; Sky mote*, browse to *$ToCoCo/src/app.c*, and hit *Compile*.
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


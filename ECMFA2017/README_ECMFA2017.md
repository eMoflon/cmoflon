# Supplementary material

This document provides a guide to supplementary material for the paper 

**R. Kluge, M. Stein, D. Giessing, A. Schürr, M. Mühlhäuser: "cMofloon: Model-Driven Generation of Embedded C Code for Wireless Sensor Networks, " in: Proc. of the ECMFA 2017, to appear**

## Slides
* You find the slides in this folder (.pptx, .pdf - without animations)

## SHARE VM
We invite you to try out cMoflon using the following SHARE VM:
* URL: <a href="http://is.ieis.tue.nl/staff/pvgorp/share/?page=ConfigureNewSession&vdi=XP-TUe_RK_cMoflon_ECMFA2017.vdi" title="" target="_blank">XP-TUe_RK_cMoflon_ECMFA2017.vdi</a> in <a href="http://fmt.cs.utwente.nl/redmine/projects/grabats/wiki" target="_blank"><img src="http://is.ieis.tue.nl/staff/pvgorp/share/images/share-logo-16full.png" alt="Online demo in SHARE" title="Sharing Hosted Autonomous Research Environments" border="0" style="vertical-align: text-top"></a>
* User: sharevm[A}es[D}tu-darmstadt[D}de ([A} = @, [D} = ., [E} = !)
* Passsword: ShareVM[A}RealTime[E}

### Getting started

* After starting a SHARE VM session, you see a Windows desktop.
* Double-click the Icon *Eclipse Neon 3* to start Eclipse.
* You are now faced with an Eclipse workspace that contains three working sets (see *Package Explorer* on the left)
   1. Working set *cMoflonDemo* contains the specification (Project *cMoflonDemo*) and source code (Project *cMoflonDemo*, *gen* folder)
   1. Working set *cMoflonSource* contains the sources code of cMoflon
   1. Working set *ToCoCoSource* contains the source code of ToCoCo
* The following subsections describe how to explore the provided resources.

### Exploring cMoflonDemo

* To see the metamodel of the demo and the topology control algorithm specifications, open the *cMoflonDemo* project.
* Then, double-click the file *cMoflonDemo.eap*.
* The modeling tool *Enterprise Architect* opens up.
![Screenshot of cMoflon Demo in EA](https://github.com/eMoflon/cmoflon/raw/master/ECMFA2017/ScreenshotCMoflonDemoEA.png)
* To the left, you see the *Project browser* with two elements.
* Open the *cMoflon Demo* element, and then the *EPackage cMoflonLanguage*
* The two entries *cMoflonDemo* and *cMoflonDemoAlgorithms* are diagrams that provide an overview of the metamodel and can be opened by double-clicking.
* To examine the specification of one of the topology control algorithms, 
   1. open *cMoflonDemoAlgorithms*,
   1. select *+ run(): void* in *LmstAlgorithm* (or *KtcAlgorithm* or *LStarKtcAlgorithm*), and
   1. press Enter or double-click *+ run(): void* once again.
* The generated .c and .h files can be found in the Eclipse project *cMoflonDemoLanguage_C* in the *gen* folder. These files can be copy-and-pasted into the appropriate folder for the topology control component of ToCoCo (see below).
   
### Exploring cMoflonSource
* To examine the cMoflon code generator, open the Eclipse project *org.cmoflon.ide.core* and navigate to */src/org.cmoflon.ide.core.runtime.codegeneration/CMoflonCodeGenerator.java*
![Screenshot of cMoflon projects](https://github.com/eMoflon/cmoflon/raw/master/ECMFA2017/ScreenshotCMoflonCode.png)

### Exploring ToCoCoSource
* The Eclipse project *ToCoCoSource* contains the full source code of the ToCoCo 2.0.0 ramework
* The *components* folder is structured along the main components in ToCoCo.
* The *components/topologycontrol* folder contains the manually implemented topology control algorithms (a-)kTC, l*-kTC, LMST, and NoTC.
   * To use the generated topology control algorithms, the generated .c and .h files need to be placed in this folder and the *Makefile* needs to be adjusted accorindly.
   
![Screenshot of ToCoCo project](https://raw.githubusercontent.com/eMoflon/cmoflon/master/ECMFA2017/ScreenshotToCoCoSource.png)

## Size comparison of eMoflon and cMoflon
We have counted the lines of code in the code bases of eMoflon 2.28.0 and cMoflon 1.0.0.
The results can be found at the following location:
https://github.com/eMoflon/cmoflon/raw/master/ECMFA2017/SizeComparisonCMoflonVsEMoflon.xlsx

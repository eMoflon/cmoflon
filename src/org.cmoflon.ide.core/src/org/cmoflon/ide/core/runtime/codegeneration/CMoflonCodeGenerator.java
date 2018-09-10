package org.cmoflon.ide.core.runtime.codegeneration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonIncludes.Components;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonStringRenderer;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.FieldAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.MethodAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.Type;
import org.cmoflon.ide.core.utilities.CMoflonProperties;
import org.cmoflon.ide.core.utilities.CMoflonWorkspaceHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.codegen.ecore.generator.GeneratorAdapterFactory.Descriptor;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenDataType;
import org.eclipse.emf.codegen.ecore.genmodel.GenEnum;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenOperation;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.core.nd.field.FieldSearchIndex;
import org.gervarro.democles.codegen.ImportManager;
import org.gervarro.democles.codegen.OperationSequenceCompiler;
import org.gervarro.democles.codegen.TemplateInvocation;
import org.gervarro.democles.compiler.CompilerPatternBody;
import org.moflon.compiler.sdm.democles.DemoclesGeneratorAdapterFactory;
import org.moflon.compiler.sdm.democles.DemoclesMethodBodyHandler;
import org.moflon.compiler.sdm.democles.SearchPlanAdapter;
import org.moflon.compiler.sdm.democles.TemplateConfigurationProvider;
import org.moflon.compiler.sdm.democles.eclipse.AdapterResource;
import org.moflon.core.utilities.LogUtils;
import org.moflon.core.utilities.MoflonUtil;
import org.moflon.core.utilities.UncheckedCoreException;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.sdm.runtime.democles.Scope;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import SDMLanguage.activities.MoflonEOperation;

/**
 * Generates the source and the header File.
 *
 * @author David Giessing
 * @author Roland Kluge
 */
public class CMoflonCodeGenerator
{

   private static final Logger logger = Logger.getLogger(CMoflonCodeGenerator.class);

   private static final String COMPONENT_TOPOLOGY_CONTROL_PREFIX = "topologycontrol";

   /**
    * List of built-in types provided by ECore
    */
   private List<String> builtInTypes;

   private IProject project;

   //Contains all algorithm names for which dropping Unidirectional edges should be inactive
   private final Set<String> dropUnidirectionalEdgesOff;

   //Contains all algorithm names for which hopcounts are requested
   private final Set<String> useHopCountProcess;

   private ImportManager democlesImportManager;

   private List<String> blockDeclarations = null;

   private GenModel genModel;

   private DemoclesGeneratorAdapterFactory codeGenerationEngine;

   private final List<String> tcClasses;

   //Contains a list of helper classes for each tcAlgorithm
   private final Map<String,List<String>> helperClasses;
   
   private final Map<String, String > tcAlgorithmCallParameters;

   private int maximumMatchCount;

   //Contains all algorithm names for which evaluation statements were requested
   private final Set<String> useEvalStatements;
   
   //Contains all algorithm names for which duplicate generation is requested
   private final Set<String> generateDuplicates;

   private final Map<String, String> typeMappings;

   private static String EVAL_STATEMENTS_START = "\t\tlist_t neighbors= component_neighbordiscovery_neighbors();" + nl() + "\t\tneighbor_t* link;" + nl()
         + "\t\tint degree=0;" + nl() + "\t\tfor(link=list_head(neighbors);link!=NULL;link=list_item_next(link)){" + nl()
         + "\t\t\tif(networkaddr_equal(link->node1,networkaddr_node_addr())||networkaddr_equal(link->node2,networkaddr_node_addr()))" + nl()
         + "\t\t\t\tdegree++;" + nl() + "\t\t}" + nl() + "\t\tprintf(\"[topologycontrol]: DEGREE: %d\\n\",degree);" + nl()
         + "\t\tunsigned long start=RTIMER_NOW();" + nl() + "\t\tprintf(\"[topologycontrol]: STATUS: Run\\n\");" + nl();

   private static String EVAL_STATEMENTS_END = "\t\tunsigned long finish=RTIMER_NOW();" + nl()
         + "\t\tunsigned long runtime= finish>start? finish-start:start-finish;" + nl() + "\t\tprintf(\"[topologycontrol]: TIME: %lu\\n\",runtime);" + nl();

   /**
    * Constants mapping
    * Key: constant name
    * Value: constant value to be used as is during code generation
    */
   private final Map<String, Map<String,String> >constantsMapping;

   // Stores the initial id to assign to the first generated TC algorithm
   private int minTcComponentConstant = CMoflonProperties.DEFAULT_TC_MIN_ALGORITHM_ID;

   private final Map<String,List<MethodAttribute> > cachedMethodSignatures;

   private final Map<String,List<FieldAttribute> > cachedFields;

   private final List<GenClass> cachedConcreteClasses;

   private final Map<String,StringBuilder> cachedPatternMatchingCode;

   private String tcAlgorithmParentClassName;

   private GenClass tcAlgorithmParentGenClass;

   private SimpleDateFormat timeFormatter;

   private boolean reduceCodeSize=CMoflonProperties.DEFAULT_REDUCE_CODE_SIZE;
   
   private static String TC_INDEPENDANT ="TC_INDEPENDANT";  

   public CMoflonCodeGenerator(Resource ecore, IProject project, GenModel genModel, Descriptor codeGenerationEngine) throws InvalidInputException
   {
      this.useEvalStatements=new HashSet<>();
	 this.generateDuplicates=new HashSet<>();
	 this.useHopCountProcess=new HashSet<>();
	 this.helperClasses=new HashMap<>();
	 this.codeGenerationEngine = (DemoclesGeneratorAdapterFactory) codeGenerationEngine;
	 this.project = project;
	 this.genModel = genModel;
	 this.tcAlgorithmCallParameters = new HashMap<>();
	 this.typeMappings = new HashMap<>();
	 this.constantsMapping = new HashMap<>();
	 this.dropUnidirectionalEdgesOff=new HashSet<>();
	 this.tcClasses = new ArrayList<>();
	 this.blockDeclarations = new ArrayList<>();
	 this.cachedMethodSignatures = new HashMap<String, List<MethodAttribute>>();
	 this.cachedFields = new HashMap<>();
	 this.cachedConcreteClasses = new ArrayList<>();
	 this.cachedPatternMatchingCode=new HashMap<String, StringBuilder>();
	 this.tcAlgorithmParentClassName = "TopologyControlAlgorithm";
	 this.tcAlgorithmParentGenClass = this.determineTopologyControlParentClass();
	 if (this.tcAlgorithmParentGenClass == null)
	 {
	    throw new IllegalStateException("Expected to find superclass '" + this.tcAlgorithmParentClassName + "' in genmodel.");
	 }
	 try {
		 this.readProperties(project);
		 }
	 catch(Exception e) {
		 e.printStackTrace();
		 throw new InvalidInputException("Could not read cMoflon Properties."+e.toString());
	 }
	 
	 this.builtInTypes = determineBuiltInTypes();
	 this.timeFormatter = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ss");
   }

   public IStatus generateCode(final IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, "Generating code", tcClasses.size() * 100 + 10);
      final MultiStatus codeGenerationResult = new MultiStatus(WorkspaceHelper.getPluginId(getClass()), 0, "Code generation failed", null);

      initializeCaches();
      
      if(this.reduceCodeSize)
    	  generateCMoflonHeader(subMon.split(1));

      for (final String tcClass : this.tcClasses)
      {
         codeGenerationResult.add(generateCodeForAlgorithm(tcClass, codeGenerationResult, subMon.split(10)));
      }

      generateSampleFiles(subMon.split(10));

      resetCaches();

      return codeGenerationResult;
   }

private void generateCMoflonHeader(final IProgressMonitor monitor) throws CoreException{
	      final SubMonitor subMon = SubMonitor.convert(monitor, "Generate cMoflon header ", 10);
	      final STGroup templateGroup = getTemplateConfigurationProvider().getTemplateGroup(CMoflonTemplateConfiguration.CMOFLON_HEADER_FILE_GENERATOR);
	      templateGroup.registerRenderer(String.class, new CMoflonStringRenderer());

	      final StringBuilder contents = new StringBuilder();
	      contents.append(getDateCommentCode());
	      contents.append(getIncludeGuardCode(CMoflonCodeGenerator.TC_INDEPENDANT, templateGroup));
	      contents.append(getIncludesCode(templateGroup,CMoflonCodeGenerator.TC_INDEPENDANT));
	      contents.append(getConstantsDefinitionsCode(CMoflonCodeGenerator.TC_INDEPENDANT, templateGroup));
	      contents.append(getMaxMatchCountDefinition(CMoflonCodeGenerator.TC_INDEPENDANT));
	      contents.append(getMatchTypeDefinitionCode(templateGroup,CMoflonCodeGenerator.TC_INDEPENDANT));
	      contents.append(getTypeMappingCode(templateGroup,CMoflonCodeGenerator.TC_INDEPENDANT));
	      contents.append(getAllBuiltInMappings(CMoflonCodeGenerator.TC_INDEPENDANT));
	      contents.append(getDefaultTypedefs(CMoflonCodeGenerator.TC_INDEPENDANT));
	      contents.append(getUserDefinedTypedefs(CMoflonCodeGenerator.TC_INDEPENDANT));
	      contents.append(getUnimplementedMethodsCode(templateGroup,CMoflonCodeGenerator.TC_INDEPENDANT));
	      contents.append(getAccessorsCode(templateGroup,CMoflonCodeGenerator.TC_INDEPENDANT));
	      contents.append(getComparisonFunctionsCode(templateGroup,CMoflonCodeGenerator.TC_INDEPENDANT));
	      contents.append(getEqualsFunctionsCode(templateGroup,CMoflonCodeGenerator.TC_INDEPENDANT));
	      contents.append(getHeaderTail(CMoflonCodeGenerator.TC_INDEPENDANT, templateGroup));
	      subMon.worked(8);

	      final String parentFolderForAlgorithm = getProjectRelativePathForAlgorithm(CMoflonCodeGenerator.TC_INDEPENDANT);
	      final String outputFileName = parentFolderForAlgorithm+"cMoflon.h";
	      final IFile headerFile = project.getFile(outputFileName);
	      if (!headerFile.exists())
	      {
	         WorkspaceHelper.addAllFolders(project, parentFolderForAlgorithm, subMon.split(1));
	         WorkspaceHelper.addFile(project, outputFileName, contents.toString(), subMon.split(1));
	      } else
	      {
	         headerFile.setContents(new ReaderInputStream(new StringReader(contents.toString())), true, true, subMon.split(2));
	      }
	   }

private IStatus generateCodeForAlgorithm(final String tcAlgorithm, MultiStatus codeGenerationResult, final IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, "Generating code for " + tcAlgorithm, 100);

      subMon.worked(10);

      generateHeaderFile(tcAlgorithm, subMon.split(45));

      generateSourceFile(tcAlgorithm, subMon.split(45));

      return Status.OK_STATUS;
   }

   private GenClass determineTopologyControlParentClass()
   {
      for (final GenPackage genPackage : genModel.getAllGenPackagesWithClassifiers())
      {
         for (final GenClass genClass : genPackage.getGenClasses())
         {
            if (tcAlgorithmParentClassName.equals(genClass.getName()))
            {
               return genClass;
            }
         }
      }
      return null;
   }

   private static final boolean isClassInGenmodel(final String className, final GenModel genModel)
   {
      for (final GenPackage genPackage : genModel.getGenPackages())
      {
         for (final GenClass genClass : genPackage.getGenClasses())
         {
            if (genClass.getName().equals(className))
               return true;
         }
      }
      return false;
   }

   private void reportMissingTopologyControlClass(final String nameOfInappropriateClass)
   {
      final List<GenClass> tcAlgorithmCandidateClasses = new ArrayList<>();
      genModel.getAllGenPackagesWithClassifiers().forEach(genPackage -> genPackage.getGenClasses().stream()
            .filter(genClass -> isTrueSubtypeOfTCAlgorithmParentClass(genClass)).forEach(genClass -> tcAlgorithmCandidateClasses.add(genClass)));
      LogUtils.error(logger, "Topology class '%s' (specified in %s) cannot be found in GenModel or is not a subtype of '"
            + this.tcAlgorithmParentGenClass.getName() + "' and will be ignored.", nameOfInappropriateClass, CMoflonProperties.CMOFLON_PROPERTIES_FILENAME);
      LogUtils.error(logger, "Candidates are " + tcAlgorithmCandidateClasses.stream().map(cand -> "'" + cand.getName() + "'").collect(Collectors.joining(", ")),
            nameOfInappropriateClass, CMoflonProperties.CMOFLON_PROPERTIES_FILENAME);
   }

   private boolean isTrueSubtypeOfTCAlgorithmParentClass(final GenClass genClass)
   {
      return this.tcAlgorithmParentGenClass != genClass && this.tcAlgorithmParentGenClass.getEcoreClass().isSuperTypeOf(genClass.getEcoreClass());
   }

   private void initializeCaches()
   {  initializeCachedMetamodelElementLists();
      initializeCachedPatternMatchingCode();
      this.blockDeclarations = this.getBlockDeclarations(this.cachedConcreteClasses);
   }

   private void initializeCachedPatternMatchingCode()
   {
	  //Initialize StringBuilder
	  for(String tcAlgorithm:tcClasses) {
		  this.cachedPatternMatchingCode.put(tcAlgorithm, new StringBuilder());
	  }
	  this.cachedPatternMatchingCode.put(CMoflonCodeGenerator.TC_INDEPENDANT, new StringBuilder());

      for (final GenPackage genPackage : this.genModel.getGenPackages())
      {
         for (final GenClass genClass : genPackage.getGenClasses())
         {
            if (!genClass.isAbstract())
            {
            	String tcAlgorithmName=this.getTCAlgorithmNameForGenClass(genClass);
               for (final GenOperation genOperation : genClass.getGenOperations())
               {
                  final String generatedMethodBody = getGeneratedMethodBody(genOperation.getEcoreOperation());
                  if (!generatedMethodBody.equals(MoflonUtil.DEFAULT_METHOD_BODY))
                  {
                	 StringBuilder intermediateBuffer = new StringBuilder();
                     LogUtils.info(logger, "Generate method body for '%s::%s'", genClass.getName(), genOperation.getName());
                     intermediateBuffer.append(nl());
                     intermediateBuffer.append(genOperation.getTypeParameters(genClass));
                     String[] typechain = genOperation.getImportedType(genClass).split("\\.");
                     String type = "";
                     if (typechain.length == 0)
                        type = genOperation.getImportedType(genClass);
                     else
                        type = typechain[typechain.length - 1];
                     if (!isBuiltInType(type) && !type.equalsIgnoreCase("void"))
                        type = getTypeName(type) + "*";
                     intermediateBuffer.append(type);
                     intermediateBuffer.append(" ");
                     final String functionName = getClassPrefixForMethods(genOperation.getEcoreOperation().getEContainingClass().getName())
                           + genOperation.getName();
                     intermediateBuffer.append(functionName);
                     intermediateBuffer.append(("("));
                     intermediateBuffer.append(getParametersFromEcore(genOperation.getEcoreOperation()));
                     intermediateBuffer.append(("){" + nl()));
                     for (final String line : generatedMethodBody.trim().replaceAll("\\r", "").split(Pattern.quote(nl())))
                     {
                    	 intermediateBuffer.append("\t" + line);
                    	 intermediateBuffer.append(nl());
                     }
                     intermediateBuffer.append(nl() + "}" + nl());
                     //Append Generated Code
                     StringBuilder cachedPatternMatchingCodeForTCAlgorithm=this.cachedPatternMatchingCode.get(tcAlgorithmName);
                     cachedPatternMatchingCodeForTCAlgorithm.append(intermediateBuffer.toString());
                  } else
                  {
                     LogUtils.info(logger, "Skip method body due to missing specification: '%s::%s'", genClass.getName(), genOperation.getName());
                  }
               }
            }
         }
      }
      
   }

   private void initializeCachedMetamodelElementLists()
   {
	  for(String tcAlgorithm:this.tcClasses) {
		  this.cachedMethodSignatures.put(tcAlgorithm, new ArrayList<MethodAttribute>());
		  this.cachedFields.put(tcAlgorithm, new ArrayList<>());
	  }
	  this.cachedMethodSignatures.put(CMoflonCodeGenerator.TC_INDEPENDANT, new ArrayList<MethodAttribute>());
      this.cachedFields.put(CMoflonCodeGenerator.TC_INDEPENDANT, new ArrayList<>());
	  genModel.getGenPackages().forEach(genPackage -> genPackage.getGenClasses().forEach(genClass -> {
    	 if (!genClass.isAbstract())
         {
            cachedConcreteClasses.add(genClass);
            List<FieldAttribute> fields;
            if(isTrueSubtypeOfTCAlgorithmParentClass(genClass)) {
            	fields=cachedFields.get(getTCAlgorithmNameForGenClass(genClass));
            	extractFieldsAndMethodsFromGenClass(fields,genClass);
            }
            else {
            	this.helperClasses.entrySet().stream().filter(entry -> entry.getValue().contains(genClass.getName())).forEach(entry -> {
            		extractFieldsAndMethodsFromGenClass(cachedFields.get(entry.getKey()), genClass);
            	});
	  			if(this.helperClasses.values().stream().noneMatch(l->l.contains(genClass.getName()))) {
	  				fields=cachedFields.get(CMoflonCodeGenerator.TC_INDEPENDANT);
	  				extractFieldsAndMethodsFromGenClass(fields,genClass);
	  			}
            }
      }
      }));
	  }
	  
private void extractFieldsAndMethodsFromGenClass(List<FieldAttribute> fields, GenClass genClass) {
	fields.addAll(getFields(genClass));
    genClass.getGenOperations().forEach(genOperation -> {
       final EClassifier operationType = genOperation.getEcoreOperation().getEType();
       /*EOperation eOp=genOperation.getEcoreOperation();
       if(eOp instanceof MoflonEOperation) {
    	   MoflonEOperation mEOp=(MoflonEOperation)eOp;
           if(mEOp.getActivity()==null) {
        	   //The method is unimplemented and is added to the list of unimplemented methods furter down
        	   ;
           }
           //The method is implemented and hence does not need a declaration
           else return;
       }*/
       //Add methods to unimplemented methods
       final String operationTypeName = operationType == null ? "void" : operationType.getName();
       final boolean isOperationTypeBuiltIn = operationType == null ? true : isBuiltInType(operationTypeName);
       final String genClassName = genOperation.getGenClass().getName();
       List<MethodAttribute> methodDeclarations;
       if(isTrueSubtypeOfTCAlgorithmParentClass(genOperation.getGenClass())) {
    	   methodDeclarations=cachedMethodSignatures.get(getTCAlgorithmNameForGenClass(genClass));
       }
       else {
    	   methodDeclarations=cachedMethodSignatures.get(CMoflonCodeGenerator.TC_INDEPENDANT);
       }
       methodDeclarations.add(new MethodAttribute(new Type(isBuiltInType(genClassName), genClassName),
             new Type(isOperationTypeBuiltIn, operationTypeName), genOperation.getName(), getParametersFromEcore(genOperation.getEcoreOperation())));
    });
 }
   /**
    * Resets all fields that store cached artefacts to their default state
    */
   private void resetCaches()
   {
      this.cachedMethodSignatures.clear();
      this.cachedFields.clear();
      this.cachedConcreteClasses.clear();
      this.blockDeclarations.clear();
   }

   private String getComponentName()
   {
      return COMPONENT_TOPOLOGY_CONTROL_PREFIX;
   }

   /**
    * This Method generates the Source File.
    * @param tcAlgorithm
    *            the name of the specific algorithm
    * @param inProcessCode
    *            the String containing the code that shall be executed in the
    *            process
    * @param genClass
    *            the genClass the code is generated for.
    * @throws CoreException
    */
   private void generateSourceFile(final String tcAlgorithm, final IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, "Generate source file for " + tcAlgorithm, 10);
      final STGroup templateGroup = getTemplateConfigurationProvider().getTemplateGroup(CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR);
      templateGroup.registerRenderer(String.class, new CMoflonStringRenderer());

      final StringBuilder contents = new StringBuilder();

      final String componentBasename = getAlgorithmBasename(tcAlgorithm);
      contents.append(getDateCommentCode());
      contents.append("#include \"" + componentBasename + ".h" + "\"" + nl());

      contents.append(getListAndBlockDeclarations(templateGroup));

      if (useHopCount(tcAlgorithm))
      {
         contents.append(getHopCountCode(tcAlgorithm, templateGroup));
      }
      contents.append(getDefaultHelperMethods());
      contents.append(getUserDefinedHelperMethods(tcAlgorithm));
      contents.append(getPatternMatchingCode(tcAlgorithm));
      contents.append(this.cachedPatternMatchingCode.get(tcAlgorithm).toString());
      contents.append(getInitMethod(templateGroup));
      contents.append(getCleanupMethod(templateGroup));
      contents.append(getProcessPreludeCode(tcAlgorithm, templateGroup));
      contents.append(getProcessBodyCode(tcAlgorithm, templateGroup));
      contents.append(getProcessClosingCode(tcAlgorithm, templateGroup));
      subMon.worked(8);

      final String parentFolderForAlgorithm = getProjectRelativePathForAlgorithm(tcAlgorithm);
      final String outputFileName = parentFolderForAlgorithm + "/" + componentBasename + ".c";
      final IFile sourceFile = project.getFile(outputFileName);
      if (!sourceFile.exists())
      {
         WorkspaceHelper.addAllFolders(project, parentFolderForAlgorithm, subMon.split(1));
         WorkspaceHelper.addFile(project, outputFileName, contents.toString(), subMon.split(1));
      } else
      {
         sourceFile.setContents(new ReaderInputStream(new StringReader(contents.toString())), true, true, subMon.split(2));
      }

   }

   /**
    * Generates the Header File including, constants, includes, method
    * declarations, accessor declarations as well as declarations for compare
    * and equals operations
    * @param tcAlgorithm
    *            needed for naming
    */
   private void generateHeaderFile(final String tcAlgorithm, final IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, "Generate header for " + tcAlgorithm, 10);
      final STGroup templateGroup = getTemplateConfigurationProvider().getTemplateGroup(CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR);
      templateGroup.registerRenderer(String.class, new CMoflonStringRenderer());

      final StringBuilder contents = new StringBuilder();
      contents.append(getDateCommentCode());
      contents.append(getIncludeGuardCode(tcAlgorithm, templateGroup));
      contents.append(getIncludesCode(templateGroup,tcAlgorithm));
      contents.append(getConstantsDefinitionsCode(tcAlgorithm, templateGroup));
      contents.append(getMaxMatchCountDefinition(tcAlgorithm));
      contents.append(getGenerateDuplicatesDefinition(tcAlgorithm));
      contents.append(getMatchTypeDefinitionCode(templateGroup,tcAlgorithm));
      contents.append(getTypeMappingCode(templateGroup,tcAlgorithm));
      contents.append(getAllBuiltInMappings(tcAlgorithm));
      contents.append(getDefaultTypedefs(tcAlgorithm));
      contents.append(getUserDefinedTypedefs(tcAlgorithm));
      contents.append(getUnimplementedMethodsCode(templateGroup,tcAlgorithm));
      contents.append(getAccessorsCode(templateGroup,tcAlgorithm));
      contents.append(getComparisonFunctionsCode(templateGroup,tcAlgorithm));
      contents.append(getEqualsFunctionsCode(templateGroup,tcAlgorithm));
      contents.append(getHeaderTail(tcAlgorithm, templateGroup));
      subMon.worked(8);

      final String parentFolderForAlgorithm = getProjectRelativePathForAlgorithm(tcAlgorithm);
      final String outputFileName = parentFolderForAlgorithm + getAlgorithmBasename(tcAlgorithm) + ".h";
      final IFile headerFile = project.getFile(outputFileName);
      if (!headerFile.exists())
      {
         WorkspaceHelper.addAllFolders(project, parentFolderForAlgorithm, subMon.split(1));
         WorkspaceHelper.addFile(project, outputFileName, contents.toString(), subMon.split(1));
      } else
      {
         headerFile.setContents(new ReaderInputStream(new StringReader(contents.toString())), true, true, subMon.split(2));
      }
   }

   private boolean useHopCount(final String algorithmName)
   {
      return this.useHopCountProcess.contains(algorithmName);
   }

   private String getDateCommentCode()
   {
      return String.format("// Generated using cMoflon on %s%s", this.timeFormatter.format(new Date()), nl());
   }

   private void generateSampleFiles(final IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, "Generate sample files", 1);
      final String appConfConstants = WorkspaceHelper.GEN_FOLDER + "/app-conf-constants.h.sample";
      final List<String> linesForSampleFile = new ArrayList<>(Arrays.asList("#define TOPOLOGYCONTROL_LINKS_HAVE_STATES"));
      int tcAlgorithmId = this.minTcComponentConstant;
      for (final String tcAlgorithm : this.tcClasses)
      {
         linesForSampleFile.add(String.format("#define %s %d", getAlgorithmPreprocessorId(tcAlgorithm), tcAlgorithmId));
         ++tcAlgorithmId;
      }
      final String content = StringUtils.join(linesForSampleFile, nl());
      IFile sampleFile = project.getFile(appConfConstants);
      if (sampleFile.exists())
      {
         sampleFile.setContents(new ReaderInputStream(new StringReader(content.toString())), true, true, subMon.split(2));
      } else
         WorkspaceHelper.addFile(project, appConfConstants, content, subMon.split(1));

   }

   private List<String> determineBuiltInTypes()
   {
      final List<String> builtInTypes = new ArrayList<String>();

      final EList<GenDataType> dataTypes = this.genModel.getEcoreGenPackage().getGenDataTypes();

      for (final GenDataType obj : dataTypes)
      {
         if (obj.getEcoreDataType() instanceof EDataType)
         {
            builtInTypes.add(obj.getEcoreDataType().getName());
         }
      }

      final EList<GenEnum> enums = this.genModel.getGenPackages().get(0).getGenEnums();
      for (final GenEnum eEnum : enums)
      {
         builtInTypes.add(eEnum.getName());
      }

      return builtInTypes;
   }

   private TemplateConfigurationProvider getTemplateConfigurationProvider()
   {
      return this.codeGenerationEngine.getTemplateConfigurationProvider();
   }

   /**
    * This methods gets all Fields of a GenClass, including Reference fields as
    * FieldAttributes usable in the StringTemplates
    *
    * @param genClass
    *            the genClass
    * @return a Collection of FieldAttributes
    */
   private Collection<? extends FieldAttribute> getFields(GenClass genClass)
   {
      final List<FieldAttribute> fields = new ArrayList<FieldAttribute>();
      final EClass clazz = genClass.getEcoreClass();

      for (final EAttribute att : clazz.getEAllAttributes())
      {
         // TODO: it is assumed, that EAttributes are not of List Type
         fields.add(new FieldAttribute(new Type(isBuiltInType(clazz.getName()), clazz.getName()),
               new Type(isBuiltInType(att.getEAttributeType().getName()), att.getEAttributeType().getName()), att.getName(), false));
      }

      for (final EReference ref : clazz.getEAllReferences())
      {
         fields.add(new FieldAttribute(new Type(isBuiltInType(clazz.getName()), clazz.getName()),
               new Type(isBuiltInType(ref.getEReferenceType().getName()), ref.getEReferenceType().getName()), ref.getName(), ref.getUpperBound() != 1));
      }

      return fields;
   }

   private String getProjectRelativePathForAlgorithm(String algorithmName)
   {
      return WorkspaceHelper.GEN_FOLDER + "/";
   }

   private String getAlgorithmBasename(final String algorithmName)
   {
      return getComponentName() + "-" + this.project.getName() + "-" + algorithmName;
   }

   private String getProcessPreludeCode(final String algorithmName, final STGroup templateGroup)
   {
      return SourceFileGenerator.generateUpperPart(getComponentName(), algorithmName, templateGroup, useHopCount(algorithmName),
            this.getAlgorithmPreprocessorId(algorithmName));
   }

   private String getAlgorithmPreprocessorId(String algorithmName)
   {
      return ("COMPONENT_" + getComponentName() + "_" + this.project.getName() + "_" + algorithmName).toUpperCase();
   }

   private String getProcessBodyCode(final String tcAlgorithm, final STGroup templateGroup)
   {
      StringBuilder processBodyCode = new StringBuilder();
      processBodyCode.append("\t\tprepareLinks();" + nl());
      processBodyCode.append("\t\t" + getTypeName(tcAlgorithm) + " tc;" + nl());
      processBodyCode.append("\t\ttc.node =  networkaddr_node_addr();" + nl());

      final ST template = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.PARAMETER_CONSTANT);
      final String algorithmInvocation = this.tcAlgorithmCallParameters.get(tcAlgorithm);
      processBodyCode.append(getParameters(algorithmInvocation, tcAlgorithm, template));
      if (this.useEvalStatements.contains(tcAlgorithm))
      {
         processBodyCode.append(EVAL_STATEMENTS_START);
         processBodyCode.append("\t\t" + getClassPrefixForMethods(tcAlgorithm) + "run(&tc);" + nl());
         processBodyCode.append(EVAL_STATEMENTS_END);
      } else
      {
         processBodyCode.append("\t\t" + getClassPrefixForMethods(tcAlgorithm) + "run(&tc);" + nl());
      }
      final ST cleanupCall = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.CLEANUP_CALL);
      processBodyCode.append(cleanupCall.render());
      return processBodyCode.toString();
   }

   private String getPatternMatchingCode(String tcAlgorithm)
   {
      StringBuilder allinjectedCode = new StringBuilder();
      for (final GenClass genClass : this.cachedConcreteClasses)
      {
    	 if((!genClass.getName().contentEquals(tcAlgorithm))&&isIrrelevantForAlgorithm(genClass,tcAlgorithm))
    		 continue;
         final String injectedCode = getPatternImplementationCode(genClass);
         if (injectedCode != null)
         {
            allinjectedCode.append(injectedCode);
         }
      }
      return allinjectedCode.toString();
   }

   private String getProcessClosingCode(final String tcAlgorithm, final STGroup templateGroup)
   {
      StringBuilder sb = new StringBuilder();
      if (!this.dropUnidirectionalEdgesOff.contains(tcAlgorithm))
      {
         sb.append(templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.DROP_UNIDIRECTIONAL_EDGES)
               .render());
      }
      sb.append(SourceFileGenerator.generateClosingPart(templateGroup, useHopCount(tcAlgorithm)));
      return sb.toString();
   }

   /**
    * Creates the code that is used by the hop-count calculation
    * @param component
    * @param algorithmName
    * @param source
    * @return
    */
   private String getHopCountCode(String algorithmName, STGroup source)
   {
      final ST hopcountTemplate = source.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.HOPCOUNT);
      hopcountTemplate.add("comp", getComponentName());
      hopcountTemplate.add("algo", algorithmName);
      final String hopCountCode = hopcountTemplate.render();
      return hopCountCode;
   }

   /**
    * Returns the prefix is placed in front of the method name when generating invocations of functions that represent methods
    *
    * @param clazz the surround class of the method
    * @return
    */
   private String getClassPrefixForMethods(final String clazz)
   {
      return clazz.substring(0, 1).toLowerCase() + clazz.substring(1) + "_";
   }

   /**
    * Returns the C type to use when referring to the given topology control class
    * @param tcClass
    * @return
    */
   private String getTypeName(final String tcClass)
   {
      return tcClass.toUpperCase() + "_T";
   }

   /**
    * This method generates the List and memory block allocations needed by the
    * generated Code.
    *
    * @return a String containing the List and Block declarations.
    */
   private String getListAndBlockDeclarations(final STGroup templateGroup)
   {
      final ST list = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.LIST_DECLARATION);
      final ST memb = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.MEMB_DECLARATION);
      final StringBuilder result = new StringBuilder(nl());
      for (final String s : this.blockDeclarations)
      {
         //Make sure STS are clean
         memb.remove("name");
         memb.remove("type");
         memb.remove("count");
         list.remove("name");
         //Fill STS
         memb.add("name", s);
         memb.add("type", "match_t");
         memb.add("count", "MAX_MATCH_COUNT");
         list.add("name", s);
         result.append(memb.render());
         result.append(list.render());
      }
      result.append(nl());
      return result.toString();
   }

   /**
    * Gets an initializer method for the Blocks and Lists declarated
    * @param templateGroup
    *
    * @return
    */
   private String getInitMethod(final STGroup templateGroup)
   {
      final ST init = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.INIT);
      init.add("blocks", this.blockDeclarations);
      init.add("duplicates",this.generateDuplicates);
      return init.render();
   }
   
   /**
    * Gets a cleanup method for eventual duplicates
    * @param templateGroup
    *
    * @return
    */
   private String getCleanupMethod(final STGroup templateGroup)
   {
      final ST cleanup = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.CLEANUP);
      cleanup.add("duplicates",this.generateDuplicates);
      return cleanup.render();
   }

   /**
    * Gets parameters for a method call inside the process structure.
    * Parameters are either defined directly in the properties or listed in the
    * constants
    *
    * @param property
    *            the String CSV containing the parameters
    * @param component
    *            Component string (needed for constants naming)
    * @param algo
    *            algo string as well needed for naming
    * @param template
    *            the StringTemplate for the parameters
    * @return a full String with comma separated parameters
    */
   private String getParameters(String property, String algo, ST template)
   {
      final StringBuilder result = new StringBuilder();
      template.add("comp", getComponentName());
      template.add("algo", algo);
      if (property == null)
         return "";
      else
      {
         String[] params = property.split(",");
         for (String p : params)
         {
            if (p.trim().contains(CMoflonProperties.PROPERTY_PREFIX_FOR_CONSTANTS))
            {
               template.remove("name");
               template.add("name", p.trim().split(CMoflonProperties.PROPERTY_PREFIX_FOR_CONSTANTS)[1]);
               result.append(template.render() + ";" + nl());
               result.append("\t\t");
            } else
               result.append(p.trim() + ";" + nl());
         }
      }
      String returnValue = result.substring(0, result.lastIndexOf(";"));
      if (!returnValue.isEmpty())
         returnValue = "\t\t" + returnValue + ";" + nl();
      return returnValue;
   }

   /**
    * Returns the implementations of the patterns
    *
    * @param genClass
    * @return returns the pattern matching code as string
    */
   private String getPatternImplementationCode(final GenClass genClass)
   {
      // Produces pattern matching code
      final StringBuilder code = new StringBuilder();

      for (final Adapter adapter : genClass.getEcoreClass().eAdapters())
      {
         if (adapter.isAdapterForType(SearchPlanAdapter.class))
         {
            final SearchPlanAdapter searchPlanAdapter = (SearchPlanAdapter) adapter;
            final String patternType = searchPlanAdapter.getPatternType();
            final OperationSequenceCompiler operationSequenceCompiler = getTemplateConfigurationProvider().getOperationSequenceCompiler(patternType);
            final TemplateInvocation template = searchPlanAdapter.prepareTemplateInvocation(operationSequenceCompiler, democlesImportManager);

            final ST st = getTemplateConfigurationProvider().getTemplateGroup(patternType).getInstanceOf(template.getTemplateName());
            final Map<String, Object> attributes = template.getAttributes();

            for (final Entry<String, Object> entry : attributes.entrySet())
            {
               st.add(entry.getKey(), entry.getValue());
            }
            String result=st.render();
            //code.append(st.render());
            code.append(result);
            code.append(nl());
            code.append(nl());
         }
      }
      return code.length() > 0 ? code.toString() : null;
   }
   
   /**
    * Checks whether the content refers to the tcAlgorithm with name name
    * @param content	the generated code
    * @param name	the name of the current tcAlgorithm
    * @return	true if code size reduction is activated an the content does not refer to the current algorithm but another tcalgorithm
    */
   private boolean isIrrelevantForAlgorithm(GenClass genClass,String currentTCAlgorithm) {
	 //Do not add code from other tc classes
	if(this.reduceCodeSize&&isTrueSubtypeOfTCAlgorithmParentClass(genClass))
		return true;
	else if(this.helperClasses.get(currentTCAlgorithm)!=null&&this.helperClasses.get(currentTCAlgorithm).contains(genClass.getName()))
		return false;
	else return true;
}

private List<String> getBlockDeclarations(final List<GenClass> cachedConcreteClasses)
   {
      final List<String> blockDeclarations = new ArrayList<>();
      for (final GenClass genClass : cachedConcreteClasses)
      {
         for (final Adapter adapter : genClass.getEcoreClass().eAdapters())
         {
            if (adapter.isAdapterForType(SearchPlanAdapter.class))
            {
               final SearchPlanAdapter searchPlanAdapter = (SearchPlanAdapter) adapter;
               final String patternType = searchPlanAdapter.getPatternType();
               final OperationSequenceCompiler operationSequenceCompiler = getTemplateConfigurationProvider().getOperationSequenceCompiler(patternType);
               final TemplateInvocation template = searchPlanAdapter.prepareTemplateInvocation(operationSequenceCompiler, democlesImportManager);

               final Map<String, Object> attributes = template.getAttributes();

               if (searchPlanAdapter.isMultiMatch())
               {
                  final String patternSignature = CompilerPatternBody.class.cast(attributes.get("body")).getHeader().getName() + attributes.get("adornment");
                  blockDeclarations.add(patternSignature);
               }
            }
         }
      }
      return blockDeclarations;
   }

   /**
    * Generates the Method Body for an eOperation or returns a Default text if
    * it is not properly implemented
    *
    * @param eOperation
    * @return the code for the eop or MoflonUtil.DEFAULT_METHOD_BODY
    */
   protected String getGeneratedMethodBody(final EOperation eOperation)
   {
      String generatedMethodBody = null;

      final AdapterResource cfResource = (AdapterResource) EcoreUtil.getExistingAdapter(eOperation, DemoclesMethodBodyHandler.CONTROL_FLOW_FILE_EXTENSION);
      if (cfResource != null)
      {
         final Scope scope = (Scope) cfResource.getContents().get(0);

         final STGroup group = getTemplateConfigurationProvider().getTemplateGroup(CMoflonTemplateConfiguration.CONTROL_FLOW_GENERATOR);
         final ST template = group.getInstanceOf("/" + CMoflonTemplateConfiguration.CONTROL_FLOW_GENERATOR + "/" + scope.getClass().getSimpleName());
         template.add("scope", scope);
         template.add("importManager", null);
         generatedMethodBody = template.render();
      }
      if (generatedMethodBody == null)
      {
         generatedMethodBody = MoflonUtil.DEFAULT_METHOD_BODY;
      }

      return generatedMethodBody;
   }

   private String getIncludesCode(final STGroup templateGroup,String algorithm)
   {
	   if(algorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)||!this.reduceCodeSize) {
		   if(algorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)){
			   return (HeaderFileGenerator.generateIncludes(Components.TOPOLOGYCONTROL,
			            templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.CMOFLON_HEADER_FILE_GENERATOR + "/" + CMoflonHeaderFileGenerator.INCLUDE))) + nl();
		   }
		   else return (HeaderFileGenerator.generateIncludes(Components.TOPOLOGYCONTROL,
			            templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.INCLUDE))) + nl();
		   }
		   else {
			   return "#include \"cMoflon.h\" "+nl();
		   }
   }

   private String getIncludeGuardCode(String algorithmName, final STGroup templateGroup)
   {
	  ST definition;
	  if(reduceCodeSize&&algorithmName.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)) {
			  definition = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.CMOFLON_HEADER_FILE_GENERATOR + "/" + CMoflonHeaderFileGenerator.HEADER_DEFINITION);  
	  }
	  else {
		  definition = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.HEADER_DEFINITION);
	      definition.add("comp", getComponentName().toUpperCase());
	      definition.add("algo", algorithmName.toUpperCase());
	  }
      String guardCode = definition.render();
      return guardCode;
   }

   private StringBuilder getConstantsDefinitionsCode(String algorithmName, STGroup templateGroup)
   {
	   //TODO:fix for CMoflonReader with prefixes
      final StringBuilder constantsCode = new StringBuilder();
      if(constantsMapping.containsKey(algorithmName)) {
	      for (final Entry<String, String> pair : constantsMapping.get(algorithmName).entrySet())
	      {
	    	 if(this.reduceCodeSize) {
	    		 if(algorithmName.contentEquals(TC_INDEPENDANT)) {
	    			 final ST constant = templateGroup
	    		               .getInstanceOf("/" + CMoflonTemplateConfiguration.CMOFLON_HEADER_FILE_GENERATOR + "/" + CMoflonHeaderFileGenerator.CONSTANTS_DEFINTION);
	    		         constantsCode.append(generateConstant(pair.getKey(), pair.getValue(), getComponentName(), algorithmName, constant));
	    		 }
	    		 else {
	    			 final ST constant = templateGroup
		    	               .getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_DEFINTION);
		    	         constantsCode.append(generateConstant(pair.getKey(), pair.getValue(), getComponentName(), algorithmName, constant)); 
	    		 }
	    	 }
	    	 else {
	    		 final ST constant = templateGroup
	    	               .getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_DEFINTION);
	    	         constantsCode.append(generateConstant(pair.getKey(), pair.getValue(), getComponentName(), algorithmName, constant)); 
	    	 }
	      }
      }
      return constantsCode;
   }

   private String getMatchTypeDefinitionCode(STGroup templateGroup,String tcAlgorithm)
   {
	   if(this.reduceCodeSize) {
		   if(tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)) {
			   ST match = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.CMOFLON_HEADER_FILE_GENERATOR + "/" + CMoflonHeaderFileGenerator.MATCH);
			   String matchTypeDef = match.render();
			   return matchTypeDef;
		   }
		   else {
			   return "";
		   }
	   }
	   else {
		   ST match = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.MATCH);
		      String matchTypeDef = match.render();
		      return matchTypeDef;
	   }
   }

   private String getMaxMatchCountDefinition(String tcAlgorithm)
   {
	  final StringBuilder mycontents = new StringBuilder();
	  mycontents.append("#ifndef MAX_MATCH_COUNT");
      mycontents.append(nl());
      mycontents.append(String.format("#define MAX_MATCH_COUNT %d%s", this.maximumMatchCount, nl()));
      mycontents.append("#endif" + nl());
	  if(this.reduceCodeSize) {
		  if(tcAlgorithm.contentEquals(TC_INDEPENDANT)) {
			  return mycontents.toString();
		  }else {
			  return "";
		  }
	  }else {
		  return mycontents.toString();  
	  }
   }
   
   private String getGenerateDuplicatesDefinition(String tcAlgorithm) {
	   final StringBuilder mycontents = new StringBuilder();
	   if(this.generateDuplicates.contains(tcAlgorithm)) {
		   mycontents.append("#ifndef GENERATE_DUPLICATES");
		   mycontents.append(nl());
		   mycontents.append("#define GENERATE_DUPLICATES"+nl());
		   mycontents.append("#endif"+nl());
		   mycontents.append("LIST(list_duplicates);"+nl()+nl());
		   return mycontents.toString();
	   }
	   else return "";
   }

   private String getTypeMappingCode(STGroup templateGroup,String tcAlgorithm)
   {
      StringBuilder typeMappingCodeBuilder = new StringBuilder();
      if(this.reduceCodeSize) {
    	  if(tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)) {
    		  ST typeMappingTemplate = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.CMOFLON_HEADER_FILE_GENERATOR + "/" + CMoflonHeaderFileGenerator.DEFINE);
    	      for (Entry<String, String> pair : typeMappings.entrySet())
    	      {
    	         typeMappingCodeBuilder.append(getTypeMappingCode(typeMappingTemplate, pair.getKey(), pair.getValue()));
    	         typeMappingCodeBuilder.append(nl());
    	      }
    		  return typeMappingCodeBuilder.toString();
    	  }
    	  else {
    		  return "";
    	  }
      }
      else {
    	  ST typeMappingTemplate = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.DEFINE);
          for (Entry<String, String> pair : typeMappings.entrySet())
          {
             typeMappingCodeBuilder.append(getTypeMappingCode(typeMappingTemplate, pair.getKey(), pair.getValue()));
          }
    	  return typeMappingCodeBuilder.toString();
      }
   }

   private String getTypeMappingCode(ST typeMappingTemplate, final String metamodelType, final Object cType)
   {
      typeMappingTemplate.remove("orig");
      typeMappingTemplate.remove("replaced");
      typeMappingTemplate.add("orig", metamodelType);
      typeMappingTemplate.add("replaced", cType);
      String typeMappingCode = typeMappingTemplate.render();
      return typeMappingCode;
   }

   private String getUnimplementedMethodsCode(final STGroup stg,final String tcAlgorithm)
   {
	   //FIXME: currently all methods are classified as unimplemented, but maybe this is needed as forward declaration
	   ST methoddecls;
	      StringBuilder builder = new StringBuilder();
		   if(this.reduceCodeSize) {
			   if(tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)) {
				   methoddecls = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.CMOFLON_HEADER_FILE_GENERATOR + "/" + CMoflonHeaderFileGenerator.METHOD_DECLARATION);
				   methoddecls.add("methods", this.cachedMethodSignatures.get(CMoflonCodeGenerator.TC_INDEPENDANT));
				   builder.append(methoddecls.render());
			  } else {
				  methoddecls = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.METHOD_DECLARATION);
				  methoddecls.add("methods", this.cachedMethodSignatures.get(tcAlgorithm));
			      builder.append(methoddecls.render());   
				  }
			  }
		   else {
			   methoddecls = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.METHOD_DECLARATION);
			   for(Map.Entry<String,List<MethodAttribute>> methods:this.cachedMethodSignatures.entrySet()) {
				   methoddecls.add("methods", methods.getValue());
				   builder.append(methoddecls.render());
				   methoddecls.remove("methods");
			   }
			      
		   }
	    return builder.toString();
   }

   private String getAccessorsCode(final STGroup stg,String tcAlgorithm)
   {
      ST declarations;
      StringBuilder builder = new StringBuilder();
	   if(this.reduceCodeSize) {
		   //TODO: filter more exactly
		   if(tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)) {
			   declarations = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.CMOFLON_HEADER_FILE_GENERATOR + "/" + CMoflonHeaderFileGenerator.DECLARATIONS);
			   declarations.add("fields", this.cachedFields.get(CMoflonCodeGenerator.TC_INDEPENDANT));
			   builder.append(declarations.render());
		  } else {
			  declarations = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.DECLARATIONS);
			  declarations.add("fields", this.cachedFields.get(tcAlgorithm));
		      builder.append(declarations.render());   
			  }
		  }
	   else {
		   declarations = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.DECLARATIONS);
		   for(Map.Entry<String,List<FieldAttribute>> fields:this.cachedFields.entrySet()) {
			   declarations.add("fields", fields.getValue());
			   builder.append(declarations.render());
			   declarations.remove("fields");
		   }
		      
	   }
    return builder.toString();
   }

   private String getComparisonFunctionsCode(STGroup stg,String tcAlgorithm)
   {      
      StringBuilder builder = new StringBuilder();
	   if(this.reduceCodeSize) {
		   if(tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)) {
			   final ST compare = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.CMOFLON_HEADER_FILE_GENERATOR + "/" + CMoflonHeaderFileGenerator.COMPARE_DECLARATION);
			   compare.add("types", getTypesFromGenModel(this.genModel,tcAlgorithm));
			   builder.append(compare.render());
		  } else {
			  final ST compare = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.COMPARE_DECLARATION);
			  compare.add("types", getTypesFromGenModel(this.genModel,tcAlgorithm));
		      builder.append(compare.render());   
			  }
		  }
	   else {
		   final ST compare = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.COMPARE_DECLARATION);
		   compare.add("types", getTypesFromGenModel(this.genModel,tcAlgorithm));
		      builder.append(compare.render());   
	   }
     return builder.toString();
   }

   private String getEqualsFunctionsCode(STGroup stg,String tcAlgorithm)
   {
	   StringBuilder builder = new StringBuilder();
	   if(this.reduceCodeSize) {
		   if(tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)) {
			   final ST equals = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.CMOFLON_HEADER_FILE_GENERATOR + "/" + CMoflonHeaderFileGenerator.EQUALS_DECLARATION);
			      equals.add("types",getBuiltInTypes());
			      builder.append(equals.render());
			      equals.remove("types");
			      equals.add("types", getTypesFromGenModel(this.genModel,tcAlgorithm));
			      builder.append(equals.render());
		  } else {
			  final ST equals = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.EQUALS_DECLARATION);
		      equals.add("types", getTypesFromGenModel(this.genModel,tcAlgorithm));
		      builder.append(equals.render());   
			  }
		  }
	   else {
		   final ST equals = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.EQUALS_DECLARATION);
		      equals.add("types",getBuiltInTypes());
		      builder.append(equals.render());
		      equals.remove("types");
		      equals.add("types", getTypesFromGenModel(this.genModel,tcAlgorithm));
		      builder.append(equals.render());   
	   }
      return builder.toString();
   }

   private String getHeaderTail(String algorithmName, STGroup stg)
   {
	  ST end;
	  if(this.reduceCodeSize&&algorithmName.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)) {
		  end = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.CMOFLON_HEADER_FILE_GENERATOR + "/" + CMoflonHeaderFileGenerator.CONSTANTS_END);
	  }
	  else {
		  end = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_END);
	      end.add("comp", getComponentName().toUpperCase());
	      end.add("algo", algorithmName.toUpperCase());
	  }
      return end.render() + nl();
   }

   /**
    * Gets a List of Types to generate compare and equals methods for
    *
    * @param genmodel
    *            to derive generated types from
    * @return
    */
   private List<Type> getTypesFromGenModel(final GenModel genmodel,String tcAlgorithm)
   {
      final List<Type> result = new ArrayList<Type>();
      // Add non built in Types
      for (GenPackage p : genmodel.getGenPackages())
      {
         for (GenClass clazz : p.getGenClasses())
         {
        	if(!this.reduceCodeSize) {
        		result.add(new Type(false, clazz.getName()));
        	}
        	else {
        		if(clazz.getName().contentEquals("Node")||clazz.getName().contentEquals("Link")||clazz.getName().contentEquals("TopologyControlAlgorithm")) {
            		//Add to cMoflon Header
        			if(tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)){
        				result.add(new Type(false, clazz.getName()));
        			}
        			else {
        				continue;
        			}
            	}
            	else {
            		if(!tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)){
        				if(isTrueSubtypeOfTCAlgorithmParentClass(clazz)&&clazz.getName().contentEquals(tcAlgorithm))
        					result.add(new Type(false, clazz.getName()));
        			}
        			else {
        				continue;
        			}
            	}
        	}
            
         }
      }
      return result;
   }
   
   private List<Type> getBuiltInTypes(){
	   final List<Type> result = new ArrayList<Type>();
	      // Add built in Types
	      for (CMoflonBuiltInTypes t : CMoflonBuiltInTypes.values())
	      {
	         result.add(new Type(true, t.name()));
	      }
	   return result;
   }

   /**
    * Checks whether a type is a built in ECore Type or not
    *
    * @param t
    *            the type to check
    * @return true if it is built in else false
    */
   private boolean isBuiltInType(final String t)
   {
      return this.builtInTypes.contains(t);
   }

   /**
    * Obtains the method parameters for a given EOperation
    *
    * @param eOperation
    *            the EOperation to obtain the parameters from
    * @return the Parameters as String
    */
   private String getParametersFromEcore(final EOperation eOperation)
   {
      STGroup source = this.getTemplateConfigurationProvider().getTemplateGroup(CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR);
      source.registerRenderer(String.class, new CMoflonStringRenderer());
      ST template = source.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.PARAMETER);
      final StringBuilder result = new StringBuilder();
      template.add("name", "this");
      template.add("type", new Type(isBuiltInType(eOperation.getEContainingClass().getName()), eOperation.getEContainingClass().getName()));
      result.append(template.render());
      EList<EParameter> parameters = eOperation.getEParameters();
      for (final EParameter p : parameters)
      {
         template.remove("name");
         template.remove("type");
         template.add("name", p.getName());
         template.add("type", new Type(isBuiltInType(p.getEType().getName()), p.getEType().getName()));
         result.append(template.render());
      }
      return result.substring(0, result.lastIndexOf(","));
   }

   private String getDefaultHelperMethods() throws CoreException
   {
      final StringBuilder result = new StringBuilder();
      result.append("// --- Begin of default cMoflon helpers" + nl());
      final String urlString = String.format("platform:/plugin/%s/resources/helper.c", WorkspaceHelper.getPluginId(getClass()));
      try
      {
         final URL url = new URL(urlString);
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream())))
         {
            result.append(reader.lines().collect(Collectors.joining(nl())));
         } catch (final IOException e)
         {
            throw new CoreException(
                  new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read default typedefs from " + url.toString(), e));
         }
      } catch (MalformedURLException e)
      {
         throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Invalid URL : " + urlString, e));
      }
      result.append("// --- End of default cMoflon helpers" + nl());
      result.append(nl());
      return result.toString();

   }

   private String getUserDefinedHelperMethods(final String tcAlgorithm) throws CoreException
   {
      final StringBuilder result = new StringBuilder();
      {
         final String projectRelativePath = "injection/custom-helpers.c";
         result.append("// --- Begin of user-defined algorithm-independent helpers (Path: '" + projectRelativePath + "')" + nl());
         final IFile helperFile = this.project.getFile(projectRelativePath);
         if (helperFile.exists())
         {
            InputStream stream = helperFile.getContents();
            try
            {
               result.append(IOUtils.toString(stream));
            } catch (IOException e)
            {
               throw new CoreException(
                     new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read user-defined helpers " + helperFile, e));
            } finally
            {
               IOUtils.closeQuietly(stream);
            }
         } else
         {
            createInjectionFolder();
            WorkspaceHelper.addFile(project, projectRelativePath, "// Algorithm-independent helper definitions." + nl(), new NullProgressMonitor());
         }
         result.append("// --- End of user-defined algorithm-independent helpers" + nl());
         result.append(nl());
      }
      {
         final String projectRelativePath = "injection/custom-helpers_" + tcAlgorithm + ".c";
         result.append("// --- Begin of user-defined helpers for " + tcAlgorithm + " (Path: '" + projectRelativePath + "')" + nl());
         final IFile helperFile = this.project.getFile(projectRelativePath);
         if (helperFile.exists())
         {
            InputStream stream = helperFile.getContents();
            try
            {
               result.append(IOUtils.toString(stream));
            } catch (IOException e)
            {
               throw new CoreException(
                     new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read user-defined helpers " + helperFile, e));
            } finally
            {
               IOUtils.closeQuietly(stream);
            }
         } else
         {
            createInjectionFolder();
            WorkspaceHelper.addFile(project, projectRelativePath, "// Helper definitions for '" + tcAlgorithm + "'." + nl(), new NullProgressMonitor());
         }
         result.append("// --- End of user-defined helpers for " + tcAlgorithm + nl());
         result.append(nl());
      }
      return result.toString();

   }

   private void createInjectionFolder() throws CoreException
   {
      WorkspaceHelper.addAllFolders(project, "injection", new NullProgressMonitor());
   }

   private String getDefaultTypedefs(String tcAlgorithm) throws CoreException
   {
	   final StringBuilder result = new StringBuilder();
	  if(tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)||!this.reduceCodeSize) {
		  result.append("// --- Begin of default cMoflon type definitions" + nl());
	      final String urlString = String.format("platform:/plugin/%s/resources/structs.c", WorkspaceHelper.getPluginId(getClass()));
	      try
	      {
	         final URL url = new URL(urlString);
	         try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream())))
	         {
	            result.append(reader.lines().collect(Collectors.joining(nl())));
	         } catch (final IOException e)
	         {
	            throw new CoreException(
	                  new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read default typedefs from " + url.toString(), e));
	         }
	      } catch (final MalformedURLException e)
	      {
	         throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Invalid URL : " + urlString, e));
	      }
	      result.append(nl());
	      result.append("// --- End of default cMoflon type definitions" + nl());
	      result.append(nl());
	  }
      return result.toString();
   }

   private String getUserDefinedTypedefs(final String tcAlgorithm) throws CoreException
   {
      final StringBuilder result = new StringBuilder();
      //Insert custom algorithm-independent typedefs
      {
    	  final String projectRelativePath = "injection/custom-typedefs.c";
          result.append("// --- Begin of user-defined algorithm-independent type definitions (Path: '" + projectRelativePath + "')" + nl());
          final IFile helperFile = this.project.getFile(projectRelativePath);
          if (helperFile.exists())
          {
             InputStream stream = helperFile.getContents();
             try
             {
                result.append(IOUtils.toString(stream));
             } catch (IOException e)
             {
                throw new CoreException(
                      new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read user-defined helpers " + helperFile, e));
             } finally
             {
                IOUtils.closeQuietly(stream);
             }
          } else
          {
             createInjectionFolder();
             WorkspaceHelper.addFile(project, projectRelativePath, "// Algorithm-independent type definitions." + nl(), new NullProgressMonitor());
          }
          result.append("// --- End of user-defined algorithm-independent type definitions" + nl());
          result.append(nl());
    	 if(this.reduceCodeSize) {
    		 if(tcAlgorithm.contentEquals(TC_INDEPENDANT)) {
    			 ;
    		 }
    		 else {
    			 result.delete(0, result.length());
    		 }
    	 }
    	 else {
    		 ;
    	 }
      }
      {
    	 //Insert algorithm specific typedefs
    	  if(tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT))
    		  return result.toString();
    	  final StringBuilder algorithmSpecificContent = new StringBuilder();
    	  final String projectRelativePath = "injection/custom-typedefs_" + tcAlgorithm + ".c";
          algorithmSpecificContent.append("// --- Begin of user-defined type definitions for " + tcAlgorithm + "(Path: '" + projectRelativePath + "')" + nl());
          final IFile helperFile = this.project.getFile(projectRelativePath);
          if (helperFile.exists())
          {
             InputStream stream = helperFile.getContents();
             try
             {
             	algorithmSpecificContent.append(IOUtils.toString(stream));
             } catch (IOException e)
             {
                throw new CoreException(
                      new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read user-defined helpers " + helperFile, e));
             } finally
             {
                IOUtils.closeQuietly(stream);
             }
          } else
          {
        	 createInjectionFolder();
             WorkspaceHelper.addFile(project, projectRelativePath, "// Type definitions for algorithm '" + tcAlgorithm + "'." + nl(), new NullProgressMonitor());
          }
          algorithmSpecificContent.append("// --- End of user-defined type definitions for " + tcAlgorithm + nl());
          algorithmSpecificContent.append(nl());
    	  if(this.reduceCodeSize) {
    		  if(tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)) {
    			  ;
    		  }else {
    			  result.append(algorithmSpecificContent.toString());
    		  }
    	  }else {
    		  result.append(algorithmSpecificContent.toString());
    	  }
      }
      return result.toString();

   }

   public static String nl()
   {
      return "\n";
   }
   
   /**
    * Gets the name of the TCAlgorithm of a GenClass or the TC_INDEPENDANT constant if the GenClass is no TCAlgorithm
    * @param genClass
    * @return
    */
   private String getTCAlgorithmNameForGenClass(GenClass genClass){
	   String tcAlgorithmName=CMoflonCodeGenerator.TC_INDEPENDANT;
	   	for(String tcAlgorithm:tcClasses) {
			if(genClass.getName().contentEquals(tcAlgorithm))
				tcAlgorithmName=tcAlgorithm;
		}
	   	return tcAlgorithmName;
   }

/**
    * Gets a String with Typedefs from EType to the C language Type.
    */
   public String getAllBuiltInMappings(String tcAlgorithm)
   {
      final StringBuilder result = new StringBuilder();
      if(tcAlgorithm.contentEquals(CMoflonCodeGenerator.TC_INDEPENDANT)||!this.reduceCodeSize) {
	      for (final CMoflonBuiltInTypes t : CMoflonBuiltInTypes.values())
	      {
	         result.append("typedef " + CMoflonBuiltInTypes.getCType(t) + " " + t.name() + ";");
	         result.append(nl());
	         result.append(nl());
	      }
      }
      return result.toString();
   }
   
   public String generateConstant(Object key, Object value, String component, String algorithm, ST template)
   {
      template.add("comp", component);
      template.add("algo", algorithm);
      template.add("name", key);
      template.add("value", value);
      return template.render();
   }
   
   private void readProperties(IProject project) throws CoreException{
	   final Properties cMoflonProperties = CMoflonWorkspaceHelper.getCMoflonPropertiesFile(project);
	   //TODO: check  initialization of fields
       for (final Entry<Object, Object> entry : cMoflonProperties.entrySet())
       {
          final String key = entry.getKey().toString();
          final String value = entry.getValue().toString();
          switch (key)
          {
          case CMoflonProperties.PROPERTY_TC_ALGORITHMS:
             this.tcClasses.addAll(Arrays.asList(value.split(",")).stream().map(s -> s.trim()).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
             this.tcClasses.stream()//
                   .filter(c -> !isClassInGenmodel(c, genModel)) //
                   .forEach(c -> reportMissingTopologyControlClass(c));
             this.tcClasses.removeAll(this.tcClasses.stream().filter(c -> !isClassInGenmodel(c, genModel)).collect(Collectors.toList()));
             this.tcClasses.stream().forEach(tcClass ->{
             	List<String>helperClasses=new ArrayList<>();
             	helperClasses.add("Node");
     	  		helperClasses.add("Link");
     	  		helperClasses.add("TopologyControlAlgorithm"); 
             	this.helperClasses.put(tcClass, helperClasses);
          			});
             continue;
          case CMoflonProperties.PROPERTY_PM_MAX_MATCH_COUNT:
             this.maximumMatchCount = Integer.parseInt(value);
             continue;
          case CMoflonProperties.PROPERTY_TC_MIN_ALGORITHM_ID:
             this.minTcComponentConstant = Integer.parseInt(value);
             continue;
          case CMoflonProperties.PROPERTY_REDUCE_CODE_SIZE:
          	this.reduceCodeSize=Boolean.parseBoolean(value);
          	continue;
          }
          if (key.startsWith(CMoflonProperties.PROPERTY_PREFIX_TOPOLOGYCONTROL))
          {
        	  //TCmethod.
        	  String copyString=key;
        	  copyString=copyString.replaceFirst(CMoflonProperties.PROPERTY_PREFIX_TOPOLOGYCONTROL, "");
        	  //Index of dot before option
        	  int dotIndex=copyString.indexOf(".");
        	  String tcAlgorithm=copyString.substring(0, dotIndex);
        	  final String option=(copyString.substring(dotIndex)).trim();
        	  switch(option) {
        	  	case CMoflonProperties.PROPERTY_POSTFIX_PARAMETERS:
        	  		this.tcAlgorithmCallParameters.put(tcAlgorithm, value);
        	  		continue;
        	  	case CMoflonProperties.PROPERTY_POSTFIX_CONSTANTS:
        	  		Arrays.asList(value.split(",")).stream().map(s ->s.trim()).forEach(kv -> {
        	  			List<String> list= Arrays.asList(kv.split("=")).stream().map(s -> s.trim()).collect(Collectors.toList());
        	  			if(list.size()!=2)
        	  				return;
        	  			else {
        	  				if(!this.constantsMapping.containsKey(tcAlgorithm)) {
        	  					//Initialize map on first parameter
        	  					Map<String,String> constantsForAlgorithm=new HashMap<>();
        	  					this.constantsMapping.put(tcAlgorithm, constantsForAlgorithm);
        	  				}
        	  				Map<String,String> constantsForAlgorithm=this.constantsMapping.get(tcAlgorithm);
        	  				constantsForAlgorithm.put(list.get(0).replaceFirst("const-", ""), list.get(1));
        	  				this.constantsMapping.put(tcAlgorithm, constantsForAlgorithm);
        	  			}
        	  		} );
        	  		continue;
        	  	case CMoflonProperties.PROPERTY_POSTFIX_HELPERCLASSES:
        	  		List<String> helperClasses=Arrays.asList(value.split(",")).stream().map(s -> s.trim()).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        	  		this.helperClasses.get(tcAlgorithm).addAll(helperClasses);
        	  		continue;
        	  	case CMoflonProperties.PROPERTY_POSTFIX_DROP_UNIDIRECTIONAL_EDGES:
        	  		if(!Boolean.parseBoolean(value))
        	  			this.dropUnidirectionalEdgesOff.add(tcAlgorithm);
        	  		continue;
        	  	case CMoflonProperties.PROPERTY_POSTFIX_INCLUDE_EVALUATION_STATEMENTS:
        	  		if(Boolean.parseBoolean(value))
        	  			this.useEvalStatements.add(tcAlgorithm);
        	  		continue;
        	  	case CMoflonProperties.PROPERTY_POSTFIX_USE_HOPCOUNT:
        	  		if(Boolean.parseBoolean(value))
        	  			this.useHopCountProcess.add(tcAlgorithm);
        	  		continue;
        	  	case CMoflonProperties.PROPERTY_POSTFIX_DUPLICATE_EDGES:
        	  		if(Boolean.parseBoolean(value))
        	  			this.generateDuplicates.add(tcAlgorithm);
        	  		continue;
        	  }
          }else if (key.startsWith(CMoflonProperties.PROPERTY_PREFIX_FOR_TYPE_MAPPINGS))
          {
              typeMappings.put(key.replaceAll(CMoflonProperties.PROPERTY_PREFIX_FOR_TYPE_MAPPINGS, ""), value.trim());
           }
       }
   }
}

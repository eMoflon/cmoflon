package org.cmoflon.ide.core.runtime.codegeneration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.codegeneration.HeaderFileGenerator.BuiltInTypes;
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
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
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

/**
 * Generates the Source and the Header File.
 * 
 * @author David Giessing
 *
 */
public class CMoflonCodeGenerator
{

   private static final Logger logger = Logger.getLogger(CMoflonCodeGenerator.class);

   private static final String COMPONENT_TOPOLOGY_CONTROL = "topologycontrol";

   /**
    * List of built-in types provided by ECore
    */
   private List<String> builtInTypes;

   private IProject project;

   /**
    * If true, the code generator shall add code that avoids unidirectional edges in the topology.
    */
   private boolean dropUnidirectionalEdges;

   /**
    * If true, an auxiliary process for determining the hop-count attribute of nodes is provided
    */
   private boolean useHopcounts;

   private ImportManager democlesImportManager;

   private List<String> blockDeclarations = null;

   private GenModel genModel;

   private DemoclesGeneratorAdapterFactory codeGenerationEngine;

   private final List<String> tcClasses;

   private final Map<String, String> tcAlgorithmParameters;

   private int maximumMatchCount;

   private final Map<String, String> typeMappings;

   private final Map<String, String> constantsMapping;

   public CMoflonCodeGenerator(Resource ecore, IProject project, GenModel genModel, Descriptor codeGenerationEngine)
   {
      try
      {
         this.codeGenerationEngine = (DemoclesGeneratorAdapterFactory) codeGenerationEngine;
         this.project = project;
         this.genModel = genModel;
         this.tcAlgorithmParameters = new HashMap<>();
         this.typeMappings = new HashMap<>();
         this.constantsMapping = new HashMap<>();
         this.tcClasses = new ArrayList<>();
         this.blockDeclarations = new ArrayList<String>();

         final Properties cMoflonProperties = CMoflonWorkspaceHelper.getCMoflonPropertiesFile(project);

         for (final Entry<Object, Object> entry : cMoflonProperties.entrySet())
         {
            final String key = entry.getKey().toString();
            final String value = entry.getValue().toString();
            if (key.equals(CMoflonProperties.PROPERTY_TC_DROP_UNIDIRECTIONAL_EDGES))
            {
               this.dropUnidirectionalEdges = Boolean.parseBoolean(value);
            } else if (key.equals(CMoflonProperties.PROPERTY_TC_USE_HOPCOUNT))
            {
               this.useHopcounts = Boolean.parseBoolean(value);
            } else if (key.equals(CMoflonProperties.PROPERTY_TC_ALGORITHMS))
            {
               this.tcClasses.addAll(Arrays.asList(value.split(",")).stream().map(s -> s.trim()).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
               this.tcClasses.stream()//
                     .filter(c -> !isClassInGenmodel(c, genModel)) //
                     .forEach(c -> reportMissingTopologyControlClass(c));
               this.tcClasses.removeAll(this.tcClasses.stream().filter(c -> !isClassInGenmodel(c, genModel)).collect(Collectors.toList()));

            } else if (key.equals(CMoflonProperties.PROPERTY_PM_MAX_MATCH_COUNT))
            {
               this.maximumMatchCount = Integer.parseInt(value);
            }

            else if (key.startsWith(CMoflonProperties.PROPERTY_PREFIX_PARAMETERS))
            {
               tcAlgorithmParameters.put(key.replaceAll(CMoflonProperties.PROPERTY_PREFIX_PARAMETERS, ""), value);
            }

            else if (key.startsWith(CMoflonProperties.PROPERTY_PREFIX_FOR_TYPE_MAPPINGS))
            {
               typeMappings.put(key.replaceAll(CMoflonProperties.PROPERTY_PREFIX_FOR_TYPE_MAPPINGS, ""), value.trim());
            } else if (key.startsWith(CMoflonProperties.PROPERTY_PREFIX_FOR_CONSTANTS))
            {
               constantsMapping.put(key.replace(CMoflonProperties.PROPERTY_PREFIX_FOR_CONSTANTS, ""), value);
            }
         }

         this.constantsMapping.put("updateinterval", "300");
         this.builtInTypes = determinBuiltInTypes();
      } catch (final CoreException e)
      {
         throw new UncheckedCoreException(e);
      }
   }

   private void reportMissingTopologyControlClass(String c)
   {
      LogUtils.error(logger, "Topology class '%s' (specified in %s) cannot be found in GenModel and will be ignored.", c,
            CMoflonProperties.CMOFLON_PROPERTIES_FILENAME);
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

   public IStatus generateCode(IProgressMonitor monitor) throws CoreException
   {
      final String newline = System.getProperties().getProperty("line.separator");
      final SubMonitor subMon = SubMonitor.convert(monitor);
      final List<MethodAttribute> methods = new ArrayList<MethodAttribute>();
      final List<FieldAttribute> fields = new ArrayList<FieldAttribute>();
      String component = COMPONENT_TOPOLOGY_CONTROL;
      String algorithmName = "";
      String generatedCode = "";
      List<GenClass> genClassesForInjectedCode = new ArrayList<GenClass>();
      logger.info("in CMoflonCodeGenerator");
      for (final GenPackage genPackage : genModel.getGenPackages())
      {
         algorithmName = genPackage.getNSName();
         logger.info("GenPackage: " + genPackage.getPackageName());
         for (final GenClass genClass : genPackage.getGenClasses())
         {
            logger.info("GenClass: " + genClass.getName());
            if (genClass.isAbstract())
               continue;

            genClassesForInjectedCode.add(genClass);
            fields.addAll(getFields(genClass));

            for (final GenOperation genOperation : genClass.getGenOperations())
            {
               String generatedMethodBody = getGeneratedMethodBody(genOperation.getEcoreOperation());
               // generate Method Header
               if (!generatedMethodBody.equals(MoflonUtil.DEFAULT_METHOD_BODY))
               {
                  LogUtils.info(logger, "Generate method body for '%s::%s'", genClass.getName(), genOperation.getName());
                  generatedCode += newline;
                  generatedCode += genOperation.getTypeParameters(genClass);
                  String[] typechain = genOperation.getImportedType(genClass).split("\\.");
                  String type = "";
                  if (typechain.length == 0)
                     type = genOperation.getImportedType(genClass);
                  else
                     type = typechain[typechain.length - 1];
                  if (!isBuiltInType(type) && !type.equalsIgnoreCase("void"))
                     type = type.toUpperCase() + "_T*";
                  generatedCode += type;
                  generatedCode += " ";
                  generatedCode += genOperation.getEcoreOperation().getEContainingClass().getName().toLowerCase() + "_";
                  generatedCode += genOperation.getName();
                  generatedCode += ("(");
                  generatedCode += getParametersFromEcore(genOperation.getEcoreOperation());
                  generatedCode += ("){" + newline);
                  for (String line : generatedMethodBody.split(nl()))
                  {
                     generatedCode += "\t" + line;
                  }
                  generatedCode += (newline + "}" + newline);
               } else
               {
                  LogUtils.info(logger, "No generated Method body for: '%s::%s'", genClass.getName(), genOperation.getName());
               }
               methods.add(new MethodAttribute(
                     new Type(isBuiltInType(genOperation.getGenClass().getName()),
                           genOperation.getGenClass().getName()),
                     new Type(
                           genOperation.getEcoreOperation().getEType() == null ? true
                                 : isBuiltInType(
                                       genOperation.getEcoreOperation().getEType() == null ? "void" : genOperation.getEcoreOperation().getEType().getName()),
                           genOperation.getEcoreOperation().getEType() == null ? "void" : genOperation.getEcoreOperation().getEType().getName()),
                     genOperation.getName(), getParametersFromEcore(genOperation.getEcoreOperation())));
            }
         }
      }

      generateHeader(component, algorithmName, null, fields, methods, subMon.split(1));

      generateSourceFile(component, algorithmName, generatedCode, genClassesForInjectedCode, subMon.split(1));

      return Status.OK_STATUS;
   }

   private List<String> determinBuiltInTypes()
   {
      final List<String> builtInTypes = new ArrayList<String>();

      final EList<GenDataType> dataTypes = this.genModel.getEcoreGenPackage().getGenDataTypes();

      for (GenDataType obj : dataTypes)
      {
         if (obj.getEcoreDataType() instanceof EDataType)
         {
            builtInTypes.add(obj.getEcoreDataType().getName());
         }
      }

      final EList<GenEnum> enums = this.genModel.getGenPackages().get(0).getGenEnums();
      for (GenEnum eEnum : enums)
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
      List<FieldAttribute> fields = new ArrayList<FieldAttribute>();
      EClass clazz = genClass.getEcoreClass();
      // Get Infos about attributes
      for (EAttribute att : clazz.getEAllAttributes())
      {
         // TODO: it is assumed, that EAttributes are not of List Type
         fields.add(new FieldAttribute(new Type(isBuiltInType(clazz.getName()), clazz.getName()),
               new Type(isBuiltInType(att.getEAttributeType().getName()), att.getEAttributeType().getName()), att.getName(), false));
      }
      // Get Infos about References
      for (EReference ref : clazz.getEAllReferences())
      {
         fields.add(new FieldAttribute(new Type(isBuiltInType(clazz.getName()), clazz.getName()),
               new Type(isBuiltInType(ref.getEReferenceType().getName()), ref.getEReferenceType().getName()), ref.getName(), ref.getUpperBound() != 1));
      }

      return fields;
   }

   /**
    * This Method generates the Source File.
    * 
    * @param component
    *            the name of the component that code is generated for
    * @param algorithmName
    *            the name of the specific algorithm
    * @param inProcessCode
    *            the String containing the code that shall be executed in the
    *            process
    * @param preparedCode
    *            helper methods, structs and more which will be placed outside
    *            the loop
    * @param genClass
    *            the genClass the code is generated for.
    * @throws CoreException 
    */
   private void generateSourceFile(String component, String algorithmName, String preparedCode, List<GenClass> genClasses, IProgressMonitor monitor)
         throws CoreException
   {
      STGroup templateGroup = getTemplateConfigurationProvider().getTemplateGroup(CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR);
      templateGroup.registerRenderer(String.class, new CMoflonStringRenderer());

      String contents = "";

      final String componentBasename = getComponentBaseName(component, algorithmName);
      contents += "#include \"" + componentBasename + ".h" + "\"" + nl();

      final String patternMatchingCode = getPatternMatchingCode(genClasses);
      contents += getListAndBlockDeclarations(templateGroup);

      if (this.useHopcounts)
      {
         contents += getHopCountCode(component, algorithmName, templateGroup);
      }
      contents += getDefaultHelperMethods();
      contents += getUserDefinedHelperMethods();
      contents += patternMatchingCode;
      contents += preparedCode;
      contents += getInitMethod(templateGroup);
      contents += getProcessPreludeCode(component, algorithmName, templateGroup);
      contents += getProcessBodyCode(component, genClasses, templateGroup);
      contents += getProcessClosingCode(templateGroup);

      final String outputFileName = CMoflonWorkspaceHelper.GEN_FOLDER + "/" + componentBasename + ".c";
      WorkspaceHelper.addFile(project, outputFileName, contents, monitor);

   }

   private String getComponentBaseName(String component, String algorithmName)
   {
      return component + "-" + algorithmName;
   }

   private String getProcessPreludeCode(String component, String algorithmName, STGroup templateGroup)
   {
      return SourceFileGenerator.generateUpperPart(component, algorithmName, templateGroup, this.useHopcounts);
   }

   private String getProcessBodyCode(String component, List<GenClass> genClasses, STGroup templateGroup)
   {
      String processBodyCode = "";
      for (final String tcClass : this.tcClasses)
      {
         processBodyCode += "\t\tprepareLinks();" + nl();
         processBodyCode += "\t\t" + getType(tcClass) + " tc;" + nl();
         processBodyCode += "\t\ttc.node =  networkaddr_node_addr();" + nl();

         final ST template = templateGroup
               .getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.PARAMETER_CONSTANT);
         final String algorithm = genClasses.get(0).getEcoreClass().getEPackage().getName();
         final String algorithmInvocation = this.tcAlgorithmParameters.get(tcClass);
         processBodyCode += getParameters(algorithmInvocation, component, algorithm, template);
         processBodyCode += "\t\t" + getClassPrefixForMethods(tcClass) + "run(&tc);" + nl();
      }
      return processBodyCode;
   }

   private String getPatternMatchingCode(List<GenClass> genClasses)
   {
      // Get PatternMatching code
      String allinjectedCode = "";
      for (GenClass genClass : genClasses)
      {
         String injectedCode = getInjectedCode(genClass);
         if (injectedCode != null)
         {
            allinjectedCode += injectedCode;
         }
      }
      return allinjectedCode;
   }

   private String getProcessClosingCode(STGroup templateGroup)
   {
      StringBuilder sb = new StringBuilder();
      if (this.dropUnidirectionalEdges)
      {
         sb.append(templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.DROP_UNIDIRECTIONAL_EDGES)
               .render());
      }
      sb.append(SourceFileGenerator.generateClosingPart(templateGroup, this.useHopcounts));
      return sb.toString();
   }

   /**
    * Creates the code that is used by the hop-count calculation
    * @param component
    * @param algorithmName
    * @param source
    * @return
    */
   private String getHopCountCode(String component, String algorithmName, STGroup source)
   {
      final ST hopcountTemplate = source.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.HOPCOUNT);
      hopcountTemplate.add("comp", component);
      hopcountTemplate.add("algo", algorithmName);
      final String hopCountCode = hopcountTemplate.render();
      return hopCountCode;
   }

   /**
    * Returns the prefix is placed in front of the method name when generating invocations of functions that represent methods
    * 
    * @param tcClass the surround class of the method
    * @return
    */
   private String getClassPrefixForMethods(final String tcClass)
   {
      return tcClass.toLowerCase() + "_";
   }

   /**
    * Returns the C type to use when referring to the given topology control class
    * @param tcClass
    * @return
    */
   private String getType(final String tcClass)
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
      return init.render();
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
   private String getParameters(String property, String component, String algo, ST template)
   {
      String result = "";
      template.add("comp", component);
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
               result += template.render() + ";" + nl();
               result += "\t\t";
            } else
               result += p.trim() + ";" + nl();
         }
      }
      String returnValue = result.substring(0, result.lastIndexOf(";"));
      if (!returnValue.isEmpty())
         returnValue = "\t\t" + returnValue + ";" + nl();
      return returnValue;
   }

   /**
    * Gets the PatternMatching Code As a sideeffect the LIST and MEMB
    * structures are generated
    * 
    * @param genClass
    * @return returns the pattern Matching code as string
    */
   private String getInjectedCode(final GenClass genClass)
   {
      // Produces pattern matching code
      final StringBuilder code = new StringBuilder();

      final EList<Adapter> adapters = genClass.getEcoreClass().eAdapters();
      for (int i = 0; i < adapters.size(); i++)
      {
         final Adapter adapter = adapters.get(i);
         if (adapter.isAdapterForType(SearchPlanAdapter.class))
         {
            final SearchPlanAdapter searchPlanAdapter = (SearchPlanAdapter) adapter;
            final String patternType = searchPlanAdapter.getPatternType();
            final OperationSequenceCompiler operationSequenceCompiler = getTemplateConfigurationProvider().getOperationSequenceCompiler(patternType);
            final TemplateInvocation template = searchPlanAdapter.prepareTemplateInvocation(operationSequenceCompiler, democlesImportManager);

            final ST st = getTemplateConfigurationProvider().getTemplateGroup(patternType).getInstanceOf(template.getTemplateName());
            final Map<String, Object> attributes = template.getAttributes();

            //TODO@rkluge: This is a nasty side-effect and should be removed in the future
            if (searchPlanAdapter.isMultiMatch())
               this.blockDeclarations.add(((CompilerPatternBody) attributes.get("body")).getHeader().getName() + attributes.get("adornment"));

            for (final Entry<String, Object> entry : attributes.entrySet())
            {
               st.add(entry.getKey(), entry.getValue());
            }
            //st.inspect();
            code.append(st.render());
            code.append(nl());
            code.append(nl());
         }
      }
      return code.length() > 0 ? code.toString() : null;
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
         //template.inspect();
         generatedMethodBody = template.render();
      }
      if (generatedMethodBody == null)
      {
         generatedMethodBody = MoflonUtil.DEFAULT_METHOD_BODY;
      }

      return generatedMethodBody;
   }

   /**
    * Generates the Header File including, constants, includes, method
    * declarations, accessor declarations as well as declarations for compare
    * and equals operations
    * 
    * @param componentName
    *            needed for naming
    * @param algorithmName
    *            needed for naming
    * @param operations
    *            List of EOPS to declare, can be null if not desired
    * @param fields
    *            the fields to generate accessors for
    * @param unimplementedMethods
    *            List of MethodAttributes, Methods that had no proper SDM
    *            implementations and therefore are unimplemented
    * @param monitor
    * @throws CoreException 
    */
   private void generateHeader(String componentName, String algorithmName, EList<EOperation> operations, List<FieldAttribute> fields,
         List<MethodAttribute> unimplementedMethods, IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor);
      final STGroup templateGroup = getTemplateConfigurationProvider().getTemplateGroup(CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR);
      templateGroup.registerRenderer(String.class, new CMoflonStringRenderer());
      String contents = "";
      contents += getIncludeGuardCode(componentName, algorithmName, templateGroup);
      contents += getIncludesCode(templateGroup);
      contents += getConstantsDefinitionsCode(componentName, algorithmName, templateGroup);
      contents += getMaxMatchCountDefinition();
      contents += getMatchTypeDefinitionCode(templateGroup);
      contents += getTypeMappingCode(templateGroup);
      contents += HeaderFileGenerator.getAllBuiltInMappings();
      contents += getDefaultTypedefs();
      contents += getUserDefinedTypedefs();
      contents += getUnimplementedMethodsCode(unimplementedMethods, templateGroup);
      contents += getAccessorsCode(fields, templateGroup);
      contents += getComparisonFunctionsCode(templateGroup);
      contents += getEqualsFunctionsCode(templateGroup);
      contents += getHeaderTail(componentName, algorithmName, templateGroup);

      final String outputFileName = CMoflonWorkspaceHelper.GEN_FOLDER + "/" + getComponentBaseName(componentName, algorithmName) + ".h";
      WorkspaceHelper.addFile(project, outputFileName, contents, subMon.split(1));
   }

   private String getIncludesCode(final STGroup templateGroup)
   {
      return (HeaderFileGenerator.generateIncludes(Components.TOPOLOGYCONTROL,
            templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.INCLUDE))) + nl();
   }

   private String getIncludeGuardCode(String componentName, String algorithmName, final STGroup templateGroup)
   {
      ST definition = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_BEGIN);
      definition.add("comp", componentName.toUpperCase());
      definition.add("algo", algorithmName.toUpperCase());
      String guardCode = definition.render();
      return guardCode;
   }

   private StringBuilder getConstantsDefinitionsCode(String componentName, String algorithmName, STGroup templateGroup)
   {
      final StringBuilder constantsCode = new StringBuilder();
      for (final Entry<String, String> pair : constantsMapping.entrySet())
      {
         final ST constant = templateGroup
               .getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_DEFINTION);
         constantsCode.append(HeaderFileGenerator.generateConstant(pair.getKey(), pair.getValue(), componentName, algorithmName, constant));
      }
      return constantsCode;
   }

   private String getMatchTypeDefinitionCode(STGroup templateGroup)
   {
      ST match = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.MATCH);
      String matchTypeDef = match.render();
      return matchTypeDef;
   }

   private String getMaxMatchCountDefinition()
   {
      String mycontents = "#ifndef MAX_MATCH_COUNT" + nl();
      mycontents += String.format("#define MAX_MATCH_COUNT %d%s", this.maximumMatchCount, nl());
      mycontents += "#endif" + nl();
      return mycontents;
   }

   private String getTypeMappingCode(STGroup templateGroup)
   {
      StringBuilder typeMappingCodeBuilder = new StringBuilder();
      ST typeMappingTemplate = templateGroup.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.DEFINE);
      for (Entry<String, String> pair : typeMappings.entrySet())
      {
         typeMappingCodeBuilder.append(getTypeMappingCode(typeMappingTemplate, pair.getKey(), pair.getValue()));
         typeMappingCodeBuilder.append(nl());
      }
      String typeMappingCode = typeMappingCodeBuilder.toString();
      return typeMappingCode;
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

   private String getUnimplementedMethodsCode(List<MethodAttribute> unimplementedMethods, STGroup stg)
   {
      ST methoddecls = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.METHOD_DECLARATION);
      methoddecls.add("methods", unimplementedMethods);
      String unimplementedMethodsCode = methoddecls.render();
      return unimplementedMethodsCode;
   }

   private String getAccessorsCode(List<FieldAttribute> fields, STGroup stg)
   {
      ST declarations = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.DECLARATIONS);
      declarations.add("fields", fields);
      String declarationsCode = declarations.render();
      return declarationsCode;
   }

   private String getComparisonFunctionsCode(STGroup stg)
   {
      ST compare = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.COMPARE_DECLARATION);
      compare.add("types", getTypes(this.genModel));
      String compareCode = compare.render();
      return compareCode;
   }

   private String getEqualsFunctionsCode(STGroup stg)
   {
      ST equals = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.EQUALS_DECLARATION);
      equals.add("types", getTypes(this.genModel));
      String equalsCode = equals.render();
      return equalsCode;
   }

   private String getHeaderTail(String componentName, String algorithmName, STGroup stg)
   {
      ST end = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_END);
      end.add("comp", componentName.toUpperCase());
      end.add("algo", algorithmName.toUpperCase());
      String headerTail = end.render() + nl();
      return headerTail;
   }

   /**
    * Gets a List of Types to generate compare and equals methods for
    * 
    * @param genmodel
    *            to derive generated types from
    * @return
    */
   private List<Type> getTypes(GenModel genmodel)
   {
      List<Type> result = new ArrayList<Type>();
      // Add built in Types
      for (BuiltInTypes t : HeaderFileGenerator.BuiltInTypes.values())
      {
         result.add(new Type(true, t.name()));
      }
      // Add non built in Types
      for (GenPackage p : genmodel.getGenPackages())
      {
         for (GenClass clazz : p.getGenClasses())
         {
            result.add(new Type(false, clazz.getName()));
         }
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
    * @param op
    *            the EOperation to obtain the parameters from
    * @return the Parameters as String
    */
   private String getParametersFromEcore(EOperation op)
   {
      STGroup source = this.getTemplateConfigurationProvider().getTemplateGroup(CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR);
      source.registerRenderer(String.class, new CMoflonStringRenderer());
      ST template = source.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.PARAMETER);
      String result = "";
      template.add("name", "this");
      template.add("type", new Type(isBuiltInType(op.getEContainingClass().getName()), op.getEContainingClass().getName()));
      result += template.render();
      EList<EParameter> parameters = op.getEParameters();
      for (EParameter p : parameters)
      {
         template.remove("name");
         template.remove("type");
         template.add("name", p.getName());
         template.add("type", new Type(isBuiltInType(p.getEType().getName()), p.getEType().getName()));
         result += template.render();
      }
      return result.substring(0, result.lastIndexOf(","));
   }

   private String getDefaultHelperMethods() throws CoreException
   {
      String result = "";
      result += "// --- Begin of default cMoflon helpers" + nl();
      final String urlString = String.format("platform:/plugin/%s/resources/helper.c", WorkspaceHelper.getPluginId(getClass()));
      try
      {
         URL url = new URL(urlString);
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream())))
         {
            result += reader.lines().collect(Collectors.joining(nl()));
         } catch (final IOException e)
         {
            throw new CoreException(
                  new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read default typedefs from " + url.toString(), e));
         }
      } catch (MalformedURLException e)
      {
         throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Invalid URL : " + urlString, e));
      }
      result += "// --- End of default cMoflon helpers" + nl();
      return result += nl();

   }

   private String getUserDefinedHelperMethods() throws CoreException
   {
      String result = "";
      final String projectRelativePath = "injection/custom-helpers.c";
      result += "// --- Begin of user-defined helpers (from path '" + projectRelativePath + "')" + nl();
      final IFile helperFile = this.project.getFile(projectRelativePath);
      if (helperFile.exists())
      {
         InputStream stream = helperFile.getContents();
         try
         {
            result += IOUtils.toString(stream);
         } catch (IOException e)
         {
            throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read user-defined helpers " + helperFile, e));
         } finally
         {
            IOUtils.closeQuietly(stream);
         }
      }
      result += "// --- End of user-defined helpers" + nl();
      return result += nl();

   }

   private String getDefaultTypedefs() throws CoreException
   {
      String result = "";
      result += "// --- Begin of default cMoflon type definitions" + nl();
      final String urlString = String.format("platform:/plugin/%s/resources/structs.c", WorkspaceHelper.getPluginId(getClass()));
      try
      {
         final URL url = new URL(urlString);
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream())))
         {
            result += reader.lines().collect(Collectors.joining(nl()));
         } catch (final IOException e)
         {
            throw new CoreException(
                  new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read default typedefs from " + url.toString(), e));
         }
      } catch (final MalformedURLException e)
      {
         throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Invalid URL : " + urlString, e));
      }
      result += nl();
      result += "// --- End of default cMoflon type definitions" + nl();
      return result += nl();

   }

   private String getUserDefinedTypedefs() throws CoreException
   {
      String result = "";
      final String projectRelativePath = "injection/custom-typedefs.c";
      result += "// --- Begin of user-defined type definitions (from path '" + projectRelativePath + "')" + nl();
      final IFile helperFile = this.project.getFile(projectRelativePath);
      if (helperFile.exists())
      {
         InputStream stream = helperFile.getContents();
         try
         {
            result += IOUtils.toString(stream);
         } catch (IOException e)
         {
            throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read user-defined helpers " + helperFile, e));
         } finally
         {
            IOUtils.closeQuietly(stream);
         }
      }
      result += "// --- End of user-defined type definitions" + nl();
      return result += nl();

   }

   private String nl()
   {
      return "\n";
   }

}

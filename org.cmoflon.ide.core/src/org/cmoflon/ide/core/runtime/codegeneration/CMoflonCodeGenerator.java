package org.cmoflon.ide.core.runtime.codegeneration;

import java.io.BufferedReader;
import java.io.IOException;
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

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.codegeneration.HeaderFileGenerator.BuiltInTypes;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonIncludes.Components;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonStringRenderer;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.FieldAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.MethodAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.Type;
import org.cmoflon.ide.core.utilities.CMoflonWorkspaceHelper;
import org.cmoflon.ide.core.utilities.CMoflonProperties;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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

         final Properties cMoflonProperties = CMoflonWorkspaceHelper.getCMoflonPropertiesFile(project);

         for (final Entry<Object, Object> entry : cMoflonProperties.entrySet())
         {
            final String key = entry.getKey().toString();
            final String value = entry.getValue().toString();
            if (key.equals(org.cmoflon.ide.core.utilities.CMoflonProperties.PROPERTY_TC_DROP_UNIDIRECTIONAL_EDGES))
            {
               this.dropUnidirectionalEdges = Boolean.parseBoolean(value);
            } else if (key.equals(org.cmoflon.ide.core.utilities.CMoflonProperties.PROPERTY_TC_USE_HOPCOUNT))
            {
               this.useHopcounts = Boolean.parseBoolean(value);
            } else if (key.equals(org.cmoflon.ide.core.utilities.CMoflonProperties.PROPERTY_TC_ALGORITHMS))
            {
               this.tcClasses.addAll(Arrays.asList(value.split(",")).stream().map(s -> s.trim()).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
            } else if (key.equals(org.cmoflon.ide.core.utilities.CMoflonProperties.PROPERTY_PM_MAX_MATCH_COUNT))
            {
               this.maximumMatchCount = Integer.parseInt(value);
            }

            else if (key.startsWith(org.cmoflon.ide.core.utilities.CMoflonProperties.PROPERTY_PREFIX_PARAMETERS))
            {
               tcAlgorithmParameters.put(key.replaceAll(org.cmoflon.ide.core.utilities.CMoflonProperties.PROPERTY_PREFIX_PARAMETERS, ""), value);
            }

            else if (key.startsWith(org.cmoflon.ide.core.utilities.CMoflonProperties.PROPERTY_PREFIX_FOR_TYPE_MAPPINGS))
            {
               typeMappings.put(key.trim(), value.trim());
            } else if (key.startsWith(org.cmoflon.ide.core.utilities.CMoflonProperties.PROPERTY_PREFIX_FOR_CONSTANTS))
            {
               constantsMapping.put(key.replace(org.cmoflon.ide.core.utilities.CMoflonProperties.PROPERTY_PREFIX_FOR_CONSTANTS, ""), value);
            }
         }

         this.builtInTypes = determinBuiltInTypes();
      } catch (final CoreException e)
      {
         throw new UncheckedCoreException(e);
      }
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
            for (GenOperation genOperation : genClass.getGenOperations())
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
                  for (String line : generatedMethodBody.split("\n"))
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

      // generate "static" code for source file
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
    * @param generatedCode
    *            helper methods, structs and more which will be placed outside
    *            the loop
    * @param genClass
    *            the genClass the code is generated for.
    * @throws CoreException 
    */
   private void generateSourceFile(String component, String algorithmName, String generatedCode, List<GenClass> genClasses, IProgressMonitor monitor)
         throws CoreException
   {
      String inProcessCode = "";
      final List<String> tcClasses = this.tcClasses;
      STGroup source = getTemplateConfigurationProvider().getTemplateGroup(CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR);
      source.registerRenderer(String.class, new CMoflonStringRenderer());
      for (final String tcClass : tcClasses)
      {
         inProcessCode += "\t\tprepareLinks();\n";
         inProcessCode += "\t\t" + tcClass.trim().toUpperCase() + "_T tc;\n";
         inProcessCode += "\t\ttc.node =  networkaddr_node_addr();\n";
         final ST template = source.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.PARAMETER_CONSTANT);
         String algorithm = genClasses.get(0).getEcoreClass().getEPackage().getName();
         String algorithmInvocation = this.tcAlgorithmParameters.get(tcClass);
         inProcessCode += getParameters(algorithmInvocation, component, algorithm, template);
         inProcessCode += "\t\t" + tcClass.trim() + "_run(&tc);\n";
      }
      String filename = component + "-" + algorithmName;
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
      String contents = "";
      // Include the header.
      contents += "#include \"" + filename + ".h" + "\"\n";
      // LIST and MEMB declarations
      ST listDecl = source.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.LIST_DECLARATION);

      ST membDecl = source.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.MEMB_DECLARATION);
      contents += getListAndBlockDeclarations(membDecl, listDecl);
      ST hopcountCode = source.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.HOPCOUNT);
      if (this.useHopcounts)
         contents += hopcountCode.add("comp", component).add("algo", algorithmName).render();
      //Insert Helper Code
      contents += getDefaultHelperMethods();
      // PM code
      contents += allinjectedCode;
      // generated Code
      contents += generatedCode;
      // Init Method for MEMB and LIST
      ST init = source.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.INIT);
      contents += getInitMethod(init);
      //Upper Part of framework (PROCESS) Code
      contents += (SourceFileGenerator.generateUpperPart(component, algorithmName, source, this.useHopcounts));
      //Code to be executed as Process 
      contents += (inProcessCode);
      //Closing Part of the Framework code
      contents += (SourceFileGenerator.generateClosingPart(dropUnidirectionalEdges, source, this.useHopcounts));
      String outputFileName = CMoflonWorkspaceHelper.GEN_FOLDER + "/" + filename + ".c";
      IFile outputFile = project.getFile(outputFileName);
      if (outputFile.exists())
      {
         outputFile.delete(true, new NullProgressMonitor());
      }
      WorkspaceHelper.addFile(project, outputFileName, contents, monitor);

   }

   /**
    * This method generates the List and memory block allocations needed by the
    * generated Code.
    * 
    * @return a String containing the List and Block declarations.
    */
   private String getListAndBlockDeclarations(ST memb, ST list)
   {
      String result = "\n";
      for (String s : this.blockDeclarations)
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
         result += memb.render();
         result += list.render();
      }
      return result += "\n";
   }

   /**
    * Gets an initializer method for the Blocks and Lists declarated
    * 
    * @return
    */
   private String getInitMethod(ST init)
   {
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
               result += template.render() + ";\n\t\t";
            } else
               result += p.trim() + ";\n ";
         }
      }
      String returnValue = result.substring(0, result.lastIndexOf(";"));
      if (!returnValue.isEmpty())
         returnValue = "\t\t" + returnValue + ";\n";
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
      if (this.blockDeclarations == null)
         this.blockDeclarations = new ArrayList<String>();

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

            ST st = getTemplateConfigurationProvider().getTemplateGroup(patternType).getInstanceOf(template.getTemplateName());
            Map<String, Object> attributes = template.getAttributes();
            if (searchPlanAdapter.isMultiMatch())
               this.blockDeclarations.add(((CompilerPatternBody) attributes.get("body")).getHeader().getName() + attributes.get("adornment"));
            for (Entry<String, Object> entry : attributes.entrySet())
            {
               st.add(entry.getKey(), entry.getValue());
            }
            //st.inspect();
            code.append(st.render());
            code.append("\n\n");
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
    * @param methods
    *            List of MethodAttributes, Methods that had no proper SDM
    *            implementations and therefore are unimplemented
    * @param monitor
    * @throws CoreException 
    */
   protected void generateHeader(String componentName, String algorithmName, EList<EOperation> operations, List<FieldAttribute> fields,
         List<MethodAttribute> methods, IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor);
      String contents = "";
      STGroup stg = getTemplateConfigurationProvider().getTemplateGroup(CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR);
      // Generate Header Def
      stg.registerRenderer(String.class, new CMoflonStringRenderer());
      ST definition = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_BEGIN);
      definition.add("comp", componentName.toUpperCase());
      definition.add("algo", algorithmName.toUpperCase());
      contents += definition.render();
      contents += (HeaderFileGenerator.generateIncludes(Components.TOPOLOGYCONTROL,
            getTemplateConfigurationProvider().getTemplateGroup(CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR)
                  .getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.INCLUDE)))
            + "\n";
      // For Each Entry in the Constants Properties we need to generate one
      // Entry
      ST constant;
      constant = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_DEFINTION);
      contents += HeaderFileGenerator.generateConstant("updateinterval", 300, componentName, algorithmName, constant);
      for (Entry<String, String> pair : constantsMapping.entrySet())
      {
         constant = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_DEFINTION);
         contents += HeaderFileGenerator.generateConstant(pair.getKey(), pair.getValue(), componentName, algorithmName, constant);
      }
      //define the max match count
      contents += String.format("#ifndef MAX_MATCH_COUNT\n#define MAX_MATCH_COUNT %d\n", this.maximumMatchCount);
      contents += "#endif\n";
      ST match = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.MATCH);
      contents += match.render();
      // Typedefs
      ST define = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.DEFINE);
      for (Entry<String, String> pair : typeMappings.entrySet())
      {
         final String key = pair.getKey();
         final Object value = pair.getValue();
         define.remove("orig");
         define.remove("replaced");
         define.add("orig", key);
         define.add("replaced", value);
         contents += define.render();
      }
      contents += HeaderFileGenerator.getAllBuiltInMappings();
      contents += getDefaultTypedefs();
      // Non implemented Methods Declarations
      ST methoddecls = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.METHOD_DECLARATION);
      methoddecls.add("methods", methods);
      contents += methoddecls.render();
      // Accessor Declarations
      ST declarations = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.DECLARATIONS);
      declarations.add("fields", fields);
      contents += declarations.render();
      // Compare and Equals Declarations
      ST compare = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.COMPARE_DECLARATION);
      ST equals = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.EQUALS_DECLARATION);
      compare.add("types", getTypes(this.genModel));
      equals.add("types", getTypes(this.genModel));
      contents += compare.render();
      contents += equals.render();
      // Create Header Tail
      ST end = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_END);
      end.add("comp", componentName.toUpperCase());
      end.add("algo", algorithmName.toUpperCase());
      contents += end.render() + "\n";
      try
      {
         String outputFileName = CMoflonWorkspaceHelper.GEN_FOLDER + "/" + componentName + "-" + algorithmName + ".h";
         IFile outputFile = project.getFile(outputFileName);
         if (outputFile.exists())
         {
            outputFile.delete(true, new NullProgressMonitor());
         }
         CMoflonWorkspaceHelper.addFile(project, outputFileName, contents, subMon.split(1));
      } catch (CoreException e)
      {
         e.printStackTrace();
      }
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
   private boolean isBuiltInType(String t)
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
      String result = null;
      final String urlString = String.format("platform:/plugin/%s/resources/helper.c", WorkspaceHelper.getPluginId(getClass()));
      try
      {
         URL url = new URL(urlString);
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream())))
         {
            result = reader.lines().collect(Collectors.joining("\n"));
         } catch (final IOException e)
         {
            throw new CoreException(
                  new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read default typedefs from " + url.toString(), e));
         }
      } catch (MalformedURLException e)
      {
         throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Invalid URL : " + urlString, e));
      }
      return result += "\n";

   }

   private String getDefaultTypedefs() throws CoreException
   {
      String result = null;
      final String urlString = String.format("platform:/plugin/%s/resources/structs.c", WorkspaceHelper.getPluginId(getClass()));
      try
      {
         final URL url = new URL(urlString);
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream())))
         {
            result = reader.lines().collect(Collectors.joining("\n"));
         } catch (final IOException e)
         {
            throw new CoreException(
                  new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Failed to read default typedefs from " + url.toString(), e));
         }
      } catch (final MalformedURLException e)
      {
         throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), "Invalid URL : " + urlString, e));
      }
      return result += "\n";

   }

}

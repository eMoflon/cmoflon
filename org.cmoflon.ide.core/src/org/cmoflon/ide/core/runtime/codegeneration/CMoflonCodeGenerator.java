package org.cmoflon.ide.core.runtime.codegeneration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.codegeneration.HeaderFileGenerator.BuiltInTypes;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonStringRenderer;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonIncludes.Components;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.FieldAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.MethodAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.Type;
import org.cmoflon.ide.core.utilities.CMoflonWorkspaceHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenOperation;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.gervarro.democles.codegen.ImportManager;
import org.gervarro.democles.codegen.OperationSequenceCompiler;
import org.gervarro.democles.codegen.TemplateInvocation;
import org.gervarro.democles.compiler.CompilerPatternBody;
import org.moflon.compiler.sdm.democles.DemoclesMethodBodyHandler;
import org.moflon.compiler.sdm.democles.SearchPlanAdapter;
import org.moflon.compiler.sdm.democles.TemplateConfigurationProvider;
import org.moflon.compiler.sdm.democles.eclipse.AdapterResource;
import org.moflon.core.utilities.MoflonUtil;
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

   // A List of the Built in ETypes of EMoflon. Created at instantiation of the
   // CMoflonCodeGenerator.
   private List<String> builtInTypes;

   private static final Logger logger = Logger.getLogger(CMoflonCodeGenerator.class);

   private static final String COMPONENT_TOPOLOGY_CONTROL = "topologycontrol";

   private Resource ecore;

   private final TemplateConfigurationProvider templateProvider;

   private Properties constantProperties;

   private Properties mapping;

   private IProject project;

   // indicates whether unidirectional Edges should be dropped.
   private boolean dropUnidirectionalEdges;

   private ImportManager democlesImportManager;

   private List<String> blockDeclarations = null;

   private GenModel genModel;

   public CMoflonCodeGenerator(Resource ecore, IProject project, GenModel genModel)
   {
      this.templateProvider = getTemplateConfigurationProvider(genModel);
      this.ecore = ecore;
      this.project = project;
      this.genModel = genModel;
      // remove the dropUniDirectionalEdges entry but store it elsewhere
      this.constantProperties = CMoflonWorkspaceHelper.getConstantsPropertiesFile(project);
      boolean setUniDir = false;
      Set<Entry<Object, Object>> entrySet = constantProperties.entrySet();
      List<Object> removeables = new ArrayList<Object>();
      for (Entry<Object, Object> entry : entrySet)
      {
         if (!setUniDir && entry.getKey().toString().equals("dropUnidirectionalEdges"))
         {
            this.dropUnidirectionalEdges = Boolean.parseBoolean(entry.getValue().toString());
            setUniDir = true;
            removeables.add(entry.getKey());

         }
      }
      // Could not do this inline, lead to Exceptions
      for (Object rem : removeables)
         this.constantProperties.remove(rem);
      this.mapping = CMoflonWorkspaceHelper.getMappingPropertiesFile(project);

      this.builtInTypes = new ArrayList<String>();
      EList<EObject> resources = this.ecore.getResourceSet().getResources().get(1).getContents().get(0).eContents();
      for (EObject obj : resources)
      {
         if (obj instanceof EDataType)
         {
            this.builtInTypes.add(((EDataType) obj).getName());
         }
      }
   }

   public IStatus generateCode(IProgressMonitor monitor)
   {
      final String newline = System.getProperties().getProperty("line.separator");
      final SubMonitor subMon = SubMonitor.convert(monitor);
      List<MethodAttribute> methods = new ArrayList<MethodAttribute>();
      List<FieldAttribute> fields = new ArrayList<FieldAttribute>();
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
            genClassesForInjectedCode.add(genClass);
            fields.addAll(getFields(genClass));
            if(genClass.isAbstract())
            	continue;
            for (GenOperation genOperation : genClass.getGenOperations())
            {
               String generatedMethodBody = getGeneratedMethodBody(genOperation.getEcoreOperation());
               // generate Method Header
               if (!generatedMethodBody.equals(MoflonUtil.DEFAULT_METHOD_BODY))
               {
                  generatedCode += newline;
                  generatedCode += genOperation.getTypeParameters(genClass);
                  generatedCode += genOperation.getImportedType(genClass);
                  generatedCode += " ";
                  generatedCode += genOperation.getName();
                  generatedCode += ("(");
                  generatedCode += getParametersFromEcore(genOperation.getEcoreOperation());
                  generatedCode += ("){" + newline);
                  for (String line : generatedMethodBody.split("\n"))
                  {
                     generatedCode += "\t" + line;
                  }
                  generatedCode += ("}" + newline);
               } else
               {
                  logger.info("No generated Method body for: " + genOperation.getName());
                  methods.add(new MethodAttribute(new Type(isBuiltInType(genOperation.getGenClass().getName()), genOperation.getGenClass().getName()),
                        new Type(isBuiltInType(genOperation.getEcoreOperation().getEType()==null?"void":genOperation.getEcoreOperation().getEType().getName()), genOperation.getEcoreOperation().getEType()==null?"void":genOperation.getEcoreOperation().getEType().getName()),
                        genOperation.getName(), getParametersFromEcore(genOperation.getEcoreOperation())));
               }
            }
         }
      }

      generateHeader(component, algorithmName, null, fields, methods, subMon.split(1));

      // generate "static" code for source file
      generateSourceFile(component, algorithmName, generatedCode, genClassesForInjectedCode, subMon.split(1));

      return Status.OK_STATUS;
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
               new Type(isBuiltInType(ref.getEReferenceType().getName()), ref.getEReferenceType().getName()), ref.getName(), ref.getUpperBound() < 0));
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
    */
   protected void generateSourceFile(String component, String algorithmName, String generatedCode, List<GenClass> genClasses, IProgressMonitor monitor)
   {
      String inProcessCode = "";
      String[] tcMethods = ((String) constantProperties.get("tcMethods")).split(",");
      STGroup source = templateProvider.getTemplateGroup(CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR);
      source.registerRenderer(String.class, new CMoflonStringRenderer());
      for (String method : tcMethods)
      {
         inProcessCode += "\t\t" + method.trim() + "("
               + getParameters(constantProperties.getProperty(method.trim()), component, genClasses.get(0).getEcoreClass().getEPackage().getName(),
                     source.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.PARAMETER_CONSTANT))
               + ");\n";
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
      // PM code
      contents += allinjectedCode;
      // generated Code
      contents += generatedCode;
      // Init Method for MEMB and LIST
      ST init = source.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + SourceFileGenerator.INIT);
      contents += getInitMethod(init);
      //Upper Part of framework (PROCESS) Code
      contents += (SourceFileGenerator.generateUpperPart(component, algorithmName, source));
      //Code to be executed as Process 
      contents += (inProcessCode);
      //Closing Part of the Framework code
      contents += (SourceFileGenerator.generateClosingPart(dropUnidirectionalEdges, source));
      try
      {
         String outputFileName = CMoflonWorkspaceHelper.GEN_FOLDER + "/" + filename + ".c";
         IFile outputFile = project.getFile(outputFileName);
         if (outputFile.exists())
         {
            outputFile.delete(true, new NullProgressMonitor());
         }
         WorkspaceHelper.addFile(project, outputFileName, contents, monitor);
      } catch (CoreException e)
      {
         e.printStackTrace();
      }

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
      memb.add("name", "local");
      memb.add("type", "LINK_T");
      memb.add("count", "MAX_MATCH_COUNT*3");
      result += memb.render();
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
            if (p.trim().contains("const-"))
            {
               template.remove("name");
               template.add("name", p.trim().split("const-")[1]);
               result += template.render() + ", ";
            } else
               result += p.trim() + ", ";
         }
      }
      return result.substring(0, result.lastIndexOf(", "));
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
            final OperationSequenceCompiler operationSequenceCompiler = templateProvider.getOperationSequenceCompiler(patternType);
            final TemplateInvocation template = searchPlanAdapter.prepareTemplateInvocation(operationSequenceCompiler, democlesImportManager);

            ST st = templateProvider.getTemplateGroup(patternType).getInstanceOf(template.getTemplateName());
            Map<String, Object> attributes = template.getAttributes();
            this.blockDeclarations.add(((CompilerPatternBody) attributes.get("body")).getHeader().getName() + attributes.get("adornment"));
            for (Entry<String, Object> entry : attributes.entrySet())
            {
               st.add(entry.getKey(), entry.getValue());
            }
            st.inspect();
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

         final STGroup group = templateProvider.getTemplateGroup(CMoflonTemplateConfiguration.CONTROL_FLOW_GENERATOR);
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

   private TemplateConfigurationProvider getTemplateConfigurationProvider(GenModel genmodel)
   {

      return new CMoflonTemplateConfiguration(genmodel);
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
    */
   protected void generateHeader(String componentName, String algorithmName, EList<EOperation> operations, List<FieldAttribute> fields,
         List<MethodAttribute> methods, IProgressMonitor monitor)
   {
      final SubMonitor subMon = SubMonitor.convert(monitor);
      String contents = "";
      STGroup stg = templateProvider.getTemplateGroup(CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR);
      // Generate Header Def
      stg.registerRenderer(String.class, new CMoflonStringRenderer());
      ST definition = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_BEGIN);
      definition.add("comp", componentName.toUpperCase());
      definition.add("algo", algorithmName.toUpperCase());
      contents += definition.render();
      contents += (HeaderFileGenerator.generateIncludes(Components.TOPOLOGYCONTROL,
            templateProvider.getTemplateGroup(CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR)
                  .getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.INCLUDE)))
            + "\n";
      // For Each Entry in the Constants Properties we need to generate one
      // Entry
      ST constant;
      for (Entry<Object, Object> pair : constantProperties.entrySet())
      {
         if (pair.getKey().toString().contains("const-"))
         {
            constant = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.CONSTANTS_DEFINTION);
            contents += HeaderFileGenerator.generateConstant(pair.getKey().toString().split("const-")[1], pair.getValue(), componentName, algorithmName,
                  constant);
         }
      }
      //define the max match count
      contents += "#ifndef MAX_MATCH_COUNT\n#define MAX_MATCH_COUNT " + constantProperties.get("MAX_MATCH_COUNT") + "\n#endif\n";
      ST match = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.MATCH);
      contents += match.render();
      // Typedefs
      ST define = stg.getInstanceOf("/" + CMoflonTemplateConfiguration.HEADER_FILE_GENERATOR + "/" + HeaderFileGenerator.DEFINE);
      for (Entry<Object, Object> pair : mapping.entrySet())
      {
         define.remove("orig");
         define.remove("replaced");
         define.add("orig", pair.getKey());
         define.add("replaced", pair.getValue());
         contents += define.render();
      }
      contents += HeaderFileGenerator.getAllBuiltInMappings();
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
      contents += end.render();
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
      STGroup source = this.templateProvider.getTemplateGroup(CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR);
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

}

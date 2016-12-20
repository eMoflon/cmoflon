package org.cmoflon.ide.core.runtime.codegeneration;

import java.util.List;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.utilities.CMoflonMonitoredMetamodelLoader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.codegen.ecore.generator.GeneratorAdapterFactory.Descriptor;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.gervarro.eclipse.task.ITask;
import org.moflon.codegen.MethodBodyHandler;
import org.moflon.codegen.eclipse.CodeGeneratorPlugin;
import org.moflon.codegen.eclipse.GenericMoflonProcess;
import org.moflon.codegen.eclipse.MonitoredGenModelBuilder;
import org.moflon.codegen.eclipse.MonitoredMetamodelLoader;
import org.moflon.compiler.sdm.democles.DefaultCodeGeneratorConfig;
import org.moflon.compiler.sdm.democles.DemoclesGeneratorAdapterFactory;
import org.moflon.compiler.sdm.democles.DemoclesMethodBodyHandler;
import org.moflon.compiler.sdm.democles.attributes.AttributeConstraintTemplateConfig;
import org.moflon.core.propertycontainer.MoflonPropertiesContainer;
import org.moflon.core.propertycontainer.MoflonPropertiesContainerHelper;

public class CMoflonCodeGeneratorTask implements ITask
{

   private static final Logger logger = Logger.getLogger(CMoflonCodeGeneratorTask.class);

   private final IFile ecoreFile;

   private final ResourceSet resourceSet;

   private List<Resource> resources;

   private MoflonPropertiesContainer moflonProperties;

   private GenModel genModel;

   public CMoflonCodeGeneratorTask(IFile ecoreFile, ResourceSet resourceSet)
   {
      this.ecoreFile = ecoreFile;
      this.resourceSet = resourceSet;
   }

   /**
    * This is almost equal to
    * {@link GenericMoflonProcess#run(IProgressMonitor)}, except for using
    * {@link CMoflonMonitoredMetamodelLoader} instead of
    * {@link MonitoredMetamodelLoader}
    */
   @Override
   public IStatus run(IProgressMonitor monitor)
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, getTaskName(), 10);
      try
      {
         // (1) Loads moflon.properties file
         final IProject project = ecoreFile.getProject();
         final URI projectURI = URI.createPlatformResourceURI(project.getName() + "/", true);
         final URI moflonPropertiesURI = URI.createURI(MoflonPropertiesContainerHelper.MOFLON_CONFIG_FILE).resolve(projectURI);
         final Resource moflonPropertiesResource = CodeGeneratorPlugin.createDefaultResourceSet().getResource(moflonPropertiesURI, true);
         this.moflonProperties = (MoflonPropertiesContainer) moflonPropertiesResource.getContents().get(0);

         subMon.worked(1);
         if (subMon.isCanceled())
         {
            return Status.CANCEL_STATUS;
         }
      } catch (WrappedException wrappedException)
      {
         final Exception exception = wrappedException.exception();
         return new Status(IStatus.ERROR, CodeGeneratorPlugin.getModuleID(), exception.getMessage(), exception);
      } catch (RuntimeException runtimeException)
      {
         return new Status(IStatus.ERROR, CodeGeneratorPlugin.getModuleID(), runtimeException.getMessage(), runtimeException);
      }

      // (2) Load metamodel
      final CMoflonMonitoredMetamodelLoader metamodelLoader = new CMoflonMonitoredMetamodelLoader(resourceSet, ecoreFile, moflonProperties);
      final IStatus metamodelLoaderStatus = metamodelLoader.run(subMon.split(2));
      if (monitor.isCanceled())
      {
         return Status.CANCEL_STATUS;
      }
      if (metamodelLoaderStatus.matches(IStatus.ERROR))
      {
         return metamodelLoaderStatus;
      }
      this.resources = metamodelLoader.getResources();

      return this.processResource(subMon.split(7));
   }

   public IStatus processResource(IProgressMonitor monitor)
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, "Code generation task", 100);
      try
      {
         final MoflonPropertiesContainer moflonProperties = getMoflonProperties();
         logger.info("Generating code for: " + moflonProperties.getMetaModelProject().getMetaModelProjectName() + "::" + moflonProperties.getProjectName());

         long toc = System.nanoTime();

         final Resource resource = getEcoreResource();
         final EPackage ePackage = (EPackage) resource.getContents().get(0);

         // (1) Instantiate code generation engine
         final CMoflonAttributeConstraintCodeGeneratorConfig defaultCodeGeneratorConfig = new CMoflonAttributeConstraintCodeGeneratorConfig(resourceSet, ecoreFile.getProject());
         MethodBodyHandler methodBodyHandler = new DemoclesMethodBodyHandler(resourceSet, defaultCodeGeneratorConfig);
         subMon.worked(5);

         // (2) Validate metamodel (including SDMs)
         final ITask validator = methodBodyHandler.createValidator(ePackage);
         final IStatus validatorStatus = validator.run(subMon.split(10));
         if (subMon.isCanceled())
         {
            return Status.CANCEL_STATUS;
         }
         if (validatorStatus.matches(IStatus.ERROR))
         {
            return validatorStatus;
         }
         // (3) Build or load GenModel
         final MonitoredGenModelBuilder genModelBuilderJob = new MonitoredGenModelBuilder(getResourceSet(), getAllResources(), getEcoreFile(), true,
               getMoflonProperties());
         final IStatus genModelBuilderStatus = genModelBuilderJob.run(subMon.split(15));
         if (subMon.isCanceled())
         {
            return Status.CANCEL_STATUS;
         }
         if (genModelBuilderStatus.matches(IStatus.ERROR))
         {
            return genModelBuilderStatus;
         }
         this.genModel = genModelBuilderJob.getGenModel();

         final IProject project = getEcoreFile().getProject();
         // (4) Load injections SKIPPED
         //
         //         final IStatus injectionStatus = createInjections(project, genModel);
         //         if (subMon.isCanceled())
         //         {
         //            return Status.CANCEL_STATUS;
         //         }
         //         if (injectionStatus.matches(IStatus.ERROR))
         //         {
         //            return injectionStatus;
         //         }

         // (5) Process GenModel ORIGINAL IMPLEMENTATION DOES NOTHING
         // subMon.subTask("Processing SDMs for project " +
         // project.getName());
         // final IMonitoredJob genModelProcessor =
         // methodBodyHandler.createGenModelProcessor(this, resource);
         // final IStatus genModelProcessorStatus = genModelProcessor
         // .run(WorkspaceHelper.createSubMonitor(monitor, 35));
         // if (monitor.isCanceled()) {
         // return Status.CANCEL_STATUS;
         // }
         // if (genModelProcessorStatus.matches(IStatus.ERROR)) {
         // return genModelProcessorStatus;
         // }

         System.out.println("Entering Code Generation in CMoflonCodeGenerator");
         // (6) Generate code
         subMon.subTask("Generating code for project " + project.getName());
         Descriptor codeGenerationEngine = new DemoclesGeneratorAdapterFactory(defaultCodeGeneratorConfig.createTemplateConfiguration(genModel), null);
         final CMoflonCodeGenerator codeGenerator = new CMoflonCodeGenerator(resource, project, this.genModel,codeGenerationEngine);
         final IStatus codeGenerationStatus = codeGenerator.generateCode(subMon.split(30));
         if (subMon.isCanceled())
         {
            return Status.CANCEL_STATUS;
         }
         if (codeGenerationStatus.matches(IStatus.ERROR))
         {
            return codeGenerationStatus;
         }
         subMon.worked(5);

         // ORIGINAL eMoflon code
         // final TemplateConfigurationProvider templateConfig =
         // defaultCodeGeneratorConfig
         // .createTemplateConfiguration(this.genModel);
         // final DemoclesGeneratorAdapterFactory codeGenerationEngine = new
         // DemoclesGeneratorAdapterFactory(
         // templateConfig, this.injectionManager);
         // final CodeGenerator codeGenerator = new
         // CodeGenerator(codeGenerationEngine);
         // final IStatus codeGenerationStatus =
         // codeGenerator.generateCode(genModel,
         // new BasicMonitor.EclipseSubProgress(subMon, 30));
         // if (subMon.isCanceled()) {
         // return Status.CANCEL_STATUS;
         // }
         // if (codeGenerationStatus.matches(IStatus.ERROR)) {
         // return codeGenerationStatus;
         // }
         // subMon.worked(5);

         long tic = System.nanoTime();

         logger.info("Completed in " + (tic - toc) / 1e9 + "s");

         return validatorStatus.isOK() ? new Status(IStatus.OK, CodeGeneratorPlugin.getModuleID(), "Code generation succeeded")
               : new MultiStatus(CodeGeneratorPlugin.getModuleID(), validatorStatus.getCode(), new IStatus[] { validatorStatus },
                     "Code generation warnings/errors", null);
      } catch (final Exception e)
      {
         return new Status(IStatus.ERROR, CodeGeneratorPlugin.getModuleID(), IStatus.ERROR, e.getMessage(), e);
      } 
   }

   private List<Resource> getAllResources()
   {
      return this.resources;
   }

   private ResourceSet getResourceSet()
   {
      return this.resourceSet;
   }

   @Override
   public String getTaskName()
   {
      return "CMoflonCodeGeneration";
   }

   public final IFile getEcoreFile()
   {
      return ecoreFile;
   }

   public final MoflonPropertiesContainer getMoflonProperties()
   {
      return moflonProperties;
   }

   public final Resource getEcoreResource()
   {
      return resources.get(0);
   }

   /**
    * Loads the injections from the /injection folder
    */
   //   private IStatus createInjections(final IProject project, final GenModel genModel) throws CoreException
   //   {
   //      IFolder injectionFolder = WorkspaceHelper.addFolder(project, WorkspaceHelper.INJECTION_FOLDER, new NullProgressMonitor());
   //      CodeInjector injector = new CodeInjectorImpl(project.getLocation().toOSString());
   //
   //      UserInjectionExtractorImpl injectionExtractor = new UserInjectionExtractorImpl(injectionFolder.getLocation().toString(), genModel);
   //      CompilerInjectionExtractorImpl compilerInjectionExtractor = new CompilerInjectionExtractorImpl(project, genModel);
   //
   //      injectionManager = new InjectionManager(injectionExtractor, compilerInjectionExtractor, injector);
   //      return injectionManager.extractInjections();
   //   }
}

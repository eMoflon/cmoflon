package org.cmoflon.ide.core.runtime.builders;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.ResourceFillingMocaCMoflonTransformation;
import org.cmoflon.ide.core.runtime.natures.CMoflonMetamodelNature;
import org.cmoflon.ide.core.utilities.CMoflonWorkspaceHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.gervarro.eclipse.task.ITask;
import org.gervarro.eclipse.task.ProgressMonitoringJob;
import org.moflon.codegen.eclipse.CodeGeneratorPlugin;
import org.moflon.codegen.eclipse.ValidationStatus;
import org.moflon.core.utilities.ErrorReporter;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.eclipse.resource.SDMEnhancedEcoreResource;
import org.moflon.ide.core.CoreActivator;
import org.moflon.ide.core.properties.MetamodelProjectUtil;
import org.moflon.ide.core.properties.MocaTreeEAPropertiesReader;
import org.moflon.ide.core.runtime.ProjectDependencyAnalyzer;
import org.moflon.ide.core.runtime.builders.MetamodelBuilder;
import org.moflon.sdm.compiler.democles.validation.result.ErrorMessage;
import org.moflon.util.plugins.MetamodelProperties;

import MocaTree.Node;

/**
 * Builder for Projects with {@link CMoflonMetamodelNature}. Similar to {@link MetamodelBuilder}. Triggers the {@link ResourceFillingMocaCMoflonTransformation}.
 * @author David Giessing
 * @author Roland Kluge
 *
 */
public class CMoflonMetamodelBuilder extends MetamodelBuilder
{

   public static final Logger logger = Logger.getLogger(CMoflonMetamodelBuilder.class);

   public static final String BUILDER_ID = CMoflonMetamodelBuilder.class.getName();

   private MocaTreeEAPropertiesReader mocaTreeReader = new MocaTreeEAPropertiesReader();

   @Override
   public void clean(final IProgressMonitor monitor) throws CoreException
   {
      MetamodelProjectUtil.cleanTempFolder(getProject(), monitor);
   }

   @Override
   protected void processResource(IResource mocaFile, int kind, Map<String, String> args, IProgressMonitor monitor)
   {
      final MultiStatus mocaToMoflonStatus = new MultiStatus(WorkspaceHelper.getPluginId(getClass()), 0, getClass().getName() + " failed", null);

      final String mocaFilePath = WorkspaceHelper.TEMP_FOLDER + "/" + getProject().getName() + WorkspaceHelper.MOCA_XMI_FILE_EXTENSION;
      if (mocaFile instanceof IFile && mocaFilePath.equals(mocaFile.getProjectRelativePath().toString()) && mocaFile.isAccessible())
      {
         try
         {
            logger.debug("Start processing .temp folder");
            SubMonitor subMon = SubMonitor.convert(monitor, "Processing .temp", 140);

            if (CMoflonWorkspaceHelper.getExportedMocaTree(this.getProject()).exists())
            {
               deleteProblemMarkers();

               final URI workspaceURI = URI.createPlatformResourceURI("/", true);
               final URI projectURI = URI.createURI(getProject().getName() + "/", true).resolve(workspaceURI);

               // Create and initialize resource set
               final ResourceSet set = CodeGeneratorPlugin.createDefaultResourceSet();
               // eMoflonEMFUtil.installCrossReferencers(set);

               // Load Moca tree in read-only mode
               final URI mocaFileURI = URI.createURI(mocaFilePath, true).resolve(projectURI);
               final Resource mocaTreeResource = set.getResource(mocaFileURI, true);
               final CMoflonMocaTreeEAPropertiesReader mocaTreeReader = new CMoflonMocaTreeEAPropertiesReader();
               final Map<String, MetamodelProperties> properties = mocaTreeReader.getProperties((Node) mocaTreeResource.getContents().get(0));

               final IProgressMonitor exporterSubMonitor = subMon.split(100);
               exporterSubMonitor.beginTask("Running MOCA-to-cMoflon transformation", properties.keySet().size());

               ResourceFillingMocaCMoflonTransformation exporter = new ResourceFillingMocaCMoflonTransformation(set, properties, exporterSubMonitor,
                     this.getProject(), this);
               exporter.mocaToEcore(mocaTreeReader.getMocaTree());
               for (final ErrorMessage message : exporter.getMocaToMoflonReport().getErrorMessages())
               {
                  mocaToMoflonStatus.add(ValidationStatus.createValidationStatus(message));
               }
               if (exporter.getEpackages().isEmpty())
               {
                  final String errorMessage = "Unable to transform exported files to Ecore models";
                  CoreActivator.createProblemMarker(mocaFile, errorMessage, IMarker.SEVERITY_ERROR, mocaFile.getProjectRelativePath().toString());
                  logger.error(errorMessage);
                  return;
               }

               // Remove mocaTreeResource
               set.getResources().remove(mocaTreeResource);

               // Enforce resource change notifications to update workspace plugin information
               ResourcesPlugin.getWorkspace().checkpoint(false);

               // Load resources (metamodels and tgg files)
               triggerProjects.clear();
               ITask[] taskArray = new ITask[exporter.getMetamodelLoaderTasks().size()];
               taskArray = exporter.getMetamodelLoaderTasks().toArray(taskArray);
               final IStatus metamodelLoaderStatus = ProgressMonitoringJob.executeSyncSubTasks(taskArray,
                     new MultiStatus(WorkspaceHelper.getPluginId(getClass()), 0, "Resource loading failed", null), subMon.newChild(5));
               CoreActivator.checkCancellation(subMon);
               if (!metamodelLoaderStatus.isOK())
               {
                  if (kind == IncrementalProjectBuilder.FULL_BUILD && !triggerProjects.isEmpty())
                  {
                     needRebuild();
                  }
                  processProblemStatus(metamodelLoaderStatus, mocaFile);
                  return;
               }

               // Analyze project dependencies
               ProjectDependencyAnalyzer[] dependencyAnalyzers = new ProjectDependencyAnalyzer[exporter.getProjectDependencyAnalyzerTasks().size()];
               dependencyAnalyzers = exporter.getProjectDependencyAnalyzerTasks().toArray(dependencyAnalyzers);
               for (ProjectDependencyAnalyzer analyzer : dependencyAnalyzers)
               {
                  analyzer.setInterestingProjects(triggerProjects);
               }
               triggerProjects.clear();
               final IStatus projectDependencyAnalyzerStatus = ProgressMonitoringJob.executeSyncSubTasks(dependencyAnalyzers,
                     new MultiStatus(WorkspaceHelper.getPluginId(getClass()), 0, "Dependency analysis failed", null), subMon.newChild(5));
               CoreActivator.checkCancellation(subMon);
               if (!projectDependencyAnalyzerStatus.isOK())
               {
                  processProblemStatus(projectDependencyAnalyzerStatus, mocaFile);
                  return;
               }

               // Prepare save options
               Map<Object, Object> saveOnlyIfChangedOption = new HashMap<Object, Object>();
               saveOnlyIfChangedOption.put(Resource.OPTION_SAVE_ONLY_IF_CHANGED, Resource.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);
               saveOnlyIfChangedOption.put(SDMEnhancedEcoreResource.SAVE_GENERATED_PACKAGE_CROSSREF_URIS, true);

               // Persist resources (metamodels, tgg files and moflon.properties files)
               for (Resource resource : set.getResources())
               {
                  try
                  {
                     resource.save(saveOnlyIfChangedOption);
                  } catch (IOException e)
                  {
                     logger.debug(e.getMessage(), e);
                  }
               }
            }
         } catch (Exception e)
         {
            logger.warn("Unable to update created projects: " + e.getMessage() == null ? e.toString() : e.getMessage());
            e.printStackTrace();
         } finally
         {
            monitor.done();
         }

         handleErrorsInEclipse(mocaToMoflonStatus);
      }
   }

   private void handleErrorsInEclipse(final IStatus validationStatus)
   {
      IProject metamodelProject = getProject();
      IFile eapFile = metamodelProject.getFile(metamodelProject.getName() + ".eap");

      if (eapFile.exists())
      {
         ErrorReporter eclipseErrorReporter = (ErrorReporter) Platform.getAdapterManager().loadAdapter(eapFile,
               "org.moflon.compiler.sdm.democles.eclipse.EclipseErrorReporter");
         if (eclipseErrorReporter != null)
         {
            if (!validationStatus.isOK())
            {

               eclipseErrorReporter.report(validationStatus);
            }
         }
      }
   }
}

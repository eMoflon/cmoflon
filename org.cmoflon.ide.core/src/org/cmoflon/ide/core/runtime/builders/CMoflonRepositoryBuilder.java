package org.cmoflon.ide.core.runtime.builders;

import java.util.Map;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.CMoflonRepositoryCodeGenerator;
import org.cmoflon.ide.core.utilities.CMoflonWorkspaceHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.gervarro.eclipse.workspace.util.AntPatternCondition;
import org.moflon.codegen.eclipse.CodeGeneratorPlugin;
import org.moflon.core.utilities.ErrorReporter;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.ide.core.runtime.builders.AbstractVisitorBuilder;
import org.moflon.ide.core.runtime.builders.RepositoryBuilder;

/**
 * Builder for projects with CMoflonRepositoryNature. Similar to {@link RepositoryBuilder}. Triggers {@link CMoflonRepositoryCodeGenerator}.
 * @author David Giessing
 * @author Roland Kluge
 */
public class CMoflonRepositoryBuilder extends AbstractVisitorBuilder
{
   public static final Logger logger = Logger.getLogger(CMoflonRepositoryBuilder.class);

   public CMoflonRepositoryBuilder()
   {
      super(new AntPatternCondition(new String[] { "model/*.ecore" }));
   }

   protected boolean generateSDMs = true;

   public static final String BUILDER_ID = CMoflonRepositoryBuilder.class.getName();

   @Override
   protected final AntPatternCondition getTriggerCondition(final IProject project)
   {
      return new AntPatternCondition(new String[0]);
   }

   @Override
   protected void clean(final IProgressMonitor monitor) throws CoreException
   {
      SubMonitor subMon = SubMonitor.convert(monitor, "Cleaning " + getProject(), 4);

      final IProject project = getProject();

      // Remove all problem markers
      project.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
      project.deleteMarkers(WorkspaceHelper.MOFLON_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
      project.deleteMarkers(WorkspaceHelper.INJECTION_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
      subMon.worked(1);

      // Remove generated code
      cleanFolderButKeepHiddenFiles(project.getFolder(WorkspaceHelper.GEN_FOLDER), subMon.split(1));
   }

   @Override
   protected void processResource(IResource ecoreResource, int kind, Map<String, String> args, IProgressMonitor monitor)
   {
      SubMonitor subMon = SubMonitor.convert(monitor, "Processing Resource", 1);
      logger.info("Processing Resource CMoflonRepositoryBuilder");
      CMoflonRepositoryCodeGenerator generator = new CMoflonRepositoryCodeGenerator(getProject());
      final IStatus status = generator.generateCode(subMon.split(1), CMoflonWorkspaceHelper.getConstantsPropertiesFile(getProject()));
      final IFile ecoreFile = Platform.getAdapterManager().getAdapter(ecoreResource, IFile.class);
      if (status.matches(IStatus.ERROR))
      {
         try
         {
            handleErrorsAndWarnings(status, ecoreFile);
         } catch (CoreException e)
         {
            logger.error("Problems while reporting errors and warnings: " + e);
         }
      }
   }

   /**
    * Handles errors and warning produced by the code generation task
    * 
    * @param status
    */
   private void handleErrorsAndWarnings(final IStatus status, final IFile ecoreFile) throws CoreException
   {
      if (indicatesThatValidationCrashed(status))
      {
         throw new CoreException(new Status(IStatus.ERROR, CodeGeneratorPlugin.getModuleID(), status.getMessage(), status.getException().getCause()));
      }
      if (status.matches(IStatus.ERROR))
      {
         handleErrorsInEA(status);
         handleErrorsInEclipse(status, ecoreFile);
      }
      if (status.matches(IStatus.WARNING))
      {
         handleInjectionWarningsAndErrors(status);
      }
   }

   private boolean indicatesThatValidationCrashed(IStatus status)
   {
      return status.getException() != null;
   }

   private void handleInjectionWarningsAndErrors(final IStatus status)
   {
      final String reporterClass = "org.moflon.moca.inject.validation.InjectionErrorReporter";
      final ErrorReporter errorReporter = (ErrorReporter) Platform.getAdapterManager().loadAdapter(getProject(), reporterClass);
      if (errorReporter != null)
      {
         errorReporter.report(status);
      } else
      {
         logger.debug("Could not load error reporter '" + reporterClass + "'");
      }
   }

   public void handleErrorsInEclipse(final IStatus status, final IFile ecoreFile)
   {
      final String reporterClass = "org.moflon.compiler.sdm.democles.eclipse.EclipseErrorReporter";
      final ErrorReporter eclipseErrorReporter = (ErrorReporter) Platform.getAdapterManager().loadAdapter(ecoreFile, reporterClass);
      if (eclipseErrorReporter != null)
      {
         eclipseErrorReporter.report(status);
      } else
      {
         logger.debug("Could not load error reporter '" + reporterClass + "'");
      }
   }

   public void handleErrorsInEA(final IStatus status)
   {
      final String reporterClass = "org.moflon.validation.EnterpriseArchitectValidationHelper";
      final ErrorReporter errorReporter = (ErrorReporter) Platform.getAdapterManager().loadAdapter(getProject(), reporterClass);
      if (errorReporter != null)
      {
         errorReporter.report(status);
      } else
      {
         logger.debug("Could not load error reporter '" + reporterClass + "'");
      }
   }

   private void cleanFolderButKeepHiddenFiles(final IFolder folder, final IProgressMonitor monitor) throws CoreException
   {
      if (!folder.exists())
         return;

      SubMonitor subMon = SubMonitor.convert(monitor, "Inspecting " + folder.getName(), 2 * folder.members().length);

      for (final IResource resource : folder.members())
      {
         // keep SVN data
         if (!resource.getName().startsWith("."))
         {
            if (WorkspaceHelper.isFolder(resource))
               cleanFolderButKeepHiddenFiles((IFolder) resource, subMon.split(1));
            else
               subMon.worked(1);

            resource.delete(true, subMon.split(1));
         } else
         {
            subMon.worked(2);
         }
      }
   }
}

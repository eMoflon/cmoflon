package org.cmoflon.ide.core.runtime;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.codegeneration.CMoflonCodeGeneratorTask;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.moflon.codegen.eclipse.CodeGeneratorPlugin;
import org.moflon.core.utilities.MoflonUtil;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.core.utilities.eMoflonEMFUtil;
import org.moflon.ide.core.preferences.EMoflonPreferencesStorage;
import org.osgi.framework.FrameworkUtil;

/**
 * Mimics {@link RepositoryCodeGenerator}. Needed to invoke {@link CMoflonCodeGenerator}
 * @author David Giessing
 *
 */
public class CMoflonRepositoryCodeGenerator
{

   private static final Logger logger = Logger.getLogger(CMoflonRepositoryCodeGenerator.class);

   protected IProject project;

   public CMoflonRepositoryCodeGenerator(final IProject project)
   {
      this.project = project;
   }

   public IStatus generateCode(final IProgressMonitor monitor, Properties cMoflonProperties)
   {
      final SubMonitor subMon = SubMonitor.convert(monitor);
      IFile ecoreFile;
      try
      {  
         project.deleteMarkers(WorkspaceHelper.MOFLON_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
         project.deleteMarkers(WorkspaceHelper.INJECTION_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
         
         ecoreFile = getEcoreFileAndHandleMissingFile();
         if (!ecoreFile.exists())
         {
            return new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(), "Unable to generate code for " + project.getName()
                  + ",  as no Ecore file according to naming convention (capitalizeFirstLetter.lastSegmentOf.projectName) was found!");
         }

         final ResourceSet resourceSet = CodeGeneratorPlugin.createDefaultResourceSet();
         eMoflonEMFUtil.installCrossReferencers(resourceSet);
         subMon.worked(1);
         final CMoflonCodeGeneratorTask gen = new CMoflonCodeGeneratorTask(ecoreFile, resourceSet);
         gen.setValidationTimeout(EMoflonPreferencesStorage.getInstance().getValidationTimeout());
         final IStatus status = gen.run(subMon.split(1));
         if (status.matches(IStatus.ERROR))
         {
            return status;
         }
      } catch (final CoreException e)
      {
         return new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(), "Error", e);
      }
      return Status.OK_STATUS;
   }

   protected IFile getEcoreFileAndHandleMissingFile() throws CoreException
   {
      if (!doesEcoreFileExist())
         createMarkersForMissingEcoreFile();

      return getEcoreFile();
   }

   protected IFile getEcoreFile()
   {
      return getEcoreFile(project);
   }

   public static IFile getEcoreFile(final IProject p)
   {
      String ecoreFileName = MoflonUtil.getDefaultNameOfFileInProjectWithoutExtension(p.getName());
      return p.getFolder(WorkspaceHelper.MODEL_FOLDER).getFile(ecoreFileName + WorkspaceHelper.ECORE_FILE_EXTENSION);
   }

   private boolean doesEcoreFileExist()
   {
      return getEcoreFile().exists();
   }

   private void createMarkersForMissingEcoreFile() throws CoreException
   {
      IFile ecoreFile = getEcoreFile();
      logger.error("Unable to generate code: " + ecoreFile + " does not exist in project!");

      // Create marker
      final IMarker marker = project.createMarker(IMarker.PROBLEM);
      marker.setAttribute(IMarker.MESSAGE, "Cannot find: " + ecoreFile.getProjectRelativePath().toString());
      marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
      marker.setAttribute(IMarker.LOCATION, ecoreFile.getProjectRelativePath().toString());
   }

   public static boolean isEcoreFileOfProject(final IResource resource, final IProject p)
   {
      return resource.exists() && resource.getProjectRelativePath().equals(getEcoreFile(p).getProjectRelativePath());
   }
}

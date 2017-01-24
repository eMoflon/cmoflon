package org.cmoflon.ide.ui.admin.wizards.metamodel;

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.natures.CMoflonMetamodelNature;
import org.cmoflon.ide.ui.CMoflonUIActivator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.moflon.core.utilities.LogUtils;
import org.moflon.core.utilities.MoflonUtilitiesActivator;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.ide.ui.admin.wizards.metamodel.NewMetamodelProjectInfoPage;

/**
 * The new metamodel wizard creates a new CMoflon metamodel project with default directory structure and default files.
 * @author David Giessing
 */
public class NewCMoflonMetamodelWizard extends Wizard implements IWorkbenchWizard
{
   // Page containing controls for taking user input
   private NewMetamodelProjectInfoPage projectInfo;

   private static Logger logger = Logger.getLogger(CMoflonUIActivator.class);

   private static final String SPECIFICATION_WORKINGSET_NAME = "Specifications";

   public NewCMoflonMetamodelWizard()
   {
      setNeedsProgressMonitor(true);
   }

   @Override
   public void addPages()
   {
      projectInfo = new NewMetamodelProjectInfoPage();
      addPage(projectInfo);
   }

   @Override
   public boolean performFinish()
   {
      IRunnableWithProgress op = new IRunnableWithProgress() {
         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException
         {
            try
            {
               doFinish(monitor);
            } catch (CoreException e)
            {
               throw new InvocationTargetException(e);
            } finally
            {
               monitor.done();
            }
         }
      };

      try
      {
         getContainer().run(true, false, op);
      } catch (final InterruptedException e)
      {
         MessageDialog.openError(getShell(), "Error while finishing wizard", e.getMessage());
         LogUtils.error(Logger.getLogger(getClass()), WorkspaceHelper.printStacktraceToString(e));
         return false;
      } catch (final InvocationTargetException e)
      {
         final Throwable targetException = e.getTargetException();
         MessageDialog.openError(getShell(), "Error while finishing wizard", targetException.getMessage());
         LogUtils.error(Logger.getLogger(getClass()), WorkspaceHelper.printStacktraceToString(targetException));
         return false;
      }

      return true;
   }

   private void doFinish(final IProgressMonitor monitor) throws CoreException
   {
      try
      {
         final SubMonitor subMon = SubMonitor.convert(monitor, "Creating metamodel project", 8);

         String projectName = projectInfo.getProjectName();
         IPath location = projectInfo.getProjectLocation();

         // Create project
         IProject newProjectHandle = createProject(projectName, CMoflonUIActivator.getModuleID(), location, subMon.split(1));

         // generate default files
         WorkspaceHelper.addFile(newProjectHandle, projectName + ".eap",
               MoflonUtilitiesActivator.getPathRelToPlugIn("resources/kTC.eap", CMoflonUIActivator.getModuleID()), CMoflonUIActivator.getModuleID(),
               subMon.split(1));

         WorkspaceHelper.addFile(newProjectHandle, ".gitignore", ".temp", subMon.split(1));

         // Add Nature and Builders
         WorkspaceHelper.addNature(newProjectHandle, CMoflonMetamodelNature.NATURE_ID, subMon.split(1));
         WorkspaceHelper.addNature(newProjectHandle, WorkspaceHelper.METAMODEL_NATURE_ID, subMon.split(1));

         newProjectHandle.refreshLocal(IResource.DEPTH_INFINITE, subMon.split(1));

      } catch (Exception e)
      {
         logger.error("Unable to add default EA project file: " + e);
         e.printStackTrace();
      } finally
      {
         monitor.done();
      }
   }
   
   /**
    * Creates a new project in current workspace
    * 
    * @param projectName
    *           name of the new project
    * @param monitor
    *           a progress monitor, or null if progress reporting is not desired
    * @param location
    *           the file system location where the project should be placed
    * @return handle to newly created project
    * @throws CoreException
    */
   private static IProject createProject(final String projectName, final String pluginId, final IPath location, final IProgressMonitor monitor)
         throws CoreException
   {
      SubMonitor subMon = SubMonitor.convert(monitor, "", 2);

      // Get project handle
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IProject newProject = root.getProject(projectName);

      // Use default location (in workspace)
      final IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(newProject.getName());
      description.setLocation(location);

      // Complain if project already exists
      if (newProject.exists())
      {
         throw new CoreException(new Status(IStatus.ERROR, pluginId, projectName + " exists already!"));
      }

      // Create project
      newProject.create(description, subMon.split(1));
      newProject.open(subMon.split(1));

      return newProject;
   }

   @Override
   public void init(IWorkbench workbench, IStructuredSelection selection)
   {

   }
}

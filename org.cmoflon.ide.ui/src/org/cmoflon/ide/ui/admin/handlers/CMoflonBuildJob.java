package org.cmoflon.ide.ui.admin.handlers;

import java.util.List;

import org.cmoflon.ide.ui.CMoflonUIActivator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

public class CMoflonBuildJob extends Job
{

   private final List<IProject> projects;

   public CMoflonBuildJob(final String name, final List<IProject> projects)
   {
      super(name);
      this.projects = projects;
   }

   @Override
   protected IStatus run(IProgressMonitor monitor)
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, projects.size());
      IStatus status = Status.OK_STATUS;
      for (IProject p : projects)
      {
         try
         {
            p.build(IncrementalProjectBuilder.CLEAN_BUILD, subMon.split(1));
            p.build(IncrementalProjectBuilder.FULL_BUILD, subMon.split(1));
         } catch (CoreException e)
         {
            status = new Status(IStatus.ERROR, CMoflonUIActivator.getModuleID(), IStatus.OK, "", e);
         }
      }
      return status;
   }

}

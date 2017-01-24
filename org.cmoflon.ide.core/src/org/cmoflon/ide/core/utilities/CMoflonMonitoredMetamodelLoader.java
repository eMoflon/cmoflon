package org.cmoflon.ide.core.utilities;

import org.cmoflon.ide.core.runtime.natures.CMoflonRepositoryNature;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.moflon.codegen.eclipse.CodeGeneratorPlugin;
import org.moflon.codegen.eclipse.GenericMonitoredResourceLoader;
import org.moflon.codegen.eclipse.MonitoredMetamodelLoader;
import org.moflon.core.propertycontainer.MoflonPropertiesContainer;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.dependency.PackageRemappingDependency;

/**
 * Mimics {@link MonitoredMetamodelLoader}, it is needed to recreate this class because of the changed project natures.
 * In the {@link MonitoredMetamodelLoader} the isAccessible method is false for the new Natures.
 */
public class CMoflonMonitoredMetamodelLoader extends GenericMonitoredResourceLoader
{

   public CMoflonMonitoredMetamodelLoader(ResourceSet resourceSet, IFile ecoreFile, MoflonPropertiesContainer moflonProperties)
   {
      super(resourceSet, ecoreFile);
   }

   @Override
   protected void createResourcesForWorkspaceProjects(IProgressMonitor monitor)
   {
      final IProject[] workspaceProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
      final SubMonitor subMon = SubMonitor.convert(monitor, "Loading workspace projects", workspaceProjects.length);
      for (IProject workspaceProject : workspaceProjects)
      {
         if (isAccessible(workspaceProject))
         {
            final URI projectURI = CodeGeneratorPlugin.lookupProjectURI(workspaceProject);
            final URI metamodelURI = CodeGeneratorPlugin.getDefaultProjectRelativeEcoreFileURI(workspaceProject).resolve(projectURI);
            new PackageRemappingDependency(metamodelURI, false, false).getResource(resourceSet, false, true);
         }
         subMon.worked(1);
      }
   }
   
   protected boolean isAccessible(IProject project)
   {
      try
      {
         return project.isAccessible() && (project.hasNature(CMoflonRepositoryNature.NATURE_ID) || project.hasNature(WorkspaceHelper.INTEGRATION_NATURE_ID));
      } catch (final CoreException e)
      {
         return false;
      }
   }
}

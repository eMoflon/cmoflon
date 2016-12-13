package org.cmoflon.ide.core.runtime;

import java.util.Map;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.builders.CMoflonMetamodelBuilder;
import org.cmoflon.ide.core.utilities.CMoflonProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.moflon.core.utilities.UncheckedCoreException;
import org.moflon.ide.core.CoreActivator;
import org.moflon.ide.core.runtime.BasicResourceFillingMocaToMoflonTransformation;
import org.moflon.ide.core.runtime.MetamodelLoader;
import org.moflon.ide.core.runtime.ProjectDependencyAnalyzer;
import org.moflon.ide.core.runtime.ResourceFillingMocaToMoflonTransformation;
import org.moflon.util.plugins.MetamodelProperties;

import MocaTree.Node;

/**
 * Replacement for {@link ResourceFillingMocaToMoflonTransformation}. Most Part copied from there.<br>
 * Invokes the {@link CMoflonProjectCreator} to create the Repository Project. 
 * @author David Giessing
 * @author Roland Kluge
 */
public class ResourceFillingMocaCMoflonTransformation extends BasicResourceFillingMocaToMoflonTransformation
{

   private Map<String, MetamodelProperties> propertiesMap;

   private static final Logger logger = Logger.getLogger(ResourceFillingMocaCMoflonTransformation.class);

   /**
    * 
    * @param monitor the monitor to be used during project construction. Every project to be generated should advance the monitor 1 tick
    * @param builder 
    * @param project 
    */
   public ResourceFillingMocaCMoflonTransformation(final ResourceSet resourceSet, final Map<String, MetamodelProperties> propertiesMap,
         final IProgressMonitor monitor, IProject project, CMoflonMetamodelBuilder builder)
   {
      super(resourceSet, builder, project);
      this.propertiesMap = propertiesMap;
   }

   protected final void reportError(final String errorMessage)
   {
      throw new UncheckedCoreException(errorMessage, CoreActivator.getModuleID());
   }

   protected void handleOpenProject(final Node node, final IProject project, final Resource resource)
   {
      try
      {
         CMoflonProjectCreator.createFoldersIfNecessary(project, new NullProgressMonitor());
      } catch (final CoreException e)
      {
         logger.warn("Failed to create folders: " + e.getMessage());
      }
   }

   protected void handleMissingProject(final Node node, final IProject project)
   {
      MetamodelProperties properties = propertiesMap.get(project.getName().split("_C")[0]);

      try
      {
         CMoflonProjectCreator createMoflonProject = new CMoflonProjectCreator(properties);

         ResourcesPlugin.getWorkspace().run(createMoflonProject, new NullProgressMonitor());
      } catch (final CoreException e)
      {
         this.reportError(e);
      }
   }
   
   @Override
	public void handleOutermostPackage(Node node, EPackage outermostPackage) {
	   final String projectName = getProjectName(node)+"_C";
		final String exportAttribute = lookupAttribute(node, MOCA_TREE_ATTRIBUTE_EXPORT);
		if (isExported(exportAttribute)) {
			final String nodeName = node.getName();
			if (MOCA_TREE_ATTRIBUTE_REPOSITORY_PROJECT.equals(nodeName) ||
					MOCA_TREE_ATTRIBUTE_INTEGRATION_PROJECT.equals(nodeName)) {
				// Handling (creating/opening) projects in Eclipse workspace
				IProject workspaceProject = workspace.getRoot().getProject(projectName);
				if (!workspaceProject.exists()) {
					handleOrReportMissingProject(node, workspaceProject);
				}
				assert workspaceProject != null && workspaceProject.exists();
				if (!workspaceProject.isAccessible()) {
					handleOrReportClosedProject(node, workspaceProject);
				}
				assert workspaceProject.isAccessible();
				handleOpenProject(node, workspaceProject);
				metamodelLoaderTasks.add(new MetamodelLoader(metamodelBuilder, set, node, outermostPackage));
				projectDependencyAnalyzerTasks.add(
						new ProjectDependencyAnalyzer(metamodelBuilder, metamodelProject, workspaceProject, outermostPackage));
			} else {
				reportError("Project " + getProjectName(node)
						+ " has unknown type " + node.getName());
			}
		} else {
			if (!MOCA_TREE_ATTRIBUTE_REPOSITORY_PROJECT.equals(node.getName())) {
				reportError("Project " + getProjectName(node)
						+ " must always be exported");
			}
			metamodelLoaderTasks.add(new MetamodelLoader(metamodelBuilder, set, node, outermostPackage));
		}
	}
}

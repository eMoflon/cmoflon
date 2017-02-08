package org.cmoflon.ide.core.utilities;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.cmoflon.ide.core.CMoflonCoreActivator;
import org.cmoflon.ide.core.runtime.natures.CMoflonRepositoryNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.moflon.core.propertycontainer.MoflonPropertiesContainer;
import org.moflon.core.propertycontainer.MoflonPropertiesContainerHelper;
import org.moflon.core.propertycontainer.SDMCodeGeneratorIds;
import org.moflon.core.utilities.MoflonUtilitiesActivator;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.ide.core.runtime.MoflonProjectCreator;
import org.moflon.util.plugins.BuildPropertiesFileBuilder;
import org.moflon.util.plugins.MetamodelProperties;
import org.moflon.util.plugins.PluginProducerWorkspaceRunnable;

/**
 * Replaces {@link MoflonProjectCreator}. Replacement was necessary as addNatureAndBuilders is declared private and can therefore not be overriden.
 * Also adds the default property files needed for code generation.
 * @author David Giessing
 *
 */
public class CMoflonProjectCreator implements IWorkspaceRunnable
{

   private static final String DEFAULT_GITIGNORE_CONTENT = StringUtils.join(Arrays.asList("/gen", "/model/*.ecore", "/model/*.genmodel"), "\n");

   private static final int DEFAULT_MAX_MATCH_COUNT = 20;

   private static String constantPropertiesContent = //
         "#Set to 'true' if dropping unidirectional edges is desired \n" //
               + "dropUnidirectionalEdges = true\n" //
               + "#Set to True if you want to use hopcounts.\n" //
               + "useHopcount = false\n" //
               + "#set number of matches allowed per PM method\n" //
               + "MAX_MATCH_COUNT = " + DEFAULT_MAX_MATCH_COUNT + "\n" //
               + "#Place the names of the topology control algorithms to use as CSV (e.g., tcMethods=KtcAlgorithm,LStarKtcAlgorithm)\n" //
               + "tcMethods = \n" //
               + "#Place the parameters for the topology control algorithm call here as CSV (e.g., LStarKtcAlgorithm = k,a)\n" //
               + "#It is also possible to use the constants from down here. For this, the value should be const-<constname>\n" //
               + "#(e.g., LStarKtcAlgorithm.const-k=3.0\n";

   private static String mapPropertiesContent = //
         "#Define your Mapping here: \n" + "# The Key is the EClass, and the value is the C Struct you want it to be mapped to.\n"
               + "# Default: Node = networkaddr_t and Link = neighbor_t\n" // 
               + "Node = networkaddr_t\n" + "Link = neighbor_t\n";

   private final MetamodelProperties metamodelProperties;

   public CMoflonProjectCreator(MetamodelProperties metamodelProperties)
   {
      this.metamodelProperties = metamodelProperties;
   }

   private String getMetaModelProjectName()
   {
      return metamodelProperties.getMetamodelProjectName();
   }

   private String getProjectName()
   {
      return metamodelProperties.getProjectName();
   }

   @Override
   public void run(final IProgressMonitor monitor) throws CoreException
   {
      final IWorkspace workspace = ResourcesPlugin.getWorkspace();
      final IWorkspaceRoot workspaceRoot = workspace.getRoot();
      final IProject workspaceProject = workspaceRoot.getProject(getProjectName());
      if (workspaceProject.exists())
      {
         return;
      }

      final SubMonitor subMon = SubMonitor.convert(monitor, "Creating project " + getProjectName(), 7);
      final IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(getProjectName());

      workspaceProject.create(description, IWorkspace.AVOID_UPDATE, subMon.split(1));
      workspaceProject.open(IWorkspace.AVOID_UPDATE, subMon.split(1));

      createFoldersIfNecessary(workspaceProject, subMon.split(4));
      createFilesIfNecessary(workspaceProject, subMon);

      WorkspaceHelper.addNature(workspaceProject, CMoflonRepositoryNature.NATURE_ID, subMon);
      final PluginProducerWorkspaceRunnable pluginProducer = new PluginProducerWorkspaceRunnable(workspaceProject, metamodelProperties);
      pluginProducer.run(subMon);
      final MoflonPropertiesContainer moflonProperties = MoflonPropertiesContainerHelper.createDefaultPropertiesContainer(workspaceProject.getName(),
            getMetaModelProjectName());
      moflonProperties.getSdmCodegeneratorHandlerId().setValue(SDMCodeGeneratorIds.DEMOCLES_ATTRIBUTES);
      subMon.worked(1);

      MoflonPropertiesContainerHelper.save(moflonProperties, subMon.split(1));

   }

   public static void createFilesIfNecessary(final IProject workspaceProject, final IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, 5);
      addFileIfNotExists(workspaceProject, ".gitignore", DEFAULT_GITIGNORE_CONTENT, subMon.split(1));
      clearBuildProperties(workspaceProject);
      new BuildPropertiesFileBuilder().createBuildProperties(workspaceProject, subMon.split(1));
      addFileIfNotExists(workspaceProject, workspaceProject.getName() + "Constants.properties", constantPropertiesContent, subMon.split(1));
      addFileIfNotExists(workspaceProject, workspaceProject.getName() + "EClassToStructs.properties", mapPropertiesContent, subMon.split(1));
      try
      {
         final String constraintsLibraryFileName = "/lib/" + workspaceProject.getName() + "AttributeConstraintsLib.xmi";
         if (!workspaceProject.getFile(constraintsLibraryFileName).exists())
         {
            WorkspaceHelper.addFile(workspaceProject, constraintsLibraryFileName,
                  MoflonUtilitiesActivator.getPathRelToPlugIn("resources/AttributeConstraintsLib.xmi", CMoflonCoreActivator.getModuleID()),
                  CMoflonCoreActivator.getModuleID(), subMon.split(1));
         }
      } catch (final Exception e)
      {
         throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(CMoflonProjectCreator.class),
               "Failed to create constraints library: " + e.getMessage(), e));
      }
   }

   public static void createFoldersIfNecessary(final IProject project, final IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, "Creating folders within project", 3);
      WorkspaceHelper.createFolderIfNotExists(WorkspaceHelper.getGenFolder(project), subMon.newChild(1));
      WorkspaceHelper.createFolderIfNotExists(WorkspaceHelper.getModelFolder(project), subMon.newChild(1));
      WorkspaceHelper.createFolderIfNotExists(WorkspaceHelper.getLibFolder(project), subMon.newChild(1));
   }

   private static void clearBuildProperties(final IProject workspaceProject) throws CoreException
   {
      WorkspaceHelper.addFile(workspaceProject, "build.properties", "# Intentionally empty\n", new NullProgressMonitor());
   }

   private static void addFileIfNotExists(IProject workspaceProject, String fileName, String content, SubMonitor subMon) throws CoreException
   {
      if (!workspaceProject.getFile(fileName).exists())
         WorkspaceHelper.addFile(workspaceProject, fileName, content, subMon);
   }

}

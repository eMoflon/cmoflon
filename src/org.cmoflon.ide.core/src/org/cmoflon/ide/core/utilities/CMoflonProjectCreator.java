package org.cmoflon.ide.core.utilities;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cmoflon.ide.core.build.CMoflonPluginProducerWorkspaceRunnable;
import org.cmoflon.ide.core.runtime.natures.CMoflonRepositoryNature;
import org.eclipse.core.resources.IFile;
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
import org.moflon.core.build.MoflonProjectCreator;
import org.moflon.core.plugins.PluginProducerWorkspaceRunnable;
import org.moflon.core.plugins.PluginProperties;
import org.moflon.core.propertycontainer.MoflonPropertiesContainer;
import org.moflon.core.propertycontainer.MoflonPropertiesContainerHelper;
import org.moflon.core.propertycontainer.SDMCodeGeneratorIds;
import org.moflon.core.utilities.MoflonConventions;
import org.moflon.core.utilities.WorkspaceHelper;

/**
 * Replaces {@link MoflonProjectCreator}. Replacement was necessary as addNatureAndBuilders is declared private and can therefore not be overriden.
 * Also adds the default property files needed for code generation.
 *
 * @author David Giessing
 * @author Roland Kluge
 */
public class CMoflonProjectCreator implements IWorkspaceRunnable
{

   private static final String PATH_TO_DEFAULT_CONSTRAINTS_LIBRARY = "resources/DefaultCMoflonAttributeConstraintsLib.xmi";

   private static final String DEFAULT_GITIGNORE_CONTENT = StringUtils.join(Arrays.asList("/gen", "/model/*.ecore", "/model/*.genmodel"), "\n");

   private final PluginProperties pluginProperties;

   public CMoflonProjectCreator(PluginProperties PluginProperties)
   {
      this.pluginProperties = PluginProperties;
   }

   private String getProjectName()
   {
      return this.pluginProperties.getProjectName();
   }

   @Override
   public void run(final IProgressMonitor monitor) throws CoreException
   {
      final IProject project = getProject();
      if (project.exists())
         return;

      final SubMonitor subMon = SubMonitor.convert(monitor, "Creating project " + getProjectName(), 7);
      final IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(getProjectName());

      project.create(description, IWorkspace.AVOID_UPDATE, subMon.split(1));
      project.open(IWorkspace.AVOID_UPDATE, subMon.split(1));
      WorkspaceHelper.addNature(project, CMoflonRepositoryNature.NATURE_ID, subMon.split(1));

      createFoldersIfNecessary(project, subMon.split(1));
      createFilesIfNecessary(project, subMon.split(1));

      createPluginSpecificFiles(project, this.pluginProperties, subMon.split(1));
      createMoflonProperties(project, this.pluginProperties, subMon.split(1));

   }

   private static void createMoflonProperties(final IProject project, final PluginProperties PluginProperties, final IProgressMonitor monitor)
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, "Creating moflon.properties.xmi", 2);
      if (!project.getFile(MoflonConventions.MOFLON_CONFIG_FILE).exists())
      {
         final MoflonPropertiesContainer moflonProperties = MoflonPropertiesContainerHelper.createDefaultPropertiesContainer(project);
         moflonProperties.getSdmCodegeneratorHandlerId().setValue(SDMCodeGeneratorIds.DEMOCLES_ATTRIBUTES);
         subMon.worked(1);

         MoflonPropertiesContainerHelper.save(moflonProperties, subMon.split(1));
      }
   }

   public static void createFilesIfNecessary(final IProject project, final IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, 4);

      if (!project.getFile(".gitignore").exists())
         WorkspaceHelper.addFile(project, ".gitignore", DEFAULT_GITIGNORE_CONTENT, subMon.split(1));

      if (!project.getFile("model/.keepmodel").exists())
         WorkspaceHelper.addFile(project, "model/.keepmodel", "# Dummy file versioning /model", subMon.split(1));

      if (!project.getFile(CMoflonProperties.CMOFLON_PROPERTIES_FILENAME).exists())
         WorkspaceHelper.addFile(project, CMoflonProperties.CMOFLON_PROPERTIES_FILENAME, CMoflonProperties.getDefaultCMoflonPropertiesContent(),
               subMon.split(1));

      initializeConstraintsLibrary(project, subMon.split(1));
   }

   private static void initializeConstraintsLibrary(final IProject project, final SubMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, 1);
      final String constraintsLibraryFileName = "/lib/" + project.getName() + "AttributeConstraintsLib.xmi";
      final IFile file = project.getFile(constraintsLibraryFileName);
      if (!file.exists())
      {
         try
         {
            final String pluginId = WorkspaceHelper.getPluginId(CMoflonProjectCreator.class);
            final InputStream stream = WorkspaceHelper.getPathRelToPlugIn(PATH_TO_DEFAULT_CONSTRAINTS_LIBRARY, pluginId).openStream();
            try
            {
               file.create(stream, true, subMon.split(1));
            } finally
            {
               IOUtils.closeQuietly(stream);
            }
         } catch (final Exception e)
         {
            throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(CMoflonProjectCreator.class),
                  "Failed to create constraints library: " + e.getMessage(), e));
         }
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
      final IFile file = workspaceProject.getFile("build.properties");
      if (!file.exists())
      {
         WorkspaceHelper.addFile(workspaceProject, "build.properties", "# Intentionally empty\n", new NullProgressMonitor());
      }
   }

   /**
    * Returns a handle to the project to create
    * @return
    */
   private IProject getProject()
   {
      final IWorkspace workspace = ResourcesPlugin.getWorkspace();
      final IWorkspaceRoot workspaceRoot = workspace.getRoot();
      final IProject workspaceProject = workspaceRoot.getProject(getProjectName());
      return workspaceProject;
   }

   public static void createPluginSpecificFiles(final IProject project, final PluginProperties PluginProperties, final IProgressMonitor monitor)
         throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, "Creating folders within project", 3);
      final PluginProducerWorkspaceRunnable pluginProducer = new CMoflonPluginProducerWorkspaceRunnable(project, PluginProperties);
      try
      {
         pluginProducer.run(subMon.split(2));
      } catch (final Exception e)
      {
         throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(CMoflonProjectCreator.class),
               "Failed to create plugin-specific features: " + e.getMessage(), e));
      }
      clearBuildProperties(project);
      subMon.worked(1);
   }
}

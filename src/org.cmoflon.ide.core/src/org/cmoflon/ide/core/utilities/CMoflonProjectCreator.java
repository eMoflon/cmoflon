package org.cmoflon.ide.core.utilities;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cmoflon.ide.core.build.CMoflonPluginProducerWorkspaceRunnable;
import org.cmoflon.ide.core.runtime.builders.CMoflonRepositoryBuilder;
import org.cmoflon.ide.core.runtime.natures.CMoflonRepositoryNature;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.gervarro.eclipse.workspace.autosetup.PluginProjectConfigurator;
import org.gervarro.eclipse.workspace.util.WorkspaceTask;
import org.moflon.core.build.MoflonProjectCreator;
import org.moflon.core.build.nature.MoflonProjectConfigurator;
import org.moflon.core.build.nature.ProjectNatureAndBuilderConfiguratorTask;
import org.moflon.core.plugins.PluginProducerWorkspaceRunnable;
import org.moflon.core.plugins.PluginProperties;
import org.moflon.core.propertycontainer.MoflonPropertiesContainer;
import org.moflon.core.propertycontainer.MoflonPropertiesContainerHelper;
import org.moflon.core.utilities.MoflonConventions;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.ide.core.project.RepositoryProjectCreator;
//TODO: subclass MoflonProjectCreator
/**
 * Replaces {@link MoflonProjectCreator}. Replacement was necessary as addNatureAndBuilders is declared private and can therefore not be overriden.
 * Also adds the default property files needed for code generation.
 *
 * @author David Giessing
 * @author Roland Kluge
 */
public class CMoflonProjectCreator extends RepositoryProjectCreator
{
   private MoflonProjectConfigurator moflonProjectConfigurator;

   private static final String PATH_TO_DEFAULT_CONSTRAINTS_LIBRARY = "resources/DefaultCMoflonAttributeConstraintsLib.xmi";

   private static final String DEFAULT_GITIGNORE_CONTENT = StringUtils.join(Arrays.asList("/gen", "/model/*.ecore", "/model/*.genmodel"), "\n");

   private final PluginProperties pluginProperties;

   public CMoflonProjectCreator(IProject project, PluginProperties projectProperties,
			MoflonProjectConfigurator projectConfigurator) {
		super(project, projectProperties, projectConfigurator);
		this.pluginProperties = projectProperties;
		this.moflonProjectConfigurator=projectConfigurator;
	}

   @Override
   public void run(final IProgressMonitor monitor) throws CoreException
   {
      final IProject project = getProject();
      if (project.exists())
         return;

   	final String projectName = pluginProperties.getProjectName();
		final SubMonitor subMon = SubMonitor.convert(monitor, "Creating project " + projectName, 13);

		// (1) Create project
		final IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
		project.create(description, IWorkspace.AVOID_UPDATE, subMon.split(1));
		project.open(IWorkspace.AVOID_UPDATE, subMon.split(1));

      final ProjectNatureAndBuilderConfiguratorTask natureAndBuilderConfiguratorTask = new ProjectNatureAndBuilderConfiguratorTask(
				project, false);
      PluginProjectConfigurator pluginConfigurator = new PluginProjectConfigurator();
      natureAndBuilderConfiguratorTask.updateNatureIDs(moflonProjectConfigurator, true);
      natureAndBuilderConfiguratorTask.updateBuildSpecs(moflonProjectConfigurator, true);
      natureAndBuilderConfiguratorTask.updateNatureIDs(pluginConfigurator, true);
      natureAndBuilderConfiguratorTask.updateBuildSpecs(pluginConfigurator, true);
      WorkspaceTask.executeInCurrentThread(natureAndBuilderConfiguratorTask, IWorkspace.AVOID_UPDATE,
				subMon.split(1));

      createFoldersIfNecessary(project, subMon.split(1));
      createFilesIfNecessary(project, subMon.split(1));
      addGitignoreFile(project, subMon.split(2));
	  addGitKeepFiles(project, subMon.split(2));
      createPluginSpecificFiles(project, this.pluginProperties, subMon.split(1));

      final MoflonPropertiesContainer moflonProperties = MoflonPropertiesContainerHelper
				.loadOrCreatePropertiesContainer(getProject(),
						MoflonConventions.getDefaultMoflonPropertiesFile(getProject()));
		initializeMoflonProperties(moflonProperties);
		MoflonPropertiesContainerHelper.save(moflonProperties, subMon.split(1));

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

   @Override
   public void createFoldersIfNecessary(final IProject project, final IProgressMonitor monitor) throws CoreException
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

@Override
protected String getNatureId() throws CoreException {
	return CMoflonRepositoryNature.NATURE_ID;
}

@Override
protected String getBuilderId() throws CoreException {
	return CMoflonRepositoryBuilder.BUILDER_ID;
}
}

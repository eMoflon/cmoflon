package org.cmoflon.ide.core.utilities;

import java.io.ByteArrayInputStream;

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
import org.eclipse.core.runtime.SubMonitor;
import org.moflon.core.propertycontainer.MoflonPropertiesContainer;
import org.moflon.core.propertycontainer.MoflonPropertiesContainerHelper;
import org.moflon.core.propertycontainer.SDMCodeGeneratorIds;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.ide.core.runtime.MoflonProjectCreator;

/**
 * Replaces {@link MoflonProjectCreator}. Replacement was necessary as addNatureAndBuilders is declared private and can therefore not be overriden.
 * Also adds the default property files needed for code generation.
 * @author David Giessing
 *
 */
public class CMoflonProjectCreator implements IWorkspaceRunnable
{

   private String projectName;

   private static String constantPropertiesContent = "#Set to True if dropping unidirectional edges is desired \n" + "dropUnidirectionalEdges = true\n"
         + "#set number of matches allowed per PM method\n" + "MAX_MATCH_COUNT = 20\n"
         + "#place the Names of the tcMethods in the Metamodel here as CSV. Naming should be: tc_<name>\n" + "tcMethods = \n"
         + "#place the parameters for the tc method call here as CSV. should look like: tc_<name> = value, value\n"
         + "# it is also possible to use the constants from down here, for this the value should be const-<constname>\n"
         + "#Define your Constants here: Should look like const-<constname>=value\n";

   private static String mapPropertiesContent = "#Define your Mapping here: \n"
         + "#the Key is the EClass, and the value is the C Struct you want it to be mapped to \n" + "Node = networkaddr_t\n" + "Link = neighbor_t\n";

   private String metaModelProjectName;

   public void setProjectName(final String projectName)
   {
      this.projectName = projectName;
   }

   public void setMetaModelProjectName(final String metaModelProjectName)
   {
      this.metaModelProjectName = metaModelProjectName;
   }

   @Override
   public void run(final IProgressMonitor monitor) throws CoreException
   {
      final IWorkspace workspace = ResourcesPlugin.getWorkspace();
      final IWorkspaceRoot workspaceRoot = workspace.getRoot();
      final IProject workspaceProject = workspaceRoot.getProject(projectName);
      if (workspaceProject.exists())
      {
         return;
      }
      final SubMonitor subMon = SubMonitor.convert(monitor, "Creating project " + projectName, 12);
      final IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);

      workspaceProject.create(description, subMon.split(1));
      workspaceProject.open(subMon.split(1));

      createFoldersIfNecessary(workspaceProject, subMon.split(4));
      addGitIgnoreFiles(workspaceProject, subMon.split(2));

      WorkspaceHelper.addNature(workspaceProject, CMoflonRepositoryNature.NATURE_ID, subMon);

      MoflonPropertiesContainer moflonProperties = MoflonPropertiesContainerHelper.createDefaultPropertiesContainer(workspaceProject.getName(),
            metaModelProjectName);
      final MoflonPropertiesContainer moflonProps = moflonProperties;
      moflonProps.getSdmCodegeneratorHandlerId().setValue(SDMCodeGeneratorIds.DEMOCLES_ATTRIBUTES);
      subMon.worked(1);

      MoflonPropertiesContainerHelper.save(moflonProperties, subMon.split(1));

      this.addFileIfNotExists(workspaceProject, workspaceProject.getName() + "Constants.properties", constantPropertiesContent, subMon.split(1));

      this.addFileIfNotExists(workspaceProject, workspaceProject.getName() + "EClassToStructs.properties", mapPropertiesContent, subMon.split(1));
   }

   private void addFileIfNotExists(IProject workspaceProject, String fileName, String content, SubMonitor subMon) throws CoreException
   {
      if (!workspaceProject.getFile(fileName).exists())
         WorkspaceHelper.addFile(workspaceProject, fileName, content, subMon);
   }

   private static void addGitIgnoreFiles(final IProject project, final IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, "Creating .gitignore files", 2);
      IFile genGitIgnore = WorkspaceHelper.getGenFolder(project).getFile(".gitignore");
      if (!genGitIgnore.exists())
      {
         genGitIgnore.create(new ByteArrayInputStream("*".getBytes()), true, subMon.split(1));
      }

      IFile modelGitIgnore = WorkspaceHelper.getModelFolder(project).getFile(".gitignore");
      if (!modelGitIgnore.exists())
      {
         modelGitIgnore.create(new ByteArrayInputStream("*".getBytes()), true, subMon.split(1));
      }
   }

   public static void createFoldersIfNecessary(final IProject project, final IProgressMonitor monitor) throws CoreException
   {
      final SubMonitor subMon = SubMonitor.convert(monitor, "Creating folders within project", 5);
      WorkspaceHelper.createFolderIfNotExists(WorkspaceHelper.getGenFolder(project), subMon.split(1));
      WorkspaceHelper.createFolderIfNotExists(WorkspaceHelper.getModelFolder(project), subMon.split(1));
   }
}

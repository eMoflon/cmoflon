package org.cmoflon.ide.core.runtime.natures;

import org.cmoflon.ide.core.runtime.builders.CMoflonMetamodelBuilder;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class CMoflonMetamodelNature implements IProjectNature
{
   public static final String NATURE_ID = CMoflonMetamodelNature.class.getName();

   private IProject project;

   @Override
   public void configure() throws CoreException
   {
      // Get project description and add model builder
      IProjectDescription desc = project.getDescription();
      ICommand command = desc.newCommand();
      command.setBuilderName(CMoflonMetamodelBuilder.BUILDER_ID);
      desc.setBuildSpec(new ICommand[] { command });
      // Reset augmented description
      project.setDescription(desc, null);
   }

   @Override
   public void deconfigure() throws CoreException
   {
   }

   @Override
   public IProject getProject()
   {
      return project;
   }

   @Override
   public void setProject(IProject project)
   {
      this.project = project;
   }

}

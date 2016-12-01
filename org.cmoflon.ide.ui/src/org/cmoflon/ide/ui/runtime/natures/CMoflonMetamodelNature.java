package org.cmoflon.ide.ui.runtime.natures;

import org.cmoflon.ide.core.CMoflonCoreActivator;
import org.cmoflon.ide.core.runtime.builders.CMoflonMetamodelBuilder;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

public class CMoflonMetamodelNature implements IProjectNature{
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

package org.cmoflon.ide.core.runtime.natures;

import org.cmoflon.ide.core.runtime.builders.CMoflonMetamodelBuilder;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class CMoflonMetamodelNature implements IProjectNature {
	public static final String NATURE_ID = CMoflonMetamodelNature.class.getName();

	private IProject project;

	@Override
	public void configure() throws CoreException {
		final IProjectDescription desc = project.getDescription();
		final ICommand command = desc.newCommand();
		command.setBuilderName(CMoflonMetamodelBuilder.BUILDER_ID);
		desc.setBuildSpec(new ICommand[] { command });
		project.setDescription(desc, null);
	}

	@Override
	public void deconfigure() throws CoreException {
		// nop
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(final IProject project) {
		this.project = project;
	}

}

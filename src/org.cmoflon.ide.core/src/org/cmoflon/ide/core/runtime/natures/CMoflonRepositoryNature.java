package org.cmoflon.ide.core.runtime.natures;

import org.cmoflon.ide.core.runtime.builders.CMoflonRepositoryBuilder;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.moflon.core.build.nature.MoflonProjectConfigurator;

public class CMoflonRepositoryNature extends MoflonProjectConfigurator
{
   public static final String NATURE_ID = CMoflonRepositoryNature.class.getName();

@Override
protected String getBuilderId() {
	return CMoflonRepositoryBuilder.BUILDER_ID;
}

@Override
public String getNatureId() {
	return NATURE_ID;
}
}

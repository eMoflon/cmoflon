package org.cmoflon.ide.core.runtime.natures;

import org.cmoflon.ide.core.runtime.builders.CMoflonRepositoryBuilder;
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

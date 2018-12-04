package org.cmoflon.ide.core.build;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.moflon.core.plugins.PluginProducerWorkspaceRunnable;
import org.moflon.core.plugins.PluginProperties;

/**
 * This is a variant of the {@link PluginProducerWorkspaceRunnable} that does
 * not depend on {@link IJavaProject}-specific things (e.g., .classpath).
 *
 * @author Roland Kluge - Initial implementation
 */
public class CMoflonPluginProducerWorkspaceRunnable extends PluginProducerWorkspaceRunnable {

	public CMoflonPluginProducerWorkspaceRunnable(final IProject project, final PluginProperties projectProperties) {
		super(project, projectProperties);
	}

	@Override
	public void run(final IProgressMonitor monitor) throws CoreException {
		this.configureManifest(monitor);
	}
}
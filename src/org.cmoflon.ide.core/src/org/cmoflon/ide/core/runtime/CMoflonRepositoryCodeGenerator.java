package org.cmoflon.ide.core.runtime;

import java.util.Properties;

import org.cmoflon.ide.core.runtime.codegeneration.CMoflonCodeGenerator;
import org.cmoflon.ide.core.runtime.codegeneration.CMoflonCodeGeneratorTask;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.moflon.core.preferences.EMoflonPreferencesActivator;
import org.moflon.core.utilities.MoflonConventions;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.core.utilities.eMoflonEMFUtil;
import org.osgi.framework.FrameworkUtil;

/**
 * Mimics {@link RepositoryCodeGenerator}. Needed to invoke
 * {@link CMoflonCodeGenerator}
 *
 * @author David Giessing
 * @author Roland Kluge
 */
public class CMoflonRepositoryCodeGenerator {

	private final IProject project;

	public CMoflonRepositoryCodeGenerator(final IProject project) {
		this.project = project;
	}

	public IStatus generateCode(final IProgressMonitor monitor, final Properties cMoflonProperties) {
		final SubMonitor subMon = SubMonitor.convert(monitor);
		try {
			this.getProject().deleteMarkers(WorkspaceHelper.MOFLON_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);

			final IFile ecoreFile = MoflonConventions.getDefaultEcoreFile(this.getProject());
			if (!ecoreFile.exists()) {
				return new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()),
						"Unable to generate code for " + getProject().getName()
								+ ",  as no Ecore file according to naming convention (capitalizeFirstLetter.lastSegmentOf.projectName) was found!");
			}

			final ResourceSet resourceSet = eMoflonEMFUtil.createDefaultResourceSet();
			eMoflonEMFUtil.installCrossReferencers(resourceSet);
			subMon.worked(1);
			final CMoflonCodeGeneratorTask gen = new CMoflonCodeGeneratorTask(ecoreFile, resourceSet,
					EMoflonPreferencesActivator.getDefault().getPreferencesStorage());
			final IStatus status = gen.run(subMon.split(1));
			if (status.matches(IStatus.ERROR)) {
				return status;
			}
		} catch (final CoreException e) {
			return new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(), "Error", e);
		}
		return Status.OK_STATUS;
	}

	/**
	 * @return the project
	 */
	public IProject getProject() {
		return project;
	}
}

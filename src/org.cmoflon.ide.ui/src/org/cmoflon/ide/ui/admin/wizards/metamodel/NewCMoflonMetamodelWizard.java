package org.cmoflon.ide.ui.admin.wizards.metamodel;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.natures.CMoflonMetamodelNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.moflon.core.utilities.ExceptionUtil;
import org.moflon.core.utilities.LogUtils;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.ide.ui.admin.wizards.metamodel.NewMetamodelProjectInfoPage;

/**
 * The new metamodel wizard creates a new CMoflon metamodel project with default
 * directory structure and default files.
 *
 * @author David Giessing
 */
public class NewCMoflonMetamodelWizard extends Wizard implements IWorkbenchWizard {
	private static final String PATH_TO_DEFAULT_SPECIFICATION = "resources/defaultCMoflonSpecification.eap";

	private NewMetamodelProjectInfoPage projectInfo;

	private static Logger logger = Logger.getLogger(NewCMoflonMetamodelWizard.class);

	public NewCMoflonMetamodelWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		projectInfo = new NewMetamodelProjectInfoPage();
		addPage(projectInfo);
	}

	@Override
	public boolean performFinish() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};

		try {
			getContainer().run(true, false, op);
		} catch (final InterruptedException e) {
			MessageDialog.openError(getShell(), "Error while finishing wizard", e.getMessage());
			LogUtils.error(Logger.getLogger(getClass()), WorkspaceHelper.printStacktraceToString(e));
			return false;
		} catch (final InvocationTargetException e) {
			final Throwable targetException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error while finishing wizard", targetException.getMessage());
			LogUtils.error(Logger.getLogger(getClass()), WorkspaceHelper.printStacktraceToString(targetException));
			return false;
		}

		return true;
	}

	private void doFinish(final IProgressMonitor monitor) throws CoreException {
		try {
			final SubMonitor subMon = SubMonitor.convert(monitor, "Creating metamodel project", 8);

			final String projectName = projectInfo.getProjectName();
			final IPath location = projectInfo.getProjectLocation();

			final IProject newProjectHandle = createProject(projectName, location,
					subMon.split(1));

			final URL pathToDefaultEapFile = WorkspaceHelper.getPathRelToPlugIn(PATH_TO_DEFAULT_SPECIFICATION,
					getPluginId());
			WorkspaceHelper.addFile(newProjectHandle, projectName + ".eap", pathToDefaultEapFile,
					getPluginId(), subMon.split(1));

			WorkspaceHelper.addFile(newProjectHandle, ".gitignore", ".temp\n*.ldb\n", subMon.split(1));

			WorkspaceHelper.addNature(newProjectHandle, CMoflonMetamodelNature.NATURE_ID, subMon.split(1));

			newProjectHandle.refreshLocal(IResource.DEPTH_INFINITE, subMon.split(1));

		} catch (final Exception e) {
			logger.error("Unable to add default EA project file: " + e);
			throw new CoreException(ExceptionUtil.createDefaultErrorStatus(getClass(), e));
		} finally {
			monitor.done();
		}
	}

	private String getPluginId() {
		return WorkspaceHelper.getPluginId(getClass());
	}

	/**
	 * Creates a new project in current workspace
	 *
	 * @param projectName
	 *            name of the new project
	 * @param monitor
	 *            a progress monitor, or null if progress reporting is not desired
	 * @param location
	 *            the file system location where the project should be placed
	 * @return handle to newly created project
	 * @throws CoreException
	 */
	private static IProject createProject(final String projectName, final IPath location,
			final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Create project " + projectName, 2);

		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject newProject = root.getProject(projectName);

		final IProjectDescription description = ResourcesPlugin.getWorkspace()
				.newProjectDescription(newProject.getName());
		description.setLocation(location);

		if (newProject.exists()) {
			throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(NewCMoflonMetamodelWizard.class), projectName + " exists already!"));
		}

		newProject.create(description, subMon.split(1));
		newProject.open(subMon.split(1));

		return newProject;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// Nothing to do here
	}
}

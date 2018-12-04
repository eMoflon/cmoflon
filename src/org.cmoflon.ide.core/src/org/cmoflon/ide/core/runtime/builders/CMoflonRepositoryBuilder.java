package org.cmoflon.ide.core.runtime.builders;

import java.util.Map;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.CMoflonRepositoryCodeGenerator;
import org.cmoflon.ide.core.runtime.natures.CMoflonRepositoryNature;
import org.cmoflon.ide.core.utilities.CMoflonProperties;
import org.cmoflon.ide.core.utilities.CMoflonWorkspaceHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.gervarro.eclipse.workspace.util.AntPatternCondition;
import org.gervarro.eclipse.workspace.util.RelevantElementCollector;
import org.gervarro.eclipse.workspace.util.VisitorCondition;
import org.moflon.core.build.AbstractVisitorBuilder;
import org.moflon.core.preferences.EMoflonPreferencesActivator;
import org.moflon.core.preferences.EMoflonPreferencesStorage;
import org.moflon.core.utilities.ErrorReporter;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.ide.core.runtime.builders.RepositoryBuilder;
import org.moflon.ide.ui.preferences.EMoflonPreferenceInitializer;

/**
 * Builder for projects with {@link CMoflonRepositoryNature}. Similar to
 * {@link RepositoryBuilder}. Triggers {@link CMoflonRepositoryCodeGenerator}.
 *
 * @author David Giessing
 * @author Roland Kluge
 */
public class CMoflonRepositoryBuilder extends AbstractVisitorBuilder {
	private static final VisitorCondition VISITOR_CONDITION = new AntPatternCondition(
			new String[] { "model/*.ecore", CMoflonProperties.CMOFLON_PROPERTIES_FILENAME, "injection/*" });

	public static final String BUILDER_ID = CMoflonRepositoryBuilder.class.getName();

	private static final Logger logger = Logger.getLogger(CMoflonRepositoryBuilder.class);

	public CMoflonRepositoryBuilder() {
		super(VISITOR_CONDITION);
	}

	@Override
	protected final AntPatternCondition getTriggerCondition(final IProject project) {
		return new AntPatternCondition(new String[0]);
	}

	@Override
	protected void clean(final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Cleaning " + getProject(), 4);

		final IProject project = getProject();

		clearProblemMarkers(project);
		subMon.worked(1);

		cleanFolderButKeepHiddenFiles(project.getFolder(WorkspaceHelper.GEN_FOLDER), subMon.split(1));
	}

	@Override
	protected void processResource(final IResource ecoreResource, final int kind, final Map<String, String> args,
			final IProgressMonitor monitor) {
		final IFile ecoreFile = Platform.getAdapterManager().getAdapter(ecoreResource, IFile.class);
		try {
			final SubMonitor subMon = SubMonitor.convert(monitor, "Processing Resource", 53);
			logger.info("Generating code for " + this.getProject());
			initializePreferencesStorage();

			final CMoflonRepositoryCodeGenerator generator = new CMoflonRepositoryCodeGenerator(getProject());

			final IStatus status = generator.generateCode(subMon.split(50),
					CMoflonWorkspaceHelper.getCMoflonPropertiesFile(getProject()));

			handleErrorsAndWarnings(status, ecoreFile);
		} catch (final CoreException e) {
			handleErrorsInEclipse(
					new Status(e.getStatus().getSeverity(), WorkspaceHelper.getPluginId(getClass()), e.getMessage(), e),
					ecoreFile);
		}
	}

	@Override
	protected void postprocess(final RelevantElementCollector buildVisitor, final int originalKind,
			final Map<String, String> builderArguments, final IProgressMonitor monitor) {
		final RelevantElementCollector filteredBuildVisitor = new SingleResourceRelevantElementCollector(buildVisitor,
				VISITOR_CONDITION, getProject());
		super.postprocess(filteredBuildVisitor, originalKind, builderArguments, monitor);
	}

	private void clearProblemMarkers(final IProject project) throws CoreException {
		project.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
		project.deleteMarkers(WorkspaceHelper.MOFLON_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
	}

	private void initializePreferencesStorage() {
		final EMoflonPreferencesStorage preferencesStorage = EMoflonPreferencesActivator.getDefault()
				.getPreferencesStorage();
		preferencesStorage.setValidationTimeout(EMoflonPreferenceInitializer.getValidationTimeoutMillis());
		preferencesStorage.setReachabilityEnabled(EMoflonPreferenceInitializer.getReachabilityEnabled());
		preferencesStorage
				.setReachabilityMaximumAdornmentSize(EMoflonPreferenceInitializer.getReachabilityMaxAdornmentSize());
	}

	/**
	 * Handles errors and warning produced by the code generation task
	 *
	 * @param status
	 */
	private void handleErrorsAndWarnings(final IStatus status, final IFile ecoreFile) throws CoreException {
		if (indicatesThatValidationCrashed(status)) {
			throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()),
					status.getMessage(), status.getException().getCause()));
		}
		if (status.matches(IStatus.ERROR)) {
			handleErrorsInEA(status);
			handleErrorsInEclipse(status, ecoreFile);
		}
		if (status.matches(IStatus.WARNING)) {
			handleInjectionWarningsAndErrors(status);
		}
	}

	private boolean indicatesThatValidationCrashed(final IStatus status) {
		return status.getException() != null;
	}

	private void handleInjectionWarningsAndErrors(final IStatus status) {
		final String reporterClass = "org.moflon.moca.inject.validation.InjectionErrorReporter";
		final ErrorReporter errorReporter = (ErrorReporter) Platform.getAdapterManager().loadAdapter(getProject(),
				reporterClass);
		if (errorReporter != null) {
			errorReporter.report(status);
		} else {
			logger.debug("Could not load error reporter '" + reporterClass + "'");
		}
	}

	private void handleErrorsInEclipse(final IStatus status, final IFile ecoreFile) {
		final String reporterClass = "org.moflon.compiler.sdm.democles.eclipse.EclipseErrorReporter";
		final ErrorReporter eclipseErrorReporter = (ErrorReporter) Platform.getAdapterManager().loadAdapter(ecoreFile,
				reporterClass);
		if (eclipseErrorReporter != null) {
			eclipseErrorReporter.report(status);
		} else {
			logger.debug("Could not load error reporter '" + reporterClass + "'");
		}
	}

	private void handleErrorsInEA(final IStatus status) {
		final String reporterClass = "org.moflon.validation.EnterpriseArchitectValidationHelper";
		final ErrorReporter errorReporter = (ErrorReporter) Platform.getAdapterManager().loadAdapter(getProject(),
				reporterClass);
		if (errorReporter != null) {
			errorReporter.report(status);
		} else {
			logger.debug("Could not load error reporter '" + reporterClass + "'");
		}
	}

	private void cleanFolderButKeepHiddenFiles(final IFolder folder, final IProgressMonitor monitor)
			throws CoreException {
		if (!folder.exists()) {
			return;
		}

		final String message = "Cleaning " + folder.getName();
		final int totalWork = 2 * folder.members().length;
		final SubMonitor subMon = SubMonitor.convert(monitor, message, totalWork);

		for (final IResource resource : folder.members()) {
			if (!isHiddenResource(resource)) {
				if (WorkspaceHelper.isFolder(resource)) {
					cleanFolderButKeepHiddenFiles(IFolder.class.cast(resource), subMon.split(1));
				} else {
					subMon.worked(1);
				}

				resource.delete(true, subMon.split(1));
			} else {
				subMon.worked(2);
			}
		}
	}

	private boolean isHiddenResource(final IResource resource) {
		return resource.getName().startsWith(".");
	}
}

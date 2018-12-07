package org.cmoflon.ide.core.runtime.builders;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.ResourceFillingMocaCMoflonTransformation;
import org.cmoflon.ide.core.runtime.natures.CMoflonMetamodelNature;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.gervarro.eclipse.task.ITask;
import org.gervarro.eclipse.task.ProgressMonitoringJob;
import org.moflon.codegen.eclipse.ValidationStatus;
import org.moflon.core.plugins.PluginProperties;
import org.moflon.core.utilities.ErrorReporter;
import org.moflon.core.utilities.ExceptionUtil;
import org.moflon.core.utilities.MoflonConventions;
import org.moflon.core.utilities.ProblemMarkerUtil;
import org.moflon.core.utilities.ProgressMonitorUtil;
import org.moflon.core.utilities.UncheckedCoreException;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.core.utilities.eMoflonEMFUtil;
import org.moflon.emf.codegen.dependency.SDMEnhancedEcoreResource;
import org.moflon.ide.core.properties.MetamodelProjectUtil;
import org.moflon.ide.core.properties.MocaTreeEAPropertiesReader;
import org.moflon.ide.core.runtime.ProjectDependencyAnalyzer;
import org.moflon.ide.core.runtime.builders.MetamodelBuilder;
import org.moflon.sdm.compiler.democles.validation.result.ErrorMessage;

/**
 * Builder for projects with {@link CMoflonMetamodelNature}. Similar to
 * eMoflon's {@link MetamodelBuilder}. Triggers the
 * {@link ResourceFillingMocaCMoflonTransformation}.
 *
 * @author David Giessing
 * @author Roland Kluge
 */
public class CMoflonMetamodelBuilder extends MetamodelBuilder {
	public static final String BUILDER_ID = CMoflonMetamodelBuilder.class.getName();

	private static final Logger logger = Logger.getLogger(CMoflonMetamodelBuilder.class);

	@Override
	protected void processResource(final IResource mocaFile, final int kind, final Map<String, String> args,
			final IProgressMonitor monitor) {
		final MultiStatus mocaToMoflonStatus = new MultiStatus(WorkspaceHelper.getPluginId(getClass()), 0,
				getClass().getName() + " failed", null);

		final String mocaFilePath = MetamodelBuilder.TEMP_FOLDER + "/" + getProject().getName() + "."
				+ MetamodelBuilder.MOCA_XMI_FILE_EXTENSION;
		if (suitableMocaFileExists(mocaFile, mocaFilePath)) {
			try {
				final SubMonitor subMon = SubMonitor.convert(monitor, "Processing .temp", 140);

				if (MetamodelProjectUtil.getExportedMocaTree(this.getProject()).exists()) {
					deleteProblemMarkers();

					final URI workspaceURI = URI.createPlatformResourceURI("/", true);
					final URI projectURI = URI.createURI(getProject().getName() + "/", true).resolve(workspaceURI);

					final ResourceSet set = eMoflonEMFUtil.createDefaultResourceSet();

					final URI mocaFileURI = URI.createURI(mocaFilePath, true).resolve(projectURI);
					final Resource mocaTreeResource = set.getResource(mocaFileURI, true);
					final MocaTreeEAPropertiesReader mocaTreeReader = new MocaTreeEAPropertiesReader();
					final Map<String, PluginProperties> properties = mocaTreeReader.getProperties(getProject());

					final IProgressMonitor exporterSubMonitor = subMon.split(100);
					exporterSubMonitor.beginTask("Running Moca-to-cMoflon transformation", properties.keySet().size());

					final ResourceFillingMocaCMoflonTransformation exporter = new ResourceFillingMocaCMoflonTransformation(
							set, properties, exporterSubMonitor, this.getProject(), this);
					exporter.mocaToEcore(mocaTreeReader.getMocaTree());
					for (final ErrorMessage message : exporter.getMocaToMoflonReport().getErrorMessages()) {
						mocaToMoflonStatus.add(ValidationStatus.createValidationStatus(message));
					}

					if (exporter.getEpackages().isEmpty()) {
						reportErrorAboutEmptyEPackages(mocaFile);
						return;
					}

					set.getResources().remove(mocaTreeResource);

					// Enforce resource change notifications to update workspace plugin information
					ResourcesPlugin.getWorkspace().checkpoint(false);

					// Load resources
					ITask[] taskArray = new ITask[exporter.getMetamodelLoaderTasks().size()];
					taskArray = exporter.getMetamodelLoaderTasks().toArray(taskArray);
					final IStatus metamodelLoaderStatus = ProgressMonitoringJob.executeSyncSubTasks(taskArray,
							resourceLoaderStatus(), subMon.newChild(5));
					ProgressMonitorUtil.checkCancellation(subMon);
					if (!metamodelLoaderStatus.isOK()) {
						if (kind == IncrementalProjectBuilder.FULL_BUILD && !getTriggerProjects().isEmpty()) {
							needRebuild();
						}
						processProblemStatus(metamodelLoaderStatus, mocaFile);
						return;
					}

					// Analyze project dependencies
					ProjectDependencyAnalyzer[] dependencyAnalyzers = new ProjectDependencyAnalyzer[exporter
							.getProjectDependencyAnalyzerTasks().size()];
					dependencyAnalyzers = exporter.getProjectDependencyAnalyzerTasks().toArray(dependencyAnalyzers);
					for (final ProjectDependencyAnalyzer analyzer : dependencyAnalyzers) {
						analyzer.setInterestingProjects(getTriggerProjects());
					}
					final IStatus projectDependencyAnalyzerStatus = ProgressMonitoringJob.executeSyncSubTasks(
							dependencyAnalyzers, dependencyAnalysisErrorStatus(), subMon.newChild(5));
					ProgressMonitorUtil.checkCancellation(subMon);
					if (!projectDependencyAnalyzerStatus.isOK()) {
						processProblemStatus(projectDependencyAnalyzerStatus, mocaFile);
						return;
					}

					saveModels(set);
				}
			} catch (final Exception e) {
				throw new UncheckedCoreException(
						new CoreException(ExceptionUtil.createDefaultErrorStatus(getClass(), e)));
			} finally {
				handleErrorsInEclipse(mocaToMoflonStatus);
				monitor.done();
			}

		}
	}

	private MultiStatus dependencyAnalysisErrorStatus() {
		return new MultiStatus(WorkspaceHelper.getPluginId(getClass()), 0, "Dependency analysis failed", null);
	}

	private MultiStatus resourceLoaderStatus() {
		return new MultiStatus(WorkspaceHelper.getPluginId(getClass()), 0, "Resource loading failed", null);
	}

	private void reportErrorAboutEmptyEPackages(final IResource mocaFile) throws CoreException {
		final String errorMessage = "Unable to transform exported files to Ecore models";
		ProblemMarkerUtil.createProblemMarker(mocaFile, errorMessage, IMarker.SEVERITY_ERROR,
				mocaFile.getProjectRelativePath().toString());
		logger.error(errorMessage);
	}

	private boolean suitableMocaFileExists(final IResource mocaFile, final String mocaFilePath) {
		return mocaFile instanceof IFile && mocaFilePath.equals(mocaFile.getProjectRelativePath().toString())
				&& mocaFile.isAccessible();
	}

	private void saveModels(final ResourceSet set) throws IOException {
		final Map<Object, Object> saveOnlyIfChangedOption = new HashMap<Object, Object>();
		saveOnlyIfChangedOption.put(Resource.OPTION_SAVE_ONLY_IF_CHANGED,
				Resource.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);
		saveOnlyIfChangedOption.put(SDMEnhancedEcoreResource.SAVE_GENERATED_PACKAGE_CROSSREF_URIS, true);

		// Persist resources (metamodels, tgg files and moflon.properties files)
		for (final Resource resource : set.getResources()) {
			resource.save(saveOnlyIfChangedOption);
		}
	}

	/**
	 * Reports problems/errors/... in Eclipse
	 */
	private void handleErrorsInEclipse(final IStatus validationStatus) {
		final IFile ecoreFile = MoflonConventions.getDefaultEcoreFile(getProject());
		final ErrorReporter eclipseErrorReporter = (ErrorReporter) Platform.getAdapterManager().loadAdapter(ecoreFile,
				"org.moflon.compiler.sdm.democles.eclipse.EclipseErrorReporter");
		if (eclipseErrorReporter != null) {
			if (!validationStatus.isOK()) {
				eclipseErrorReporter.report(validationStatus);
			}
		}
	}
}

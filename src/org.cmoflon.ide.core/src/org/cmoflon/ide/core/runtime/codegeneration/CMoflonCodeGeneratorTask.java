package org.cmoflon.ide.core.runtime.codegeneration;

import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.utilities.CMoflonMonitoredMetamodelLoader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.emf.codegen.ecore.generator.GeneratorAdapterFactory.Descriptor;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.gervarro.eclipse.task.ITask;
import org.moflon.codegen.MethodBodyHandler;
import org.moflon.compiler.sdm.democles.DemoclesGeneratorAdapterFactory;
import org.moflon.compiler.sdm.democles.DemoclesMethodBodyHandler;
import org.moflon.core.preferences.EMoflonPreferencesStorage;
import org.moflon.core.propertycontainer.MoflonPropertiesContainer;
import org.moflon.core.utilities.LogUtils;
import org.moflon.core.utilities.MoflonConventions;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.core.utilities.eMoflonEMFUtil;
import org.moflon.emf.build.GenericMoflonProcess;
import org.moflon.emf.build.MonitoredGenModelBuilder;
import org.moflon.emf.build.MonitoredMetamodelLoader;

/**
 * The task that controls the cMoflon code generation process
 *
 * @author David Giessing
 * @author Roland Kluge
 */
public class CMoflonCodeGeneratorTask implements ITask {
	private static final Logger logger = Logger.getLogger(CMoflonCodeGeneratorTask.class);

	private final IFile ecoreFile;

	private final ResourceSet resourceSet;

	private final EMoflonPreferencesStorage preferencesStorage;

	private List<Resource> resources;

	private MoflonPropertiesContainer moflonProperties;

	private GenModel genModel;

	/**
	 * Initializes the code generation task
	 * 
	 * @param ecoreFile
	 *            the ECore file to generate code for
	 * @param resourceSet
	 *            the {@link ResourceSet} of the running build process
	 * @param preferencesStorage
	 *            the container that stores IDE preferences
	 */
	public CMoflonCodeGeneratorTask(final IFile ecoreFile, final ResourceSet resourceSet,
			final EMoflonPreferencesStorage preferencesStorage) {
		this.ecoreFile = ecoreFile;
		this.resourceSet = resourceSet;
		this.preferencesStorage = preferencesStorage;
	}

	/**
	 * This is almost equal to {@link GenericMoflonProcess#run(IProgressMonitor)},
	 * except for using {@link CMoflonMonitoredMetamodelLoader} instead of
	 * {@link MonitoredMetamodelLoader}
	 */
	@Override
	public IStatus run(final IProgressMonitor monitor) {
		final SubMonitor subMon = SubMonitor.convert(monitor, getTaskName(), 10);
		try {
			// (1) Loads moflon.properties file
			final IProject project = ecoreFile.getProject();
			final URI projectURI = URI.createPlatformResourceURI(project.getName() + "/", true);
			final URI moflonPropertiesURI = URI.createURI(MoflonConventions.MOFLON_CONFIG_FILE).resolve(projectURI);
			final Resource moflonPropertiesResource = eMoflonEMFUtil.createDefaultResourceSet()
					.getResource(moflonPropertiesURI, true);
			this.moflonProperties = (MoflonPropertiesContainer) moflonPropertiesResource.getContents().get(0);

			subMon.worked(1);
			if (subMon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
		} catch (WrappedException wrappedException) {
			final Exception exception = wrappedException.exception();
			return new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), exception.getMessage(),
					exception);
		} catch (RuntimeException runtimeException) {
			return new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), runtimeException.getMessage(),
					runtimeException);
		}

		// (2) Load metamodel
		final CMoflonMonitoredMetamodelLoader metamodelLoader = new CMoflonMonitoredMetamodelLoader(resourceSet,
				ecoreFile, moflonProperties);
		final IStatus metamodelLoaderStatus = metamodelLoader.run(subMon.split(2));
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		if (metamodelLoaderStatus.matches(IStatus.ERROR)) {
			return metamodelLoaderStatus;
		}
		this.resources = metamodelLoader.getResources();

		return this.processResource(subMon.split(7));
	}

	@SuppressWarnings("deprecation")
	public IStatus processResource(final IProgressMonitor monitor) {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Code generation task", 100);
		try {
			final MoflonPropertiesContainer moflonProperties = getMoflonProperties();
			final String metaModelProjectName = moflonProperties.getMetaModelProject().getMetaModelProjectName();
			final String fullProjectName;
			if ("NO_META_MODEL_PROJECT_NAME_SET_YET".equals(metaModelProjectName)) {
				fullProjectName = moflonProperties.getProjectName();
			} else {
				fullProjectName = metaModelProjectName + "::" + moflonProperties.getProjectName();
			}
			logger.info("Generating code for: " + fullProjectName);

			long toc = System.nanoTime();

			final Resource resource = getEcoreResource();
			final EPackage ePackage = (EPackage) resource.getContents().get(0);

			// (1) Instantiate code generation engine
			final CMoflonAttributeConstraintCodeGeneratorConfig defaultCodeGeneratorConfig = new CMoflonAttributeConstraintCodeGeneratorConfig(
					resourceSet, ecoreFile.getProject(), preferencesStorage);
			MethodBodyHandler methodBodyHandler = new DemoclesMethodBodyHandler(resourceSet,
					defaultCodeGeneratorConfig);
			subMon.worked(5);

			// (2) Validate metamodel (including SDMs)
			final ITask validator = methodBodyHandler.createValidator(ePackage);
			final WorkspaceJob validationJob = new WorkspaceJob("cMoflon Validation") {
				@Override
				public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
					final SubMonitor subMon = SubMonitor.convert(monitor, "Validation job", 100);
					return validator.run(subMon.split(100));
				}
			};
			JobGroup jobGroup = new JobGroup("Validation job group", 1, 1);
			validationJob.setJobGroup(jobGroup);
			validationJob.schedule();
			jobGroup.join(preferencesStorage.getValidationTimeout(), subMon.split(10));
			final IStatus validatorStatus = validationJob.getResult();

			if (validatorStatus == null) {
				try {
					validationJob.getThread().stop();
				} catch (ThreadDeath e) {
					// Simply ignore it
				}
				throw new OperationCanceledException("Validation took longer than "
						+ (preferencesStorage.getValidationTimeout() / 1000)
						+ " seconds. This could(!) mean that some of your patterns have no valid search plan. You may increase the timeout value using the eMoflon property page");
			} else if (subMon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if (validatorStatus.matches(IStatus.ERROR)) {
				return validatorStatus;
			}

			// (3) Build or load GenModel
			final MonitoredGenModelBuilder genModelBuilderJob = new MonitoredGenModelBuilder(getResourceSet(),
					getAllResources(), getEcoreFile(), true, getMoflonProperties());
			final IStatus genModelBuilderStatus = genModelBuilderJob.run(subMon.split(15));
			if (subMon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if (genModelBuilderStatus.matches(IStatus.ERROR)) {
				return genModelBuilderStatus;
			}
			this.genModel = genModelBuilderJob.getGenModel();

			final IProject project = getEcoreFile().getProject();

			// (6) Generate code
			subMon.subTask("Generating code for project " + project.getName());
			Descriptor codeGenerationEngine = new DemoclesGeneratorAdapterFactory(
					defaultCodeGeneratorConfig.createTemplateConfiguration(genModel), null);
			final CMoflonCodeGenerator codeGenerator = new CMoflonCodeGenerator(resource, project, this.genModel,
					codeGenerationEngine);
			final IStatus codeGenerationStatus = codeGenerator.generateCode(subMon.split(30));
			if (subMon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if (codeGenerationStatus.matches(IStatus.ERROR)) {
				return codeGenerationStatus;
			}
			subMon.worked(5);

			long tic = System.nanoTime();

			final double timeInSeconds = (tic - toc) / 1e9;
			LogUtils.info(logger, String.format(Locale.US, "Completed in %.3fs", timeInSeconds));

			return validatorStatus.isOK()
					? new Status(IStatus.OK, WorkspaceHelper.getPluginId(getClass()), "Code generation succeeded")
					: new MultiStatus(WorkspaceHelper.getPluginId(getClass()), validatorStatus.getCode(),
							new IStatus[] { validatorStatus }, "Code generation warnings/errors", null);
		} catch (final Exception e) {
			if (e instanceof NullPointerException)
				LogUtils.error(logger, e);
			return new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()), IStatus.ERROR,
					e.getClass().getName() + ": " + e.getMessage(), e);
		}
	}

	@Override
	public String getTaskName() {
		return "CMoflonCodeGeneration";
	}

	private List<Resource> getAllResources() {
		return this.resources;
	}

	private ResourceSet getResourceSet() {
		return this.resourceSet;
	}

	private final IFile getEcoreFile() {
		return ecoreFile;
	}

	private final MoflonPropertiesContainer getMoflonProperties() {
		return moflonProperties;
	}

	private final Resource getEcoreResource() {
		return resources.get(0);
	}
}

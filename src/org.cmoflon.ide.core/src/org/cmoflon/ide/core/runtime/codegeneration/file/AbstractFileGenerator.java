package org.cmoflon.ide.core.runtime.codegeneration.file;

import static org.cmoflon.ide.core.utilities.FormattingUtils.nl;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.cmoflon.ide.core.runtime.codegeneration.BuildProcessConfigurationProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcoreFactory;
import org.moflon.compiler.sdm.democles.DemoclesGeneratorAdapterFactory;
import org.moflon.compiler.sdm.democles.TemplateConfigurationProvider;
import org.moflon.core.utilities.WorkspaceHelper;

public class AbstractFileGenerator {

	protected static final GenClass TC_INDEPENDENT_CLASS = GenModelFactory.eINSTANCE.createGenClass();

	/**
	 * Name of the class that serves as parent class of TC algorithms
	 */
	protected static final String DEFAULT_TC_PARENT_CLASS_NAME = "TopologyControlAlgorithm";

	static {
		final EClass tcIndependentEClass = EcoreFactory.eINSTANCE.createEClass();
		tcIndependentEClass.setName("TC_INDEPENDANT");
		TC_INDEPENDENT_CLASS.setEcoreClass(tcIndependentEClass);
	}

	private final DemoclesGeneratorAdapterFactory codeGenerationEngine;

	private final SimpleDateFormat timeFormatter = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ss");

	private final IProject project;

	private final BuildProcessConfigurationProvider buildProcessConfigurationProvider;

	private final GenModel genModel;

	public AbstractFileGenerator(final IProject project, final GenModel genModel,
			final DemoclesGeneratorAdapterFactory codeGenerationEngine,
			final BuildProcessConfigurationProvider buildProcessConfigurationProvider) {
		this.project = project;
		this.genModel = genModel;
		this.codeGenerationEngine = codeGenerationEngine;
		this.buildProcessConfigurationProvider = buildProcessConfigurationProvider;
	}

	/**
	 * The Democles code generator to invoke for generating pattern-matching code
	 */
	public DemoclesGeneratorAdapterFactory getCodeGenerationEngine() {
		return codeGenerationEngine;
	}

	/**
	 * The currently built Eclipse project
	 */
	public IProject getProject() {
		return project;
	}

	public BuildProcessConfigurationProvider getBuildProcessConfigurationProvider() {
		return buildProcessConfigurationProvider;
	}

	public TemplateConfigurationProvider getTemplateConfigurationProvider() {
		return codeGenerationEngine.getTemplateConfigurationProvider();
	}

	public String getDateCommentCode() {
		return String.format("// Generated using cMoflon on %s%s", timeFormatter.format(new Date()), nl());
	}

	public void createInjectionFolder() throws CoreException {
		WorkspaceHelper.addAllFolders(getProject(), "injection", new NullProgressMonitor());
	}

	/**
	 * @return the genModel
	 */
	public GenModel getGenModel() {
		return genModel;
	}
}

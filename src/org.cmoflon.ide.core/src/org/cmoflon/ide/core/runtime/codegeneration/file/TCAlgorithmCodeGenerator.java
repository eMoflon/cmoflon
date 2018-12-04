package org.cmoflon.ide.core.runtime.codegeneration.file;

import org.cmoflon.ide.core.runtime.codegeneration.BuildProcessConfigurationProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.moflon.compiler.sdm.democles.DemoclesGeneratorAdapterFactory;

public class TCAlgorithmCodeGenerator extends AbstractFileGenerator {

	private final TCAlgorithmHeaderFileGenerator headerFileGenerator;
	private final TCAlgorithmSourceFileGenerator sourceFileGenerator;

	public TCAlgorithmCodeGenerator(final IProject project, final GenModel genModel,
			final DemoclesGeneratorAdapterFactory codeGenerationEngine,
			final BuildProcessConfigurationProvider buildProcessConfigurationProvider) {
		super(project, genModel, codeGenerationEngine, buildProcessConfigurationProvider);

		this.sourceFileGenerator = new TCAlgorithmSourceFileGenerator(project, genModel, codeGenerationEngine,
				buildProcessConfigurationProvider);
		this.headerFileGenerator = new TCAlgorithmHeaderFileGenerator(project, genModel, codeGenerationEngine,
				buildProcessConfigurationProvider);
	}

	public IStatus generateCodeForAlgorithm(final GenClass tcClass, final MultiStatus codeGenerationResult,
			final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generating code for " + tcClass, 100);

		this.headerFileGenerator.generateHeaderFile(tcClass, subMon.split(50));

		this.sourceFileGenerator.generateSourceFile(tcClass, subMon.split(50));

		return Status.OK_STATUS;
	}

}

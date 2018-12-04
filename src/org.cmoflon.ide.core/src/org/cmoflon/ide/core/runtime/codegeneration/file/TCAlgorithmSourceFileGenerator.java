package org.cmoflon.ide.core.runtime.codegeneration.file;

import static org.cmoflon.ide.core.utilities.FormattingUtils.idt2;
import static org.cmoflon.ide.core.utilities.FormattingUtils.nl;
import static org.cmoflon.ide.core.utilities.FormattingUtils.nl2;
import static org.cmoflon.ide.core.utilities.FormattingUtils.prependEachLineWithPrefix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.cmoflon.ide.core.runtime.codegeneration.BuildProcessConfigurationProvider;
import org.cmoflon.ide.core.runtime.codegeneration.CMoflonCodeGenerator;
import org.cmoflon.ide.core.runtime.codegeneration.CMoflonTemplateConstants;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonStringRenderer;
import org.cmoflon.ide.core.utilities.CMoflonProperties;
import org.cmoflon.ide.core.utilities.FormattingUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.ecore.EClass;
import org.gervarro.democles.codegen.ImportManager;
import org.gervarro.democles.codegen.OperationSequenceCompiler;
import org.gervarro.democles.codegen.TemplateInvocation;
import org.gervarro.democles.compiler.CompilerPattern;
import org.gervarro.democles.compiler.CompilerPatternBody;
import org.moflon.compiler.sdm.democles.DemoclesGeneratorAdapterFactory;
import org.moflon.compiler.sdm.democles.SearchPlanAdapter;
import org.moflon.core.utilities.WorkspaceHelper;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

public class TCAlgorithmSourceFileGenerator extends AbstractFileGenerator {

	public TCAlgorithmSourceFileGenerator(final IProject project, final GenModel genModel,
			final DemoclesGeneratorAdapterFactory codeGenerationEngine,
			final BuildProcessConfigurationProvider buildProcessConfigurationProvider) {
		super(project, genModel, codeGenerationEngine, buildProcessConfigurationProvider);
	}

	/**
	 * This Method generates the Source File.
	 *
	 * @param tcClass
	 *                          the name of the specific algorithm
	 * @param inProcessCode
	 *                          the String containing the code that shall be
	 *                          executed in the process
	 * @param genClass
	 *                          the genClass the code is generated for.
	 * @throws CoreException
	 */
	void generateSourceFile(final GenClass tcClass, final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generate source file for " + tcClass, 10);

		final Map<GenClass, StringBuilder> cachedPatternMatchingCode = this.getBuildProcessConfigurationProvider()
				.getCachedPatternMatchingCode();
		final String patternMatchingCode = cachedPatternMatchingCode.get(tcClass).toString();

		final STGroup templateGroup = getTemplateConfigurationProvider()
				.getTemplateGroup(CMoflonTemplateConstants.SOURCE_FILE_GENERATOR);
		templateGroup.registerRenderer(String.class, new CMoflonStringRenderer());

		final StringBuilder contents = new StringBuilder();

		final String componentBasename = getAlgorithmBasename(tcClass);
		contents.append(getDateCommentCode());
		contents.append("#include \"" + componentBasename + ".h" + "\"").append(nl());

		contents.append(getListAndBlockDeclarations(templateGroup));

		if (useHopCount(tcClass)) {
			contents.append(getHopCountCode(tcClass, templateGroup));
		}
		contents.append(getDefaultHelperMethods());
		contents.append(getUserDefinedHelperMethods(tcClass));
		contents.append(generatedGlobalMatchDataStructure(tcClass));
		contents.append(getPatternMatchingCode(tcClass));
		contents.append(patternMatchingCode);
		contents.append(getInitMethod(templateGroup));
		contents.append(getCleanupMethod(templateGroup));
		contents.append(getProcessPreludeCode(tcClass, templateGroup));
		contents.append(getProcessBodyCode(tcClass, templateGroup));
		contents.append(getProcessClosingCode(tcClass, templateGroup));
		subMon.worked(8);

		final String parentFolderForAlgorithm = this.getBuildProcessConfigurationProvider()
				.getProjectRelativePathForAlgorithm(tcClass);
		final String outputFileName = parentFolderForAlgorithm + "/" + componentBasename + ".c";
		final IFile sourceFile = getProject().getFile(outputFileName);
		if (!sourceFile.exists()) {
			WorkspaceHelper.addAllFolders(getProject(), parentFolderForAlgorithm, subMon.split(1));
			WorkspaceHelper.addFile(getProject(), outputFileName, contents.toString(), subMon.split(1));
		} else {
			sourceFile.setContents(new ReaderInputStream(new StringReader(contents.toString())), true, true,
					subMon.split(2));
		}

	}

	/**
	 * Generates the code fragment for the global match data structure
	 *
	 * @param tcClass
	 *                    the current TC class
	 * @return the code fragment for the match data structure
	 */
	private String generatedGlobalMatchDataStructure(final GenClass tcClass) {
		final int maximumMatchSize = determineMaximumMatchSize(tcClass);

		final StringBuilder allinjectedCode = new StringBuilder();
		allinjectedCode.append("// Common match data structure").append(nl());
		allinjectedCode.append(String.format("static void* _result[%d];", maximumMatchSize)).append(nl2());
		return allinjectedCode.toString();
	}

	/**
	 * Determines the maximum number of elements in any match returned by the
	 * {@link CompilerPattern} invocations that are attached this {@link GenClass}
	 *
	 * @param tcClass
	 *                    the {@link GenClass} to analyze
	 * @return the maximum match size. Is 0 if no pattern invocations can be found.
	 */
	private int determineMaximumMatchSize(final GenClass tcClass) {
		int maximumMatchSize = 0;
		final EClass eClass = tcClass.getEcoreClass();
		for (final Adapter adapter : eClass.eAdapters()) {
			if (adapter.isAdapterForType(SearchPlanAdapter.class)) {
				final SearchPlanAdapter searchPlanAdapter = (SearchPlanAdapter) adapter;
				final String patternType = searchPlanAdapter.getPatternType();
				final OperationSequenceCompiler operationSequenceCompiler = getTemplateConfigurationProvider()
						.getOperationSequenceCompiler(patternType);
				final TemplateInvocation template = searchPlanAdapter
						.prepareTemplateInvocation(operationSequenceCompiler, getDemoclesImportManager());

				final Map<String, Object> attributes = template.getAttributes();
				final CompilerPatternBody body = CompilerPatternBody.class.cast(attributes.get("body"));
				final int currentMatchSize = body.getHeader().getInternalSymbolicParameters().size();
				maximumMatchSize = Math.max(maximumMatchSize, currentMatchSize);
			}
		}
		return maximumMatchSize;
	}

	private String getProcessPreludeCode(final GenClass tcClass, final STGroup templateGroup) {
		return SourceFileGenerator.generateUpperPart(getComponentName(), tcClass, templateGroup, useHopCount(tcClass),
				getAlgorithmPreprocessorId(tcClass));
	}

	private String getProcessBodyCode(final GenClass tcClass, final STGroup sourceFileGeneratorTemplateGroup) {
		final StringBuilder processBodyCode = new StringBuilder();
		processBodyCode.append(idt2() + "prepareLinks();").append(nl());
		processBodyCode.append(idt2() + getTypeName(tcClass) + " tc;").append(nl());
		processBodyCode.append(idt2() + "tc.node =  networkaddr_node_addr();").append(nl());
		processBodyCode.append(generateAlgorithmInvocationCode(tcClass, sourceFileGeneratorTemplateGroup));
		processBodyCode.append(generateCleanupCode(sourceFileGeneratorTemplateGroup));
		return processBodyCode.toString();
	}

	private String generateAlgorithmInvocationCode(final GenClass tcClass, final STGroup templateGroup) {

		final StringBuilder algorithmInvocationCode = new StringBuilder();

		final Map<GenClass, String> tcAlgorithmCallParameters = this.getBuildProcessConfigurationProvider()
				.getTcAlgorithmCallParameters();
		final Set<String> useEvalStatements = this.getBuildProcessConfigurationProvider().getUseEvalStatements();
		final String algorithmInvocation = tcAlgorithmCallParameters.get(tcClass);
		algorithmInvocationCode.append(getParameters(algorithmInvocation, tcClass, templateGroup));

		if (useEvalStatements.contains(tcClass.getName())) {
			algorithmInvocationCode.append(prependEachLineWithPrefix(generateEvaluationBeginCode(), idt2()));
		}

		final String algorithmInvocationStatement = getClassPrefixForMethods(tcClass) + "run(&tc);";
		algorithmInvocationCode.append(idt2()).append(algorithmInvocationStatement).append(nl());

		if (useEvalStatements.contains(tcClass.getName())) {
			algorithmInvocationCode.append(prependEachLineWithPrefix(generateEvaluationEndCode(), idt2()));

		}
		final String result = algorithmInvocationCode.toString();
		return result;
	}

	private String generateEvaluationEndCode() {
		final STGroup evalStatementGroup = getTemplateConfigurationProvider()
				.getTemplateGroup(CMoflonTemplateConstants.EVALUATION_STATEMENTS);
		final ST templateForEnd = evalStatementGroup.getInstanceOf(CMoflonTemplateConstants.EVALUATION_STATEMETNS_END);
		final String render = templateForEnd.render();
		return render;
	}

	private String generateEvaluationBeginCode() {
		final STGroup evalStatementGroup = getTemplateConfigurationProvider()
				.getTemplateGroup(CMoflonTemplateConstants.EVALUATION_STATEMENTS);
		final ST templateForBegin = evalStatementGroup
				.getInstanceOf(CMoflonTemplateConstants.EVALUATION_STATEMENTS_BEGIN);
		final String render = templateForBegin.render();
		return render;
	}

	private String generateCleanupCode(final STGroup templateGroup) {
		final ST cleanupCallTemplate = templateGroup.getInstanceOf(CMoflonTemplateConstants.SOURCE_CLEANUP_CALL);
		final String cleanupCode = cleanupCallTemplate.render();
		return cleanupCode;
	}

	private String getPatternMatchingCode(final GenClass tcClass) {
		final StringBuilder allinjectedCode = new StringBuilder();

		final List<GenClass> cachedConcreteClasses = this.getBuildProcessConfigurationProvider()
				.getCachedConcreteClasses();
		for (final GenClass genClass : cachedConcreteClasses) {
			if ((!genClass.getName().contentEquals(tcClass.getName())) && isIrrelevantForAlgorithm(genClass, tcClass)) {
				continue;
			}
			final String injectedCode = getPatternImplementationCode(genClass);
			if (injectedCode != null) {
				allinjectedCode.append(injectedCode);
			}
		}
		return allinjectedCode.toString();
	}

	private String getProcessClosingCode(final GenClass tcClass, final STGroup templateGroup) {
		final Set<String> dropUnidirectionalEdgesOff = this.getBuildProcessConfigurationProvider()
				.getDropUnidirectionalEdgesOff();
		final StringBuilder sb = new StringBuilder();
		if (!dropUnidirectionalEdgesOff.contains(tcClass.getName())) {
			final String templateCode = templateGroup
					.getInstanceOf(CMoflonTemplateConstants.SOURCE_DROP_UNIDIRECTIONAL_EDGES).render();
			sb.append(prependEachLineWithPrefix(templateCode, idt2()));
		}
		sb.append(SourceFileGenerator.generateClosingPart(templateGroup, useHopCount(tcClass)));
		return sb.toString();
	}

	/**
	 * Creates the code that is used by the hop-count calculation
	 *
	 * @param tcClass
	 * @param source
	 * @return
	 */
	private String getHopCountCode(final GenClass tcClass, final STGroup source) {
		final ST hopcountTemplate = source.getInstanceOf(CMoflonTemplateConstants.SOURCE_HOPCOUNT);
		hopcountTemplate.add("comp", getComponentName());
		hopcountTemplate.add("algo", tcClass.getName());
		final String hopCountCode = hopcountTemplate.render();
		return hopCountCode;
	}

	/**
	 * This method generates the List and memory block allocations needed by the
	 * generated Code.
	 *
	 * @return a String containing the List and Block declarations.
	 */
	private String getListAndBlockDeclarations(final STGroup templateGroup) {
		final ST listTemplate = templateGroup.getInstanceOf(CMoflonTemplateConstants.SOURCE_LIST_DECLARATION);
		final ST memberDeclarationTemplate = templateGroup
				.getInstanceOf(CMoflonTemplateConstants.SOURCE_MEMB_DECLARATION);
		final StringBuilder result = new StringBuilder(nl());

		final List<String> blockDeclarations = this.getBuildProcessConfigurationProvider().getBlockDeclarations();
		for (final String s : blockDeclarations) {
			// Make sure STS are clean
			memberDeclarationTemplate.remove("name");
			memberDeclarationTemplate.remove("type");
			memberDeclarationTemplate.remove("count");
			listTemplate.remove("name");
			// Fill STS
			memberDeclarationTemplate.add("name", s);
			memberDeclarationTemplate.add("type", "match_t");
			memberDeclarationTemplate.add("count", "MAX_MATCH_COUNT");
			listTemplate.add("name", s);
			result.append(memberDeclarationTemplate.render());
			result.append(listTemplate.render());
		}
		result.append(nl());
		return result.toString();
	}

	/**
	 * Gets an initializer method for the Blocks and Lists declarated
	 *
	 * @param templateGroup
	 *
	 * @return
	 */
	private String getInitMethod(final STGroup templateGroup) {
		final Set<String> generateDuplicates = this.getBuildProcessConfigurationProvider().getGenerateDuplicates();
		final ST init = templateGroup.getInstanceOf(CMoflonTemplateConstants.SOURCE_INIT);
		init.add("blocks", getBuildProcessConfigurationProvider().getBlockDeclarations());
		init.add("duplicates", generateDuplicates);
		return init.render();
	}

	/**
	 * Gets a cleanup method for eventual duplicates
	 *
	 * @param templateGroup
	 *
	 * @return
	 */
	private String getCleanupMethod(final STGroup templateGroup) {
		final Set<String> generateDuplicates = this.getBuildProcessConfigurationProvider().getGenerateDuplicates();
		final ST cleanup = templateGroup.getInstanceOf(CMoflonTemplateConstants.SOURCE_CLEANUP);
		cleanup.add("duplicates", generateDuplicates);
		return cleanup.render();
	}

	/**
	 * Gets parameters for a method call inside the process structure. Parameters
	 * are either defined directly in the properties or listed in the constants
	 *
	 * @param property
	 *                          the String CSV containing the parameters
	 * @param component
	 *                          Component string (needed for constants naming)
	 * @param tcClass
	 *                          algo string as well needed for naming
	 * @param templateGroup
	 *                          the StringTemplate group
	 * @return a full String with comma separated parameters
	 */
	private String getParameters(final String property, final GenClass tcClass, final STGroup templateGroup) {
		if (property == null) {
			return "";
		}

		final StringBuilder result = new StringBuilder();
		final String[] params = property.split(CMoflonCodeGenerator.PARAMETER_SEPARATOR);
		for (final String p : params) {
			if (p.trim().contains(CMoflonProperties.PROPERTY_PREFIX_FOR_CONSTANTS)) {
				final ST parametersTemplate = templateGroup
						.getInstanceOf(CMoflonTemplateConstants.SOURCE_FILE_PARAMETER_CONSTANT);
				parametersTemplate.add("comp", getComponentName());
				parametersTemplate.add("algo", tcClass.getName());
				parametersTemplate.add("name", p.trim().split(CMoflonProperties.PROPERTY_PREFIX_FOR_CONSTANTS)[1]);
				result.append(parametersTemplate.render()).append(nl());
			} else {
				final ST parametersTemplate = templateGroup
						.getInstanceOf(CMoflonTemplateConstants.SOURCE_FILE_PARAMETER_RAW);
				final String name = p.split(Pattern.quote("="))[0];
				final String value = p.split(Pattern.quote("="))[1];
				parametersTemplate.add("name", name);
				parametersTemplate.add("value", value);
				result.append(parametersTemplate.render()).append(nl());
			}
		}

		final String returnValue = prependEachLineWithPrefix(result.toString(), idt2());
		return returnValue + FormattingUtils.nl();
	}

	/**
	 * Returns the implementations of the patterns
	 *
	 * @param genClass
	 * @return returns the pattern matching code as string
	 */
	private String getPatternImplementationCode(final GenClass genClass) {
		final StringBuilder code = new StringBuilder();

		for (final Adapter adapter : genClass.getEcoreClass().eAdapters()) {
			if (adapter.isAdapterForType(SearchPlanAdapter.class)) {
				final SearchPlanAdapter searchPlanAdapter = (SearchPlanAdapter) adapter;
				final String patternType = searchPlanAdapter.getPatternType();
				final OperationSequenceCompiler operationSequenceCompiler = getTemplateConfigurationProvider()
						.getOperationSequenceCompiler(patternType);
				final TemplateInvocation template = searchPlanAdapter
						.prepareTemplateInvocation(operationSequenceCompiler, getDemoclesImportManager());

				final ST st = getTemplateConfigurationProvider().getTemplateGroup(patternType)
						.getInstanceOf(template.getTemplateName());
				final Map<String, Object> attributes = template.getAttributes();

				for (final Entry<String, Object> entry : attributes.entrySet()) {
					st.add(entry.getKey(), entry.getValue());
				}
				final String result = st.render();
				code.append(result);
				code.append(nl2());
			}
		}
		return code.length() > 0 ? code.toString() : null;
	}

	private boolean useHopCount(final GenClass tcClass) {
		final Set<String> useHopCountProcess = this.getBuildProcessConfigurationProvider().getUseHopCountProcess();
		return useHopCountProcess.contains(tcClass.getName());
	}

	/**
	 * Checks whether the content refers to the tcAlgorithm with name name
	 *
	 * @param content
	 *                    the generated code
	 * @param name
	 *                    the name of the current tcAlgorithm
	 * @return true if code size reduction is activated an the content does not
	 *         refer to the current algorithm but another TC algorithm
	 */
	private boolean isIrrelevantForAlgorithm(final GenClass genClass, final GenClass tcClass) {
		final Map<GenClass, List<String>> helperClasses = this.getBuildProcessConfigurationProvider()
				.getHelperClasses();
		// Do not add code from other tc classes
		if (isTrueSubtypeOfTCAlgorithmParentClass(genClass)) {
			return true;
		} else if (helperClasses.get(tcClass) != null && helperClasses.get(tcClass).contains(genClass.getName())) {
			return false;
		} else {
			return true;
		}
	}

	private String getDefaultHelperMethods() throws CoreException {
		final StringBuilder result = new StringBuilder();
		result.append("// --- Begin of default cMoflon helpers").append(nl());
		final String urlString = String.format("platform:/plugin/%s/resources/helper.c",
				WorkspaceHelper.getPluginId(getClass()));
		try {
			final URL url = new URL(urlString);
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(url.openConnection().getInputStream()))) {
				result.append(reader.lines().collect(Collectors.joining(nl())));
			} catch (final IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()),
						"Failed to read default typedefs from " + url.toString(), e));
			}
		} catch (final MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()),
					"Invalid URL : " + urlString, e));
		}
		result.append("// --- End of default cMoflon helpers").append(nl2());
		return result.toString();

	}

	private String getUserDefinedHelperMethods(final GenClass tcClass) throws CoreException {
		final StringBuilder combiner = new StringBuilder();
		combiner.append(generateUserDefinedAlgorithmIndependentCode());
		combiner.append(generateUserDefinedAlgorithmSpecificCode(tcClass));
		return combiner.toString();

	}

	private String generateUserDefinedAlgorithmSpecificCode(final GenClass tcClass) throws CoreException {
		final StringBuilder result = new StringBuilder();
		final String algorithmName = tcClass.getName();
		final String projectRelativePath = "injection/custom-helpers_" + algorithmName + ".c";
		result.append(
				"// --- Begin of user-defined helpers for " + algorithmName + " (Path: '" + projectRelativePath + "')")
				.append(nl());
		final IFile helperFile = getProject().getFile(projectRelativePath);
		if (helperFile.exists()) {
			final InputStream stream = helperFile.getContents();
			try {
				result.append(IOUtils.toString(stream));
			} catch (final IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()),
						"Failed to read user-defined helpers " + helperFile, e));
			} finally {
				IOUtils.closeQuietly(stream);
			}
		} else {
			createInjectionFolder();
			WorkspaceHelper.addFile(getProject(), projectRelativePath,
					"// Helper definitions for '" + algorithmName + "'." + nl(), new NullProgressMonitor());
		}
		result.append("// --- End of user-defined helpers for " + algorithmName).append(nl2());
		final String string = result.toString();
		return string;
	}

	private String generateUserDefinedAlgorithmIndependentCode() throws CoreException {
		final StringBuilder result = new StringBuilder();
		final String projectRelativePath = "injection/custom-helpers.c";
		result.append(
				"// --- Begin of user-defined algorithm-independent helpers (Path: '" + projectRelativePath + "')")
				.append(nl());
		final IFile helperFile = getProject().getFile(projectRelativePath);
		if (helperFile.exists()) {
			final InputStream stream = helperFile.getContents();
			try {
				result.append(IOUtils.toString(stream));
			} catch (final IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()),
						"Failed to read user-defined helpers " + helperFile, e));
			} finally {
				IOUtils.closeQuietly(stream);
			}
		} else {
			createInjectionFolder();
			WorkspaceHelper.addFile(getProject(), projectRelativePath,
					"// Algorithm-independent helper definitions." + nl(), new NullProgressMonitor());
		}
		result.append("// --- End of user-defined algorithm-independent helpers").append(nl2());
		final String string = result.toString();
		return string;
	}

	private ImportManager getDemoclesImportManager() {
		return null;
	}
}

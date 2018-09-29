package org.cmoflon.ide.core.runtime.codegeneration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonIncludes.ToCoCoComponents;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonStringRenderer;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.FieldAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.MethodAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.Type;
import org.cmoflon.ide.core.utilities.CMoflonProperties;
import org.cmoflon.ide.core.utilities.CMoflonWorkspaceHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.codegen.ecore.generator.GeneratorAdapterFactory.Descriptor;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenDataType;
import org.eclipse.emf.codegen.ecore.genmodel.GenEnum;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenOperation;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.gervarro.democles.codegen.ImportManager;
import org.gervarro.democles.codegen.OperationSequenceCompiler;
import org.gervarro.democles.codegen.TemplateInvocation;
import org.gervarro.democles.compiler.CompilerPatternBody;
import org.moflon.compiler.sdm.democles.DemoclesGeneratorAdapterFactory;
import org.moflon.compiler.sdm.democles.DemoclesMethodBodyHandler;
import org.moflon.compiler.sdm.democles.SearchPlanAdapter;
import org.moflon.compiler.sdm.democles.TemplateConfigurationProvider;
import org.moflon.compiler.sdm.democles.eclipse.AdapterResource;
import org.moflon.core.utilities.LogUtils;
import org.moflon.core.utilities.MoflonUtil;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.sdm.runtime.democles.Scope;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

/**
 * Generates the source and the header files in /gen
 *
 * @author David Giessing
 * @author Roland Kluge
 */
public class CMoflonCodeGenerator {

	private static final Logger logger = Logger.getLogger(CMoflonCodeGenerator.class);
	/**
	 * The currently built Eclipse project
	 */
	private final IProject project;
	/**
	 * The GenModel of the project
	 */
	private final GenModel genModel;

	/**
	 * The Democles import manager to be used during the build process
	 */
	private ImportManager democlesImportManager;

	/**
	 * The Democles code generator to invoke for generating pattern-matching code
	 */
	private final DemoclesGeneratorAdapterFactory codeGenerationEngine;

	/**
	 * Contains preprocessor directives to be placed in the file with the same name
	 * in TOCOCO_HOME/src
	 */
	private static final String APPLICATION_DEFAULTS_FILENAME = "app-conf-constants.h.sample";

	/**
	 * Contains Makefile directives to be placed in the file with the same name in
	 * TOCOCO_HOME/src
	 */
	private static final String MAKE_CONF_FILENAME = "Makefile-conf-default.include";

	/**
	 * Name of the class that serves as parent class of TC algorithms
	 */
	private static final String DEFAULT_TC_PARENT_CLASS_NAME = "TopologyControlAlgorithm";

	/**
	 * Name of the TC component in ToCoCo
	 */
	private static final String COMPONENT_TOPOLOGY_CONTROL_PREFIX = "topologycontrol";

	/**
	 * List of built-in types provided by ECore
	 */
	private final List<String> builtInTypes;

	/**
	 * Contains all algorithm names for which dropping unidirectional edges should
	 * be inactive
	 */
	private final Set<String> dropUnidirectionalEdgesOff = new HashSet<>();

	/**
	 * Contains all algorithm names for which hop-count data are requested
	 */
	private final Set<String> useHopCountProcess = new HashSet<>();

	private final List<String> blockDeclarations = new ArrayList<>();

	private final List<GenClass> tcClasses = new ArrayList<>();

	private final Map<String, List<String>> helperClasses = new HashMap<>();

	private final Map<String, String> tcAlgorithmCallParameters = new HashMap<>();

	private int maximumMatchCount;

	// Contains all algorithm names for which evaluation statements were requested
	private final Set<String> useEvalStatements = new HashSet<>();

	// Contains all algorithm names for which duplicate generation is requested
	private final Set<String> generateDuplicates = new HashSet<>();

	private final Map<String, String> typeMappings = new HashMap<>();

	/**
	 * Constants mapping Key: constant name Value: constant value to be used as is
	 * during code generation
	 */
	private final Map<String, Map<String, String>> constantsMapping = new HashMap<>();

	private final Map<String, List<MethodAttribute>> cachedMethodSignatures = new HashMap<>();

	private final Map<String, List<FieldAttribute>> cachedFields = new HashMap<>();

	private final List<GenClass> cachedConcreteClasses = new ArrayList<>();

	private final Map<String, StringBuilder> cachedPatternMatchingCode = new HashMap<>();

	private final String tcAlgorithmParentClassName = DEFAULT_TC_PARENT_CLASS_NAME;

	private final SimpleDateFormat timeFormatter = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ss");

	private final GenClass tcAlgorithmParentGenClass;

	private boolean reduceCodeSize = CMoflonProperties.DEFAULT_REDUCE_CODE_SIZE;

	private static final String TC_INDEPENDANT = "TC_INDEPENDANT";

	public CMoflonCodeGenerator(final Resource ecore, final IProject project, final GenModel genModel,
			final Descriptor codeGenerationEngine) {
		this.codeGenerationEngine = (DemoclesGeneratorAdapterFactory) codeGenerationEngine;
		this.project = project;
		this.genModel = genModel;
		tcAlgorithmParentGenClass = determineTopologyControlParentClass();

		readProperties(project);

		builtInTypes = determineBuiltInTypes();
	}

	public IStatus generateCode(final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generating code", tcClasses.size() * 10 + 20);
		final MultiStatus codeGenerationResult = new MultiStatus(WorkspaceHelper.getPluginId(getClass()), 0,
				"Code generation failed", null);

		initializeCaches();

		if (reduceCodeSize) {
			generateCMoflonHeader(subMon.split(10));
		}

		for (final GenClass tcClass : tcClasses) {
			codeGenerationResult.add(generateCodeForAlgorithm(tcClass, codeGenerationResult, subMon.split(10)));
		}

		generateSampleFiles(subMon.split(10));

		resetCaches();

		return codeGenerationResult;
	}

	private void generateCMoflonHeader(final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generate cMoflon header ", 10);
		final STGroup templateGroup = getTemplateConfigurationProvider()
				.getTemplateGroup(CMoflonTemplateConstants.CMOFLON_HEADER_FILE_GENERATOR);
		templateGroup.registerRenderer(String.class, new CMoflonStringRenderer());

		final StringBuilder contents = new StringBuilder();
		contents.append(getDateCommentCode());
		contents.append(getIncludeGuardCode(null, templateGroup));
		contents.append(getIncludesCode(templateGroup, null));
		contents.append(getConstantsDefinitionsCode(null, templateGroup));
		contents.append(getMaxMatchCountDefinition(null));
		contents.append(getMatchTypeDefinitionCode(templateGroup, null));
		contents.append(getTypeMappingCode(templateGroup, null));
		contents.append(getAllBuiltInMappings(null));
		contents.append(getDefaultTypedefs(null));
		contents.append(getUserDefinedTypedefs(null));
		contents.append(getUnimplementedMethodsCode(templateGroup, null));
		contents.append(getAccessorsCode(templateGroup, null));
		contents.append(getComparisonFunctionsCode(templateGroup, null));
		contents.append(getEqualsFunctionsCode(templateGroup, null));
		contents.append(getHeaderTail(null, templateGroup));
		subMon.worked(8);

		final String parentFolderForAlgorithm = getProjectRelativePathForAlgorithm(null);
		final String outputFileName = parentFolderForAlgorithm + "cMoflon.h";
		final IFile headerFile = project.getFile(outputFileName);
		if (!headerFile.exists()) {
			WorkspaceHelper.addAllFolders(project, parentFolderForAlgorithm, subMon.split(1));
			WorkspaceHelper.addFile(project, outputFileName, contents.toString(), subMon.split(1));
		} else {
			headerFile.setContents(new ReaderInputStream(new StringReader(contents.toString())), true, true,
					subMon.split(2));
		}
	}

	private IStatus generateCodeForAlgorithm(final GenClass tcClass, final MultiStatus codeGenerationResult,
			final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generating code for " + tcClass, 100);

		subMon.worked(10);

		generateHeaderFile(tcClass, subMon.split(45));

		generateSourceFile(tcClass, subMon.split(45));

		return Status.OK_STATUS;
	}

	private GenClass determineTopologyControlParentClass() {
		for (final GenPackage genPackage : genModel.getAllGenPackagesWithClassifiers()) {
			for (final GenClass genClass : genPackage.getGenClasses()) {
				if (tcAlgorithmParentClassName.equals(genClass.getName())) {
					return genClass;
				}
			}
		}

		throw new IllegalStateException(
				"Expected to find superclass '" + tcAlgorithmParentClassName + "' in genmodel.");
	}

	private final boolean isClassInGenmodel(final String className) {
		return getGenclassByName(className).isPresent();
	}

	private Optional<? extends GenClass> getGenclassByName(final String className) {
		for (final GenPackage genPackage : genModel.getGenPackages()) {
			for (final GenClass genClass : genPackage.getGenClasses()) {
				if (genClass.getName().equals(className)) {
					return Optional.of(genClass);
				}
			}
		}
		return Optional.empty();
	}

	private void reportMissingTopologyControlClass(final String nameOfInappropriateClass) {
		final List<GenClass> tcAlgorithmCandidateClasses = new ArrayList<>();
		genModel.getAllGenPackagesWithClassifiers()
				.forEach(genPackage -> genPackage.getGenClasses().stream()
						.filter(genClass -> isTrueSubtypeOfTCAlgorithmParentClass(genClass))
						.forEach(genClass -> tcAlgorithmCandidateClasses.add(genClass)));
		LogUtils.error(logger,
				"Topology class '%s' (specified in %s) cannot be found in GenModel or is not a subtype of '"
						+ tcAlgorithmParentGenClass.getName() + "' and will be ignored.",
				nameOfInappropriateClass, CMoflonProperties.CMOFLON_PROPERTIES_FILENAME);
		LogUtils.error(logger,
				"Candidates are " + tcAlgorithmCandidateClasses.stream().map(cand -> "'" + cand.getName() + "'")
						.collect(Collectors.joining(", ")),
				nameOfInappropriateClass, CMoflonProperties.CMOFLON_PROPERTIES_FILENAME);
	}

	private boolean isTrueSubtypeOfTCAlgorithmParentClass(final GenClass genClass) {
		return tcAlgorithmParentGenClass != genClass
				&& tcAlgorithmParentGenClass.getEcoreClass().isSuperTypeOf(genClass.getEcoreClass());
	}

	private boolean useHopCount(final GenClass tcClass) {
		return useHopCountProcess.contains(tcClass.getName());
	}

	private String getDateCommentCode() {
		return String.format("// Generated using cMoflon on %s%s", timeFormatter.format(new Date()), nl());
	}

	private void initializeCaches() {
		initializeCachedMetamodelElementLists();
		initializeCachedPatternMatchingCode();
		blockDeclarations.addAll(getBlockDeclarations(cachedConcreteClasses));
	}

	/**
	 * Resets all fields that store cached artefacts to their default state
	 */
	private void resetCaches() {
		cachedMethodSignatures.clear();
		cachedFields.clear();
		cachedConcreteClasses.clear();
		blockDeclarations.clear();
	}

	private void initializeCachedPatternMatchingCode() {
		// Initialize StringBuilder
		for (final GenClass tcAlgorithm : tcClasses) {
			cachedPatternMatchingCode.put(tcAlgorithm.getName(), new StringBuilder());
		}
		cachedPatternMatchingCode.put(TC_INDEPENDANT, new StringBuilder());

		for (final GenPackage genPackage : genModel.getGenPackages()) {
			for (final GenClass genClass : genPackage.getGenClasses()) {
				if (!genClass.isAbstract()) {
					final String tcAlgorithmName = getTCAlgorithmNameForGenClass(genClass);
					for (final GenOperation genOperation : genClass.getGenOperations()) {
						final String generatedMethodBody = getGeneratedMethodBody(genOperation.getEcoreOperation());
						if (!generatedMethodBody.equals(MoflonUtil.DEFAULT_METHOD_BODY)) {
							final StringBuilder intermediateBuffer = new StringBuilder();
							LogUtils.info(logger, "Generate method body for '%s::%s'", genClass.getName(),
									genOperation.getName());
							intermediateBuffer.append(nl());
							intermediateBuffer.append(genOperation.getTypeParameters(genClass));
							final String[] typechain = genOperation.getImportedType(genClass).split("\\.");
							String type = "";
							if (typechain.length == 0) {
								type = genOperation.getImportedType(genClass);
							} else {
								type = typechain[typechain.length - 1];
							}
							if (!isBuiltInType(type) && !type.equalsIgnoreCase("void")) {
								type = getTypeName(type) + "*";
							}
							intermediateBuffer.append(type);
							intermediateBuffer.append(" ");
							final String functionName = getClassPrefixForMethods(genClass) + genOperation.getName();
							intermediateBuffer.append(functionName);
							intermediateBuffer.append("(");
							intermediateBuffer.append(getParametersFromEcore(genOperation.getEcoreOperation()));
							intermediateBuffer.append("){").append(nl());
							for (final String line : generatedMethodBody.trim().replaceAll("\\r", "")
									.split(Pattern.quote(nl()))) {
								intermediateBuffer.append(idt() + line).append(nl());
							}
							intermediateBuffer.append(nl() + "}").append(nl());

							final StringBuilder cachedPatternMatchingCodeForTCAlgorithm = cachedPatternMatchingCode
									.get(tcAlgorithmName);
							cachedPatternMatchingCodeForTCAlgorithm.append(intermediateBuffer.toString());
						} else {
							LogUtils.info(logger, "Skip method body due to missing specification: '%s::%s'",
									genClass.getName(), genOperation.getName());
						}
					}
				}
			}
		}

	}

	private void initializeCachedMetamodelElementLists() {
		for (final GenClass tcAlgorithm : tcClasses) {
			cachedMethodSignatures.put(tcAlgorithm.getName(), new ArrayList<MethodAttribute>());
			cachedFields.put(tcAlgorithm.getName(), new ArrayList<>());
		}
		cachedMethodSignatures.put(TC_INDEPENDANT, new ArrayList<MethodAttribute>());
		cachedFields.put(TC_INDEPENDANT, new ArrayList<>());
		genModel.getGenPackages().forEach(genPackage -> genPackage.getGenClasses().forEach(genClass -> {
			if (!genClass.isAbstract()) {
				cachedConcreteClasses.add(genClass);
				List<FieldAttribute> fields;
				if (isTrueSubtypeOfTCAlgorithmParentClass(genClass)) {
					fields = cachedFields.get(getTCAlgorithmNameForGenClass(genClass));
					extractFieldsAndMethodsFromGenClass(fields, genClass);
				} else {
					helperClasses.entrySet().stream().filter(entry -> entry.getValue().contains(genClass.getName()))
							.forEach(entry -> {
								extractFieldsAndMethodsFromGenClass(cachedFields.get(entry.getKey()), genClass);
							});
					if (helperClasses.values().stream().noneMatch(l -> l.contains(genClass.getName()))) {
						fields = cachedFields.get(TC_INDEPENDANT);
						extractFieldsAndMethodsFromGenClass(fields, genClass);
					}
				}
			}
		}));
	}

	private void extractFieldsAndMethodsFromGenClass(final List<FieldAttribute> fields, final GenClass genClass) {
		fields.addAll(getFields(genClass));
		genClass.getGenOperations().forEach(genOperation -> {
			final EClassifier operationType = genOperation.getEcoreOperation().getEType();
			final String operationTypeName = operationType == null ? "void" : operationType.getName();
			final boolean isOperationTypeBuiltIn = operationType == null ? true : isBuiltInType(operationTypeName);
			final String genClassName = genOperation.getGenClass().getName();
			List<MethodAttribute> methodDeclarations;
			if (isTrueSubtypeOfTCAlgorithmParentClass(genOperation.getGenClass())) {
				methodDeclarations = cachedMethodSignatures.get(getTCAlgorithmNameForGenClass(genClass));
			} else {
				methodDeclarations = cachedMethodSignatures.get(TC_INDEPENDANT);
			}
			methodDeclarations.add(new MethodAttribute(new Type(isBuiltInType(genClassName), genClassName),
					new Type(isOperationTypeBuiltIn, operationTypeName), genOperation.getName(),
					getParametersFromEcore(genOperation.getEcoreOperation())));
		});
	}

	private String getComponentName() {
		return COMPONENT_TOPOLOGY_CONTROL_PREFIX;
	}

	/**
	 * This Method generates the Source File.
	 *
	 * @param tcClass
	 *            the name of the specific algorithm
	 * @param inProcessCode
	 *            the String containing the code that shall be executed in the
	 *            process
	 * @param genClass
	 *            the genClass the code is generated for.
	 * @throws CoreException
	 */
	private void generateSourceFile(final GenClass tcClass, final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generate source file for " + tcClass, 10);
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
		contents.append(getPatternMatchingCode(tcClass));
		contents.append(cachedPatternMatchingCode.get(tcClass.getName()).toString());
		contents.append(getInitMethod(templateGroup));
		contents.append(getCleanupMethod(templateGroup));
		contents.append(getProcessPreludeCode(tcClass, templateGroup));
		contents.append(getProcessBodyCode(tcClass, templateGroup));
		contents.append(getProcessClosingCode(tcClass, templateGroup));
		subMon.worked(8);

		final String parentFolderForAlgorithm = getProjectRelativePathForAlgorithm(tcClass);
		final String outputFileName = parentFolderForAlgorithm + "/" + componentBasename + ".c";
		final IFile sourceFile = project.getFile(outputFileName);
		if (!sourceFile.exists()) {
			WorkspaceHelper.addAllFolders(project, parentFolderForAlgorithm, subMon.split(1));
			WorkspaceHelper.addFile(project, outputFileName, contents.toString(), subMon.split(1));
		} else {
			sourceFile.setContents(new ReaderInputStream(new StringReader(contents.toString())), true, true,
					subMon.split(2));
		}

	}

	/**
	 * Generates the Header File including, constants, includes, method
	 * declarations, accessor declarations as well as declarations for compare and
	 * equals operations
	 *
	 * @param tcClass
	 *            needed for naming
	 */
	private void generateHeaderFile(final GenClass tcClass, final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generate header for " + tcClass, 10);
		final STGroup templateGroup = getTemplateConfigurationProvider()
				.getTemplateGroup(CMoflonTemplateConstants.HEADER_FILE_GENERATOR);
		templateGroup.registerRenderer(String.class, new CMoflonStringRenderer());

		final StringBuilder contents = new StringBuilder();
		contents.append(getDateCommentCode());
		contents.append(getIncludeGuardCode(tcClass, templateGroup));
		contents.append(getIncludesCode(templateGroup, tcClass));
		contents.append(getConstantsDefinitionsCode(tcClass, templateGroup));
		contents.append(getMaxMatchCountDefinition(tcClass));
		contents.append(getGenerateDuplicatesDefinition(tcClass));
		contents.append(getMatchTypeDefinitionCode(templateGroup, tcClass));
		contents.append(getTypeMappingCode(templateGroup, tcClass));
		contents.append(getAllBuiltInMappings(tcClass));
		contents.append(getDefaultTypedefs(tcClass));
		contents.append(getUserDefinedTypedefs(tcClass));
		contents.append(getUnimplementedMethodsCode(templateGroup, tcClass));
		contents.append(getAccessorsCode(templateGroup, tcClass));
		contents.append(getComparisonFunctionsCode(templateGroup, tcClass));
		contents.append(getEqualsFunctionsCode(templateGroup, tcClass));
		contents.append(getHeaderTail(tcClass, templateGroup));
		subMon.worked(8);

		final String parentFolderForAlgorithm = getProjectRelativePathForAlgorithm(tcClass);
		final String outputFileName = parentFolderForAlgorithm + getAlgorithmBasename(tcClass) + ".h";
		final IFile headerFile = project.getFile(outputFileName);
		if (!headerFile.exists()) {
			WorkspaceHelper.addAllFolders(project, parentFolderForAlgorithm, subMon.split(1));
			WorkspaceHelper.addFile(project, outputFileName, contents.toString(), subMon.split(1));
		} else {
			headerFile.setContents(new ReaderInputStream(new StringReader(contents.toString())), true, true,
					subMon.split(2));
		}
	}

	private void generateSampleFiles(final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generate sample file", 4);
		{
			final String appConfConstantsFilename = WorkspaceHelper.GEN_FOLDER + "/" + APPLICATION_DEFAULTS_FILENAME;
			final List<String> lines = new ArrayList<>();
			lines.add("#define TOPOLOGYCONTROL_LINKS_HAVE_STATES");
			for (final GenClass tcAlgorithm : tcClasses) {
				lines.add(String.format("#define %s %d", getAlgorithmPreprocessorId(tcAlgorithm),
						getUniqueId(tcAlgorithm)));
				lines.add(String.format("#define %s %s", getAlgorithmImplementationFileId(tcAlgorithm),
						getAlgorithmImplementationFileName(tcAlgorithm)));
				lines.add("");
			}
			final String content = StringUtils.join(lines, nl());
			WorkspaceHelper.addFile(project, appConfConstantsFilename, content, subMon.split(2));
		}
		{
			final String makefileConfFilename = WorkspaceHelper.GEN_FOLDER + "/" + MAKE_CONF_FILENAME;
			final List<String> lines = new ArrayList<>();
			lines.add("ifndef TOPOLOGYCONTROL_PREDEFINED_IMPL_FILE");
			lines.add(String.format("#%sUncomment the line that corresponds to the current TC algorithm", idt2()));
			for (final GenClass tcAlgorithm : tcClasses) {
				lines.add(String.format("#%sTOPOLOGYCONTROL_PREDEFINED_IMPL_FILE=%s", //
						idt2(), getAlgorithmImplementationFileName(tcAlgorithm)));
			}
			lines.add("endif");
			final String content = StringUtils.join(lines, nl());
			WorkspaceHelper.addFile(project, makefileConfFilename, content, subMon.split(2));
		}
	}

	private int getUniqueId(final GenClass tcAlgorithm) {
		return 10000 + tcAlgorithm.getName().hashCode() % 10000;
	}

	private List<String> determineBuiltInTypes() {
		final List<String> builtInTypes = new ArrayList<String>();

		final EList<GenDataType> dataTypes = genModel.getEcoreGenPackage().getGenDataTypes();

		for (final GenDataType obj : dataTypes) {
			if (obj.getEcoreDataType() instanceof EDataType) {
				builtInTypes.add(obj.getEcoreDataType().getName());
			}
		}

		final EList<GenEnum> enums = genModel.getGenPackages().get(0).getGenEnums();
		for (final GenEnum eEnum : enums) {
			builtInTypes.add(eEnum.getName());
		}

		return builtInTypes;
	}

	private TemplateConfigurationProvider getTemplateConfigurationProvider() {
		return codeGenerationEngine.getTemplateConfigurationProvider();
	}

	/**
	 * This methods gets all Fields of a GenClass, including Reference fields as
	 * FieldAttributes usable in the StringTemplates
	 *
	 * @param genClass
	 *            the genClass
	 * @return a Collection of FieldAttributes
	 */
	private Collection<? extends FieldAttribute> getFields(final GenClass genClass) {
		final List<FieldAttribute> fields = new ArrayList<FieldAttribute>();
		final EClass clazz = genClass.getEcoreClass();

		for (final EAttribute att : clazz.getEAllAttributes()) {
			// TODO: it is assumed, that EAttributes are not of List Type
			fields.add(new FieldAttribute(new Type(isBuiltInType(clazz.getName()), clazz.getName()),
					new Type(isBuiltInType(att.getEAttributeType().getName()), att.getEAttributeType().getName()),
					att.getName(), false));
		}

		for (final EReference ref : clazz.getEAllReferences()) {
			fields.add(new FieldAttribute(new Type(isBuiltInType(clazz.getName()), clazz.getName()),
					new Type(isBuiltInType(ref.getEReferenceType().getName()), ref.getEReferenceType().getName()),
					ref.getName(), ref.getUpperBound() != 1));
		}

		return fields;
	}

	private String getProjectRelativePathForAlgorithm(final GenClass tcClass) {
		return WorkspaceHelper.GEN_FOLDER + "/";
	}

	private String getAlgorithmBasename(final GenClass algorithmClass) {
		return getComponentName() + "-" + project.getName() + "-" + algorithmClass.getName();
	}

	private String getProcessPreludeCode(final GenClass tcClass, final STGroup templateGroup) {
		return SourceFileGenerator.generateUpperPart(getComponentName(), tcClass, templateGroup, useHopCount(tcClass),
				getAlgorithmPreprocessorId(tcClass));
	}

	private String getAlgorithmPreprocessorId(final GenClass tcClass) {
		return ("COMPONENT_" + getComponentName() + "_" + project.getName() + "_" + tcClass.getName()).toUpperCase();
	}

	private String getAlgorithmImplementationFileId(final GenClass tcClass) {
		return ("COMPONENT_" + getComponentName() + "_IMPL_FILE_" + project.getName() + "_" + tcClass.getName())
				.toUpperCase();
	}

	private String getAlgorithmImplementationFileName(final GenClass tcClass) {
		return getComponentName() + "-" + project.getName() + "-" + tcClass.getName() + ".c";
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

	private String generateAlgorithmInvocationCode(final GenClass tcClass,
			final STGroup sourceFileGeneratorTemplateGroup) {
		final ST template = sourceFileGeneratorTemplateGroup
				.getInstanceOf(CMoflonTemplateConstants.SOURCE_FILE_PARAMETER_CONSTANT);
		final String algorithmInvocation = tcAlgorithmCallParameters.get(tcClass.getName());
		final String algorithmInvocationStatement = getClassPrefixForMethods(tcClass) + "run(&tc);";
		final StringBuilder algorithmInvocationCode = new StringBuilder();
		algorithmInvocationCode.append(getParameters(algorithmInvocation, tcClass, template));
		if (useEvalStatements.contains(tcClass.getName())) {
			final STGroup evalStatementGroup = getTemplateConfigurationProvider()
					.getTemplateGroup(CMoflonTemplateConstants.EVALUATION_STATEMENTS);
			final ST templateForBegin = evalStatementGroup
					.getInstanceOf(CMoflonTemplateConstants.EVALUATION_STATEMENTS_BEGIN);
			final ST templateForEnd = evalStatementGroup
					.getInstanceOf(CMoflonTemplateConstants.EVALUATION_STATEMETNS_END);

			algorithmInvocationCode.append(prependEachLineWithPrefix(templateForBegin.render(), idt2()));
			algorithmInvocationCode.append(idt2() + algorithmInvocationStatement).append(nl());
			algorithmInvocationCode.append(prependEachLineWithPrefix(templateForEnd.render(), idt2()));

		} else {
			algorithmInvocationCode.append(idt2() + algorithmInvocationStatement).append(nl());
		}
		final String result = algorithmInvocationCode.toString();
		return result;
	}

	private String prependEachLineWithPrefix(final String code, final String prefix) {
		return Arrays.asList(code.split(Pattern.quote(nl()))).stream().map(s -> prefix + s)
				.collect(Collectors.joining(nl()));
	}

	private String generateCleanupCode(final STGroup templateGroup) {
		final ST cleanupCallTemplate = templateGroup.getInstanceOf(CMoflonTemplateConstants.SOURCE_CLEANUP_CALL);
		final String cleanupCode = cleanupCallTemplate.render();
		return cleanupCode;
	}

	private String getPatternMatchingCode(final GenClass tcClass) {
		final StringBuilder allinjectedCode = new StringBuilder();
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
		final StringBuilder sb = new StringBuilder();
		if (!dropUnidirectionalEdgesOff.contains(tcClass.getName())) {
			sb.append(templateGroup.getInstanceOf(CMoflonTemplateConstants.SOURCE_DROP_UNIDIRECTIONAL_EDGES).render());
		}
		sb.append(SourceFileGenerator.generateClosingPart(templateGroup, useHopCount(tcClass)));
		return sb.toString();
	}

	/**
	 * Creates the code that is used by the hop-count calculation
	 *
	 * @param component
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
	 * Returns the prefix is placed in front of the method name when generating
	 * invocations of functions that represent methods
	 *
	 * @param tcClass
	 *            the surround class of the method
	 * @return
	 */
	private String getClassPrefixForMethods(final GenClass tcClass) {
		return tcClass.getName().substring(0, 1).toLowerCase() + tcClass.getName().substring(1) + "_";
	}

	/**
	 * Returns the C type to use when referring to the given topology control class
	 *
	 * @param tcClass
	 * @return
	 */
	private String getTypeName(final GenClass tcClass) {
		final String algorithmName = tcClass.getName();
		return getTypeName(algorithmName);
	}

	/**
	 * Returns the C type to use when referring to the given topology control class
	 *
	 * @param tcClass
	 * @return
	 */
	private String getTypeName(final String algorithmName) {
		return algorithmName.toUpperCase() + "_T";
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
		final ST init = templateGroup.getInstanceOf(CMoflonTemplateConstants.SOURCE_INIT);
		init.add("blocks", blockDeclarations);
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
		final ST cleanup = templateGroup.getInstanceOf(CMoflonTemplateConstants.SOURCE_CLEANUP);
		cleanup.add("duplicates", generateDuplicates);
		return cleanup.render();
	}

	/**
	 * Gets parameters for a method call inside the process structure. Parameters
	 * are either defined directly in the properties or listed in the constants
	 *
	 * @param property
	 *            the String CSV containing the parameters
	 * @param component
	 *            Component string (needed for constants naming)
	 * @param tcClass
	 *            algo string as well needed for naming
	 * @param template
	 *            the StringTemplate for the parameters
	 * @return a full String with comma separated parameters
	 */
	private String getParameters(final String property, final GenClass tcClass, final ST template) {
		final StringBuilder result = new StringBuilder();
		template.add("comp", getComponentName());
		template.add("algo", tcClass.getName());
		if (property == null) {
			return "";
		} else {
			final String[] params = property.split(",");
			for (final String p : params) {
				if (p.trim().contains(CMoflonProperties.PROPERTY_PREFIX_FOR_CONSTANTS)) {
					template.remove("name");
					template.add("name", p.trim().split(CMoflonProperties.PROPERTY_PREFIX_FOR_CONSTANTS)[1]);
					result.append(template.render() + ";").append(nl());
					result.append(idt2() + "");
				} else {
					result.append(p.trim() + ";").append(nl());
				}
			}
		}
		String returnValue = result.substring(0, result.lastIndexOf(";"));
		if (!returnValue.isEmpty()) {
			returnValue = idt2() + returnValue + ";" + nl();
		}
		return returnValue;
	}

	/**
	 * Returns the implementations of the patterns
	 *
	 * @param genClass
	 * @return returns the pattern matching code as string
	 */
	private String getPatternImplementationCode(final GenClass genClass) {
		// Produces pattern matching code
		final StringBuilder code = new StringBuilder();

		for (final Adapter adapter : genClass.getEcoreClass().eAdapters()) {
			if (adapter.isAdapterForType(SearchPlanAdapter.class)) {
				final SearchPlanAdapter searchPlanAdapter = (SearchPlanAdapter) adapter;
				final String patternType = searchPlanAdapter.getPatternType();
				final OperationSequenceCompiler operationSequenceCompiler = getTemplateConfigurationProvider()
						.getOperationSequenceCompiler(patternType);
				final TemplateInvocation template = searchPlanAdapter
						.prepareTemplateInvocation(operationSequenceCompiler, democlesImportManager);

				final ST st = getTemplateConfigurationProvider().getTemplateGroup(patternType)
						.getInstanceOf(template.getTemplateName());
				final Map<String, Object> attributes = template.getAttributes();

				for (final Entry<String, Object> entry : attributes.entrySet()) {
					st.add(entry.getKey(), entry.getValue());
				}
				final String result = st.render();
				// code.append(st.render());
				code.append(result);
				code.append(nl());
				code.append(nl());
			}
		}
		return code.length() > 0 ? code.toString() : null;
	}

	/**
	 * Checks whether the content refers to the tcAlgorithm with name name
	 *
	 * @param content
	 *            the generated code
	 * @param name
	 *            the name of the current tcAlgorithm
	 * @return true if code size reduction is activated an the content does not
	 *         refer to the current algorithm but another tcalgorithm
	 */
	private boolean isIrrelevantForAlgorithm(final GenClass genClass, final GenClass tcClass) {
		// Do not add code from other tc classes
		if (reduceCodeSize && isTrueSubtypeOfTCAlgorithmParentClass(genClass)) {
			return true;
		} else if (helperClasses.get(tcClass.getName()) != null
				&& helperClasses.get(tcClass.getName()).contains(genClass.getName())) {
			return false;
		} else {
			return true;
		}
	}

	private List<String> getBlockDeclarations(final List<GenClass> cachedConcreteClasses) {
		final List<String> blockDeclarations = new ArrayList<>();
		for (final GenClass genClass : cachedConcreteClasses) {
			for (final Adapter adapter : genClass.getEcoreClass().eAdapters()) {
				if (adapter.isAdapterForType(SearchPlanAdapter.class)) {
					final SearchPlanAdapter searchPlanAdapter = (SearchPlanAdapter) adapter;
					final String patternType = searchPlanAdapter.getPatternType();
					final OperationSequenceCompiler operationSequenceCompiler = getTemplateConfigurationProvider()
							.getOperationSequenceCompiler(patternType);
					final TemplateInvocation template = searchPlanAdapter
							.prepareTemplateInvocation(operationSequenceCompiler, democlesImportManager);

					final Map<String, Object> attributes = template.getAttributes();

					if (searchPlanAdapter.isMultiMatch()) {
						final String patternSignature = CompilerPatternBody.class.cast(attributes.get("body"))
								.getHeader().getName() + attributes.get("adornment");
						blockDeclarations.add(patternSignature);
					}
				}
			}
		}
		return blockDeclarations;
	}

	/**
	 * Generates the Method Body for an eOperation or returns a Default text if it
	 * is not properly implemented
	 *
	 * @param eOperation
	 * @return the code for the eop or MoflonUtil.DEFAULT_METHOD_BODY
	 */
	protected String getGeneratedMethodBody(final EOperation eOperation) {
		String generatedMethodBody = null;

		final AdapterResource cfResource = (AdapterResource) EcoreUtil.getExistingAdapter(eOperation,
				DemoclesMethodBodyHandler.CONTROL_FLOW_FILE_EXTENSION);
		if (cfResource != null) {
			final Scope scope = (Scope) cfResource.getContents().get(0);

			final STGroup group = getTemplateConfigurationProvider()
					.getTemplateGroup(CMoflonTemplateConstants.CONTROL_FLOW_GENERATOR);
			final ST template = group.getInstanceOf(getTemplateForControlFlowClass(scope));
			template.add("scope", scope);
			template.add("importManager", null);
			generatedMethodBody = template.render();
		}
		if (generatedMethodBody == null) {
			generatedMethodBody = MoflonUtil.DEFAULT_METHOD_BODY;
		}

		return generatedMethodBody;
	}

	private String getTemplateForControlFlowClass(final Scope scope) {
		return "/" + CMoflonTemplateConstants.CONTROL_FLOW_GENERATOR + "/" + scope.getClass().getSimpleName();
	}

	private String getIncludesCode(final STGroup templateGroup, final GenClass tcClass) {
		if (tcClass == null || !reduceCodeSize) {
			if (tcClass == null) {
				return (HeaderFileGenerator.generateIncludes(ToCoCoComponents.TOPOLOGYCONTROL, templateGroup));
			} else {
				return (HeaderFileGenerator.generateIncludes(ToCoCoComponents.TOPOLOGYCONTROL, templateGroup));
			}
		} else {
			return "#include \"cMoflon.h\" " + nl();
		}
	}

	private String getIncludeGuardCode(final GenClass tcClass, final STGroup templateGroup) {
		final ST definition = templateGroup.getInstanceOf(CMoflonTemplateConstants.HEADER_DEFINITION);
		if (!(reduceCodeSize && tcClass != null)) {
			final String algorithmName = tcClass.getName();
			definition.add("comp", getComponentName().toUpperCase());
			definition.add("algo", algorithmName.toUpperCase());
		}
		final String guardCode = definition.render();
		return guardCode;
	}

	private StringBuilder getConstantsDefinitionsCode(final GenClass tcClass, final STGroup templateGroup) {
		// TODO:fix for CMoflonReader with prefixes
		final StringBuilder constantsCode = new StringBuilder();
		if (constantsMapping.containsKey(tcClass.getName())) {
			for (final Entry<String, String> pair : constantsMapping.get(tcClass.getName()).entrySet()) {
				if (reduceCodeSize) {
					if (tcClass.getName().contentEquals(TC_INDEPENDANT)) {

						constantsCode.append(generateConstant(pair.getKey(), pair.getValue(), getComponentName(),
								tcClass, templateGroup));
					} else {
						constantsCode.append(generateConstant(pair.getKey(), pair.getValue(), getComponentName(),
								tcClass, templateGroup));
					}
				} else {
					constantsCode.append(generateConstant(pair.getKey(), pair.getValue(), getComponentName(), tcClass,
							templateGroup));
				}
			}
		}
		return constantsCode;
	}

	private String getMatchTypeDefinitionCode(final STGroup templateGroup, final GenClass tcClass) {
		if (reduceCodeSize) {
			if (tcClass == null) {
				final ST match = templateGroup.getInstanceOf(CMoflonTemplateConstants.HEADER_MATCH);
				final String matchTypeDef = match.render();
				return matchTypeDef;
			} else {
				return "";
			}
		} else {
			final ST match = templateGroup.getInstanceOf(CMoflonTemplateConstants.HEADER_MATCH);
			final String matchTypeDef = match.render();
			return matchTypeDef;
		}
	}

	private String getMaxMatchCountDefinition(final GenClass tcClass) {
		final StringBuilder mycontents = new StringBuilder();
		mycontents.append("#ifndef MAX_MATCH_COUNT").append(nl());
		mycontents.append(String.format("#define MAX_MATCH_COUNT %d%s", maximumMatchCount, nl()));
		mycontents.append("#endif").append(nl());
		if (reduceCodeSize) {
			if (tcClass.getName().contentEquals(TC_INDEPENDANT)) {
				return mycontents.toString();
			} else {
				return "";
			}
		} else {
			return mycontents.toString();
		}
	}

	private String getGenerateDuplicatesDefinition(final GenClass tcClass) {
		final StringBuilder mycontents = new StringBuilder();
		if (generateDuplicates.contains(tcClass.getName())) {
			mycontents.append("#ifndef GENERATE_DUPLICATES");
			mycontents.append(nl());
			mycontents.append("#define GENERATE_DUPLICATES").append(nl());
			mycontents.append("#endif").append(nl());
			mycontents.append("LIST(list_duplicates);" + nl()).append(nl());
			return mycontents.toString();
		} else {
			return "";
		}
	}

	private String getTypeMappingCode(final STGroup templateGroup, final GenClass tcClass) {
		final StringBuilder typeMappingCodeBuilder = new StringBuilder();
		if (reduceCodeSize) {
			if (tcClass == null) {
				final ST typeMappingTemplate = templateGroup.getInstanceOf(CMoflonTemplateConstants.HEADER_DEFINE);
				for (final Entry<String, String> pair : typeMappings.entrySet()) {
					typeMappingCodeBuilder
							.append(getTypeMappingCode(typeMappingTemplate, pair.getKey(), pair.getValue()));
					typeMappingCodeBuilder.append(nl());
				}
				return typeMappingCodeBuilder.toString();
			} else {
				return "";
			}
		} else {
			final ST typeMappingTemplate = templateGroup.getInstanceOf(CMoflonTemplateConstants.HEADER_DEFINE);
			for (final Entry<String, String> pair : typeMappings.entrySet()) {
				typeMappingCodeBuilder.append(getTypeMappingCode(typeMappingTemplate, pair.getKey(), pair.getValue()));
			}
			return typeMappingCodeBuilder.toString();
		}
	}

	private String getTypeMappingCode(final ST typeMappingTemplate, final String metamodelType, final Object cType) {
		typeMappingTemplate.remove("orig");
		typeMappingTemplate.remove("replaced");
		typeMappingTemplate.add("orig", metamodelType);
		typeMappingTemplate.add("replaced", cType);
		final String typeMappingCode = typeMappingTemplate.render();
		return typeMappingCode;
	}

	private String getUnimplementedMethodsCode(final STGroup stg, final GenClass tcClass) {
		// FIXME: currently all methods are classified as unimplemented, but maybe this
		// is needed as forward declaration
		final StringBuilder builder = new StringBuilder();
		if (reduceCodeSize) {
			if (tcClass == null) {
				final ST methoddecls = stg.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_METHOD_DECLARATION);
				methoddecls.add("methods", cachedMethodSignatures.get(TC_INDEPENDANT));
				builder.append(methoddecls.render());
			} else {
				final ST methoddecls = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_METHOD_DECLARATION);
				methoddecls.add("methods", cachedMethodSignatures.get(tcClass.getName()));
				builder.append(methoddecls.render());
			}
		} else {
			final ST methoddecls = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_METHOD_DECLARATION);
			for (final Map.Entry<String, List<MethodAttribute>> methods : cachedMethodSignatures.entrySet()) {
				methoddecls.add("methods", methods.getValue());
				builder.append(methoddecls.render());
				methoddecls.remove("methods");
			}

		}
		return builder.toString();
	}

	private String getAccessorsCode(final STGroup stg, final GenClass tcClass) {

		final StringBuilder builder = new StringBuilder();
		if (reduceCodeSize) {
			// TODO: filter more exactly
			if (tcClass == null) {
				final ST declarations = stg.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_DECLARATION);
				declarations.add("fields", cachedFields.get(TC_INDEPENDANT));
				builder.append(declarations.render());
			} else {
				final ST declarations = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_DECLARATION);
				declarations.add("fields", cachedFields.get(tcClass.getName()));
				builder.append(declarations.render());
			}
		} else {
			final ST declarations = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_DECLARATION);
			for (final Map.Entry<String, List<FieldAttribute>> fields : cachedFields.entrySet()) {
				declarations.add("fields", fields.getValue());
				builder.append(declarations.render());
				declarations.remove("fields");
			}

		}
		return builder.toString();
	}

	private String getComparisonFunctionsCode(final STGroup stg, final GenClass tcClass) {
		final StringBuilder builder = new StringBuilder();
		if (reduceCodeSize) {
			if (tcClass == null) {
				final ST compare = stg.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_COMPARE_DECLARATION);
				compare.add("types", getTypesFromGenModel(tcClass));
				builder.append(compare.render());
			} else {
				final ST compare = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_COMPARE_DECLARATION);
				compare.add("types", getTypesFromGenModel(tcClass));
				builder.append(compare.render());
			}
		} else {
			final ST compare = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_COMPARE_DECLARATION);
			compare.add("types", getTypesFromGenModel(tcClass));
			builder.append(compare.render());
		}
		return builder.toString();
	}

	private String getEqualsFunctionsCode(final STGroup stg, final GenClass tcClass) {
		final StringBuilder builder = new StringBuilder();
		if (reduceCodeSize) {
			if (tcClass == null) {
				final ST equals = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_EQUALS_DELCARATION);
				equals.add("types", getBuiltInTypes());
				builder.append(equals.render());
				equals.remove("types");
				equals.add("types", getTypesFromGenModel(tcClass));
				builder.append(equals.render());
			} else {
				final ST equals = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_EQUALS_DELCARATION);
				equals.add("types", getTypesFromGenModel(tcClass));
				builder.append(equals.render());
			}
		} else {
			final ST equals = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_EQUALS_DELCARATION);
			equals.add("types", getBuiltInTypes());
			builder.append(equals.render());
			equals.remove("types");
			equals.add("types", getTypesFromGenModel(tcClass));
			builder.append(equals.render());
		}
		return builder.toString();
	}

	private String getHeaderTail(final GenClass tcClass, final STGroup stg) {
		ST end;
		if (reduceCodeSize && tcClass == null) {
			end = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_CONSTANTS_END);
		} else {
			end = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_CONSTANTS_END);
			end.add("comp", getComponentName().toUpperCase());
			end.add("algo", tcClass.getName().toUpperCase());
		}
		return end.render() + nl();
	}

	/**
	 * Gets a List of Types to generate compare and equals methods for
	 *
	 * @return
	 */
	private List<Type> getTypesFromGenModel(final GenClass tcClass) {
		final List<Type> result = new ArrayList<Type>();
		// Add non built in Types
		for (final GenPackage p : genModel.getGenPackages()) {
			for (final GenClass clazz : p.getGenClasses()) {
				if (!reduceCodeSize) {
					result.add(new Type(false, clazz.getName()));
				} else {
					if (clazz.getName().contentEquals("Node") || clazz.getName().contentEquals("Link")
							|| clazz.getName().contentEquals(DEFAULT_TC_PARENT_CLASS_NAME)) {
						// Add to cMoflon Header
						if (tcClass == null) {
							result.add(new Type(false, clazz.getName()));
						} else {
							continue;
						}
					} else {
						if (tcClass != null) {
							if (isTrueSubtypeOfTCAlgorithmParentClass(clazz)
									&& clazz.getName().contentEquals(tcClass.getName())) {
								result.add(new Type(false, clazz.getName()));
							}
						} else {
							continue;
						}
					}
				}

			}
		}
		return result;
	}

	private List<Type> getBuiltInTypes() {
		final List<Type> result = new ArrayList<Type>();
		// Add built in Types
		for (final CMoflonBuiltInTypes t : CMoflonBuiltInTypes.values()) {
			result.add(new Type(true, t.name()));
		}
		return result;
	}

	/**
	 * Checks whether a type is a built in ECore Type or not
	 *
	 * @param t
	 *            the type to check
	 * @return true if it is built in else false
	 */
	private boolean isBuiltInType(final String t) {
		return builtInTypes.contains(t);
	}

	/**
	 * Obtains the method parameters for a given EOperation
	 *
	 * @param eOperation
	 *            the EOperation to obtain the parameters from
	 * @return the Parameters as String
	 */
	private String getParametersFromEcore(final EOperation eOperation) {
		final STGroup source = getTemplateConfigurationProvider()
				.getTemplateGroup(CMoflonTemplateConstants.SOURCE_FILE_GENERATOR);
		source.registerRenderer(String.class, new CMoflonStringRenderer());
		final ST template = source.getInstanceOf(CMoflonTemplateConstants.SOURCE_PARAMETER);
		final StringBuilder result = new StringBuilder();
		template.add("name", "this");
		template.add("type", new Type(isBuiltInType(eOperation.getEContainingClass().getName()),
				eOperation.getEContainingClass().getName()));
		result.append(template.render());
		final EList<EParameter> parameters = eOperation.getEParameters();
		for (final EParameter p : parameters) {
			template.remove("name");
			template.remove("type");
			template.add("name", p.getName());
			template.add("type", new Type(isBuiltInType(p.getEType().getName()), p.getEType().getName()));
			result.append(template.render());
		}
		return result.substring(0, result.lastIndexOf(","));
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
		final StringBuilder result = new StringBuilder();
		{
			final String projectRelativePath = "injection/custom-helpers.c";
			result.append(
					"// --- Begin of user-defined algorithm-independent helpers (Path: '" + projectRelativePath + "')")
					.append(nl());
			final IFile helperFile = project.getFile(projectRelativePath);
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
				WorkspaceHelper.addFile(project, projectRelativePath,
						"// Algorithm-independent helper definitions." + nl(), new NullProgressMonitor());
			}
			result.append("// --- End of user-defined algorithm-independent helpers").append(nl());
			result.append(nl());
		}
		{
			final String projectRelativePath = "injection/custom-helpers_" + tcClass.getName() + ".c";
			result.append("// --- Begin of user-defined helpers for " + tcClass.getName() + " (Path: '"
					+ projectRelativePath + "')").append(nl());
			final IFile helperFile = project.getFile(projectRelativePath);
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
				WorkspaceHelper.addFile(project, projectRelativePath,
						"// Helper definitions for '" + tcClass + "'." + nl(), new NullProgressMonitor());
			}
			result.append("// --- End of user-defined helpers for " + tcClass).append(nl2());
		}
		return result.toString();

	}

	private void createInjectionFolder() throws CoreException {
		WorkspaceHelper.addAllFolders(project, "injection", new NullProgressMonitor());
	}

	private String getDefaultTypedefs(final GenClass tcClass) throws CoreException {
		if (tcClass == null || !reduceCodeSize) {
			final StringBuilder result = new StringBuilder();
			result.append("// --- Begin of default cMoflon type definitions").append(nl());
			final String urlString = String.format("platform:/plugin/%s/resources/structs.c",
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
			result.append(nl()).append("// --- End of default cMoflon type definitions").append(nl2());
			return result.toString();
		} else {
			return "";
		}
	}

	private String getUserDefinedTypedefs(final GenClass tcClass) throws CoreException {
		final StringBuilder result = new StringBuilder();
		// Insert custom algorithm-independent typedefs
		{
			final String projectRelativePath = "injection/custom-typedefs.c";
			result.append("// --- Begin of user-defined algorithm-independent type definitions (Path: '"
					+ projectRelativePath + "')").append(nl());
			final IFile helperFile = project.getFile(projectRelativePath);
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
				WorkspaceHelper.addFile(project, projectRelativePath,
						"// Algorithm-independent type definitions." + nl(), new NullProgressMonitor());
			}
			result.append("// --- End of user-defined algorithm-independent type definitions").append(nl2());
			if (reduceCodeSize) {
				if (tcClass != null) {
					result.delete(0, result.length());
				}
			}
		}
		{
			// Insert algorithm specific typedefs
			if (tcClass == null) {
				return result.toString();
			}
			final StringBuilder algorithmSpecificContent = new StringBuilder();
			for (final GenClass algorithmClass : tcClasses) {
				final String algorithmName = algorithmClass.getName();
				final String projectRelativePath = "injection/custom-typedefs_" + algorithmName + ".c";
				algorithmSpecificContent.append("// --- Begin of user-defined type definitions for " + algorithmName
						+ "(Path: '" + projectRelativePath + "')").append(nl());
				final IFile helperFile = project.getFile(projectRelativePath);
				if (helperFile.exists()) {
					final InputStream stream = helperFile.getContents();
					try {
						algorithmSpecificContent.append(IOUtils.toString(stream));
					} catch (final IOException e) {
						throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()),
								"Failed to read user-defined helpers " + helperFile, e));
					} finally {
						IOUtils.closeQuietly(stream);
					}
				} else {
					createInjectionFolder();
					WorkspaceHelper.addFile(project, projectRelativePath,
							"// Type definitions for algorithm '" + algorithmName + "'." + nl(),
							new NullProgressMonitor());
				}
				algorithmSpecificContent.append("// --- End of user-defined type definitions for " + algorithmName)
						.append(nl2());
			}
			if (reduceCodeSize) {
				if (tcClass != null) {
					result.append(algorithmSpecificContent.toString());
				}
			} else {
				result.append(algorithmSpecificContent.toString());
			}
		}
		return result.toString();

	}

	/**
	 * Gets the name of the TCAlgorithm of a GenClass or the TC_INDEPENDANT constant
	 * if the GenClass is no TCAlgorithm
	 *
	 * @param genClass
	 * @return
	 */
	private String getTCAlgorithmNameForGenClass(final GenClass genClass) {
		return this.tcClasses.stream()//
				.filter(tcClass -> tcClass.getName().equals(genClass.getName())) //
				.map(tcClass -> tcClass.getName()).findAny().orElse(TC_INDEPENDANT);
	}

	/**
	 * Gets a String with Typedefs from EType to the C language Type.
	 */
	public String getAllBuiltInMappings(final GenClass tcClass) {
		final StringBuilder result = new StringBuilder();
		if (tcClass == null || !reduceCodeSize) {
			for (final CMoflonBuiltInTypes t : CMoflonBuiltInTypes.values()) {
				result.append("typedef " + CMoflonBuiltInTypes.getCType(t) + " " + t.name() + ";");
				result.append(nl()).append(nl());
			}
		}
		return result.toString();
	}

	public String generateConstant(final Object key, final Object value, final String component, final GenClass tcClass,
			final STGroup templateGroup) {
		final ST constantTemplate = templateGroup.getInstanceOf(CMoflonTemplateConstants.HEADER_CONSTANTS_DEFINITION);
		constantTemplate.add("comp", component);
		constantTemplate.add("algo", tcClass.getName());
		constantTemplate.add("name", key);
		constantTemplate.add("value", value);
		return constantTemplate.render();
	}

	private void readProperties(final IProject project) {
		final Properties cMoflonProperties;
		try {
			cMoflonProperties = CMoflonWorkspaceHelper.getCMoflonPropertiesFile(project);
		} catch (final CoreException e) {
			throw new IllegalArgumentException("Could not read cMoflon Properties." + e.toString(), e);
		}
		// TODO: check initialization of fields
		for (final Entry<Object, Object> entry : cMoflonProperties.entrySet()) {
			final String key = entry.getKey().toString();
			final String value = entry.getValue().toString();
			switch (key) {
			case CMoflonProperties.PROPERTY_TC_ALGORITHMS:
				final List<String> classNames = splitForNonEmptyValues(value, ",");
				classNames.stream()//
						.filter(c -> !isClassInGenmodel(c)) //
						.forEach(c -> reportMissingTopologyControlClass(c));
				if (classNames.isEmpty()) {
					LogUtils.warn(logger,
							"No topology control algorithm selected for code generation. Please specify property %s in %s",
							CMoflonProperties.PROPERTY_TC_ALGORITHMS, CMoflonProperties.CMOFLON_PROPERTIES_FILENAME);
				}
				this.tcClasses.addAll(classNames.stream()//
						.filter(c -> isClassInGenmodel(c))//
						.map(c -> getGenclassByName(c).get())//
						.collect(Collectors.toList()));

				tcClasses.stream().forEach(tcClass -> initializeHelperClassesIfNecessary(tcClass));

				continue;
			case CMoflonProperties.PROPERTY_PM_MAX_MATCH_COUNT:
				maximumMatchCount = Integer.parseInt(value);
				continue;
			case CMoflonProperties.PROPERTY_REDUCE_CODE_SIZE:
				reduceCodeSize = Boolean.parseBoolean(value);
				continue;
			}
			if (key.startsWith(CMoflonProperties.PROPERTY_PREFIX_TOPOLOGYCONTROL)) {
				readTopologyControlRelatedProperty(key, value);
			} else if (key.startsWith(CMoflonProperties.PROPERTY_PREFIX_FOR_TYPE_MAPPINGS)) {
				typeMappings.put(key.replaceAll(CMoflonProperties.PROPERTY_PREFIX_FOR_TYPE_MAPPINGS, ""), value.trim());
			}
		}
	}

	private void readTopologyControlRelatedProperty(final String key, final String value) {
		{
			// TCmethod.
			String copyString = key;
			copyString = copyString.replaceFirst(CMoflonProperties.PROPERTY_PREFIX_TOPOLOGYCONTROL, "");
			// Index of dot before option
			final int dotIndex = copyString.indexOf(".");
			final String tcAlgorithm = copyString.substring(0, dotIndex);
			final String option = (copyString.substring(dotIndex)).trim();
			switch (option) {
			case CMoflonProperties.PROPERTY_POSTFIX_PARAMETERS:
				tcAlgorithmCallParameters.put(tcAlgorithm, value);
				return;
			case CMoflonProperties.PROPERTY_POSTFIX_CONSTANTS:
				Arrays.asList(value.split(",")).stream().map(s -> s.trim()).forEach(kv -> {
					final List<String> list = Arrays.asList(kv.split("=")).stream().map(s -> s.trim())
							.collect(Collectors.toList());
					if (list.size() != 2) {
						return;
					} else {
						if (!constantsMapping.containsKey(tcAlgorithm)) {
							// Initialize map on first parameter
							final Map<String, String> constantsForAlgorithm = new HashMap<>();
							constantsMapping.put(tcAlgorithm, constantsForAlgorithm);
						}
						final Map<String, String> constantsForAlgorithm = constantsMapping.get(tcAlgorithm);
						constantsForAlgorithm.put(list.get(0).replaceFirst("const-", ""), list.get(1));
						constantsMapping.put(tcAlgorithm, constantsForAlgorithm);
					}
				});
				return;
			case CMoflonProperties.PROPERTY_POSTFIX_HELPERCLASSES:
				if (this.helperClasses.containsKey(tcAlgorithm)) {
					final List<String> helperClassesList = splitForNonEmptyValues(value, ",");
					this.helperClasses.get(tcAlgorithm).addAll(helperClassesList);
				}
				return;
			case CMoflonProperties.PROPERTY_POSTFIX_DROP_UNIDIRECTIONAL_EDGES:
				if (!Boolean.parseBoolean(value)) {
					dropUnidirectionalEdgesOff.add(tcAlgorithm);
				}
				return;
			case CMoflonProperties.PROPERTY_POSTFIX_INCLUDE_EVALUATION_STATEMENTS:
				if (Boolean.parseBoolean(value)) {
					useEvalStatements.add(tcAlgorithm);
				}
				return;
			case CMoflonProperties.PROPERTY_POSTFIX_USE_HOPCOUNT:
				if (Boolean.parseBoolean(value)) {
					useHopCountProcess.add(tcAlgorithm);
				}
				return;
			case CMoflonProperties.PROPERTY_POSTFIX_DUPLICATE_EDGES:
				if (Boolean.parseBoolean(value)) {
					generateDuplicates.add(tcAlgorithm);
				}
				return;
			}
		}
	}

	private List<String> splitForNonEmptyValues(final String commaSeparatedValues, final String separator) {
		return Arrays.asList(commaSeparatedValues.split(separator)).stream().map(s -> s.trim())
				.filter(s -> !s.isEmpty()).collect(Collectors.toList());
	}

	private void initializeHelperClassesIfNecessary(final GenClass tcClass) {
		final String algorithmName = tcClass.getName();
		if (!helperClasses.containsKey(algorithmName)) {
			final List<String> helperClassesList = new ArrayList<>();
			helperClassesList.add("Node");
			helperClassesList.add("Link");
			helperClassesList.add(DEFAULT_TC_PARENT_CLASS_NAME);
			helperClasses.put(algorithmName, helperClassesList);
		}
	}

	public static String nl() {
		return "\n";
	}

	public static String nl2() {
		return nl() + nl();
	}

	private static String idt() {
		return "  ";
	}

	private static String idt2() {
		return idt() + idt();
	}
}

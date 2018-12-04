package org.cmoflon.ide.core.runtime.codegeneration;

import static org.cmoflon.ide.core.utilities.FormattingUtils.idt;
import static org.cmoflon.ide.core.utilities.FormattingUtils.idt2;
import static org.cmoflon.ide.core.utilities.FormattingUtils.nl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.codegeneration.file.AbstractFileGenerator;
import org.cmoflon.ide.core.runtime.codegeneration.file.CMoflonCommonHeaderGenerator;
import org.cmoflon.ide.core.runtime.codegeneration.file.TCAlgorithmCodeGenerator;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonStringRenderer;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.FieldAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.MethodAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.Type;
import org.cmoflon.ide.core.utilities.CMoflonProperties;
import org.cmoflon.ide.core.utilities.CMoflonWorkspaceHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.codegen.ecore.generator.GeneratorAdapterFactory.Descriptor;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenOperation;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
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
public class CMoflonCodeGenerator extends AbstractFileGenerator {

	public static final String PARAMETER_SEPARATOR = ",";
	private static final String KEY_VALUE_SEPARATOR = "=";
	private static final Logger logger = Logger.getLogger(CMoflonCodeGenerator.class);

	/**
	 * The Democles import manager to be used during the build process
	 */
	private ImportManager democlesImportManager;

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

	private final CMoflonCommonHeaderGenerator commonHeaderFileGenerator;
	private final TCAlgorithmCodeGenerator topologyControlComponentCodeGenerator;

	public CMoflonCodeGenerator(final Resource ecore, final IProject project, final GenModel genModel,
			final Descriptor codeGenerationEngine) {
		super(project, genModel, ((DemoclesGeneratorAdapterFactory) codeGenerationEngine),
				new BuildProcessConfigurationProvider(genModel));

		readProperties(project);

		this.commonHeaderFileGenerator = new CMoflonCommonHeaderGenerator(this.getProject(), this.getGenModel(),
				this.getCodeGenerationEngine(), this.getBuildProcessConfigurationProvider());
		this.topologyControlComponentCodeGenerator = new TCAlgorithmCodeGenerator(this.getProject(), this.getGenModel(),
				this.getCodeGenerationEngine(), this.getBuildProcessConfigurationProvider());
	}

	public IStatus generateCode(final IProgressMonitor monitor) throws CoreException {
		final List<GenClass> tcClasses = this.getBuildProcessConfigurationProvider().getTcClasses();
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generating code", tcClasses.size() * 10 + 20);
		final MultiStatus codeGenerationResult = new MultiStatus(WorkspaceHelper.getPluginId(getClass()), 0,
				"Code generation failed", null);

		initializeCaches();

		this.commonHeaderFileGenerator.generateCMoflonHeader(subMon.split(10));

		for (final GenClass tcClass : tcClasses) {
			codeGenerationResult.add(topologyControlComponentCodeGenerator.generateCodeForAlgorithm(tcClass,
					codeGenerationResult, subMon.split(10)));
		}

		generateSampleFiles(subMon.split(10));

		this.getBuildProcessConfigurationProvider().resetCaches();

		return codeGenerationResult;
	}

	private final boolean isClassInGenmodel(final String className) {
		return getGenclassByName(className).isPresent();
	}

	private Optional<? extends GenClass> getGenclassByName(final String className) {
		for (final GenPackage genPackage : getGenModel().getGenPackages()) {
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
		getGenModel().getAllGenPackagesWithClassifiers()
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

	private void initializeCaches() {
		initializeCachedMetamodelElementLists();
		initializeCachedPatternMatchingCode();
		final List<GenClass> cachedConcreteClasses = getBuildProcessConfigurationProvider().getCachedConcreteClasses();
		final List<String> blockDeclarations = getBuildProcessConfigurationProvider().getBlockDeclarations();
		blockDeclarations.addAll(getBlockDeclarations(cachedConcreteClasses));
	}

	private void initializeCachedPatternMatchingCode() {
		final Map<GenClass, StringBuilder> cachedPatternMatchingCode = getBuildProcessConfigurationProvider()
				.getCachedPatternMatchingCode();
		getGenModel().getGenPackages()
				.forEach(genPackage -> genPackage.getGenClasses().stream()
						.filter(genClass -> isTrueSubtypeOfTCAlgorithmParentClass(genClass))//
						.forEach(genClass -> {
							cachedPatternMatchingCode.put(genClass, new StringBuilder());
						}));
		cachedPatternMatchingCode.put(TC_INDEPENDENT_CLASS, new StringBuilder());

		for (final GenPackage genPackage : getGenModel().getGenPackages()) {
			for (final GenClass genClass : genPackage.getGenClasses()) {
				if (!genClass.isAbstract()) {
					for (final GenOperation genOperation : genClass.getGenOperations()) {
						registerPatternMatchingCode(genClass, genOperation);
					}
				}
			}
		}

	}

	private void registerPatternMatchingCode(final GenClass genClass, final GenOperation genOperation) {
		final String generatedMethodBody = getGeneratedMethodBody(genOperation.getEcoreOperation());
		if (!generatedMethodBody.equals(MoflonUtil.DEFAULT_METHOD_BODY)) {
			final StringBuilder intermediateBuffer = new StringBuilder();
			LogUtils.info(logger, "Generate method body for '%s::%s'", genClass.getName(), genOperation.getName());
			intermediateBuffer.append(nl());
			intermediateBuffer.append("static ");
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
			for (final String line : generatedMethodBody.trim().replaceAll("\\r", "").split(Pattern.quote(nl()))) {
				intermediateBuffer.append(idt() + line).append(nl());
			}
			intermediateBuffer.append(nl() + "}").append(nl());

			final Map<GenClass, StringBuilder> cachedPatternMatchingCode = this.getBuildProcessConfigurationProvider()
					.getCachedPatternMatchingCode();
			final StringBuilder cachedPatternMatchingCodeForTCAlgorithm = cachedPatternMatchingCode.get(genClass);
			cachedPatternMatchingCodeForTCAlgorithm.append(intermediateBuffer.toString());
		} else {
			LogUtils.info(logger, "Skip method body due to missing specification: '%s::%s'", genClass.getName(),
					genOperation.getName());
		}
	}

	private void initializeCachedMetamodelElementLists() {

		final Map<GenClass, List<MethodAttribute>> cachedMethodSignatures = this.getBuildProcessConfigurationProvider()
				.getCachedMethodSignatures();
		final Map<GenClass, List<FieldAttribute>> cachedFields = this.getBuildProcessConfigurationProvider()
				.getCachedFields();
		final List<GenClass> cachedConcreteClasses = this.getBuildProcessConfigurationProvider()
				.getCachedConcreteClasses();
		getGenModel().getGenPackages()
				.forEach(genPackage -> genPackage.getGenClasses().stream()
						.filter(genClass -> isTrueSubtypeOfTCAlgorithmParentClass(genClass))//
						.forEach(genClass -> {
							cachedMethodSignatures.put(genClass, new ArrayList<MethodAttribute>());
							cachedFields.put(genClass, new ArrayList<>());
						}));
		cachedMethodSignatures.put(TC_INDEPENDENT_CLASS, new ArrayList<MethodAttribute>());
		cachedFields.put(TC_INDEPENDENT_CLASS, new ArrayList<>());
		getGenModel().getGenPackages().forEach(genPackage -> genPackage.getGenClasses().forEach(genClass -> {
			if (!genClass.isAbstract()) {
				cachedConcreteClasses.add(genClass);
				List<FieldAttribute> fields;
				if (isTrueSubtypeOfTCAlgorithmParentClass(genClass)) {
					fields = cachedFields.get(genClass);
					extractFieldsAndMethodsFromGenClass(fields, genClass);
				} else {
					final Map<GenClass, List<String>> helperClasses = this.getBuildProcessConfigurationProvider()
							.getHelperClasses();
					helperClasses.entrySet().stream().filter(entry -> entry.getValue().contains(genClass.getName()))
							.forEach(entry -> {
								extractFieldsAndMethodsFromGenClass(cachedFields.get(entry.getKey()), genClass);
							});
					if (helperClasses.values().stream().noneMatch(l -> l.contains(genClass.getName()))) {
						fields = cachedFields.get(TC_INDEPENDENT_CLASS);
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
			final Map<GenClass, List<MethodAttribute>> cachedMethodSignatures = this
					.getBuildProcessConfigurationProvider().getCachedMethodSignatures();
			if (isTrueSubtypeOfTCAlgorithmParentClass(genOperation.getGenClass())) {
				methodDeclarations = cachedMethodSignatures.get(genClass);
			} else {
				methodDeclarations = cachedMethodSignatures.get(TC_INDEPENDENT_CLASS);
			}
			methodDeclarations.add(new MethodAttribute(new Type(isBuiltInType(genClassName), genClassName),
					new Type(isOperationTypeBuiltIn, operationTypeName), genOperation.getName(),
					getParametersFromEcore(genOperation.getEcoreOperation())));
		});
	}

	private void generateSampleFiles(final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generate sample file", 4);
		generatePreprocessorConstantsFile(subMon);
		generateMakefileConstantsFile(subMon);
	}

	/**
	 * Generates the file {@link #MAKE_CONF_FILENAME}.
	 *
	 * @param subMon
	 *                   the progress monitor. Shall advance two work units
	 * @throws CoreException
	 *                           if writing the file fails
	 */
	private void generateMakefileConstantsFile(final SubMonitor subMon) throws CoreException {
		final String makefileConfFilename = WorkspaceHelper.GEN_FOLDER + "/" + MAKE_CONF_FILENAME;
		final List<String> lines = new ArrayList<>();
		lines.add("ifndef TOPOLOGYCONTROL_PREDEFINED_IMPL_FILE");
		lines.add(String.format("#%sUncomment the line that corresponds to the current TC algorithm", idt2()));
		final List<GenClass> tcClasses = this.getBuildProcessConfigurationProvider().getTcClasses();
		for (final GenClass tcAlgorithm : tcClasses) {
			lines.add(String.format("#%sTOPOLOGYCONTROL_PREDEFINED_IMPL_FILE=%s", //
					idt2(), getAlgorithmImplementationFileName(tcAlgorithm)));
		}
		lines.add("endif");
		final String content = StringUtils.join(lines, nl());
		WorkspaceHelper.addFile(getProject(), makefileConfFilename, content, subMon.split(2));
	}

	/**
	 * Generates the file {@link #APPLICATION_DEFAULTS_FILENAME}.
	 *
	 * @param subMon
	 *                   the progress monitor. Shall advance two work units
	 * @throws CoreException
	 *                           if writing the file fails
	 */
	private void generatePreprocessorConstantsFile(final SubMonitor subMon) throws CoreException {
		final String appConfConstantsFilename = WorkspaceHelper.GEN_FOLDER + "/" + APPLICATION_DEFAULTS_FILENAME;
		final List<String> lines = new ArrayList<>();
		lines.add("#define TOPOLOGYCONTROL_LINKS_HAVE_STATES");
		final List<GenClass> tcClasses = this.getBuildProcessConfigurationProvider().getTcClasses();
		for (final GenClass tcAlgorithm : tcClasses) {
			lines.add(
					String.format("#define %s %d", getAlgorithmPreprocessorId(tcAlgorithm), getUniqueId(tcAlgorithm)));
			lines.add(String.format("#define %s %s", getAlgorithmImplementationFileId(tcAlgorithm),
					getAlgorithmImplementationFileName(tcAlgorithm)));
			lines.add("");
		}
		final String content = StringUtils.join(lines, nl());
		WorkspaceHelper.addFile(getProject(), appConfConstantsFilename, content, subMon.split(2));
	}

	private int getUniqueId(final GenClass tcAlgorithm) {
		return 10000 + tcAlgorithm.getName().hashCode() % 10000;
	}

	/**
	 * This methods gets all Fields of a GenClass, including Reference fields as
	 * FieldAttributes usable in the StringTemplates
	 *
	 * @param genClass
	 *                     the genClass
	 * @return a Collection of FieldAttributes
	 */
	private Collection<? extends FieldAttribute> getFields(final GenClass genClass) {
		final List<FieldAttribute> fields = new ArrayList<FieldAttribute>();
		final EClass clazz = genClass.getEcoreClass();

		for (final EAttribute att : clazz.getEAllAttributes()) {
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

	private String getAlgorithmImplementationFileId(final GenClass tcClass) {
		return ("COMPONENT_" + getComponentName() + "_IMPL_FILE_" + getProject().getName() + "_" + tcClass.getName())
				.toUpperCase();
	}

	private String getAlgorithmImplementationFileName(final GenClass tcClass) {
		return getComponentName() + "-" + getProject().getName() + "-" + tcClass.getName() + ".c";
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
	private String getGeneratedMethodBody(final EOperation eOperation) {
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

	/**
	 * Checks whether a type is a built in ECore Type or not
	 *
	 * @param t
	 *              the type to check
	 * @return true if it is built in else false
	 */
	private boolean isBuiltInType(final String t) {
		return this.getBuildProcessConfigurationProvider().getBuiltInTypes().contains(t);
	}

	/**
	 * Obtains the method parameters for a given EOperation
	 *
	 * @param eOperation
	 *                       the EOperation to obtain the parameters from
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
		return result.substring(0, result.lastIndexOf(PARAMETER_SEPARATOR));
	}

	private void readProperties(final IProject project) {
		final Properties cMoflonProperties;
		try {
			cMoflonProperties = CMoflonWorkspaceHelper.getCMoflonPropertiesFile(project);
		} catch (final CoreException e) {
			throw new IllegalArgumentException("Could not read cMoflon Properties." + e.toString(), e);
		}
		for (final Entry<Object, Object> entry : cMoflonProperties.entrySet()) {
			final String key = entry.getKey().toString();
			final String value = entry.getValue().toString();
			final List<GenClass> tcClasses = this.getBuildProcessConfigurationProvider().getTcClasses();
			switch (key) {
			case CMoflonProperties.PROPERTY_TC_ALGORITHMS:
				final List<String> classNames = splitForNonEmptyValues(value, PARAMETER_SEPARATOR);
				if (classNames.isEmpty()) {
					LogUtils.warn(logger,
							"No topology control algorithm selected for code generation. Please specify property %s in %s",
							CMoflonProperties.PROPERTY_TC_ALGORITHMS, CMoflonProperties.CMOFLON_PROPERTIES_FILENAME);
				}
				classNames.stream()//
						.filter(c -> !isClassInGenmodel(c)) //
						.forEach(c -> reportMissingTopologyControlClass(c));

				tcClasses.addAll(classNames.stream()//
						.filter(c -> isClassInGenmodel(c))//
						.map(c -> getGenclassByName(c).get())//
						.collect(Collectors.toList()));

				tcClasses.stream().forEach(tcClass -> initializeHelperClassesIfNecessary(tcClass));

				continue;
			case CMoflonProperties.PROPERTY_PM_MAX_MATCH_COUNT:
				this.getBuildProcessConfigurationProvider().setMaximumMatchCount(Integer.parseInt(value));
				continue;
			}
			if (key.startsWith(CMoflonProperties.PROPERTY_PREFIX_TOPOLOGYCONTROL)) {
				readTopologyControlRelatedProperty(key, value);
			} else if (key.startsWith(CMoflonProperties.PROPERTY_PREFIX_FOR_TYPE_MAPPINGS)) {
				getBuildProcessConfigurationProvider().getTypeMappings()
						.put(key.replaceAll(CMoflonProperties.PROPERTY_PREFIX_FOR_TYPE_MAPPINGS, ""), value.trim());
			}
		}
	}

	private void readTopologyControlRelatedProperty(final String key, final String value) {
		final String copyString = key.replaceFirst(CMoflonProperties.PROPERTY_PREFIX_TOPOLOGYCONTROL, "");

		final int indexOfDotBeforeOption = copyString.indexOf(".");

		final String tcAlgorithm = copyString.substring(0, indexOfDotBeforeOption);
		final Optional<? extends GenClass> maybeTcClass = getGenclassByName(tcAlgorithm);
		if (!maybeTcClass.isPresent()) {
			logger.warn(String.format("Missing class %s in metamodel. Ignoring all related options", tcAlgorithm));
			return;
		}
		final GenClass tcClass = maybeTcClass.get();

		final String option = (copyString.substring(indexOfDotBeforeOption)).trim();
		switch (option) {
		case CMoflonProperties.PROPERTY_POSTFIX_PARAMETERS:
			this.getBuildProcessConfigurationProvider().getTcAlgorithmCallParameters().put(tcClass, value);
			return;
		case CMoflonProperties.PROPERTY_POSTFIX_CONSTANTS:
			registerConstants(value, tcClass, true);
			return;
		case CMoflonProperties.PROPERTY_POSTFIX_HELPERCLASSES:
			final Map<GenClass, List<String>> helperClasses = this.getBuildProcessConfigurationProvider()
					.getHelperClasses();
			if (helperClasses.containsKey(tcClass)) {
				final List<String> helperClassesList = splitForNonEmptyValues(value, PARAMETER_SEPARATOR);
				helperClasses.get(tcClass).addAll(helperClassesList);
			}
			return;
		case CMoflonProperties.PROPERTY_POSTFIX_DROP_UNIDIRECTIONAL_EDGES:
			final Set<String> dropUnidirectionalEdgesOff = this.getBuildProcessConfigurationProvider()
					.getDropUnidirectionalEdgesOff();
			if (!Boolean.parseBoolean(value)) {
				dropUnidirectionalEdgesOff.add(tcAlgorithm);
			}
			return;
		case CMoflonProperties.PROPERTY_POSTFIX_INCLUDE_EVALUATION_STATEMENTS:
			if (Boolean.parseBoolean(value)) {
				final Set<String> useEvalStatements = this.getBuildProcessConfigurationProvider()
						.getUseEvalStatements();
				useEvalStatements.add(tcAlgorithm);
			}
			return;
		case CMoflonProperties.PROPERTY_POSTFIX_USE_HOPCOUNT:
			if (Boolean.parseBoolean(value)) {
				final Set<String> useHopCountProcess = this.getBuildProcessConfigurationProvider()
						.getUseHopCountProcess();
				useHopCountProcess.add(tcAlgorithm);
				registerConstants(CMoflonProperties.DEFAULT_HOPCOUNT_CONSTANTS, tcClass, false);
			}
			return;
		case CMoflonProperties.PROPERTY_POSTFIX_DUPLICATE_EDGES:
			if (Boolean.parseBoolean(value)) {
				final Set<String> generateDuplicates = this.getBuildProcessConfigurationProvider()
						.getGenerateDuplicates();
				generateDuplicates.add(tcAlgorithm);
			}
			return;
		}
	}

	private void registerConstants(final String parameterValueList, final GenClass tcClass, final boolean override) {
		splitForNonEmptyValues(parameterValueList, PARAMETER_SEPARATOR).stream().map(s -> s.trim()).forEach(kv -> {
			final List<String> list = splitForNonEmptyValues(kv, KEY_VALUE_SEPARATOR);
			if (list.size() != 2) {
				logger.warn(String.format("Cannot parse the following entry: '%s'", kv));
				return;
			} else {
				final Map<String, String> constantsForAlgorithm = initializeConstantsMappingIfNecessary(tcClass);
				final String constantName = list.get(0).replaceFirst("const-", "");
				final String constantValue = list.get(1);
				if (override || !constantsForAlgorithm.containsKey(constantName)) {
					constantsForAlgorithm.put(constantName, constantValue);
				}
			}
		});
	}

	private Map<String, String> initializeConstantsMappingIfNecessary(final GenClass tcClass) {
		final Map<GenClass, Map<String, String>> constantsMapping = this.getBuildProcessConfigurationProvider()
				.getConstantsMapping();
		if (!constantsMapping.containsKey(tcClass)) {
			final Map<String, String> constantsForAlgorithm = new HashMap<>();
			constantsMapping.put(tcClass, constantsForAlgorithm);
			return constantsForAlgorithm;
		} else {
			return constantsMapping.get(tcClass);
		}
	}

	private List<String> splitForNonEmptyValues(final String commaSeparatedValues, final String separator) {
		return Arrays.asList(commaSeparatedValues.split(separator)).stream().map(s -> s.trim())
				.filter(s -> !s.isEmpty()).collect(Collectors.toList());
	}

	private void initializeHelperClassesIfNecessary(final GenClass tcClass) {
		final Map<GenClass, List<String>> helperClasses = this.getBuildProcessConfigurationProvider()
				.getHelperClasses();
		if (!helperClasses.containsKey(tcClass)) {
			final List<String> helperClassesList = new ArrayList<>();
			helperClassesList.add("Node");
			helperClassesList.add("Link");
			helperClassesList.add(DEFAULT_TC_PARENT_CLASS_NAME);
			helperClasses.put(tcClass, helperClassesList);
		}
	}
}

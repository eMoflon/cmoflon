package org.cmoflon.ide.core.runtime.codegeneration;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonEcoreModelAdaptor;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonStringRenderer;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.ControlFlowModelAdaptor;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.PatternMatcherModelAdaptor;
import org.eclipse.emf.codegen.ecore.genmodel.GenBase;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EModelElement;
import org.gervarro.democles.codegen.GeneratorOperation;
import org.gervarro.democles.codegen.ImportManager;
import org.gervarro.democles.codegen.OperationSequenceCompiler;
import org.gervarro.democles.codegen.PatternInvocationConstraintTemplateProvider;
import org.gervarro.democles.codegen.emf.EMFTemplateProvider;
import org.gervarro.democles.codegen.emf.EcoreToGenModelConverter;
import org.gervarro.democles.codegen.stringtemplate.AdornmentHandler;
import org.gervarro.democles.codegen.stringtemplate.FullyQualifiedName;
import org.gervarro.democles.codegen.stringtemplate.ImportHandler;
import org.gervarro.democles.codegen.stringtemplate.emf.EcoreModelAdaptor;
import org.gervarro.democles.codegen.stringtemplate.emf.GenModelAdaptor;
import org.gervarro.democles.common.runtime.VariableRuntime;
import org.gervarro.democles.constraint.emf.EMFVariable;
import org.gervarro.democles.relational.RelationalConstraintTemplateProvider;
import org.gervarro.democles.specification.ConstraintVariable;
import org.moflon.compiler.sdm.democles.AssignmentTemplateProvider;
import org.moflon.compiler.sdm.democles.AttributeAssignmentTemplateProvider;
import org.moflon.compiler.sdm.democles.BindingAndBlackTemplateProvider;
import org.moflon.compiler.sdm.democles.DefaultTemplateConfiguration;
import org.moflon.compiler.sdm.democles.EMFGreenTemplateProvider;
import org.moflon.compiler.sdm.democles.EMFRedTemplateProvider;
import org.moflon.compiler.sdm.democles.TemplateConfigurationProvider;
import org.moflon.compiler.sdm.democles.stringtemplate.LoggingSTErrorListener;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.sdm.runtime.democles.CFNode;
import org.moflon.sdm.runtime.democles.CFVariable;
import org.moflon.sdm.runtime.democles.PatternInvocation;
import org.moflon.sdm.runtime.democles.VariableReference;
import org.stringtemplate.v4.STGroup;

/**
 * Mimics behavior of {@link DefaultTemplateConfiguration}. Includes the changed
 * StringTemplates as well as the {@link CMoflonStringRenderer}
 *
 * @author David Giessing
 *
 */
public class CMoflonTemplateConfiguration implements TemplateConfigurationProvider {
	/**
	 * String template group prefix for control flow
	 */
	public static final String CONTROL_FLOW_GENERATOR = "ControlFlowGenerator";

	/**
	 * String template group prefix for header file structure of the topology
	 * control component
	 */
	public static final String HEADER_FILE_GENERATOR = "HeaderFileGenerator";

	/**
	 * String template group prefix for cMoflon header file structure of the
	 * topology control component
	 */
	public static final String CMOFLON_HEADER_FILE_GENERATOR = "CMoflonHeaderFileGenerator";

	/**
	 * String template group prefix for source file structure of the topology
	 * control component
	 */
	public static final String SOURCE_FILE_GENERATOR = "SourceFileGenerator";

	/**
	 * String template group prefix for evaluation statements
	 */
	public static final String EVALUATION_STATEMENTS = "EvaluationStatements";

	private final HashMap<String, STGroup> templates = new HashMap<String, STGroup>();

	protected final HashMap<String, OperationSequenceCompiler> operationSequenceCompilers = new HashMap<String, OperationSequenceCompiler>();

	static final String EVALUATION_STATEMETNS_END = "/" + EVALUATION_STATEMENTS + "/EvaluationStatementEnd";

	static final String EVALUATION_STATEMENTS_BEGIN = "/" + EVALUATION_STATEMENTS + "/EvaluationStatementBegin";

	private static final Logger logger = Logger.getLogger(CMoflonTemplateConfiguration.class);

	public CMoflonTemplateConfiguration(GenModel genModel) {
		final EcoreToGenModelConverter ecoreToGenModelConverter = new EcoreToGenModelConverter(genModel);
		final EcoreModelAdaptor ecoreModelAdaptor = new CMoflonEcoreModelAdaptor(ecoreToGenModelConverter);

		final STGroup controlFlowTemplateGroup = createControlFlowTemplates();
		controlFlowTemplateGroup.registerRenderer(String.class, new CMoflonStringRenderer());
		templates.put(CONTROL_FLOW_GENERATOR, controlFlowTemplateGroup);

		final STGroup headerGroup = new STGroup();
		headerGroup.setListener(new LoggingSTErrorListener(logger));
		headerGroup.loadGroupFile("/" + HEADER_FILE_GENERATOR + "/", getTemplateUriPrefix() + "/header/header.stg");
		templates.put(HEADER_FILE_GENERATOR, headerGroup);

		final STGroup cMoflonHeaderGroup = new STGroup();
		cMoflonHeaderGroup.setListener(new LoggingSTErrorListener(logger));
		cMoflonHeaderGroup.loadGroupFile("/" + CMOFLON_HEADER_FILE_GENERATOR + "/",
				getTemplateUriPrefix() + "/header/cMoflonHeader.stg");
		templates.put(CMOFLON_HEADER_FILE_GENERATOR, cMoflonHeaderGroup);

		final STGroup sourceGroup = new STGroup();
		sourceGroup.setListener(new LoggingSTErrorListener(logger));
		sourceGroup.loadGroupFile("/" + SOURCE_FILE_GENERATOR + "/", getTemplateUriPrefix() + "/cFile/cFile.stg");
		templates.put(SOURCE_FILE_GENERATOR, sourceGroup);

		final STGroup evaluationStatementsGroup = new STGroup();
		evaluationStatementsGroup.setListener(new LoggingSTErrorListener(logger));
		evaluationStatementsGroup.loadGroupFile("/" + EVALUATION_STATEMENTS + "/", getTemplateUriPrefix() + "/stringtemplate/EvaluationStatements.stg");
		templates.put(EVALUATION_STATEMENTS, evaluationStatementsGroup);

		final STGroup bindingAndBlackTemplateGroup = createBindingAndBlackTemplates();
		bindingAndBlackTemplateGroup.registerModelAdaptor(EModelElement.class, ecoreModelAdaptor);
		bindingAndBlackTemplateGroup.registerModelAdaptor(EMFVariable.class, ecoreModelAdaptor);
		bindingAndBlackTemplateGroup.registerRenderer(EMFVariable.class, ecoreModelAdaptor);
		bindingAndBlackTemplateGroup.registerRenderer(EClassifier.class, ecoreModelAdaptor);
		bindingAndBlackTemplateGroup.registerRenderer(String.class, new CMoflonStringRenderer());
		templates.put(CMoflonCodeGeneratorConfig.BINDING_AND_BLACK_PATTERN_MATCHER_GENERATOR,
				bindingAndBlackTemplateGroup);

		final STGroup bindingTemplateGroup = createBindingTemplates();
		bindingTemplateGroup.registerModelAdaptor(EModelElement.class, ecoreModelAdaptor);
		bindingTemplateGroup.registerModelAdaptor(EMFVariable.class, ecoreModelAdaptor);
		bindingTemplateGroup.registerRenderer(EMFVariable.class, ecoreModelAdaptor);
		bindingTemplateGroup.registerRenderer(EClassifier.class, ecoreModelAdaptor);
		bindingTemplateGroup.registerRenderer(String.class, new CMoflonStringRenderer());
		templates.put(CMoflonCodeGeneratorConfig.BINDING_PATTERN_MATCHER_GENERATOR, bindingTemplateGroup);

		final STGroup blackTemplateGroup = createBlackTemplates();
		blackTemplateGroup.registerModelAdaptor(EModelElement.class, ecoreModelAdaptor);
		blackTemplateGroup.registerModelAdaptor(EMFVariable.class, ecoreModelAdaptor);
		blackTemplateGroup.registerRenderer(EMFVariable.class, ecoreModelAdaptor);
		blackTemplateGroup.registerRenderer(EClassifier.class, ecoreModelAdaptor);
		blackTemplateGroup.registerRenderer(String.class, new CMoflonStringRenderer());
		templates.put(CMoflonCodeGeneratorConfig.BLACK_PATTERN_MATCHER_GENERATOR, blackTemplateGroup);

		final STGroup redTemplateGroup = createRedTemplates();
		redTemplateGroup.registerModelAdaptor(EModelElement.class, ecoreModelAdaptor);
		redTemplateGroup.registerModelAdaptor(EMFVariable.class, ecoreModelAdaptor);
		redTemplateGroup.registerRenderer(EMFVariable.class, ecoreModelAdaptor);
		redTemplateGroup.registerRenderer(EClassifier.class, ecoreModelAdaptor);
		redTemplateGroup.registerRenderer(String.class, new CMoflonStringRenderer());
		templates.put(CMoflonCodeGeneratorConfig.RED_PATTERN_MATCHER_GENERATOR, redTemplateGroup);

		final STGroup greenTemplateGroup = createGreenTemplates();
		greenTemplateGroup.registerModelAdaptor(EModelElement.class, ecoreModelAdaptor);
		greenTemplateGroup.registerModelAdaptor(EMFVariable.class, ecoreModelAdaptor);
		greenTemplateGroup.registerRenderer(EMFVariable.class, ecoreModelAdaptor);
		greenTemplateGroup.registerRenderer(EClassifier.class, ecoreModelAdaptor);
		greenTemplateGroup.registerRenderer(String.class, new CMoflonStringRenderer());
		templates.put(CMoflonCodeGeneratorConfig.GREEN_PATTERN_MATCHER_GENERATOR, greenTemplateGroup);

		final STGroup expressionTemplateGroup = createExpressionTemplates();
		expressionTemplateGroup.registerModelAdaptor(EModelElement.class, ecoreModelAdaptor);
		expressionTemplateGroup.registerModelAdaptor(EMFVariable.class, ecoreModelAdaptor);
		expressionTemplateGroup.registerRenderer(EMFVariable.class, ecoreModelAdaptor);
		expressionTemplateGroup.registerRenderer(EClassifier.class, ecoreModelAdaptor);
		expressionTemplateGroup.registerRenderer(String.class, new CMoflonStringRenderer());
		templates.put(CMoflonCodeGeneratorConfig.EXPRESSION_PATTERN_MATCHER_GENERATOR, expressionTemplateGroup);

		operationSequenceCompilers.put(CMoflonCodeGeneratorConfig.BINDING_AND_BLACK_PATTERN_MATCHER_GENERATOR,
				createBindingAndBlackOperationSequenceCompiler());
		operationSequenceCompilers.put(CMoflonCodeGeneratorConfig.BINDING_PATTERN_MATCHER_GENERATOR,
				createBindingOperationSequenceCompiler());
		operationSequenceCompilers.put(CMoflonCodeGeneratorConfig.BLACK_PATTERN_MATCHER_GENERATOR,
				createBlackOperationSequenceCompiler());
		operationSequenceCompilers.put(CMoflonCodeGeneratorConfig.RED_PATTERN_MATCHER_GENERATOR,
				createRedOperationSequenceCompiler());
		operationSequenceCompilers.put(CMoflonCodeGeneratorConfig.GREEN_PATTERN_MATCHER_GENERATOR,
				createGreenOperationSequenceCompiler());
		operationSequenceCompilers.put(CMoflonCodeGeneratorConfig.EXPRESSION_PATTERN_MATCHER_GENERATOR,
				createExpressionOperationSequenceCompiler());
	}

	private STGroup createControlFlowTemplates() {
		final STGroup group = new STGroup();
		group.setListener(new LoggingSTErrorListener(logger));
		group.loadGroupFile("/" + CONTROL_FLOW_GENERATOR + "/",
				getTemplateUriPrefix() + "stringtemplate/ControlFlow.stg");
		final ControlFlowModelAdaptor adaptor = new ControlFlowModelAdaptor();
		group.registerModelAdaptor(PatternInvocation.class, adaptor);
		group.registerModelAdaptor(VariableReference.class, adaptor);
		group.registerModelAdaptor(CFNode.class, adaptor);
		group.registerModelAdaptor(CFVariable.class, adaptor);
		final ImportHandler importRenderer = new ImportHandler();
		group.registerModelAdaptor(ImportManager.class, importRenderer);
		group.registerModelAdaptor(FullyQualifiedName.class, importRenderer);
		return group;
	}

	@Override
	public STGroup getTemplateGroup(String patternType) {
		return templates.get(patternType);
	}

	@Override
	public OperationSequenceCompiler getOperationSequenceCompiler(String patternType) {
		return operationSequenceCompilers.get(patternType);
	}

	public static final STGroup createRedTemplates() {
		final STGroup group = new STGroup();
		group.setListener(new LoggingSTErrorListener(logger));
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "democlestemplates/DemoclesCommon.stg");
		group.loadGroupFile("/regular/", getTemplateUriPrefix() + "stringtemplate/RegularPatternMatcher.stg");
		group.loadGroupFile("/emf-delete/", getTemplateUriPrefix() + "stringtemplate/EMFDeleteOperation.stg");
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "democlestemplates/EMFConstant.stg");
		final ImportHandler importRenderer = new ImportHandler();
		group.registerModelAdaptor(ImportManager.class, importRenderer);
		group.registerModelAdaptor(FullyQualifiedName.class, importRenderer);

		final PatternMatcherModelAdaptor parameterRenderer = new PatternMatcherModelAdaptor();
		group.registerModelAdaptor(ConstraintVariable.class, parameterRenderer);
		group.registerModelAdaptor(VariableRuntime.class, parameterRenderer);
		group.registerModelAdaptor(Integer.class, new AdornmentHandler());
		group.registerRenderer(String.class, new CMoflonStringRenderer());

		group.registerModelAdaptor(GenBase.class, new GenModelAdaptor());
		return group;
	}

	@SuppressWarnings("unchecked")
	public static final OperationSequenceCompiler createRedOperationSequenceCompiler() {
		return new OperationSequenceCompiler(new EMFRedTemplateProvider());
	}

	@SuppressWarnings("unchecked")
	public static final OperationSequenceCompiler createGreenOperationSequenceCompiler() {
		return new OperationSequenceCompiler(new AttributeAssignmentTemplateProvider(), new EMFGreenTemplateProvider());
	}

	public static final STGroup createGreenTemplates() {
		final STGroup group = new STGroup();
		group.setListener(new LoggingSTErrorListener(logger));
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "democlestemplates/DemoclesCommon.stg");
		group.loadGroupFile("/regular/", getTemplateUriPrefix() + "stringtemplate/RegularPatternMatcher.stg");
		group.loadGroupFile("/assignment/", getTemplateUriPrefix() + "stringtemplate/Assignment.stg");
		group.loadGroupFile("/emf-create/", getTemplateUriPrefix() + "stringtemplate/EMFCreateOperation.stg");
		group.loadGroupFile("/emf/", getTemplateUriPrefix() + "democlestemplates/EMFOperation.stg");
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "democlestemplates/EMFConstant.stg");
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "stringtemplate/Number.stg");
		final ImportHandler importRenderer = new ImportHandler();
		group.registerModelAdaptor(ImportManager.class, importRenderer);
		group.registerModelAdaptor(FullyQualifiedName.class, importRenderer);

		final PatternMatcherModelAdaptor parameterRenderer = new PatternMatcherModelAdaptor();
		group.registerModelAdaptor(ConstraintVariable.class, parameterRenderer);
		group.registerModelAdaptor(VariableRuntime.class, parameterRenderer);
		group.registerModelAdaptor(Integer.class, new AdornmentHandler());
		group.registerRenderer(String.class, new CMoflonStringRenderer());

		group.registerModelAdaptor(GenBase.class, new GenModelAdaptor());
		return group;
	}

	@SuppressWarnings("unchecked")
	public static final OperationSequenceCompiler createExpressionOperationSequenceCompiler() {
		return new OperationSequenceCompiler(new AssignmentTemplateProvider(), new EMFTemplateProvider());
	}

	public static final STGroup createExpressionTemplates() {
		final STGroup group = new STGroup();
		group.setListener(new LoggingSTErrorListener(logger));
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "democlestemplates/DemoclesCommon.stg");
		group.loadGroupFile("/expression/", getTemplateUriPrefix() + "stringtemplate/ExpressionPatternMatcher.stg");
		group.loadGroupFile("/assignment/", getTemplateUriPrefix() + "stringtemplate/Assignment.stg");
		group.loadGroupFile("/emf/", getTemplateUriPrefix() + "democlestemplates/EMFOperation.stg");
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "democlestemplates/EMFConstant.stg");
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "stringtemplate/Number.stg");
		final ImportHandler importRenderer = new ImportHandler();
		group.registerModelAdaptor(ImportManager.class, importRenderer);
		group.registerModelAdaptor(FullyQualifiedName.class, importRenderer);

		final PatternMatcherModelAdaptor parameterRenderer = new PatternMatcherModelAdaptor();
		group.registerModelAdaptor(ConstraintVariable.class, parameterRenderer);
		group.registerModelAdaptor(VariableRuntime.class, parameterRenderer);
		group.registerModelAdaptor(Integer.class, new AdornmentHandler());
		group.registerRenderer(String.class, new CMoflonStringRenderer());

		group.registerModelAdaptor(GenBase.class, new GenModelAdaptor());
		return group;
	}

	@SuppressWarnings("unchecked")
	public static final OperationSequenceCompiler createBindingAndBlackOperationSequenceCompiler() {
		return new OperationSequenceCompiler(new BindingAndBlackTemplateProvider());
	}

	public static final STGroup createBindingAndBlackTemplates() {
		final STGroup group = new STGroup();
		group.setListener(new LoggingSTErrorListener(logger));
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "democlestemplates/DemoclesCommon.stg");
		group.loadGroupFile("/regular/", getTemplateUriPrefix() + "stringtemplate/RegularPatternMatcher.stg");
		group.loadGroupFile("/priority/", getTemplateUriPrefix() + "stringtemplate/PrioritizedPatternCall.stg");
		final ImportHandler importRenderer = new ImportHandler();
		group.registerModelAdaptor(ImportManager.class, importRenderer);
		group.registerModelAdaptor(FullyQualifiedName.class, importRenderer);

		final PatternMatcherModelAdaptor parameterRenderer = new PatternMatcherModelAdaptor();
		group.registerModelAdaptor(GeneratorOperation.class, parameterRenderer);
		group.registerModelAdaptor(ConstraintVariable.class, parameterRenderer);
		group.registerModelAdaptor(VariableRuntime.class, parameterRenderer);
		group.registerModelAdaptor(Integer.class, new AdornmentHandler());
		group.registerRenderer(String.class, new CMoflonStringRenderer());

		group.registerModelAdaptor(GenBase.class, new GenModelAdaptor());
		return group;
	}

	@SuppressWarnings("unchecked")
	public static final OperationSequenceCompiler createBindingOperationSequenceCompiler() {
		return new OperationSequenceCompiler(new AssignmentTemplateProvider(), new EMFTemplateProvider());
	}

	public static final STGroup createBindingTemplates() {
		final STGroup group = new STGroup();
		group.setListener(new LoggingSTErrorListener(logger));
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "democlestemplates/DemoclesCommon.stg");
		group.loadGroupFile("/regular/", getTemplateUriPrefix() + "stringtemplate/RegularPatternMatcher.stg");
		group.loadGroupFile("/assignment/", getTemplateUriPrefix() + "stringtemplate/Assignment.stg");
		group.loadGroupFile("/emf/", getTemplateUriPrefix() + "democlestemplates/EMFOperation.stg");
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "democlestemplates/EMFConstant.stg");
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "stringtemplate/Number.stg");
		final ImportHandler importRenderer = new ImportHandler();
		group.registerModelAdaptor(ImportManager.class, importRenderer);
		group.registerModelAdaptor(FullyQualifiedName.class, importRenderer);

		final PatternMatcherModelAdaptor parameterRenderer = new PatternMatcherModelAdaptor();
		group.registerModelAdaptor(ConstraintVariable.class, parameterRenderer);
		group.registerModelAdaptor(VariableRuntime.class, parameterRenderer);
		group.registerModelAdaptor(Integer.class, new AdornmentHandler());
		group.registerRenderer(String.class, new CMoflonStringRenderer());

		group.registerModelAdaptor(GenBase.class, new GenModelAdaptor());
		return group;
	}

	@SuppressWarnings("unchecked")
	public static OperationSequenceCompiler createBlackOperationSequenceCompiler() {
		return new OperationSequenceCompiler(new PatternInvocationConstraintTemplateProvider(),
				new RelationalConstraintTemplateProvider(), new EMFTemplateProvider());
	}

	public static final STGroup createBlackTemplates() {
		final STGroup group = new STGroup();
		group.setListener(new LoggingSTErrorListener(logger));
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "democlestemplates/DemoclesCommon.stg");
		group.loadGroupFile("/regular/", getTemplateUriPrefix() + "stringtemplate/RegularPatternMatcher.stg");
		group.loadGroupFile("/core/", getTemplateUriPrefix() + "democlestemplates/RelationalOperation.stg");
		group.loadGroupFile("/emf/", getTemplateUriPrefix() + "democlestemplates/EMFOperation.stg");
		group.loadGroupFile("/pattern/", getTemplateUriPrefix() + "democlestemplates/PatternCallOperation.stg");
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "democlestemplates/EMFConstant.stg");
		group.loadGroupFile("/democles/", getTemplateUriPrefix() + "stringtemplate/Number.stg");
		final ImportHandler importRenderer = new ImportHandler();
		group.registerModelAdaptor(ImportManager.class, importRenderer);
		group.registerModelAdaptor(FullyQualifiedName.class, importRenderer);

		final PatternMatcherModelAdaptor parameterRenderer = new PatternMatcherModelAdaptor();
		group.registerModelAdaptor(ConstraintVariable.class, parameterRenderer);
		group.registerModelAdaptor(VariableRuntime.class, parameterRenderer);
		group.registerModelAdaptor(Integer.class, new AdornmentHandler());
		group.registerRenderer(String.class, new CMoflonStringRenderer());

		group.registerModelAdaptor(GenBase.class, new GenModelAdaptor());
		return group;
	}

	private static String getTemplateUriPrefix() {
		return String.format("platform:/plugin/%s/templates/",
				WorkspaceHelper.getPluginId(CMoflonTemplateConfiguration.class));
	}
}

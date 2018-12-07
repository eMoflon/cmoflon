package org.cmoflon.ide.core.runtime.codegeneration;

/**
 * Constants for addressing StringTemplate groups and templates
 *
 * @author Roland Kluge - Initial implementation
 *
 */
public interface CMoflonTemplateConstants {
	/**
	 * String template group prefix for control flow
	 */
	String CONTROL_FLOW_GENERATOR = "ControlFlowGenerator";
	String CONTROL_FLOW_PREFIX = "/" + CONTROL_FLOW_GENERATOR + "/";
	/**
	 * String template group prefix for header file structure of the topology
	 * control component
	 */
	String HEADER_FILE_GENERATOR = "HeaderFileGenerator";
	String HEADER_PREFIX = "/" + HEADER_FILE_GENERATOR + "/";
	/**
	 * String template group prefix for cMoflon header file structure of the
	 * topology control component
	 */
	String CMOFLON_HEADER_FILE_GENERATOR = "CMoflonHeaderFileGenerator";
	String CMOFLON_HEADER_PREFIX = "/" + CMOFLON_HEADER_FILE_GENERATOR + "/";
	/**
	 * String template group prefix for source file structure of the topology
	 * control component
	 */
	String SOURCE_FILE_GENERATOR = "SourceFileGenerator";
	String SOURCE_PREFIX = "/" + SOURCE_FILE_GENERATOR + "/";
	/**
	 * String template group prefix for evaluation statements
	 */
	String EVALUATION_STATEMENTS = "EvaluationStatements";
	String EVALUATION_PREFIX = "/" + EVALUATION_STATEMENTS + "/";
	String SOURCE_FILE_PARAMETER_CONSTANT = SOURCE_PREFIX + CMoflonTemplateConstants.PARAMETER_CONSTANT;
	String SOURCE_FILE_PARAMETER_RAW = SOURCE_PREFIX + CMoflonTemplateConstants.PARAMETER_RAW;
	String EVALUATION_STATEMETNS_END = "/" + EVALUATION_STATEMENTS + "/EvaluationStatementEnd";
	String EVALUATION_STATEMENTS_BEGIN = "/" + EVALUATION_STATEMENTS + "/EvaluationStatementBegin";

	String EQUALS_DECLARATION = "equalsDeclaration";

	String CMOFLON_HEADER_COMPARE_DECLARATION = CMOFLON_HEADER_PREFIX + CMoflonTemplateConstants.COMPARE_DECLARATION;
	String CMOFLON_HEADER_DECLARATION = CMOFLON_HEADER_PREFIX + CMoflonTemplateConstants.DECLARATIONS;
	String CMOFLON_HEADER_METHOD_DECLARATION = CMOFLON_HEADER_PREFIX + CMoflonTemplateConstants.METHOD_DECLARATION;
	String CMOFLON_HEADER_DEFINITION = CMOFLON_HEADER_PREFIX + CMoflonTemplateConstants.HEADER_TEMPLATE;
	String CMOFLON_HEADER_INCLUDE = CMOFLON_HEADER_PREFIX + CMoflonTemplateConstants.INCLUDE;
	String CMOFLON_HEADER_MATCH = CMOFLON_HEADER_PREFIX + CMoflonTemplateConstants.MATCH;
	String CMOFLON_HEADER_DEFINE = CMOFLON_HEADER_PREFIX + CMoflonTemplateConstants.DEFINE;
	String CMOFLON_HEADER_EQUALS_DELCARATION = CMOFLON_HEADER_PREFIX + CMoflonTemplateConstants.EQUALS_DECLARATION;
	String CMOFLON_HEADER_CONSTANTS_END = CMOFLON_HEADER_PREFIX + CMoflonTemplateConstants.CONSTANTS_END;

	String SOURCE_PARAMETER = SOURCE_PREFIX + CMoflonTemplateConstants.PARAMETER;
	String SOURCE_CLEANUP = SOURCE_PREFIX + CMoflonTemplateConstants.CLEANUP;
	String SOURCE_INIT = SOURCE_PREFIX + CMoflonTemplateConstants.INIT;
	String SOURCE_MEMB_DECLARATION = SOURCE_PREFIX + CMoflonTemplateConstants.MEMB_DECLARATION;
	String SOURCE_LIST_DECLARATION = SOURCE_PREFIX + CMoflonTemplateConstants.LIST_DECLARATION;
	String SOURCE_HOPCOUNT = SOURCE_PREFIX + CMoflonTemplateConstants.HOPCOUNT;
	String SOURCE_DROP_UNIDIRECTIONAL_EDGES = SOURCE_PREFIX + CMoflonTemplateConstants.DROP_UNIDIRECTIONAL_EDGES;
	String SOURCE_CLEANUP_CALL = SOURCE_PREFIX + CMoflonTemplateConstants.CLEANUP_CALL;

	String HEADER_COMPARE_DECLARATION = HEADER_PREFIX + CMoflonTemplateConstants.COMPARE_DECLARATION;
	String HEADER_EQUALS_DELCARATION = HEADER_PREFIX + CMoflonTemplateConstants.EQUALS_DECLARATION;
	String HEADER_DECLARATION = HEADER_PREFIX + CMoflonTemplateConstants.DECLARATIONS;
	String HEADER_METHOD_DECLARATION = HEADER_PREFIX + CMoflonTemplateConstants.METHOD_DECLARATION;
	String HEADER_DEFINITION = HEADER_PREFIX + CMoflonTemplateConstants.HEADER_TEMPLATE;
	String HEADER_CONSTANTS_END = HEADER_PREFIX + CMoflonTemplateConstants.CONSTANTS_END;
	String HEADER_CONSTANTS_DEFINITION = HEADER_PREFIX + CMoflonTemplateConstants.CONSTANTS_DEFINTION;

	String DROP_UNIDIRECTIONAL_EDGES = "dropUnidirectionalEdges";
	String PARAMETER_CONSTANT = "parameterConstant";
	String PARAMETER_RAW = "parameterRaw";
	String PARAMETER = "parameter";
	String MEMB_DECLARATION = "membDeclaration";
	String LIST_DECLARATION = "listDeclaration";
	String INIT = "init";
	String HOPCOUNT = "hopcount";
	String CLEANUP = "cleanup";
	String CLEANUP_CALL = "cleanupCall";
	String HEADER_TEMPLATE = "headerDefinition";
	String CONSTANTS_END = "endHeader";
	String INCLUDE = "include";
	String COMPARE_DECLARATION = "compareDeclaration";
	String DECLARATIONS = "getDeclarations";
	String METHOD_DECLARATION = "methodDeclaration";
	String DEFINE = "define";
	String MATCH = "getMatch";
	String CONSTANTS_DEFINTION = "constants";

}

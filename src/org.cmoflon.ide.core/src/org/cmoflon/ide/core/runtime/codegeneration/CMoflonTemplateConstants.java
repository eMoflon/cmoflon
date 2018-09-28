package org.cmoflon.ide.core.runtime.codegeneration;

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
	String SOURCE_FILE_PARAMETER_CONSTANT = SOURCE_PREFIX + SourceFileGenerator.PARAMETER_CONSTANT;
	String HEADER_COMPARE_DECLARATION = HEADER_PREFIX + HeaderFileGenerator.COMPARE_DECLARATION;
	String HEADER_EQUALS_DELCARATION = HEADER_PREFIX + HeaderFileGenerator.EQUALS_DECLARATION;
	String EVALUATION_STATEMETNS_END = "/" + EVALUATION_STATEMENTS + "/EvaluationStatementEnd";
	String EVALUATION_STATEMENTS_BEGIN = "/" + EVALUATION_STATEMENTS + "/EvaluationStatementBegin";

	String SOURCE_PARAMETER = SOURCE_PREFIX + SourceFileGenerator.PARAMETER;
	String CMOFLON_HEADER_COMPARE_DECLARATION = CMOFLON_HEADER_PREFIX + CMoflonHeaderFileGenerator.COMPARE_DECLARATION;
	String HEADER_DECLARATION = HEADER_PREFIX + HeaderFileGenerator.DECLARATIONS;
	String CMOFLON_HEADER_DECLARATION = CMOFLON_HEADER_PREFIX + CMoflonHeaderFileGenerator.DECLARATIONS;
	String HEADER_METHOD_DECLARATION = HEADER_PREFIX + HeaderFileGenerator.METHOD_DECLARATION;
	String CMOFLON_HEADER_METHOD_DECLARATION = CMOFLON_HEADER_PREFIX + CMoflonHeaderFileGenerator.METHOD_DECLARATION;
	String HEADER_DEFINITION = CMOFLON_HEADER_PREFIX + CMoflonHeaderFileGenerator.HEADER_DEFINITION;
	String SOURCE_CLEANUP = SOURCE_PREFIX + SourceFileGenerator.CLEANUP;
	String SOURCE_INIT = SOURCE_PREFIX + SourceFileGenerator.INIT;
	String SOURCE_MEMB_DECLARATION = SOURCE_PREFIX + SourceFileGenerator.MEMB_DECLARATION;
	String SOURCE_LIST_DECLARATION = SOURCE_PREFIX + SourceFileGenerator.LIST_DECLARATION;
	String SOURCE_HOPCOUNT = SOURCE_PREFIX + SourceFileGenerator.HOPCOUNT;
	String SOURCE_DROP_UNIDIRECTIONAL_EDGES = SOURCE_PREFIX + SourceFileGenerator.DROP_UNIDIRECTIONAL_EDGES;
	String SOURCE_CLEANUP_CALL = SOURCE_PREFIX + SourceFileGenerator.CLEANUP_CALL;
	String HEADER_CONSTANTS_END = CMOFLON_HEADER_PREFIX + CMoflonHeaderFileGenerator.CONSTANTS_END;
	String HEADER_CONSTANTS_DEFINITION = CMOFLON_HEADER_PREFIX + CMoflonHeaderFileGenerator.CONSTANTS_DEFINTION;
	String HEADER_DEFINE = CMOFLON_HEADER_PREFIX + CMoflonHeaderFileGenerator.DEFINE;
	String HEADER_MATCH = CMOFLON_HEADER_PREFIX + CMoflonHeaderFileGenerator.MATCH;
	String HEADER_INCLUDE = CMOFLON_HEADER_PREFIX + CMoflonHeaderFileGenerator.INCLUDE;

}

package org.cmoflon.ide.core.utilities;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CMoflonProperties {

	public static final String PROPERTY_PREFIX_TOPOLOGYCONTROL = "tc.";

	public static final String PROPERTY_TC_ALGORITHMS = PROPERTY_PREFIX_TOPOLOGYCONTROL + "algorithms";

	public static final String PROPERTY_PREFIX_FOR_CONSTANTS = "const-";

	public static final String PROPERTY_POSTFIX_USE_HOPCOUNT = ".enableHopcountProcess";

	public static final String PROPERTY_POSTFIX_DROP_UNIDIRECTIONAL_EDGES = ".dropUnidirectionalEdges";

	public static final String PROPERTY_POSTFIX_DUPLICATE_EDGES = ".generateDuplicates";

	public static final String PROPERTY_POSTFIX_INCLUDE_EVALUATION_STATEMENTS = ".useEvaluationStatements";

	public static final String PROPERTY_POSTFIX_PARAMETERS = ".parameters";

	public static final String PROPERTY_POSTFIX_HELPERCLASSES = ".helperclasses";

	public static final String PROPERTY_POSTFIX_CONSTANTS = ".constants";

	public static final String PROPERTY_PM_MAX_MATCH_COUNT = "pm.maxmatches";

	/**
	 * Properties starting with this prefix are treated as mappings from EClasses to
	 * C types
	 */
	public static final String PROPERTY_PREFIX_FOR_TYPE_MAPPINGS = "mappingOf.";

	public static final String CMOFLON_PROPERTIES_FILENAME = "cMoflon.properties";

	private static final String PROPERTY_TC_MIN_ALGORITHM_ID = PROPERTY_PREFIX_TOPOLOGYCONTROL + "minId";

	private static final int DEFAULT_MAX_MATCH_COUNT = 40;

	private static final int DEFAULT_TC_MIN_ALGORITHM_ID = 1000;

	private static final Map<String, String> DEFAULT_TYPE_MAPPINGS = createDefaultTypeMappingsMap();

	private static final String DEFAULT_CMOFLON_PROPERTIES_CONTENT = //
			"# Set number of matches allowed per foreach invocation\n" //
					+ PROPERTY_PM_MAX_MATCH_COUNT + " = " + CMoflonProperties.DEFAULT_MAX_MATCH_COUNT + "\n\n" //
					+ "# Sets the minimum ID to use as preprocessor constant for the generated TC algorithms (default: "
					+ DEFAULT_TC_MIN_ALGORITHM_ID + ")\n" //
					+ PROPERTY_TC_MIN_ALGORITHM_ID + "=" + DEFAULT_TC_MIN_ALGORITHM_ID + "\n\n" //
					+ "# Place the names of the topology control algorithms to use as CSV (e.g., "
					+ PROPERTY_TC_ALGORITHMS + "=KtcAlgorithm,LStarKtcAlgorithm)\n" //
					+ PROPERTY_TC_ALGORITHMS + " = \n\n" //
					+ "# Place the options for the topology control algorithms here\n" //
					+ "# Format is tc.<Algorithm Name>.<option> = [CSV]\n"
					+ "# <option>={parameters,constants,helperclasses,enableHopcountProcess,generateDuplicates,dropUnidirectionalEdges,useEvaluationStatements}\n"
					+ "# You can find a description on how to use any of these options in the documentation under: https://github.com/eMoflon/cmoflon/wiki/cMoflon-Properties\n"
					+ "\n\n" //
					+ "# " + " Type mapping definitions follow \n"//
					+ "# The key is the EClass, and the value is the C Struct you want it to be mapped to.\n" //
					+ "# Default: " + PROPERTY_PREFIX_FOR_TYPE_MAPPINGS + "Node = " + DEFAULT_TYPE_MAPPINGS.get("Node")
					+ " and " + PROPERTY_PREFIX_FOR_TYPE_MAPPINGS + "Link = " + DEFAULT_TYPE_MAPPINGS.get("Link") + "\n" //
					+ getCMoflonPropertiesLinesForDefaultTypeMappings() //
					+ "\n";

	/**
	 * Constant definitions for algorithms that rely on hop-count information
	 */
	public static final String DEFAULT_HOPCOUNT_CONSTANTS = "const-updateinterval=300, const-broadcasthopcount_immediate_max = 10, const-broadcasthopcount_immediate_min = 1, const-broadcasthopcount_smalldelay_min = 55, const-broadcasthopcount_smalldelay_max = 65, const-broadcasthopcount_periodic_min = 270, const-broadcasthopcount_periodic_max = 330";

	static String getDefaultCMoflonPropertiesContent() {
		return DEFAULT_CMOFLON_PROPERTIES_CONTENT;
	}

	private static Map<String, String> createDefaultTypeMappingsMap() {
		final Map<String, String> map = new HashMap<>();
		map.put("Node", "networkaddr_t");
		map.put("Link", "neighbor_t");
		return Collections.unmodifiableMap(map);
	}

	private static String getCMoflonPropertiesLinesForDefaultTypeMappings() {
		return DEFAULT_TYPE_MAPPINGS.entrySet().stream()//
				.map(entry -> PROPERTY_PREFIX_FOR_TYPE_MAPPINGS + entry.getKey() + "=" + entry.getValue() + "\n")//
				.collect(Collectors.joining(""));
	}
}

package org.cmoflon.ide.core.utilities;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CMoflonProperties
{

   public static final String PROPERTY_PREFIX_FOR_CONSTANTS = "const-";

   public static final String PROPERTY_TC_USE_HOPCOUNT = "tc.enableHopcountProcess";

   public static final String PROPERTY_PREFIX_TC_USE_HOPCOUNT = "tc.enableHopCountProcessFor.";

   public static final String PROPERTY_TC_DROP_UNIDIRECTIONAL_EDGES = "tc.dropUnidirectionalEdges";

   public static final String PROPERTY_TC_ALGORITHMS = "tc.algorithms";

   public static final String PROPERTY_PM_MAX_MATCH_COUNT = "pm.maxmatches";

   /**
    * Properties starting with this prefix are treated as mappings from EClasses to C types
    */
   public static final String PROPERTY_PREFIX_FOR_TYPE_MAPPINGS = "mappingOf.";

   public static final String PROPERTY_PREFIX_PARAMETERS = "tc.parametersOf.";

   public static final String CMOFLON_PROPERTIES_FILENAME = "cMoflon.properties";

   public static final int DEFAULT_MAX_MATCH_COUNT = 20;

   public static final String PROPERTY_TC_MIN_ALGORITHM_ID = "tc.minId";

   private static final boolean DEFAULT_USE_HOPCOUNT = false;

   public static final int DEFAULT_TC_MIN_ALGORITHM_ID = 1000;

   public static final String PROPERTY_INCLUDE_EVALUATION_STATEMENTS = "tc.useEvaluationStatements";

   private static final boolean DEFAULT_INCLUDE_EVALUATION_STATEMENTS = false;

   public static Map<String, String> DEFAULT_CONSTANTS = createDefaultConstantsMap();

   public static Map<String, String> DEFAULT_TYPE_MAPPINGS = createDefaultTypeMappingsMap();

   private static final String DEFAULT_CMOFLON_PROPERTIES_CONTENT = //
         "#Set to 'true' if dropping unidirectional edges is desired \n" //
               + PROPERTY_TC_DROP_UNIDIRECTIONAL_EDGES + " = true\n\n" //
               + "# Set to True if you want to use hopcounts.\n" //
               + PROPERTY_TC_USE_HOPCOUNT + " = " + CMoflonProperties.DEFAULT_USE_HOPCOUNT + "\n\n" //
               + "# You may also override this behavior for individual algorithms by using the property prefix '" + PROPERTY_PREFIX_TC_USE_HOPCOUNT + "'.\n" //
               + "# (e.g., " + PROPERTY_PREFIX_TC_USE_HOPCOUNT + "LStarKtcAlgorithm = true)\n\n" // 
               + "# Set number of matches allowed per foreach invocation\n" //
               + PROPERTY_PM_MAX_MATCH_COUNT + " = " + CMoflonProperties.DEFAULT_MAX_MATCH_COUNT + "\n\n" //
               + "# Set to true if you desire to use the Evaluation Scripts provided by the ToCoCo Framework. This will add monitoring of runtime and Node Degrees\n\n"
               + PROPERTY_INCLUDE_EVALUATION_STATEMENTS + " = " + CMoflonProperties.DEFAULT_INCLUDE_EVALUATION_STATEMENTS + "\n\n" //
               + "# Sets the minimum ID to use as preprocessor constant for the generated TC algorithms (default: " + DEFAULT_TC_MIN_ALGORITHM_ID + ")\n" //
               + PROPERTY_TC_MIN_ALGORITHM_ID + "=" + DEFAULT_TC_MIN_ALGORITHM_ID + "\n\n" //
               + "# Place the names of the topology control algorithms to use as CSV (e.g., " + PROPERTY_TC_ALGORITHMS + "=KtcAlgorithm,LStarKtcAlgorithm)\n" //
               + PROPERTY_TC_ALGORITHMS + " = \n\n" //
               + "# Place the parameters for the topology control algorithm call here as CSV\n" //
               + "# (e.g., " + PROPERTY_PREFIX_PARAMETERS + "KtcAlgorithm=" + PROPERTY_PREFIX_FOR_CONSTANTS + "k\n\n"
               + "# It is also possible to define constants. A constant declaration must look as follows: const-[constname]\n" //
               + "# (e.g., " + PROPERTY_PREFIX_FOR_CONSTANTS + "k=3.0)\n" //
               + getCMoflonPropertiesLinesForDefaultConstants() //
               + "\n\n" //
               + "# " + " Type mapping definitions follow \n"//
               + "# The Key is the EClass, and the value is the C Struct you want it to be mapped to.\n" //
               + "# Default: " + PROPERTY_PREFIX_FOR_TYPE_MAPPINGS + "Node = " + DEFAULT_TYPE_MAPPINGS.get("Node") + " and " + PROPERTY_PREFIX_FOR_TYPE_MAPPINGS + "Link = " + DEFAULT_TYPE_MAPPINGS.get("Link")
               + "\n" // 
               + getCMoflonPropertiesLinesForDefaultTypeMappings() //
               + "\n";

   public static String getDefaultCMoflonPropertiesContent()
   {
      return DEFAULT_CMOFLON_PROPERTIES_CONTENT;
   }

   private static Map<String, String> createDefaultTypeMappingsMap()
   {
      final Map<String, String> map = new HashMap<>();
      map.put("Node", "networkaddr_t");
      map.put("Link", "neighbor_t");
      return Collections.unmodifiableMap(map);
   }

   private static Map<String, String> createDefaultConstantsMap()
   {
      final Map<String, String> map = new HashMap<>();
      map.put("updateinterval", "300");
      return Collections.unmodifiableMap(map);
   }

   private static String getCMoflonPropertiesLinesForDefaultTypeMappings()
   {
      return DEFAULT_TYPE_MAPPINGS.entrySet().stream()//
            .map(entry -> PROPERTY_PREFIX_FOR_TYPE_MAPPINGS + entry.getKey() + "=" + entry.getValue() + "\n")//
            .collect(Collectors.joining(""));
   }

   private static String getCMoflonPropertiesLinesForDefaultConstants()
   {
      return DEFAULT_CONSTANTS.entrySet().stream()//
            .map(entry -> PROPERTY_PREFIX_FOR_CONSTANTS + entry.getKey() + "=" + entry.getValue() + "\n")//
            .collect(Collectors.joining(""));
   }
}

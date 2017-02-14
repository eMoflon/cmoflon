package org.cmoflon.ide.core.utilities;

public class CMoflonProperties
{

   public static final String PROPERTY_PREFIX_FOR_CONSTANTS = "const-";

   public static final String PROPERTY_TC_USE_HOPCOUNT = "tc.enableHopcountProcess";

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

   public static final String DEFAULT_NODE_TYPE = "networkaddr_t";

   public static final String DEFAULT_LINK_TYPE = "neighbor_t";

   public static final boolean DEFAULT_USE_HOPCOUNT = false;

   public static final String PROPERTY_TC_MIN_ALGORITHM_ID = "tc.minId";

   public static final int DEFAULT_TC_MIN_ALGORITHM_ID = 1000;

   public static final String DEFAULT_CMOFLON_PROPERTIES_CONTENT = //
         "#Set to 'true' if dropping unidirectional edges is desired \n" //
               + PROPERTY_TC_DROP_UNIDIRECTIONAL_EDGES + " = true\n\n" //
               + "# Set to True if you want to use hopcounts.\n" //
               + PROPERTY_TC_USE_HOPCOUNT + " = " + CMoflonProperties.DEFAULT_USE_HOPCOUNT + "\n\n" //
               + "# Set number of matches allowed per foreach invocation\n" //
               + PROPERTY_PM_MAX_MATCH_COUNT + " = " + CMoflonProperties.DEFAULT_MAX_MATCH_COUNT + "\n\n" //
               + "# Sets the minimum ID to use as preprocessor constant for the generated TC algorithms (default: " + DEFAULT_TC_MIN_ALGORITHM_ID + ")\n" //
               + PROPERTY_TC_MIN_ALGORITHM_ID + "=" + DEFAULT_TC_MIN_ALGORITHM_ID + "\n\n" //
               + "# Place the names of the topology control algorithms to use as CSV (e.g., " + PROPERTY_TC_ALGORITHMS + "=KtcAlgorithm,LStarKtcAlgorithm)\n" //
               + PROPERTY_TC_ALGORITHMS + " = \n\n" //
               + "# Place the parameters for the topology control algorithm call here as CSV\n" //
               + "# (e.g., " + PROPERTY_PREFIX_PARAMETERS + "KtcAlgorithm=" + PROPERTY_PREFIX_FOR_CONSTANTS + "k\n\n"
               + "# It is also possible to define constants. A constant declaration must look as follows: const-[constname]\n" //
               + "# (e.g., " + PROPERTY_PREFIX_FOR_CONSTANTS + "k=3.0)\n" //
               + "\n\n" //
               + "# " + " Type mapping definitions follow \n"//
               + "# The Key is the EClass, and the value is the C Struct you want it to be mapped to.\n" + "# Default: " + PROPERTY_PREFIX_FOR_TYPE_MAPPINGS
               + "Node = " + CMoflonProperties.DEFAULT_NODE_TYPE + " and " + PROPERTY_PREFIX_FOR_TYPE_MAPPINGS + "Link = " + CMoflonProperties.DEFAULT_LINK_TYPE
               + "\n" // 
               + PROPERTY_PREFIX_FOR_TYPE_MAPPINGS + "Node = " + CMoflonProperties.DEFAULT_NODE_TYPE + "\n" //
               + PROPERTY_PREFIX_FOR_TYPE_MAPPINGS + "Link = " + CMoflonProperties.DEFAULT_LINK_TYPE + "\n\n";

}

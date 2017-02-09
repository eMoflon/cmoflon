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
   static String DEFAULT_CMOFLON_PROPERTIES_CONTENT = //
   "#Set to 'true' if dropping unidirectional edges is desired \n" //
         + PROPERTY_TC_DROP_UNIDIRECTIONAL_EDGES + " = true\n" //
         + "# Set to True if you want to use hopcounts.\n" //
         + PROPERTY_TC_USE_HOPCOUNT + " = " + CMoflonProperties.DEFAULT_USE_HOPCOUNT + "\n" //
         + "# Set number of matches allowed per foreach invocation\n" //
         + PROPERTY_PM_MAX_MATCH_COUNT + " = " + CMoflonProperties.DEFAULT_MAX_MATCH_COUNT + "\n" //
         + "# Place the names of the topology control algorithms to use as CSV (e.g., tcMethods=KtcAlgorithm,LStarKtcAlgorithm)\n" //
         + PROPERTY_TC_ALGORITHMS + " = \n" //
         + "# Place the parameters for the topology control algorithm call here as CSV\n" //
         + "# (e.g., parametersOf.LStarKtcAlgorithm=" + PROPERTY_PREFIX_FOR_CONSTANTS + "k,1.5"
         + "# It is also possible to define constants. A constant declaration must look as follows: const-[constname]\n" //
         + "\n" //
         + "# (e.g., " + PROPERTY_PREFIX_FOR_CONSTANTS + "k=3.0)\n" //
         + "\n" //
         + "# " + " Type mapping definitions follow \n"//
         + "# The Key is the EClass, and the value is the C Struct you want it to be mapped to.\n" + "# Default: "
         + PROPERTY_PREFIX_FOR_TYPE_MAPPINGS + "Node = " + CMoflonProperties.DEFAULT_NODE_TYPE + " and "
         + PROPERTY_PREFIX_FOR_TYPE_MAPPINGS + "Link = " + CMoflonProperties.DEFAULT_LINK_TYPE + "\n" // 
         + "mappingOf.Node = " + CMoflonProperties.DEFAULT_NODE_TYPE + "\n" //
         + "mappingOf.Link = " + CMoflonProperties.DEFAULT_LINK_TYPE + "\n";
   public static final String CMOFLON_PROPERTIES_FILENAME = "cMoflon.properties";
   static final int DEFAULT_MAX_MATCH_COUNT = 20;
   static final String DEFAULT_NODE_TYPE = "networkaddr_t";
   static final String DEFAULT_LINK_TYPE = "neighbor_t";
   static final boolean DEFAULT_USE_HOPCOUNT = false;

}

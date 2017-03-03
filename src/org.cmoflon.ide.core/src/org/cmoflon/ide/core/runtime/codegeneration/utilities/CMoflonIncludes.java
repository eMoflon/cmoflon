package org.cmoflon.ide.core.runtime.codegeneration.utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Class storing all Includes needed for Contiki and the Components of the TC Evaluation Framework
 * @author David Giessing
 *
 */
public class CMoflonIncludes
{

   public enum Components {
      TOPOLOGYCONTROL, APPLICATION, RADIO, NEIGHBORDISCOVERY, NETWORK, POWERCONTROL
   }

   //These are the Includes that should be relevant for ANY Contiki Application
   private static final String STDLIB = "<stdlib.h>";

   private static final String STDIO = "<stdio.h>";

   private static final String CONTIKI = "\"contiki.h\"";

   private static final String CONTIKILIB = "\"contiki-lib.h\"";

   private static final String APPCONF = "\"../../app-conf.h\"";

   private static final String BOOT = "\"../../lib/boot.h\"";

   private static final String COMPONENTS = "\"../../lib/components.h\"";

   private static final String UTILITIES = "\"../../lib/utilities.h\"";
   
   private static final String UNIQUE_ID = "\"../../lib/uniqueid.h\"";

   private static final String FLOAT = "<float.h>";

   //These are the Includes that should be relevant for Applications of type Neighbordiscovery
   private static final String WATCHDOG = "\"dev/watchdog.h\"";

   private static final String NEIGHBORS = "\"../../lib/neighbors.h\"";

   private static final String NETWORKADDR = "\"../../lib/networkaddr.h\"";

   public static List<String> getCMoflonIncludes()
   {
      List<String> result = new ArrayList<String>();
      result.add(STDLIB);
      result.add(STDIO);
      result.add(FLOAT);
      result.add(CONTIKI);
      result.add(CONTIKILIB);
      result.add(APPCONF);
      result.add(BOOT);
      result.add(COMPONENTS);
      result.add(UTILITIES);
      return result;
   }

   /**
    * Gets the component specific includes
    * @param comp the component for which the includes are needed
    * @return a List containing the include Strings needed for an application of type comp
    */
   public static List<String> getComponentSpecificIncludes(Components comp)
   {
      List<String> result = new ArrayList<String>();
      switch (comp)
      {
      case TOPOLOGYCONTROL:
         result.add(NEIGHBORS);
         result.add(NETWORKADDR);
         result.add(WATCHDOG);
         result.add(UNIQUE_ID);
         return result;
      case APPLICATION:
         return result;
      case NEIGHBORDISCOVERY:
         return result;
      case NETWORK:
         return result;
      case POWERCONTROL:
         return result;
      case RADIO:
         return result;
      default:
         return result;
      }

   }

}

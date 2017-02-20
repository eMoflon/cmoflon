package org.cmoflon.ide.core.runtime.codegeneration;

import java.util.List;

import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonIncludes;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonIncludes.Components;
import org.stringtemplate.v4.ST;

public class HeaderFileGenerator
{

   public static final String CONSTANTS_BEGIN = "headerDefinition";

   public static final String CONSTANTS_DEFINTION = "constants";

   public static final String CONSTANTS_END = "endHeader";

   public static final String METHOD_DECLARATION = "methodDeclaration";

   public static final String COMPARE_DECLARATION = "compareDeclaration";

   public static final String EQUALS_DECLARATION = "equalsDeclaration";

   public static final String DECLARATIONS = "getDeclarations";

   public static final String MATCH = "getMatch";

   public static final String DEFINE = "define";

   public static final String INCLUDE = "include";

   public static String generateConstant(Object key, Object value, String component, String algorithm, ST template)
   {
      template.add("comp", component);
      template.add("algo", algorithm);
      template.add("name", key);
      template.add("value", value);
      return template.render();
   }

   /**
    * Gets a String with Typedefs from EType to the C language Type.
    */
   public static String getAllBuiltInMappings()
   {
      final StringBuilder result = new StringBuilder();
      for (final CMoflonBuiltInTypes t : CMoflonBuiltInTypes.values())
      {
         result.append("typedef " + CMoflonBuiltInTypes.getCType(t) + " " + t.name() + ";");
         result.append(CMoflonCodeGenerator.nl());
         result.append(CMoflonCodeGenerator.nl());
      }
      return result.toString();
   }

   /**
    * Generates the general Includes for CMoflon as well as the Component Specific stuff
    * @param comp The desired Component
    * @param include	The StringTemplate for the includes
    * @return 
    */
   public static String generateIncludes(Components comp, ST include)
   {

      final StringBuilder result = new StringBuilder();
      List<String> includes = CMoflonIncludes.getCMoflonIncludes();
      includes.addAll(CMoflonIncludes.getComponentSpecificIncludes(comp));
      for (String path : includes)
      {
         include.add("path", path);
         result.append(include.render());
         include.remove("path");
      }
      return result.toString();
   }
}
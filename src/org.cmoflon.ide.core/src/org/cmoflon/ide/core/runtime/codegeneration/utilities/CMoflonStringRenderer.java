package org.cmoflon.ide.core.runtime.codegeneration.utilities;

import java.util.Locale;

import org.gervarro.democles.codegen.stringtemplate.StringRenderer;

/**
 * Improved StringRenderer for support of lowercasing or uppercasing full passages.
 * @author David Giessing
 *
 */
public class CMoflonStringRenderer extends StringRenderer
{

   @Override
   public String toString(Object o, String formatString, Locale locale)
   {
      if (null != formatString)
      {
         switch (formatString)
         {
         case "fulluppercase":
            return String.class.cast(o).toUpperCase();
         case "fulllowercase":
            return String.class.cast(o).toLowerCase();
         case "firstlowercase":
         {
            final String original = String.class.cast(o);
            final String result = original.substring(0, 1).toLowerCase() + original.substring(1);
            return result;
         }
         default:
            return super.toString(o, formatString, locale);
         }
      } else 
      {
         return super.toString(o, formatString, locale);
      }
   }

}

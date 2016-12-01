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
      if ("fulluppercase".equals(formatString))
      {
         return ((String) o).toUpperCase();
      } else if ("fulllowercase".equals(formatString))
      {
         return ((String) o).toLowerCase();
      } else
         return super.toString(o, formatString, locale);
   }

}

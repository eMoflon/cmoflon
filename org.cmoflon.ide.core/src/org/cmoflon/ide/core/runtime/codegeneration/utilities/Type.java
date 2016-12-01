package org.cmoflon.ide.core.runtime.codegeneration.utilities;

/**
 * Simple Type representation for the Code Generation. A Type is either built in or not and has a name.
 * @author David Giessing
 *
 */
public class Type
{

   private boolean builtIn;

   private String name;

   public Type(boolean builtIn, String name)
   {
      this.builtIn = builtIn;
      this.name = name;
   }

   public boolean isBuiltIn()
   {
      return builtIn;
   }

   public String getName()
   {
      return name;
   }
}

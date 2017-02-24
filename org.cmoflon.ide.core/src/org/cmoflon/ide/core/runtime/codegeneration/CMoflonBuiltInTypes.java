package org.cmoflon.ide.core.runtime.codegeneration;

/**
 * All types that are supported by default by cMoflon
 * 
 * @author David Giessing
 * @author Roland Kluge
 *
 */
public enum CMoflonBuiltInTypes {
   EBoolean, EDouble, EFloat, EInt, ELong, EChar, EShort, EByte, EString;

   /**
    * Returns the String for each built in EType that is also part of C programming language.
    * @param t the type
    * @return a string with the type specifier corresponding to the EType
    */
   public static String getCType(final CMoflonBuiltInTypes t)
   {
      switch (t)
      {
      case EDouble:
         return "double";
      case EFloat:
         return "float";
      case EBoolean:
         return "bool";
      case EInt:
         return "int";
      case ELong:
         return "long";
      case EByte:
         return "char";
      case EShort:
         return "short";
      case EChar:
         return "char";
      case EString:
         return "const char*";
      default:
         return "void";
      }
   }
}
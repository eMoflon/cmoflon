package org.cmoflon.ide.core.runtime.codegeneration.utilities;

/**
 * This class represents a simplified structure of class attributes needed for the code generation.<br>
 * A class attribute is either a method or a field.
 * @author David Giessing
 *
 */
public class ClassAttribute
{

   //the name of the attribute
   protected String name;

   //The Type of this attribute (return type for methods)
   protected Type type;

   //The Type(Class) this Attribute belongs to.
   protected Type owningType;

   public ClassAttribute(Type owningType, Type type, String name)
   {
      this.name = name;
      this.type = type;
      this.owningType = owningType;
   }

   public Type getOwningType()
   {
      return owningType;
   }

   public Type getType()
   {
      return this.type;
   }

   public String getName()
   {
      return this.name;
   }

}

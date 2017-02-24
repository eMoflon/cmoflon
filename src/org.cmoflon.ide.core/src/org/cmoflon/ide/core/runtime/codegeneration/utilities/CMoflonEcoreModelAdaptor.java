package org.cmoflon.ide.core.runtime.codegeneration.utilities;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.gervarro.democles.codegen.emf.EcoreToGenModelConverter;
import org.gervarro.democles.codegen.stringtemplate.emf.EcoreModelAdaptor;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

/**
 * This class implements custom property accessors for cMoflon
 *
 * @author David Giessing
 *
 */
public class CMoflonEcoreModelAdaptor extends EcoreModelAdaptor
{

   public CMoflonEcoreModelAdaptor(EcoreToGenModelConverter converter)
   {
      super(converter);
   }

   @Override
   public synchronized Object getProperty(Interpreter interpreter, ST template, Object object, Object property, String propertyName)
         throws STNoSuchPropertyException
   {
      if ("EClassIsBuiltIn".equals(propertyName))
         if (object instanceof EDataType)
            return true;
         else
            return false;
      
      // This is a little hack because we could not find out how to distinguish EEnums from other types within ST
      if ("isEnumType".equals(propertyName))
         return object instanceof EEnum;
      //For all other properties are unchanged
      return super.getProperty(interpreter, template, object, property, propertyName);
   }

}

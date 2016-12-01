package org.cmoflon.ide.core.runtime.codegeneration.utilities;

import org.eclipse.emf.ecore.EDataType;
import org.gervarro.democles.codegen.emf.EcoreToGenModelConverter;
import org.gervarro.democles.codegen.stringtemplate.emf.EcoreModelAdaptor;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

/**
 * This class is needed to deffer whether the Class of an Object in the ECore model is a built in type or not.
 * @author David Giessing
 *
 */
public class CMoflonEcoreModelAdaptor extends EcoreModelAdaptor
{

   public CMoflonEcoreModelAdaptor(EcoreToGenModelConverter converter)
   {
      super(converter);
      // TODO Auto-generated constructor stub
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
      //For all other properties are unchanged
      return super.getProperty(interpreter, template, object, property, propertyName);
   }

}

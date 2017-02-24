package org.cmoflon.ide.core.runtime.builders;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.moflon.ide.core.properties.MocaTreeEAPropertiesReader;
import org.moflon.ide.core.runtime.BasicResourceFillingMocaToMoflonTransformation;
import org.moflon.util.plugins.MetamodelProperties;

import MocaTree.Attribute;
import MocaTree.Node;
import MocaTree.Text;
import MocaTree.impl.NodeImpl;

/**
 * This cMoflon-specific 
 *  
 * @author David Giessing - Initial implementation
 * @author Roland Kluge 
 */
public class CMoflonMocaTreeEAPropertiesReader extends MocaTreeEAPropertiesReader
{
   @Override
   public Map<String, MetamodelProperties> getProperties(Node mocaTree) throws CoreException
   {
      final Node root = (Node) ((NodeImpl) mocaTree.getChildren().get(0));
      for (final Text child : root.getChildren())
      {
         final Node node = (Node) child;
         if (!node.getAttribute(BasicResourceFillingMocaToMoflonTransformation.MOCA_TREE_ATTRIBUTE_WORKINGSET).iterator().next().getValue()
               .contains("eMoflon Languages"))
         {
            final Attribute nameAttribute = getSingleAttribute(node, BasicResourceFillingMocaToMoflonTransformation.MOFLON_TREE_ATTRIBUTE_NAME);
            final String oldValue = nameAttribute.getValue();
            final String newValue = oldValue + "_C";

            nameAttribute.setValue(nameAttribute.getValue().replaceAll(oldValue, newValue));
            final Attribute nsPrefixAttribute = getSingleAttribute(node, BasicResourceFillingMocaToMoflonTransformation.MOCA_TREE_ATTRIBUTE_NS_PREFIX);
            nsPrefixAttribute.setValue(nsPrefixAttribute.getValue().replaceAll(oldValue, newValue));
            final Attribute nsUriAttribute = getSingleAttribute(node, BasicResourceFillingMocaToMoflonTransformation.MOCA_TREE_ATTRIBUTE_NS_URI);
            nsUriAttribute.setValue(nsUriAttribute.getValue().replaceAll(oldValue, newValue));
            final Attribute pluginIdAttribute = getSingleAttribute(node, BasicResourceFillingMocaToMoflonTransformation.MOCA_TREE_ATTRIBUTE_PLUGINID);
            pluginIdAttribute.setValue(pluginIdAttribute.getValue().replaceAll(oldValue, newValue));
         }
      }
      return super.getProperties(mocaTree);
   }

   private Attribute getSingleAttribute(final Node node, String attribute)
   {
      return node.getAttribute(attribute).iterator().next();
   }
}

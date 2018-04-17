package org.cmoflon.ide.core.runtime.builders;

import org.eclipse.core.runtime.CoreException;
import org.moflon.ide.core.properties.MocaTreeConstants;
import org.moflon.ide.core.properties.MocaTreeEAPropertiesReader;

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

   @Deprecated // Remove with MocaTreeConstants equivalent from upcoming eMoflon release
   private static final String MOCA_TREE_ATTRIBUTE_NS_PREFIX = "Moflon::NsPrefix";
   @Deprecated // Remove with MocaTreeConstants equivalent from upcoming eMoflon release
   private static final String MOCA_TREE_ATTRIBUTE_PLUGINID = "Moflon::PluginID";
   @Deprecated // Remove with MocaTreeConstants equivalent from upcoming eMoflon release
   private static final String MOCA_TREE_ATTRIBUTE_WORKINGSET = "Moflon::WorkingSet";

   public static void updateProperties(final Node mocaTreeRoot) throws CoreException
   {
      for (final Text child : ((NodeImpl) mocaTreeRoot.getChildren().get(0)).getChildren())
      {
         final Node node = (Node) child;
         if (!node.getAttribute(MOCA_TREE_ATTRIBUTE_WORKINGSET).iterator().next().getValue()
               .contains("eMoflon Languages"))
         {
            final Attribute nameAttribute = getSingleAttribute(node, MocaTreeConstants.MOFLON_TREE_ATTRIBUTE_NAME);
            final String oldValue = nameAttribute.getValue();
            final String newValue = oldValue + "_C";

            nameAttribute.setValue(nameAttribute.getValue().replaceAll(oldValue, newValue));
            final Attribute nsPrefixAttribute = getSingleAttribute(node, MOCA_TREE_ATTRIBUTE_NS_PREFIX);
            nsPrefixAttribute.setValue(nsPrefixAttribute.getValue().replaceAll(oldValue, newValue));
            final Attribute nsUriAttribute = getSingleAttribute(node, MocaTreeConstants.MOCA_TREE_ATTRIBUTE_NS_URI);
            nsUriAttribute.setValue(nsUriAttribute.getValue().replaceAll(oldValue, newValue));
            final Attribute pluginIdAttribute = getSingleAttribute(node, MOCA_TREE_ATTRIBUTE_PLUGINID);
            pluginIdAttribute.setValue(pluginIdAttribute.getValue().replaceAll(oldValue, newValue));
         }
      }
   }

   private static Attribute getSingleAttribute(final Node node, String attribute)
   {
      return node.getAttribute(attribute).iterator().next();
   }
}

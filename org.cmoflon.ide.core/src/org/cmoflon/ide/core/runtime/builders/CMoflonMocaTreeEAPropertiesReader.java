package org.cmoflon.ide.core.runtime.builders;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.moflon.codegen.eclipse.CodeGeneratorPlugin;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.core.utilities.eMoflonEMFUtil;
import org.moflon.ide.core.CoreActivator;
import org.moflon.ide.core.properties.MocaTreeEAPropertiesReader;
import org.moflon.ide.core.runtime.BasicResourceFillingMocaToMoflonTransformation;
import org.moflon.ide.core.runtime.builders.MetamodelBuilder;
import org.moflon.util.plugins.MetamodelProperties;

import MocaTree.Node;
import MocaTree.Text;
import MocaTree.impl.NodeImpl;

public class CMoflonMocaTreeEAPropertiesReader extends MocaTreeEAPropertiesReader {


	
	@Override
	public Map<String, MetamodelProperties> getProperties(Node mocaTree) throws CoreException {
		Node root = (Node)((NodeImpl) mocaTree.getChildren().get(0));
        for(Text child:root.getChildren())
        //if(!((Node) child).getAttribute().get(7).getValue().contains("eMoflon Languages")){
        if(!((Node) child).getAttribute(BasicResourceFillingMocaToMoflonTransformation.MOCA_TREE_ATTRIBUTE_WORKINGSET).iterator().next().getValue().contains("eMoflon Languages")){
            String oldValue=((Node) child).getAttribute().get(1).getValue();
            String newValue=oldValue+"_C";
            
            ((Node)child).getAttribute(BasicResourceFillingMocaToMoflonTransformation.MOFLON_TREE_ATTRIBUTE_NAME).iterator().next().setValue(((Node)child).getAttribute(BasicResourceFillingMocaToMoflonTransformation.MOFLON_TREE_ATTRIBUTE_NAME).iterator().next().getValue().replaceAll(oldValue, newValue));
            ((Node)child).getAttribute(BasicResourceFillingMocaToMoflonTransformation.MOCA_TREE_ATTRIBUTE_NS_PREFIX).iterator().next().setValue(((Node)child).getAttribute(BasicResourceFillingMocaToMoflonTransformation.MOCA_TREE_ATTRIBUTE_NS_PREFIX).iterator().next().getValue().replaceAll(oldValue, newValue));
            ((Node)child).getAttribute(BasicResourceFillingMocaToMoflonTransformation.MOCA_TREE_ATTRIBUTE_NS_URI).iterator().next().setValue(((Node)child).getAttribute(BasicResourceFillingMocaToMoflonTransformation.MOCA_TREE_ATTRIBUTE_NS_URI).iterator().next().getValue().replaceAll(oldValue, newValue));
            ((Node)child).getAttribute(BasicResourceFillingMocaToMoflonTransformation.MOCA_TREE_ATTRIBUTE_PLUGINID).iterator().next().setValue(((Node)child).getAttribute(BasicResourceFillingMocaToMoflonTransformation.MOCA_TREE_ATTRIBUTE_PLUGINID).iterator().next().getValue().replaceAll(oldValue, newValue));
        }
		return super.getProperties(mocaTree);
	}
}

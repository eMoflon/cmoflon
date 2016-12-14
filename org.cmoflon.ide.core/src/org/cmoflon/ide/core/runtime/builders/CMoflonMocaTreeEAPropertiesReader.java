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
import org.moflon.util.plugins.MetamodelProperties;

import MocaTree.Node;
import MocaTree.Text;
import MocaTree.impl.NodeImpl;

public class CMoflonMocaTreeEAPropertiesReader extends MocaTreeEAPropertiesReader {

	@Override
	public Map<String, MetamodelProperties> getProperties(IProject metamodelProject) throws CoreException {

	      IFile mocaFile = WorkspaceHelper.getExportedMocaTree(metamodelProject);

	      if (mocaFile.exists())
	      {
	         // Create and initialize resource set
	         set = CodeGeneratorPlugin.createDefaultResourceSet();
	         eMoflonEMFUtil.installCrossReferencers(set);
	         
	         // Load Moca tree in read-only mode
	         URI mocaFileURI = URI.createPlatformResourceURI(mocaFile.getFullPath().toString(), true);
	         Resource mocaTreeResource = set.getResource(mocaFileURI, true);
	         Node node=(NodeImpl) mocaTreeResource.getContents().get(0);
	         Node root = (Node)((NodeImpl) node.getChildren().get(0));
	         for(Text child:root.getChildren())
	         if(!((Node) child).getAttribute().get(7).getValue().contains("eMoflon Languages")){
	             String oldValue=((Node) child).getAttribute().get(1).getValue();
	             String newValue=oldValue+"_C";
	             ((Node)child).getAttribute().get(1).setValue(((Node)child).getAttribute().get(1).getValue().replaceAll(oldValue, newValue));
	             ((Node)child).getAttribute().get(2).setValue(((Node)child).getAttribute().get(2).getValue().replaceAll(oldValue, newValue));
	             ((Node)child).getAttribute().get(3).setValue(((Node)child).getAttribute().get(3).getValue().replaceAll(oldValue, newValue));
	             ((Node)child).getAttribute().get(4).setValue(((Node)child).getAttribute().get(4).getValue().replaceAll(oldValue, newValue));
	         }
	         mocaTree = (Node) mocaTreeResource.getContents().get(0);
	         Map<String, MetamodelProperties> properties = getProperties(mocaTree);
	         properties.keySet().forEach(p->properties.get(p).setMetamodelProjectName(metamodelProject.getName()));
	         return properties;
	      } else
	      {
	         throw new CoreException(new Status(IStatus.ERROR, CoreActivator.getModuleID(), "Cannot extract project properties, since Moca tree is missing."));
	      }
	   }
}

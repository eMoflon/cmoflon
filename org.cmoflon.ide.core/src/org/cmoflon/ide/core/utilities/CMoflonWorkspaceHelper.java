package org.cmoflon.ide.core.utilities;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.moflon.core.utilities.WorkspaceHelper;

/**
 * Extends the {@link WorkspaceHelper} with functionality for getting the properties files.
 * @author David Giessing
 *
 */
public class CMoflonWorkspaceHelper extends WorkspaceHelper
{

   public static Properties getConstantsPropertiesFile(IProject project)
   {
      Properties properties = new Properties();
      BufferedInputStream stream = null;
      try
      {
         stream = new BufferedInputStream(
               new FileInputStream(project.getLocation().toString().concat(new Path("/" + project.getName() + "Constants.properties").toString())));
         properties.load(stream);
         stream.close();

      } catch (IOException e1)
      {
         // TODO Auto-generated catch block
         e1.printStackTrace();
      }
      return properties;
   }

   public static Properties getMappingPropertiesFile(IProject project)
   {
      Properties properties = new Properties();
      BufferedInputStream stream = null;
      try
      {
         stream = new BufferedInputStream(
               new FileInputStream(project.getLocation().toString().concat(new Path("/" + project.getName() + "EClassToStructs.properties").toString())));
         properties.load(stream);
         stream.close();

      } catch (IOException e1)
      {
         // TODO Auto-generated catch block
         e1.printStackTrace();
      }
      return properties;
   }
}

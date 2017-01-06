package org.cmoflon.ide.core.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.CharSet;
import org.cmoflon.ide.core.CMoflonCoreActivator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.moflon.core.utilities.WorkspaceHelper;
import org.moflon.ide.core.CoreActivator;

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

package org.cmoflon.ide.core.utilities;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.moflon.core.utilities.WorkspaceHelper;

/**
 * Extends the {@link WorkspaceHelper} with functionality for getting the properties files.
 * @author David Giessing
 *
 */
public class CMoflonWorkspaceHelper extends WorkspaceHelper
{

   public static Properties getConstantsPropertiesFile(IProject project) throws CoreException
   {
      final String inputFile = project.getLocation().toString().concat(new Path("/" + project.getName() + "Constants.properties").toString());
      return readProperties(inputFile);
   }

   public static Properties getMappingPropertiesFile(IProject project) throws CoreException
   {
      final String inputFile = project.getLocation().toString().concat(new Path("/" + project.getName() + "EClassToStructs.properties").toString());
      return readProperties(inputFile);
   }

   private static Properties readProperties(final String inputFile) throws CoreException
   {
      final Properties properties = new Properties();
      BufferedInputStream stream = null;
      try
      {
         stream = new BufferedInputStream(new FileInputStream(inputFile));
         properties.load(stream);
         stream.close();

      } catch (final IOException e)
      {
         throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(CMoflonWorkspaceHelper.class), "Failed to read properties from " + inputFile, e));
      } finally {
         IOUtils.closeQuietly(stream);
      }
      return properties;
   }

}

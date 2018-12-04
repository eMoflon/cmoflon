package org.cmoflon.ide.core.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.moflon.core.utilities.WorkspaceHelper;

/**
 * Extends the {@link WorkspaceHelper} with functionality for getting the
 * properties files.
 * 
 * @author David Giessing
 * @author Roland Kluge
 */
public class CMoflonWorkspaceHelper {

	/**
	 * Returns the cMoflon-specific properties of the given project
	 *
	 * @param project
	 *                    the project
	 * @return the cMoflon properties
	 * @throws CoreException
	 *                           e.g., if an IOException occurred
	 */
	public static Properties getCMoflonPropertiesFile(final IProject project) throws CoreException {
		final IFile file = project.getFile(CMoflonProperties.CMOFLON_PROPERTIES_FILENAME);
		final Properties properties = new Properties();
		InputStream stream = null;
		try {
			stream = file.getContents();
			properties.load(stream);

		} catch (final IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(CMoflonWorkspaceHelper.class),
					"Failed to read properties from " + file, e));
		} finally {
			IOUtils.closeQuietly(stream);
		}
		return properties;
	}

}

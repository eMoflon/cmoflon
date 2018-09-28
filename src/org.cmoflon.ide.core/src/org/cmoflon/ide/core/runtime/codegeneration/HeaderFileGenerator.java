package org.cmoflon.ide.core.runtime.codegeneration;

import java.util.List;

import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonIncludes;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonIncludes.ToCoCoComponents;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

public class HeaderFileGenerator {

	public static final String METHOD_DECLARATION = "methodDeclaration";

	public static final String COMPARE_DECLARATION = "compareDeclaration";

	public static final String EQUALS_DECLARATION = "equalsDeclaration";

	public static final String DECLARATIONS = "getDeclarations";

	/**
	 * Generates the general Includes for CMoflon as well as the Component Specific
	 * stuff
	 *
	 * @param comp
	 *            The desired Component
	 * @param templateGroup
	 *            The StringTemplate for the includes
	 * @return
	 */
	public static String generateIncludes(final ToCoCoComponents comp, final STGroup templateGroup) {
		final ST template = templateGroup.getInstanceOf(CMoflonTemplateConstants.HEADER_INCLUDE);
		final StringBuilder result = new StringBuilder();
		final List<String> includes = CMoflonIncludes.getCMoflonIncludes();
		includes.addAll(CMoflonIncludes.getComponentSpecificIncludes(comp));
		for (final String path : includes) {
			template.add("path", path);
			result.append(template.render());
			template.remove("path");
		}
		return result.toString();
	}
}
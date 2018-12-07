package org.cmoflon.ide.core.runtime.codegeneration.file;

import org.cmoflon.ide.core.runtime.codegeneration.CMoflonTemplateConstants;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

class SourceFileGenerator {

	private static final String PROCESS_BEGIN = "processBegin";

	private static final String PROCESS_END = "processEnd";

	private static final String BOOT_COMP_WAIT = "bootCompWait";

	private static final String MAIN_LOOP = "mainLoop";

	private static final String WATCHDOG_START = "watchdogStart";

	private static final String WATCHDOG_STOP = "watchdogStop";

	/**
	 * Generates the closing part of a ToCoCo process
	 *
	 * @param templateGroup
	 * @param hopcount
	 * @return
	 */
	public static String generateClosingPart(final STGroup templateGroup, final boolean hopcount) {
		final StringBuilder sb = new StringBuilder();
		sb.append(
				templateGroup.getInstanceOf("/" + CMoflonTemplateConstants.SOURCE_FILE_GENERATOR + "/" + WATCHDOG_START)
						.add("hopcount", hopcount).render());
		sb.append(templateGroup.getInstanceOf("/" + CMoflonTemplateConstants.SOURCE_FILE_GENERATOR + "/" + PROCESS_END)
				.render());
		return sb.toString();
	}

	/**
	 * Generates most of the source files upper part
	 *
	 * @param component
	 * @param tcClass
	 * @param templateGroup
	 * @param componentPreprocessorId
	 * @return
	 */
	public static String generateUpperPart(final String component, final GenClass tcClass, final STGroup templateGroup,
			final boolean hopcount, final String componentPreprocessorId) {
		final String algorithmName = tcClass.getName();
		final StringBuilder result = new StringBuilder();
		final ST procBegin = templateGroup
				.getInstanceOf("/" + CMoflonTemplateConstants.SOURCE_FILE_GENERATOR + "/" + PROCESS_BEGIN);
		procBegin.add("component", component);
		procBegin.add("algo", algorithmName);
		procBegin.add("componentId", componentPreprocessorId);
		result.append(procBegin.render());

		final ST bootComp = templateGroup
				.getInstanceOf("/" + CMoflonTemplateConstants.SOURCE_FILE_GENERATOR + "/" + BOOT_COMP_WAIT);
		bootComp.add("component", component);
		bootComp.add("hopcount", hopcount);
		bootComp.add("algo", algorithmName);
		result.append(bootComp.render());

		final ST mainLoop = templateGroup
				.getInstanceOf("/" + CMoflonTemplateConstants.SOURCE_FILE_GENERATOR + "/" + MAIN_LOOP);
		mainLoop.add("hopcount", hopcount);
		mainLoop.add("component", component);
		mainLoop.add("algo", algorithmName);
		result.append(mainLoop.render());

		final ST watchDogStop = templateGroup
				.getInstanceOf("/" + CMoflonTemplateConstants.SOURCE_FILE_GENERATOR + "/" + WATCHDOG_STOP);
		result.append(watchDogStop.render());

		return result.toString();
	}

}

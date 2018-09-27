package org.cmoflon.ide.core.runtime.codegeneration;

import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

public class SourceFileGenerator {

	public static final String PROCESS_BEGIN = "processBegin";

	public static final String PROCESS_END = "processEnd";

	public static final String BOOT_COMP_WAIT = "bootCompWait";

	public static final String MAIN_LOOP = "mainLoop";

	public static final String WATCHDOG_START = "watchdogStart";

	public static final String WATCHDOG_STOP = "watchdogStop";

	public static final String DROP_UNIDIRECTIONAL_EDGES = "dropUnidirectionalEdges";

	public static final String PARAMETER_CONSTANT = "parameterConstant";

	public static final String PARAMETER = "parameter";

	public static final String MEMB_DECLARATION = "membDeclaration";

	public static final String LIST_DECLARATION = "listDeclaration";

	public static final String INIT = "init";

	public static final String GUARD_START = "guardStart";

	public static final String GUARD_END = "guardEnd";

	public static final String HOPCOUNT = "hopcount";

	public static final String CLEANUP = "cleanup";

	public static final String CLEANUP_CALL = "cleanupCall";

	// TODO@rkluge contains general as well as specific parts, should be splitted
	// later on
	/**
	 * Generates the closing part of a ToCoCo process
	 *
	 * @param templateGroup
	 * @param hopcount
	 * @return
	 */
	public static String generateClosingPart(final STGroup templateGroup, final boolean hopcount) {
		final StringBuilder sb = new StringBuilder();
		sb.append(templateGroup
				.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + WATCHDOG_START)
				.add("hopcount", hopcount).render());
		sb.append(templateGroup
				.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + PROCESS_END).render());
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
	// TODO@rkluge contains general as well as specific parts, should be splitted
	// later on
	public static String generateUpperPart(final String component, final GenClass tcClass, final STGroup templateGroup,
			final boolean hopcount, final String componentPreprocessorId) {
		final String algorithmName = tcClass.getName();
		final StringBuilder result = new StringBuilder();
		final ST procBegin = templateGroup
				.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + PROCESS_BEGIN);
		procBegin.add("component", component);
		procBegin.add("algo", algorithmName);
		procBegin.add("componentId", componentPreprocessorId);
		result.append(procBegin.render());

		final ST bootComp = templateGroup
				.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + BOOT_COMP_WAIT);
		bootComp.add("component", component);
		bootComp.add("hopcount", hopcount);
		bootComp.add("algo", algorithmName);
		result.append(bootComp.render());

		final ST mainLoop = templateGroup
				.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + MAIN_LOOP);
		mainLoop.add("hopcount", hopcount);
		mainLoop.add("component", component);
		mainLoop.add("algo", algorithmName);
		result.append(mainLoop.render());

		final ST watchDogStop = templateGroup
				.getInstanceOf("/" + CMoflonTemplateConfiguration.SOURCE_FILE_GENERATOR + "/" + WATCHDOG_STOP);
		result.append(watchDogStop.render());

		return result.toString();
	}

}

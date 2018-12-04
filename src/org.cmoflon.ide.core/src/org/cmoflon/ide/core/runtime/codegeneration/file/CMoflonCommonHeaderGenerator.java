package org.cmoflon.ide.core.runtime.codegeneration.file;

import static org.cmoflon.ide.core.utilities.FormattingUtils.nl;
import static org.cmoflon.ide.core.utilities.FormattingUtils.nl2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.cmoflon.ide.core.runtime.codegeneration.BuildProcessConfigurationProvider;
import org.cmoflon.ide.core.runtime.codegeneration.CMoflonBuiltInTypes;
import org.cmoflon.ide.core.runtime.codegeneration.CMoflonTemplateConstants;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonIncludes;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonIncludes.ToCoCoComponents;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.CMoflonStringRenderer;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.FieldAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.MethodAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.Type;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.moflon.compiler.sdm.democles.DemoclesGeneratorAdapterFactory;
import org.moflon.core.utilities.WorkspaceHelper;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

public class CMoflonCommonHeaderGenerator extends AbstractFileGenerator {

	public CMoflonCommonHeaderGenerator(final IProject project, final GenModel genModel,
			final DemoclesGeneratorAdapterFactory codeGenerationEngine,
			final BuildProcessConfigurationProvider buildProcessConfigurationProvider) {
		super(project, genModel, codeGenerationEngine, buildProcessConfigurationProvider);
	}

	public void generateCMoflonHeader(final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generate cMoflon header ", 10);
		final STGroup templateGroup = getTemplateConfigurationProvider()
				.getTemplateGroup(CMoflonTemplateConstants.CMOFLON_HEADER_FILE_GENERATOR);
		templateGroup.registerRenderer(String.class, new CMoflonStringRenderer());

		final StringBuilder contents = new StringBuilder();
		contents.append(getDateCommentCode());
		contents.append(getIncludeGuardCMoflonHeader());
		contents.append(getIncludesCode(templateGroup, null));
		contents.append(getMaxMatchCountDefinition(null));
		contents.append(getMatchTypeDefinitionCode(templateGroup, null));
		contents.append(getTypeMappingCode(templateGroup, null));
		contents.append(getAllBuiltInMappings(null));
		contents.append(getDefaultTypedefs(null));
		contents.append(getUserDefinedTypedefs(null));
		contents.append(getUnimplementedMethodsCode(templateGroup, null));
		contents.append(getAccessorsCode(templateGroup, null));
		contents.append(getComparisonFunctionsCode(templateGroup, null));
		contents.append(getEqualsFunctionsCode(templateGroup, null));
		contents.append(getPrintPatternEnablingCode());
		contents.append(getHeaderTailCode(null, templateGroup));
		subMon.worked(8);

		final String parentFolderForAlgorithm = this.getBuildProcessConfigurationProvider()
				.getProjectRelativePathForAlgorithm(null);
		final String outputFileName = parentFolderForAlgorithm + "cMoflon.h";
		final IFile headerFile = getProject().getFile(outputFileName);
		if (!headerFile.exists()) {
			WorkspaceHelper.addAllFolders(getProject(), parentFolderForAlgorithm, subMon.split(1));
			WorkspaceHelper.addFile(getProject(), outputFileName, contents.toString(), subMon.split(1));
		} else {
			headerFile.setContents(new ReaderInputStream(new StringReader(contents.toString())), true, true,
					subMon.split(2));
		}
	}

	private String getIncludeGuardCMoflonHeader() {
		final STGroup templateGroup = getTemplateConfigurationProvider()
				.getTemplateGroup(CMoflonTemplateConstants.CMOFLON_HEADER_FILE_GENERATOR);
		final ST definition = templateGroup.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_DEFINITION);
		final String guardCode = definition.render();
		return guardCode;
	}

	private String getPrintPatternEnablingCode() {
		final StringBuilder sb = new StringBuilder();
		sb.append(
				"// Uncomment the following directive to enable the printing of pattern invocations for debuging purposes.")
				.append(nl());
		sb.append("#ifndef CMOFLON_PRINT_PATTERN_INVOCATION").append(nl());
		sb.append("//#define CMOFLON_PRINT_PATTERN_INVOCATION").append(nl());
		sb.append("#endif").append(nl2());
		return sb.toString();
	}

	private String getIncludesCode(final STGroup templateGroup, final GenClass tcClass) {
		return (generateIncludes(ToCoCoComponents.TOPOLOGYCONTROL, templateGroup));
	}

	/**
	 * Generates the general Includes for CMoflon as well as the Component Specific
	 * stuff
	 *
	 * @param comp
	 *                          The desired Component
	 * @param templateGroup
	 *                          The StringTemplate for the includes
	 * @return
	 */
	private static String generateIncludes(final ToCoCoComponents comp, final STGroup templateGroup) {
		final ST template = templateGroup.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_INCLUDE);
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

	private String getMaxMatchCountDefinition(final GenClass tcClass) {
		final StringBuilder mycontents = new StringBuilder();
		final int maxMatchCount = this.getBuildProcessConfigurationProvider().getMaximumMatchCount();
		mycontents.append(
				"// The following preprocessor constant is relevant for match collections (foreach story nodes)")
				.append(nl());
		mycontents.append("#ifndef MAX_MATCH_COUNT").append(nl());
		mycontents.append(String.format("#define MAX_MATCH_COUNT %d%s", maxMatchCount, nl()));
		mycontents.append("#endif").append(nl2());
		return mycontents.toString();
	}

	private String getMatchTypeDefinitionCode(final STGroup templateGroup, final GenClass tcClass) {
		final ST match = templateGroup.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_MATCH);
		final String matchTypeDef = match.render();
		return matchTypeDef;
	}

	private String getTypeMappingCode(final STGroup templateGroup, final GenClass tcClass) {
		final StringBuilder typeMappingCodeBuilder = new StringBuilder();
		final ST typeMappingTemplate = templateGroup.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_DEFINE);
		for (final Entry<String, String> pair : getBuildProcessConfigurationProvider().getTypeMappings().entrySet()) {
			typeMappingCodeBuilder.append(getTypeMappingCode(typeMappingTemplate, pair.getKey(), pair.getValue()));
			typeMappingCodeBuilder.append(nl());
		}
		return typeMappingCodeBuilder.toString();
	}

	private String getTypeMappingCode(final ST typeMappingTemplate, final String metamodelType, final Object cType) {
		typeMappingTemplate.remove("orig");
		typeMappingTemplate.remove("replaced");
		typeMappingTemplate.add("orig", metamodelType);
		typeMappingTemplate.add("replaced", cType);
		final String typeMappingCode = typeMappingTemplate.render();
		return typeMappingCode;
	}

	/**
	 * Gets a String with Typedefs from EType to the C language Type.
	 */
	private String getAllBuiltInMappings(final GenClass tcClass) {
		final StringBuilder result = new StringBuilder();
		if (tcClass == null) {
			for (final CMoflonBuiltInTypes t : CMoflonBuiltInTypes.values()) {
				result.append("typedef " + CMoflonBuiltInTypes.getCType(t) + " " + t.name() + ";");
				result.append(nl2());
			}
		}
		return result.toString();
	}

	private String getDefaultTypedefs(final GenClass tcClass) throws CoreException {
		final StringBuilder result = new StringBuilder();
		result.append("// --- Begin of default cMoflon type definitions").append(nl());
		final String urlString = String.format("platform:/plugin/%s/resources/structs.c",
				WorkspaceHelper.getPluginId(getClass()));
		try {
			final URL url = new URL(urlString);
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(url.openConnection().getInputStream()))) {
				result.append(reader.lines().collect(Collectors.joining(nl())));
			} catch (final IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()),
						"Failed to read default typedefs from " + url.toString(), e));
			}
		} catch (final MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()),
					"Invalid URL : " + urlString, e));
		}
		result.append(nl()).append("// --- End of default cMoflon type definitions").append(nl2());
		return result.toString();
	}

	private String getUserDefinedTypedefs(final GenClass tcClass) throws CoreException {
		final StringBuilder result = new StringBuilder();
		final String projectRelativePath = "injection/custom-typedefs.c";
		result.append("// --- Begin of user-defined algorithm-independent type definitions (Path: '"
				+ projectRelativePath + "')").append(nl());
		final IFile helperFile = getProject().getFile(projectRelativePath);
		if (helperFile.exists()) {
			final InputStream stream = helperFile.getContents();
			try {
				result.append(IOUtils.toString(stream));
			} catch (final IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()),
						"Failed to read user-defined helpers " + helperFile, e));
			} finally {
				IOUtils.closeQuietly(stream);
			}
		} else {
			createInjectionFolder();
			WorkspaceHelper.addFile(getProject(), projectRelativePath,
					"// Algorithm-independent type definitions." + nl(), new NullProgressMonitor());
		}
		result.append("// --- End of user-defined algorithm-independent type definitions").append(nl2());
		if (tcClass != null) {
			result.delete(0, result.length());
		}
		return result.toString();

	}

	private String getUnimplementedMethodsCode(final STGroup stg, final GenClass tcClass) {
		final StringBuilder builder = new StringBuilder();
		final Map<GenClass, List<MethodAttribute>> cachedMethodSignatures = this.getBuildProcessConfigurationProvider()
				.getCachedMethodSignatures();
		final ST methoddecls = stg.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_METHOD_DECLARATION);
		methoddecls.add("methods", cachedMethodSignatures.get(TC_INDEPENDENT_CLASS));
		builder.append(methoddecls.render());
		return builder.toString();
	}

	private String getAccessorsCode(final STGroup stg, final GenClass tcClass) {

		final StringBuilder builder = new StringBuilder();
		final Map<GenClass, List<FieldAttribute>> cachedFields = this.getBuildProcessConfigurationProvider()
				.getCachedFields();
		final ST declarations = stg.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_DECLARATION);
		declarations.add("fields", cachedFields.get(TC_INDEPENDENT_CLASS));
		builder.append(declarations.render());
		return builder.toString();
	}

	private String getComparisonFunctionsCode(final STGroup stg, final GenClass tcClass) {
		final StringBuilder builder = new StringBuilder();
		final ST compare = stg.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_COMPARE_DECLARATION);
		compare.add("types", getBuiltInTypes());
		builder.append(compare.render());
		compare.remove("types");
		compare.add("types", getTypesFromGenModel(tcClass));
		builder.append(compare.render());
		return builder.toString();
	}

	private String getEqualsFunctionsCode(final STGroup stg, final GenClass tcClass) {
		final StringBuilder builder = new StringBuilder();
		final ST equals = stg.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_EQUALS_DELCARATION);
		equals.add("types", getBuiltInTypes());
		builder.append(equals.render());
		equals.remove("types");
		equals.add("types", getTypesFromGenModel(tcClass));
		builder.append(equals.render());
		return builder.toString();
	}

	private List<Type> getBuiltInTypes() {
		final List<Type> result = new ArrayList<Type>();
		// Add built in Types
		for (final CMoflonBuiltInTypes t : CMoflonBuiltInTypes.values()) {
			result.add(new Type(true, t.name()));
		}
		return result;
	}

	/**
	 * Gets a List of Types to generate compare and equals methods for
	 *
	 * @return
	 */
	private List<Type> getTypesFromGenModel(final GenClass tcClass) {
		final List<Type> result = new ArrayList<Type>();
		// Add non built in Types
		for (final GenPackage p : getGenModel().getGenPackages()) {
			for (final GenClass clazz : p.getGenClasses()) {
				if (clazz.getName().contentEquals("Node") || clazz.getName().contentEquals("Link")
						|| clazz.getName().contentEquals(DEFAULT_TC_PARENT_CLASS_NAME)) {
					result.add(new Type(false, clazz.getName()));
				}
			}
		}
		return result;
	}

	private String getHeaderTailCode(final GenClass tcClass, final STGroup stg) {
		final ST end = stg.getInstanceOf(CMoflonTemplateConstants.CMOFLON_HEADER_CONSTANTS_END);
		return end.render() + nl();
	}
}

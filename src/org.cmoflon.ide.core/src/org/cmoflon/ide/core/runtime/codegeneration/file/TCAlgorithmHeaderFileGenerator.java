package org.cmoflon.ide.core.runtime.codegeneration.file;

import static org.cmoflon.ide.core.utilities.FormattingUtils.nl;
import static org.cmoflon.ide.core.utilities.FormattingUtils.nl2;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.Validate;
import org.cmoflon.ide.core.runtime.codegeneration.BuildProcessConfigurationProvider;
import org.cmoflon.ide.core.runtime.codegeneration.CMoflonTemplateConstants;
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

public class TCAlgorithmHeaderFileGenerator extends AbstractFileGenerator {

	public TCAlgorithmHeaderFileGenerator(final IProject project, final GenModel genModel,
			final DemoclesGeneratorAdapterFactory codeGenerationEngine,
			final BuildProcessConfigurationProvider buildProcessConfigurationProvider) {
		super(project, genModel, codeGenerationEngine, buildProcessConfigurationProvider);
	}

	/**
	 * Generates the Header File including, constants, includes, method
	 * declarations, accessor declarations as well as declarations for compare and
	 * equals operations
	 *
	 * @param tcClass
	 *                    needed for naming
	 */
	void generateHeaderFile(final GenClass tcClass, final IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMon = SubMonitor.convert(monitor, "Generate header for " + tcClass, 10);
		final STGroup templateGroup = getTemplateConfigurationProvider()
				.getTemplateGroup(CMoflonTemplateConstants.HEADER_FILE_GENERATOR);
		templateGroup.registerRenderer(String.class, new CMoflonStringRenderer());

		final StringBuilder contents = new StringBuilder();
		contents.append(getDateCommentCode());
		contents.append(getIncludeGuardForAlgorithm(tcClass));
		contents.append(getIncludesCode(templateGroup, tcClass));
		contents.append(getConstantsDefinitionsCode(tcClass, templateGroup));
		contents.append(getGenerateDuplicatesDefinition(tcClass));
		contents.append(getUserDefinedTypedefs(tcClass));
		contents.append(getUnimplementedMethodsCode(templateGroup, tcClass));
		contents.append(getAccessorsCode(templateGroup, tcClass));
		contents.append(getComparisonFunctionsCode(templateGroup, tcClass));
		contents.append(getEqualsFunctionsCode(templateGroup, tcClass));
		contents.append(getHeaderTailCode(tcClass, templateGroup));
		subMon.worked(8);

		final String parentFolderForAlgorithm = this.getBuildProcessConfigurationProvider()
				.getProjectRelativePathForAlgorithm(tcClass);
		final String outputFileName = parentFolderForAlgorithm + getAlgorithmBasename(tcClass) + ".h";
		final IFile headerFile = getProject().getFile(outputFileName);
		if (!headerFile.exists()) {
			WorkspaceHelper.addAllFolders(getProject(), parentFolderForAlgorithm, subMon.split(1));
			WorkspaceHelper.addFile(getProject(), outputFileName, contents.toString(), subMon.split(1));
		} else {
			headerFile.setContents(new ReaderInputStream(new StringReader(contents.toString())), true, true,
					subMon.split(2));
		}
	}

	private String getIncludesCode(final STGroup templateGroup, final GenClass tcClass) {
		return "#include \"cMoflon.h\" " + nl();
	}

	private String getIncludeGuardForAlgorithm(final GenClass tcClass) {
		final STGroup templateGroup = getTemplateConfigurationProvider()
				.getTemplateGroup(CMoflonTemplateConstants.HEADER_FILE_GENERATOR);
		final ST definition = templateGroup.getInstanceOf(CMoflonTemplateConstants.HEADER_DEFINITION);
		final String algorithmName = tcClass.getName();
		definition.add("comp", getComponentName().toUpperCase());
		definition.add("algo", algorithmName.toUpperCase());
		return definition.render();
	}

	private StringBuilder getConstantsDefinitionsCode(final GenClass tcClass, final STGroup templateGroup) {
		Validate.notNull(tcClass);
		final Map<GenClass, Map<String, String>> constantsMapping = this.getBuildProcessConfigurationProvider()
				.getConstantsMapping();
		final StringBuilder constantsCode = new StringBuilder();
		if (constantsMapping.containsKey(tcClass)) {
			for (final Entry<String, String> pair : constantsMapping.get(tcClass).entrySet()) {
				constantsCode.append(
						generateConstant(pair.getKey(), pair.getValue(), getComponentName(), tcClass, templateGroup));
			}
		}

		return constantsCode;
	}

	private String getUnimplementedMethodsCode(final STGroup stg, final GenClass tcClass) {
		final StringBuilder builder = new StringBuilder();
		final ST methoddecls = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_METHOD_DECLARATION);
		final Map<GenClass, List<MethodAttribute>> cachedMethodSignatures = this.getBuildProcessConfigurationProvider()
				.getCachedMethodSignatures();
		methoddecls.add("methods", cachedMethodSignatures.get(tcClass));
		builder.append(methoddecls.render());
		return builder.toString();
	}

	private String getAccessorsCode(final STGroup stg, final GenClass tcClass) {

		final StringBuilder builder = new StringBuilder();
		final Map<GenClass, List<FieldAttribute>> cachedFields = this.getBuildProcessConfigurationProvider()
				.getCachedFields();
		final ST declarations = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_DECLARATION);
		declarations.add("fields", cachedFields.get(tcClass));
		builder.append(declarations.render());
		return builder.toString();
	}

	private String getComparisonFunctionsCode(final STGroup stg, final GenClass tcClass) {
		final StringBuilder builder = new StringBuilder();
		final ST compare = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_COMPARE_DECLARATION);
		compare.add("types", getTypesFromGenModel(tcClass));
		builder.append(compare.render());
		return builder.toString();
	}

	private String getEqualsFunctionsCode(final STGroup stg, final GenClass tcClass) {
		final StringBuilder builder = new StringBuilder();
		final ST equals = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_EQUALS_DELCARATION);
		equals.add("types", getTypesFromGenModel(tcClass));
		builder.append(equals.render());
		return builder.toString();
	}

	private String getHeaderTailCode(final GenClass tcClass, final STGroup stg) {
		final ST end = stg.getInstanceOf(CMoflonTemplateConstants.HEADER_CONSTANTS_END);
		end.add("comp", getComponentName().toUpperCase());
		end.add("algo", tcClass.getName().toUpperCase());
		return end.render() + nl();
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
				if (!(isNodeClass(clazz) || isLinkClass(clazz) || isTopologyControlParentClass(clazz))
						&& isTrueSubtypeOfTCAlgorithmParentClass(clazz)
						&& clazz.getName().contentEquals(tcClass.getName())) {
					result.add(new Type(false, clazz.getName()));
				}
			}
		}
		return result;
	}

	private boolean isTopologyControlParentClass(final GenClass clazz) {
		return clazz.getName().contentEquals(DEFAULT_TC_PARENT_CLASS_NAME);
	}

	private boolean isLinkClass(final GenClass clazz) {
		return clazz.getName().contentEquals("Link");
	}

	private boolean isNodeClass(final GenClass clazz) {
		return clazz.getName().contentEquals("Node");
	}

	private String getUserDefinedTypedefs(final GenClass tcClass) throws CoreException {
		final StringBuilder result = new StringBuilder();
		// Insert algorithm specific typedefs
		if (tcClass == null) {
			return result.toString();
		}
		final List<GenClass> tcClasses = this.getBuildProcessConfigurationProvider().getTcClasses();
		final StringBuilder algorithmSpecificContent = new StringBuilder();
		for (final GenClass algorithmClass : tcClasses) {
			final String algorithmName = algorithmClass.getName();
			final String projectRelativePath = "injection/custom-typedefs_" + algorithmName + ".c";
			algorithmSpecificContent.append("// --- Begin of user-defined type definitions for " + algorithmName
					+ "(Path: '" + projectRelativePath + "')").append(nl());
			final IFile helperFile = getProject().getFile(projectRelativePath);
			if (helperFile.exists()) {
				final InputStream stream = helperFile.getContents();
				try {
					algorithmSpecificContent.append(IOUtils.toString(stream));
				} catch (final IOException e) {
					throw new CoreException(new Status(IStatus.ERROR, WorkspaceHelper.getPluginId(getClass()),
							"Failed to read user-defined helpers " + helperFile, e));
				} finally {
					IOUtils.closeQuietly(stream);
				}
			} else {
				createInjectionFolder();
				WorkspaceHelper.addFile(getProject(), projectRelativePath,
						"// Type definitions for algorithm '" + algorithmName + "'." + nl(), new NullProgressMonitor());
			}
			algorithmSpecificContent.append("// --- End of user-defined type definitions for " + algorithmName)
					.append(nl2());
		}
		if (tcClass != null) {
			result.append(algorithmSpecificContent.toString());
		}
		return result.toString();

	}

	private String generateConstant(final Object key, final Object value, final String component,
			final GenClass tcClass, final STGroup templateGroup) {
		final ST constantTemplate = templateGroup.getInstanceOf(CMoflonTemplateConstants.HEADER_CONSTANTS_DEFINITION);
		constantTemplate.add("comp", component);
		constantTemplate.add("algo", tcClass.getName());
		constantTemplate.add("name", key);
		constantTemplate.add("value", value);
		return constantTemplate.render();
	}

	private String getGenerateDuplicatesDefinition(final GenClass tcClass) {
		final Set<String> generateDuplicates = this.getBuildProcessConfigurationProvider().getGenerateDuplicates();
		final StringBuilder mycontents = new StringBuilder();
		if (generateDuplicates.contains(tcClass.getName())) {
			mycontents.append("#ifndef COMPONENT_TOPOLOGYCONTROL_GENERATE_DUPLICATES").append(nl());
			mycontents.append("#define COMPONENT_TOPOLOGYCONTROL_GENERATE_DUPLICATES").append(nl());
			mycontents.append("#endif").append(nl());
			mycontents.append("LIST(list_duplicates);" + nl()).append(nl());
			return mycontents.toString();
		} else {
			return "";
		}
	}
}

package org.cmoflon.ide.core.runtime.codegeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cmoflon.ide.core.runtime.codegeneration.utilities.FieldAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.MethodAttribute;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenDataType;
import org.eclipse.emf.codegen.ecore.genmodel.GenEnum;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EDataType;
import org.moflon.core.utilities.WorkspaceHelper;

public class BuildProcessConfigurationProvider {

	private int maximumMatchCount;
	private final Map<String, String> typeMappings = new HashMap<>();

	private final Map<GenClass, List<MethodAttribute>> cachedMethodSignatures = new HashMap<>();

	private final Map<GenClass, List<FieldAttribute>> cachedFields = new HashMap<>();

	private final List<GenClass> cachedConcreteClasses = new ArrayList<>();

	private final Map<GenClass, StringBuilder> cachedPatternMatchingCode = new HashMap<>();

	private final List<String> blockDeclarations = new ArrayList<>();

	/**
	 * List of built-in types provided by ECore
	 */
	private final List<String> builtInTypes;
	/**
	 * Contains all algorithm names for which dropping unidirectional edges should
	 * be inactive
	 */
	private final Set<String> dropUnidirectionalEdgesOff = new HashSet<>();

	/**
	 * Contains all algorithm names for which hop-count data are requested
	 */
	private final Set<String> useHopCountProcess = new HashSet<>();

	private final List<GenClass> tcClasses = new ArrayList<>();

	private final Map<GenClass, List<String>> helperClasses = new HashMap<>();

	private final Map<GenClass, String> tcAlgorithmCallParameters = new HashMap<>();

	// Contains all algorithm names for which evaluation statements were requested
	private final Set<String> useEvalStatements = new HashSet<>();

	// Contains all algorithm names for which duplicate generation is requested
	private final Set<String> generateDuplicates = new HashSet<>();

	/**
	 * Constants mapping Key: constant name Value: constant value to be used as is
	 * during code generation
	 */
	private final Map<GenClass, Map<String, String>> constantsMapping = new HashMap<>();
	private final GenModel genModel;

	public BuildProcessConfigurationProvider(final GenModel genModel) {
		this.genModel = genModel;
		this.builtInTypes = determineBuiltInTypes();
	}

	private List<String> determineBuiltInTypes() {
		final List<String> builtInTypes = new ArrayList<String>();

		final EList<GenDataType> dataTypes = getGenModel().getEcoreGenPackage().getGenDataTypes();

		for (final GenDataType obj : dataTypes) {
			if (obj.getEcoreDataType() instanceof EDataType) {
				builtInTypes.add(obj.getEcoreDataType().getName());
			}
		}

		final EList<GenEnum> enums = getGenModel().getGenPackages().get(0).getGenEnums();
		for (final GenEnum eEnum : enums) {
			builtInTypes.add(eEnum.getName());
		}

		return builtInTypes;
	}

	public void setMaximumMatchCount(final int maximumMatchCount) {
		this.maximumMatchCount = maximumMatchCount;
	}

	public int getMaximumMatchCount() {
		return maximumMatchCount;
	}

	public Map<String, String> getTypeMappings() {
		return typeMappings;
	}

	public String getProjectRelativePathForAlgorithm(final GenClass tcClass) {
		return WorkspaceHelper.GEN_FOLDER + "/";
	}

	/**
	 * @return the cachedMethodSignatures
	 */
	public Map<GenClass, List<MethodAttribute>> getCachedMethodSignatures() {
		return cachedMethodSignatures;
	}

	/**
	 * @return the cachedFields
	 */
	public Map<GenClass, List<FieldAttribute>> getCachedFields() {
		return cachedFields;
	}

	/**
	 * @return the cachedConcreteClasses
	 */
	public List<GenClass> getCachedConcreteClasses() {
		return cachedConcreteClasses;
	}

	/**
	 * @return the cachedPatternMatchingCode
	 */
	public Map<GenClass, StringBuilder> getCachedPatternMatchingCode() {
		return cachedPatternMatchingCode;
	}

	/**
	 * @return the blockDeclarations
	 */
	public List<String> getBlockDeclarations() {
		return blockDeclarations;
	}

	public void resetCaches() {
		getCachedMethodSignatures().clear();
		getCachedFields().clear();
		getCachedConcreteClasses().clear();
		getBlockDeclarations().clear();
	}

	/**
	 * @return the genModel
	 */
	public GenModel getGenModel() {
		return genModel;
	}

	public List<String> getBuiltInTypes() {
		return builtInTypes;
	}

	public Set<String> getDropUnidirectionalEdgesOff() {
		return dropUnidirectionalEdgesOff;
	}

	public Set<String> getUseHopCountProcess() {
		return useHopCountProcess;
	}

	public List<GenClass> getTcClasses() {
		return tcClasses;
	}

	public Map<GenClass, List<String>> getHelperClasses() {
		return helperClasses;
	}

	public Map<GenClass, String> getTcAlgorithmCallParameters() {
		return tcAlgorithmCallParameters;
	}

	public Set<String> getUseEvalStatements() {
		return useEvalStatements;
	}

	public Set<String> getGenerateDuplicates() {
		return generateDuplicates;
	}

	public Map<GenClass, Map<String, String>> getConstantsMapping() {
		return constantsMapping;
	}

}

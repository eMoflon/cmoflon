package org.cmoflon.ide.core.runtime.codegeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cmoflon.ide.core.runtime.codegeneration.utilities.FieldAttribute;
import org.cmoflon.ide.core.runtime.codegeneration.utilities.MethodAttribute;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.moflon.core.utilities.WorkspaceHelper;

public class BuildProcessConfigurationProvider {

	private int maximumMatchCount;
	private final Map<String, String> typeMappings = new HashMap<>();

	private final Map<GenClass, List<MethodAttribute>> cachedMethodSignatures = new HashMap<>();

	private final Map<GenClass, List<FieldAttribute>> cachedFields = new HashMap<>();

	private final List<GenClass> cachedConcreteClasses = new ArrayList<>();

	private final Map<GenClass, StringBuilder> cachedPatternMatchingCode = new HashMap<>();

	private final List<String> blockDeclarations = new ArrayList<>();

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
}

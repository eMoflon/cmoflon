package org.cmoflon.ide.core.runtime.codegeneration.utilities;

/**
 * Simple Type representation for the Code Generation. A Type is either built in
 * or not and has a name.
 *
 * @author David Giessing
 *
 */
public class Type {

	private final boolean builtIn;

	private final String name;

	public Type(final boolean builtIn, final String name) {
		this.builtIn = builtIn;
		this.name = name;
	}

	public boolean isBuiltIn() {
		return builtIn;
	}

	public String getName() {
		return name;
	}

	public boolean isBoolean() {
		if (name.toLowerCase().contains("boolean")) {
			return true;
		} else {
			return false;
		}
	}
}

package org.cmoflon.ide.core.runtime.codegeneration.utilities;

/**
 * Represents a method. A method can have parameters. Parameters are stored all
 * together as a String.
 *
 * @author david
 *
 */
public class MethodAttribute extends ClassAttribute {

	private final String parameters;

	public MethodAttribute(final Type owningtype, final Type type, final String name, final String parameters) {
		super(owningtype, type, name);
		this.parameters = parameters;
	}

	public String getParameters() {
		return this.parameters;
	}
}

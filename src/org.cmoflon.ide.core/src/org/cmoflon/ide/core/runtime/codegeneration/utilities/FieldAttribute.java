package org.cmoflon.ide.core.runtime.codegeneration.utilities;

/**
 * Represents a field. In addition to the fields of a class attributes fields
 * can be of list type.
 * 
 * @author David Giessing
 *
 */
public class FieldAttribute extends ClassAttribute {

	private boolean islist;

	public FieldAttribute(Type owningType, Type type, String name, boolean isList) {
		super(owningType, type, name);
		this.islist = isList;
	}

	public boolean getIslist() {
		return this.islist;
	}
}

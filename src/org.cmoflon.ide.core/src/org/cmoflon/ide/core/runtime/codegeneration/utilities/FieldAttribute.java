package org.cmoflon.ide.core.runtime.codegeneration.utilities;

/**
 * Represents a field. In addition to the fields of a class attributes fields
 * can be of list type.
 *
 * @author David Giessing
 *
 */
public class FieldAttribute extends ClassAttribute {

	private final boolean islist;

	public FieldAttribute(final Type owningType, final Type type, final String name, final boolean isList) {
		super(owningType, type, name);
		this.islist = isList;
	}

	public boolean getIslist() {
		return this.islist;
	}

	@Override
	public String toString() {
		return "FieldAttribute [islist=" + islist + ", getOwningType()=" + getOwningType() + ", getType()=" + getType()
				+ ", getName()=" + getName() + "]";
	}

}

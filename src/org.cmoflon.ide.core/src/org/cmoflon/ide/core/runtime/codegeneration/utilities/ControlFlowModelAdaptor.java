package org.cmoflon.ide.core.runtime.codegeneration.utilities;

import java.util.ArrayList;
import java.util.List;

import org.moflon.sdm.runtime.democles.Action;
import org.moflon.sdm.runtime.democles.CFNode;
import org.moflon.sdm.runtime.democles.CFVariable;
import org.moflon.sdm.runtime.democles.ContinueStatement;
import org.moflon.sdm.runtime.democles.PatternInvocation;
import org.moflon.sdm.runtime.democles.RepetitionNode;
import org.moflon.sdm.runtime.democles.VariableReference;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.ObjectModelAdaptor;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

/**
 * Copy of EMoflons ControlFlowModelAdaptor. Could not be imported because the
 * package is not exported.
 *
 */
public class ControlFlowModelAdaptor extends ObjectModelAdaptor {
	public Object getProperty(final Interpreter interpreter, final ST template, final Object object,
			final Object property, final String propertyName) throws STNoSuchPropertyException {
		if (object instanceof VariableReference && "index".equals(propertyName)) {
			final VariableReference varRef = (VariableReference) object;
			return varRef.getInvocation().getParameters().indexOf(varRef);
		}
		if (object instanceof CFVariable && "index".equals(propertyName)) {
			final CFVariable variable = (CFVariable) object;
			final Action action = variable.getConstructor();
			if (action instanceof PatternInvocation) {
				final PatternInvocation invocation = (PatternInvocation) action;
				final List<VariableReference> parameters = invocation.getParameters();
				for (int i = 0; i < parameters.size(); i++) {
					if (parameters.get(i).getFrom() == variable) {
						return i;
					}
				}
			}
		}
		if (object instanceof PatternInvocation) {
			final PatternInvocation invocation = (PatternInvocation) object;
			if ("boundParameters".equals(propertyName)) {
				final ArrayList<VariableReference> boundParameters = new ArrayList<VariableReference>(
						invocation.getParameters().size());
				for (final VariableReference reference : invocation.getParameters()) {
					if (!reference.isFree()) {
						boundParameters.add(reference);
					}
				}
				return boundParameters;
			} else if ("freeParameters".equals(propertyName)) {
				final ArrayList<VariableReference> freeParameters = new ArrayList<VariableReference>(
						invocation.getParameters().size());
				for (final VariableReference reference : invocation.getParameters()) {
					if (reference.isFree()) {
						freeParameters.add(reference);
					}
				}
				return freeParameters;
			} else if ("id".equals(propertyName)) {
				return invocation.getCfNode().getId() + "_"
						+ invocation.getPattern().eResource().getURI().fileExtension();
			}
		}
		if (object instanceof RepetitionNode && "onlyShortcuts".equals(propertyName)) {
			final RepetitionNode repetitionNode = (RepetitionNode) object;
			for (final CFNode lastNode : repetitionNode.getLastNodes()) {
				if (!(lastNode instanceof ContinueStatement)) {
					return false;
				}
			}
			return true;
		}
		return super.getProperty(interpreter, template, object, property, propertyName);
	}

}

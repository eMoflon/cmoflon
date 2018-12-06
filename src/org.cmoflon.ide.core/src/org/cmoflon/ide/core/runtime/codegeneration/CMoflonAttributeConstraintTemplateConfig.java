package org.cmoflon.ide.core.runtime.codegeneration;

import java.util.List;

import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.gervarro.democles.codegen.OperationSequenceCompiler;
import org.gervarro.democles.codegen.PatternInvocationConstraintTemplateProvider;
import org.gervarro.democles.codegen.emf.EMFTemplateProvider;
import org.gervarro.democles.relational.RelationalConstraintTemplateProvider;
import org.moflon.compiler.sdm.democles.DefaultCodeGeneratorConfig;
import org.moflon.compiler.sdm.democles.attributes.AttributeConstraintsTemplateProvider;
import org.moflon.sdm.constraints.operationspecification.AttributeConstraintLibrary;
import org.moflon.sdm.constraints.operationspecification.AttributeConstraintsOperationActivator;
import org.moflon.sdm.constraints.operationspecification.OperationSpecificationGroup;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupString;

/**
 * @author David Giessing
 */
class CMoflonAttributeConstraintTemplateConfig extends CMoflonTemplateConfiguration {
	CMoflonAttributeConstraintTemplateConfig(final GenModel genModel,
			final java.util.List<AttributeConstraintLibrary> attributeConstraintLibs) {
		super(genModel);
		addAttrConstTemplatesToBlackTemplates(attributeConstraintLibs);
		operationSequenceCompilers.put(DefaultCodeGeneratorConfig.BLACK_PATTERN_MATCHER_GENERATOR,
				createBlackOperationSequenceCompiler());
	}

	@SuppressWarnings("unchecked")
	private static OperationSequenceCompiler createBlackOperationSequenceCompiler() {
		return new OperationSequenceCompiler(new PatternInvocationConstraintTemplateProvider(),
				new RelationalConstraintTemplateProvider(), new EMFTemplateProvider(),
				new AttributeConstraintsTemplateProvider());
	}

	private void addAttrConstTemplatesToBlackTemplates(final List<AttributeConstraintLibrary> attributeConstraintLibs) {
		final STGroup group = getTemplateGroup(DefaultCodeGeneratorConfig.BLACK_PATTERN_MATCHER_GENERATOR);
		for (final AttributeConstraintLibrary library : attributeConstraintLibs) {

			for (final OperationSpecificationGroup opSpecGroup : library.getOperationSpecifications()) {
				if (!opSpecGroup.isTemplateGroupGenerated()) {
					opSpecGroup.gernerateTemplate();
				}

				final STGroup newGroup = new STGroupString("someName", opSpecGroup.getTemplateGroupString(),
						AttributeConstraintsOperationActivator.OUTER_ST_DELIMITER,
						AttributeConstraintsOperationActivator.OUTER_ST_DELIMITER);
				for (final String templateName : newGroup.getTemplateNames()) {
					final ST template = newGroup.getInstanceOf(templateName);
					group.rawDefineTemplate(
							"/" + library.getPrefix() + "/" + opSpecGroup.getOperationIdentifier() + template.getName(),
							template.impl, null);
				}

			}

		}

	}

}

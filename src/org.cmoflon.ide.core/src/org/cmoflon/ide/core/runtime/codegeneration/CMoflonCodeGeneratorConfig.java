package org.cmoflon.ide.core.runtime.codegeneration;

import java.io.IOException;

import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.moflon.compiler.sdm.democles.DefaultValidatorConfig;
import org.moflon.compiler.sdm.democles.ExpressionPatternMatcherGenerator;
import org.moflon.compiler.sdm.democles.PatternMatcherCompiler;
import org.moflon.compiler.sdm.democles.RegularPatternMatcherGenerator;
import org.moflon.core.utilities.preferences.EMoflonPreferencesStorage;
import org.moflon.sdm.compiler.democles.validation.scope.PatternMatcher;

/**
 * Overrides {@link DefaultValidatorConfig}. 
 * Needed to obtain correct {@link CMoflonTemplateConfiguration}.
 * 
 * @author David Giessing
 *
 */
public class CMoflonCodeGeneratorConfig extends DefaultValidatorConfig
{

   /**
    * Pattern matcher ID for binding+black patterns
    */
   public static final String BINDING_AND_BLACK_PATTERN_MATCHER_GENERATOR = "BindingAndBlackPatternMatcherGenerator";

   /**
    * Pattern matcher ID for binding patterns
    */
   public static final String BINDING_PATTERN_MATCHER_GENERATOR = "BindingPatternMatcherGenerator";

   /**
    * Pattern matcher ID for black patterns
    */
   public static final String BLACK_PATTERN_MATCHER_GENERATOR = "BlackPatternMatcherGenerator";

   /**
    * Pattern matcher ID for red patterns
    */
   public static final String RED_PATTERN_MATCHER_GENERATOR = "RedPatternMatcherGenerator";

   /**
    * Pattern matcher ID for green patterns
    */
   public static final String GREEN_PATTERN_MATCHER_GENERATOR = "GreenPatternMatcherGenerator";

   /**
    * Pattern matcher ID for expression patterns
    */
   public static final String EXPRESSION_PATTERN_MATCHER_GENERATOR = "ExpressionPatternMatcherGenerator";
   
   /**
    * The cached preferences store
    */
   private final EMoflonPreferencesStorage preferencesStorage;

   /**
    * Initializes this configuration with the build process's {@link ResourceSet} and the {@link EMoflonPreferencesStorage} to consult for configuration parameters.
    * @param resourceSet
    * @param preferencesStorage
    */
   public CMoflonCodeGeneratorConfig(final ResourceSet resourceSet, final EMoflonPreferencesStorage preferencesStorage)
   {
      super(resourceSet);

      this.preferencesStorage = preferencesStorage;
   }

   /**
    * Returns a preconfigured {@link CMoflonTemplateConfiguration} for the given {@link GenModel}.
    */
   @Override
   public CMoflonTemplateConfiguration createTemplateConfiguration(final GenModel genModel)
   {
      return new CMoflonTemplateConfiguration(genModel);
   }

   /**
    * Creates and returns a pattern matcher for binding+black patterns
    */
   @Override
   protected PatternMatcher configureBindingAndBlackPatternMatcher(final Resource resource) throws IOException
   {
      final PatternMatcherCompiler bindingAndBlackPatternMatcherCompiler = configureBindingAndBlackPatternMatcherCompiler(resource);
      final RegularPatternMatcherGenerator bindingAndBlackPatternMatcherGenerator = new RegularPatternMatcherGenerator(bindingAndBlackPatternMatcherCompiler,
            BINDING_AND_BLACK_PATTERN_MATCHER_GENERATOR, getPreferencesStorage());
      resource.getContents().add(bindingAndBlackPatternMatcherGenerator);
      return bindingAndBlackPatternMatcherGenerator;
   }

   /**
    * Creates and returns a pattern matcher for binding patterns
    */
   @Override
   protected PatternMatcher configureBindingPatternMatcher(final Resource resource) throws IOException
   {
      final PatternMatcherCompiler bindingPatternMatcherCompiler = configureBindingPatternMatcherCompiler(resource);
      final RegularPatternMatcherGenerator bindingPatternMatcherGenerator = new RegularPatternMatcherGenerator(bindingPatternMatcherCompiler,
            BINDING_PATTERN_MATCHER_GENERATOR, getPreferencesStorage());
      resource.getContents().add(bindingPatternMatcherGenerator);
      return bindingPatternMatcherGenerator;
   }

   /**
    * Creates and returns a pattern matcher for black patterns
    */
   @Override
   protected PatternMatcher configureBlackPatternMatcher(final Resource resource) throws IOException
   {
      final PatternMatcherCompiler blackPatternMatcherCompiler = configureBlackPatternMatcherCompiler(resource);
      final RegularPatternMatcherGenerator blackPatternMatcherGenerator = new RegularPatternMatcherGenerator(blackPatternMatcherCompiler,
            BLACK_PATTERN_MATCHER_GENERATOR, getPreferencesStorage());
      resource.getContents().add(blackPatternMatcherGenerator);
      return blackPatternMatcherGenerator;
   }

   /**
    * Creates and returns a pattern matcher for red patterns
    */
   @Override
   protected PatternMatcher configureRedPatternMatcher(final Resource resource) throws IOException
   {
      final PatternMatcherCompiler redPatternMatcherCompiler = configureRedPatternMatcherCompiler(resource);
      final RegularPatternMatcherGenerator redPatternMatcherGenerator = new RegularPatternMatcherGenerator(redPatternMatcherCompiler,
            RED_PATTERN_MATCHER_GENERATOR, getPreferencesStorage());
      resource.getContents().add(redPatternMatcherGenerator);
      return redPatternMatcherGenerator;
   }

   /**
    * Creates and returns a pattern matcher for green patterns
    */
   @Override
   protected PatternMatcher configureGreenPatternMatcher(final Resource resource) throws IOException
   {
      final PatternMatcherCompiler greenPatternMatcherCompiler = configureGreenPatternMatcherCompiler(resource);
      final RegularPatternMatcherGenerator greenPatternMatcherGenerator = new RegularPatternMatcherGenerator(greenPatternMatcherCompiler,
            GREEN_PATTERN_MATCHER_GENERATOR, getPreferencesStorage());
      resource.getContents().add(greenPatternMatcherGenerator);
      return greenPatternMatcherGenerator;
   }

   /**
    * Creates and returns a pattern matcher for expression patterns
    */
   @Override
   protected PatternMatcher configureExpressionPatternMatcher(final Resource resource) throws IOException
   {
      final PatternMatcherCompiler expressionPatternMatcherCompiler = configureExpressionPatternMatcherCompiler(resource);
      final ExpressionPatternMatcherGenerator expressionPatternMatcherGenerator = new ExpressionPatternMatcherGenerator(expressionPatternMatcherCompiler,
            EXPRESSION_PATTERN_MATCHER_GENERATOR, getPreferencesStorage());
      resource.getContents().add(expressionPatternMatcherGenerator);
      return expressionPatternMatcherGenerator;
   }

   /**
    * Returns the cached preferences store
    * @return
    */
   protected final EMoflonPreferencesStorage getPreferencesStorage()
   {
      return preferencesStorage;
   }
}

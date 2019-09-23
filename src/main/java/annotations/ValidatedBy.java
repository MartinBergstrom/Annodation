package annotations;

import main.Validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation supposed to be added to the different field-validator
 * annotations in order to provide a list of the available validators for the given
 * field/annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ValidatedBy
{

    Class<? extends Validator<?>>[] implClass();

}

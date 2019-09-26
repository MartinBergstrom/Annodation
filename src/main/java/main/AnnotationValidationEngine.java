package main;

import annotations.ValidateWith;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationValidationEngine
{
    /**
     * Main entry point to perform validation.
     *
     * Will search recursively for all fields/classes containing the meta-annotation {@link ValidateWith} then trigger
     * the validator for the given type (if provided)
     *
     * @param obj the java bean to run validation on, including it's fields recursively
     * @param <T> the type of object
     *
     */
    public static <T> void runValidationOnBean(T obj)
    {
        runValidationOnFieldsWithAnnotationValidation(obj);
    }

    @SuppressWarnings("unchecked")
    private static <V, T> V getValueFromField(Field field, T object)
    {
        try {
            field.setAccessible(true);
            return (V) field.get(object);
        } catch (IllegalAccessException e) {
            throw new AnnodationSetupException("Exception during validation occurred, could not obtain value");
        }
    }

    private static <T> void runValidationOnFieldsWithAnnotationValidation(T obj)
    {
        hasValidationAnnotation(obj.getClass()).ifPresent(aClass -> performValidationOnClass(obj, aClass));
        for (Field field : obj.getClass().getDeclaredFields())
        {
            if (field.getType().getDeclaredFields().length > 0)
            {
                runValidationOnFieldsWithAnnotationValidation(getValueFromField(field, obj));
            }
            hasValidationAnnotation(field).ifPresent(aClass -> performValidationOnField(obj, field, aClass));
        }
    }

    @SuppressWarnings("unchecked")
    private static <V> void performValidationOnClass(V obj, Class<? extends Annotation> annotationClazzOnClass)
    {
        Class<? extends Validator<V>> validator = getValidator(annotationClazzOnClass, (Class<V>) obj.getClass());
        try {
            validator.newInstance().validate(obj);
        } catch (IllegalAccessException| InstantiationException e) {
            e.printStackTrace();
            throw new AnnodationSetupException("Could not instantiate Validator class to be performed on class: " + obj.getClass().getSimpleName() +
                    " . Verify Validator class visibility");
        }
    }

    private static <T, V> void performValidationOnField(T obj, Field field, Class<? extends Annotation> annotationClazzOnField)
    {
        try {
            field.setAccessible(true);
            V value = getValueFromField(field, obj);
            @SuppressWarnings("unchecked")
            Class<? extends Validator<V>> validator = getValidator(annotationClazzOnField, (Class<V>) value.getClass());
            validator.newInstance().validate(value);
        } catch (IllegalAccessException| InstantiationException e) {
            e.printStackTrace();
            throw new AnnodationSetupException("Could not instantiate Validator class to be performed on field: " + field.getName()+
                    " . Verify Validator class visibility");
        }
    }

    private static Optional<? extends Class<? extends Annotation>> hasValidationAnnotation(AnnotatedElement annotatedElement)
    {
        if (annotatedElement.getDeclaredAnnotations().length > 0)
        {
            return Arrays.stream(annotatedElement.getDeclaredAnnotations())
                    .map(Annotation::annotationType)
                    .filter(annotationType -> annotationType.isAnnotationPresent(ValidateWith.class))
                    .findFirst();
        }
        return Optional.empty();
    }

    private static <T extends Class<? extends Annotation>, V> Class<? extends Validator<V>> getValidator(T annotationClazz,
                                                                                                         Class<V> valueClazz)
    {
        Class<? extends Validator<?>>[] availableValidators = annotationClazz.getAnnotation(ValidateWith.class).implClass();
        Class<? extends Validator<V>> foundValidatorClazzMatchingValueType = findValidatorForType(annotationClazz,
                                                                                                  availableValidators,
                                                                                                  valueClazz);
        if (foundValidatorClazzMatchingValueType == null)
        {
            throw new RuntimeException("Annotation: " + annotationClazz.getSimpleName() + " has no validator for type: "
                    + valueClazz.getName() + ". Please verify validators");
        }
        return foundValidatorClazzMatchingValueType;
    }

    private static <T, V> Class<? extends Validator<V>> findValidatorForType(T annotationClazz,
                                                                             Class<? extends Validator<?>>[] validatorClazzes,
                                                                             Class<V> valueClazz)
    {
       List<Class<? extends Validator<?>>> foundMatching = Stream.of(validatorClazzes)
               .filter(validatorClazz -> validatorMatchingType(valueClazz, validatorClazz))
               .collect(Collectors.toList());
       if (foundMatching.isEmpty())
       {
           return null;
       }
       if(foundMatching.size() > 1)
       {
           throw new RuntimeException("More than one possible validator found for type: " + valueClazz.getName() +
                   " on annotation class: " + annotationClazz.getClass().getSimpleName());
       }
        @SuppressWarnings("unchecked")
        Class<? extends Validator<V>> result = (Class<? extends Validator<V>>) foundMatching.get(0);
        return result;
    }

    private static <V> boolean validatorMatchingType(Class<V> valueClazz, Class<? extends Validator<?>> validatorClazz)
    {
        if (validatorClazz.getGenericInterfaces() != null && validatorClazz.getGenericInterfaces().length == 1)
        {
            Type genericInterface = validatorClazz.getGenericInterfaces()[0];
            if (genericInterface instanceof ParameterizedType)
            {
                Type[] genericTypes = ((ParameterizedType) genericInterface).getActualTypeArguments();
                if (genericTypes != null && genericTypes.length == 1)
                {
                    return valueClazz.equals(genericTypes[0]);
                }
            }
        }
        return false;
    }

}

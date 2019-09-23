package main;

import annotations.ValidatedBy;

import java.lang.annotation.Annotation;
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
     * Will search recursively for all fields containing the meta-annotation {@link ValidatedBy} then trigger
     * the validator for the given type (if provided)
     *
     * @param obj the java bean to run validation on, including it's fields recursively
     * @param <T> the type of object
     *
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static <T> void runValidationOnBean(T obj) throws IllegalAccessException, InstantiationException
    {
        runValidationOnFieldsWithAnnotationValidation(obj);
    }

    @SuppressWarnings("unchecked")
    private static <V, T> V getValueFromField(Field field, T object) throws IllegalAccessException
    {
        return (V) field.get(object);
    }

    private static <T, V> void runValidationOnFieldsWithAnnotationValidation(T obj) throws IllegalAccessException, InstantiationException
    {
        for (Field field : obj.getClass().getDeclaredFields())
        {
            if (field.getType().getDeclaredFields().length > 0)
            {
                field.setAccessible(true);
                runValidationOnFieldsWithAnnotationValidation(field.get(obj));
            }

            Optional<? extends Class<? extends Annotation>> annotationClazz = hasValidationAnnotation(field);
            if (annotationClazz.isPresent())
            {
                field.setAccessible(true);
                V value = getValueFromField(field, obj);
                @SuppressWarnings("unchecked")
                Class<? extends Validator<V>> validator = getValidator(annotationClazz.get(), (Class<V>) value.getClass());
                validator.newInstance().validate(value);
            }
        }
    }

    private static Optional<? extends Class<? extends Annotation>> hasValidationAnnotation(Field field)
    {
        if (field.getDeclaredAnnotations().length > 0)
        {
            return Arrays.stream(field.getDeclaredAnnotations())
                    .map(Annotation::annotationType)
                    .filter(annotationType -> annotationType.isAnnotationPresent(ValidatedBy.class))
                    .findFirst();
        }
        return Optional.empty();
    }

    private static <T extends Class<? extends Annotation>, V> Class<? extends Validator<V>> getValidator(T annotationClazz, Class<V> valueClazz)
    {
        Class<? extends Validator<?>>[] availableValidators = annotationClazz.getAnnotation(ValidatedBy.class).implClass();
        Class<? extends Validator<V>> foundValidatorClazzMatchingValueType = findValidatorForType(annotationClazz, availableValidators, valueClazz);
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
package main;

import annotations.ValidateWith;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test that you can put annotation of class to validate it
 * with custom validator
 */
public class TestClassValidator {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldThrowExceptionWhenValidatorFails() {

        MockPojo mockPojo = new MockPojo("not_valid");

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("class validation failed");

        AnnotationValidationEngine.runValidationOnBean(mockPojo);
    }

    @Test
    public void shouldThrowExceptionWhenFieldValidatorFailsAndHasClassValidator() {

        MockPojoWithFieldValidators mockPojo = new MockPojoWithFieldValidators("valid", 3);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("field validation failed");

        AnnotationValidationEngine.runValidationOnBean(mockPojo);
    }

    @Test
    public void shouldPassWhenBothClassAndFieldValidators() {

        AnnotationValidationEngine.runValidationOnBean(new MockPojoWithFieldValidators("valid", 2));
    }

    public static class MockValidatorOnClass implements Validator<MockPojo> {

        @Override
        public void validate(MockPojo input) {
            if (!input.getField().equalsIgnoreCase("valid")) {
                throw new RuntimeException("class validation failed");
            }
        }
    }

    public static class MockValidatorOnClass2 implements Validator<MockPojoWithFieldValidators> {
        @Override
        public void validate(MockPojoWithFieldValidators input) {
            if (!input.getField().equalsIgnoreCase("valid")) {
                throw new RuntimeException("class validation failed");
            }
        }
    }

    public static class MockValidatorOnField implements Validator<Integer> {
        @Override
        public void validate(Integer input) {
            if (input != 2){
                throw new RuntimeException("integer field validation failed");
            }
        }
    }

    @TestClassAnnotation
    public static class MockPojo {
        private String field;

        public MockPojo(String field) {
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }

    @TestClassAnnotation
    public static class MockPojoWithFieldValidators {
        private String field;

        @TestFieldAnnotation
        private int integerField;

        public MockPojoWithFieldValidators(String field, int integerField) {
            this.field = field;
            this.integerField = integerField;
        }

        public String getField() {
            return field;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @ValidateWith(implClass = {MockValidatorOnClass.class, MockValidatorOnClass2.class})
    @interface TestClassAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @ValidateWith(implClass = MockValidatorOnField.class)
    @interface TestFieldAnnotation {
    }
}

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

public class TestMultipleFieldValidator {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldThrowExceptionWhenValidationFailedOnStringField() {
        MockPojoMultipleFields mockPojo = new MockPojoMultipleFields("not_hello", 2L);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("string validation Failed");

        AnnodationValidationEngine.runValidationOnBean(mockPojo);
    }

    @Test
    public void shouldThrowExceptionWhenValidationFailedOnLongField() {
        MockPojoMultipleFields mockPojo = new MockPojoMultipleFields("hello", 5L);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("long validation Failed");

        AnnodationValidationEngine.runValidationOnBean(mockPojo);
    }

    @Test
    public void shouldNotThrowExceptionWhenValidationPassed() {
        MockPojoMultipleFields mockPojo = new MockPojoMultipleFields("hello", 2L);

        AnnodationValidationEngine.runValidationOnBean(mockPojo);
    }

    public static class MockPojoMultipleFields {
        @TestAnnotationMultiple
        private String field;
        @TestAnnotationMultiple
        private long longField;


        public MockPojoMultipleFields(String field, long longField) {
            this.field = field;
            this.longField = longField;
        }
    }

    public static class MockValidatorString implements Validator<String> {
        @Override
        public void validate(String input) {
            if (!input.equalsIgnoreCase("hello")) {
                throw new RuntimeException("string validation Failed");
            }
        }
    }

    public static class MockValidatorLong implements Validator<Long> {
        @Override
        public void validate(Long input) {
            if (input != 2L) {
                throw new RuntimeException("long validation Failed");
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @ValidateWith(implClass = {MockValidatorString.class, MockValidatorLong.class})
    @interface TestAnnotationMultiple {
    }

}

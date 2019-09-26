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
 * Test that you can provide single validator class to annotation
 */
public class TestSingleFieldValidator {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldThrowExceptionWhenValidationFailedOnStringField() {
        MockPojoSingle mockPojoSingle = new MockPojoSingle("not_hello");

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("string validation Failed");

        AnnodationValidationEngine.runValidationOnBean(mockPojoSingle);
    }

    @Test
    public void shouldNotThrowExceptionWhenValidationPassed() {
        MockPojoSingle mockPojoSingle = new MockPojoSingle("hello");

        AnnodationValidationEngine.runValidationOnBean(mockPojoSingle);
    }

    public static class MockValidatorString implements Validator<String> {
        @Override
        public void validate(String input) {
            if (!input.equalsIgnoreCase("hello")) {
                throw new RuntimeException("string validation Failed");
            }
        }
    }

    public static class MockPojoSingle {
        @TestAnnotationSingle
        private String field;


        public MockPojoSingle(String field) {
            this.field = field;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @ValidateWith(implClass = MockValidatorString.class)
    @interface TestAnnotationSingle {
    }

}

package main;

/**
 * Generic validator
 *
 * @param <V> type of value to be validated
 */
public interface Validator<V>
{

    /**
     * Runs validation.
     * Should throw exception if validation does not pass
     *
     * @param input value to be validated
     */
    void validate(V input);

}

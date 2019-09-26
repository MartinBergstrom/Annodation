# Annodation
Lightweight java bean validation with annotations, inspired by javax.validation.ConstraintValidator.

Simply create a custom annotation and provide a validator class for that fields type. 
Works both on fields and classes.

You can provide several validators for different types of input, if no validator matching the type of field/class,
it will throw.


Example:


```
public class MyChessPojo {
  
  @ChessLetter
  private char chessLetter;

}

--------------------------

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@ValidatedWith(implClass = ChessLetterValidator.class)
public @interface ChessLetter { }

--------------------------

public class ChessLetterValidator implements Validator<Character> {

  @Override
    public void validate(Character input) {
      // validate that the char is A - H
    }
}

```

The main entry point is
[AnnodationValidationEngine.java](https://github.com/MartinBergstrom/Annodation/blob/master/src/main/java/main/AnnodationValidationEngine.java)

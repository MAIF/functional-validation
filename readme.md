# Functional Validation  [![ga-badge][]][ga] [![jar-badge][]][jar]

[ga]:               https://github.com/MAIF/functional-validation/actions?query=workflow%3ABuild
[ga-badge]:         https://github.com/MAIF/functional-validation/workflows/Build/badge.svg
[jar]:              https://maven-badges.herokuapp.com/maven-central/fr.maif/functional-validation
[jar-badge]:        https://maven-badges.herokuapp.com/maven-central/fr.maif/functional-validation/badge.svg


This lib provide helpers to validate bean and compose validations stacking errors.  

## Imports

Jcenter hosts this library.

### Maven

```xml
<dependency>
    <groupId>fr.maif</groupId>
    <artifactId>functional-validation</artifactId>
    <version>1.0.0-BETA1</version>
</dependency>
```

### Gradle

```
implementation 'fr.maif:functional-validation:1.0.0-BETA1'
```

## Combining validations  


```java
public class Viking {

    String name;
    String email;
    String website;
    Integer age;

    public Viking(String name, String email, String website, Integer age) {
        this.name = name;
        this.email = email;
        this.website = website;
        this.age = age;
    }
}

public class VikingValidation {

    public static final String EMAIL_PATTERN = "...";
    public static final String WEBSITE_URL_PATTERN = "...";

    @SuppressWarnings("unchecked")
    public static <E> Rule<E> pattern(String elt, String pattern, E error) {
        Pattern compiled = Pattern.compile(pattern);
        if (compiled.matcher(elt).matches()) {
            return Rule.valid();
        } else {
            return Rule.invalid(error);
        }
    }

    public static Rule<String> validateWebsite(String elt) {
        return pattern(elt, WEBSITE_URL_PATTERN, "Website invalid");
    }

    public static Rule<String> validateEmail(String elt) {
        return pattern(elt, EMAIL_PATTERN, "Email invalid");
    }

    private static Rule<String> validateAge(Integer age) {
        if (age > 0 && age < 130) {
            return Rule.valid();
        } else {
            return Rule.invalid("Age should be between 0 and 130");
        }
    }

    public static <E> Rule<String> notNull(E elt) {
        if (elt == null) {
            return Rule.invalid("should not be null");
        } else {
            return Rule.valid();
        }
    }

    public static Rule<String> validateViking(Viking viking) {
        return notNull(viking.email).andThen(() -> validateEmail(viking.email))
                .and(notNull(viking.website).andThen(() -> validateWebsite(viking.website)))
                .and(validateAge(viking.age));
    }

    // Or with combine instead of and 
    public static Rule<String> validateViking2(Viking viking) {
        return Rule.combine(List.of(
                    notNull(viking.email).andThen(() -> validateEmail(viking.email)),
                    notNull(viking.website).andThen(() -> validateWebsite(viking.website)),
                    validateAge(viking.age)
        ));
    }
}

```

And then 

```java
Rule<String> validation = VikingValidation.validateViking(new Viking(
        "Ragnard",
        "ragnar@gmail.com",
        "https://ragnard.com",
        20
));

System.out.println(validation.isValid()); // true
System.out.println(validation.isInvalid()); // false
System.out.println(validation.getErrors()); // empty
String foldOk = validation.fold(
        errors -> "Validation failed with " + errors.mkString(", "),
        () -> "Validation succeed"
);
System.out.println(foldOk);// "Validation succeed"

Rule<String> validationOnError = VikingValidation.validateViking(new Viking(
        "Ragnard",
        "ragnar",
        "ragnard",
        150
));

System.out.println(validationOnError.isValid()); // false
System.out.println(validationOnError.isInvalid()); // true
System.out.println(validationOnError.getErrors()); // ["Email invalid", "Website invalid", "Age should be between 0 and 130"]
String foldKo = validationOnError.fold(
        errors -> "Validation failed with " + errors.mkString(", "),
        () -> "Validation succeed"
); // "Validation failed with Email invalid, Website invalid, Age should be between 0 and 130"

System.out.println(foldKo); // "Validation failed with Email invalid, Website invalid, Age should be between 0 and 130"
```


## Bean validation 

Bean validation can be used and mix with rules 

```java

public class Viking {
    @NotNull
    public final String name;
    @Email
    public final String email;
    @Pattern(regexp = WEBSITE_URL_PATTERN)
    public final String website;
    @Min(0)
    @Max(130)
    public final Integer age;

    public Viking(String name, String email, String website, Integer age) {
        this.name = name;
        this.email = email;
        this.website = website;
        this.age = age;
    }
}

```

and then 

```java

Rule<ConstraintViolation<Viking>> validate = Rule.validate(new Viking(
        "Ragnard",
        "ragnar",
        "ragnard",
        150
));

Rule<String> validationWithStringError = validate.mapError(ConstraintViolation::getMessage);

Rule<String> and = validationWithStringError.and(otherRule);
```

## Async validation 

Sometime, validation is done using an external system where the call end with a future. 

```java
String name = "Ragnard";

Viking viking = new Viking(
        name,
        "ragnar",
        "ragnard",
        150
);
Rule<ConstraintViolation<Viking>> validate = Rule.validate(viking);

Rule<String> validationWithStringError = validate.mapError(ConstraintViolation::getMessage);

// First validation and second only if the first is valid
Future<Rule<String>> rules1 = validationWithStringError.andThenF(() -> validateUserExists(viking.name));

// Or with the two validations together
Future<Rule<String>> rules2 = validationWithStringError.combineF(validateUserExists(name));

// Or combining multiple rules (same as previous)
Future<Rule<String>> rules3 = Rule.combineF(List(
        Future(validationWithStringError),
        validateUserExists(name)
));

```


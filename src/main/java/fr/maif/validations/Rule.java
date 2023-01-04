package fr.maif.validations;

import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple0;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.concurrent.Future;
import io.vavr.control.Either;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Rule is used to represent the result a business rule.
 * You can combine a Rule with a other Rule and stack.
 *
 * A Rule can be Valid or Invalid.
 *
 *
 * @param <E>
 */
public interface Rule<E> {

    Rule<?> VALID = new Valid<>();

    Lazy<Validator> validator = Lazy.of(() ->
            jakarta.validation.Validation.buildDefaultValidatorFactory().usingContext()
                    .addValueExtractor(new SeqExtractor())
                    .addValueExtractor(new OptionExtractor())
                    .getValidator()
    );

    /**
     * A valid Rule
     * @param <E>
     * @return the valid rule
     */
    static <E> Rule<E> valid() {
        return (Rule<E>)VALID;
    }

    /**
     * Use bean validation to validate a bean and return a Rule with the ConstraintViolation in case of an invalid object.
     *
     * @param obj the object to validate
     * @param <A>
     * @return The valid or invalid rule
     */
    static <A> Rule<ConstraintViolation<A>> validate(A obj) {
        Set<ConstraintViolation<A>> validation = validator.get().validate(obj);
        if (validation.isEmpty()) {
            return valid();
        } else {
            return new Invalid<>(List.ofAll(validation));
        }
    }

    /**
     * Create a Rule from a vavr Validation
     *
     * @param validation the vavr validation
     * @param <A>
     * @return the rule
     */
    static <A> Rule<A> fromVavrValidation(io.vavr.control.Validation<A, ?> validation) {
        return validation.fold(
                Rule::invalid,
                __ -> valid()
        );
    }

    @SafeVarargs
    static <A> Rule<A> invalid(A... invalid) {
        return new Invalid<>(List.of(invalid));
    }

    /**
     * Combine many Future of rule together the get a future rule.
     * @param fValidations an array of future rules
     * @param <A>
     * @return the combined rules
     */
    @SafeVarargs
    static <A> Future<Rule<A>> combineF(Future<Rule<A>>... fValidations) {
        return combineF(List.of(fValidations));
    }

    /**
     * Combine many Future of rule together the get a future rule.
     * @param fValidations an array of future rules
     * @param <A>
     * @return the combined rules
     */
    static <A> Future<Rule<A>> combineF(Seq<Future<Rule<A>>> fValidations) {
        return Future.sequence(fValidations).map(validations -> {
            if (validations.isEmpty()) {
                return valid();
            } else {
                return validations.reduceLeft(Rule::and);
            }
        });
    }

    /**
     * Combine all rules together and accumulate the errors.
     * @param rules the rules to accumulate
     * @param <A>
     * @return the rule
     */
    @SafeVarargs
    static <A> Rule<A> combine(Rule<A>... rules) {
        return combine(List.of(rules));
    }

    /**
     * Combine all rules together and accumulate the errors.
     * @param validations the rules to accumulate
     * @param <A>
     * @return the rule
     */
    static <A> Rule<A> combine(Seq<Rule<A>> validations) {
        if (validations.isEmpty()) {
            return valid();
        } else {
            return validations.reduceLeft(Rule::and);
        }
    }

    /**
     * @return true if the rule is valid
     */
    Boolean isValid();

    /**
     * @return true if the rule is invalid
     */
    Boolean isInvalid();

    /**
     * Combine two rules
     * @param other the other rule
     * @return the rule
     */
    Rule<E> and(Rule<E> other);

    /**
     * Combine this rule with a future rule
     * @param other the future rule
     * @return a future rule
     */
    default Future<Rule<E>> andF(Future<Rule<E>> other) {
        return other.map(this::and);
    }

    /**
     * If this rule is valid then evaluate the future rule in param
     *
     * @param other the future rule to evaluate if the is rule is valid
     * @return the future rule result
     */
    default Future<Rule<E>> andThenF(Supplier<Future<Rule<E>>> other) {
        if (this.isInvalid()) {
            return Future.successful(this);
        } else {
            return other.get();
        }
    }

    /**
     * combine two rules
     * @param other the other rule
     * @return the rule
     */
    default Rule<E> combine(Rule<E> other) {
        return and(other);
    }

    /**
     * combine this rule with a future rule
     * @param other a future rule
     * @return the future combined rule
     */
    default Future<Rule<E>> combineF(Future<Rule<E>> other) {
        return this.andF(other);
    }

    /**
     * chained this rule with an other only if this rule is valid.
     *
     * @param other the other rule
     * @return the rule
     */
    Rule<E> andThen(Supplier<Rule<E>> other);

    /**
     * This rule or another rule. On error only if the two are on error.
     * @param other fallback
     * @return the rule
     */
    Rule<E> or(Rule<E> other);

    /**
     * This rule or another future rule. On error only if the two are on error.
     *
     * @param other a future fallback
     * @return the future rule
     */
    default Future<Rule<E>> orF(Future<Rule<E>> other) {
        return other.map(this::or);
    }

    /**
     * Convert the error side
     *
     * @param func the function to apply
     * @param <E1>
     * @return the rule with the errors mapped
     */
    <E1> Rule<E1> mapError(Function<E, E1> func);

    /**
     * Folds either the invalid or the valid side of this disjunction.
     *
     * @param onError maps the error value if this is a JsError
     * @param ok maps the success value if this is a JsSuccess
     * @param <A>
     * @return the value
     */
    <A> A fold(Function<Seq<E>, A> onError, Supplier<A> ok);

    /**
     * Convertion to a vavr Either
     *
     * @return a vavr Either
     */
    Either<Seq<E>, Tuple0> toEither();

    /**
     * Convertion to a vavr Either specifying the right side
     * @param ok the right side
     * @param <A>
     * @return teh Either
     */
    <A> Either<Seq<E>, A> toEither(A ok);

    /**
     * Get all the errors if this rule is invalid
     * @return all the errors
     */
    Seq<E> getErrors();

    class Valid<E> implements Rule<E> {

        private Valid() { }

        @Override
        public Boolean isValid() {
            return true;
        }

        @Override
        public Boolean isInvalid() {
            return false;
        }

        @Override
        public Rule<E> and(Rule<E> other) {
            if (other.isInvalid()) {
                return other;
            } else {
                return this;
            }
        }

        @Override
        public Rule<E> or(Rule<E> other) {
            return this;
        }

        @Override
        public Rule<E> andThen(Supplier<Rule<E>> other) {
            return other.get();
        }

        @Override
        public <A> A fold(Function<Seq<E>, A> onError, Supplier<A> ok) {
            return ok.get();
        }

        @Override
        public <E1> Rule<E1> mapError(Function<E, E1> func) {
            return (Rule<E1>) this;
        }

        @Override
        public Either<Seq<E>, Tuple0> toEither() {
            return Either.right(Tuple.empty());
        }

        @Override
        public <A> Either<Seq<E>, A> toEither(A ok) {
            return Either.right(ok);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Valid.class.getSimpleName() + "[", "]")
                    .toString();
        }

        @Override
        public Seq<E> getErrors() {
            return List.empty();
        }
    }

    class Invalid<E> implements Rule<E> {

        public final Seq<E> errors;

        public Invalid(Seq<E> errors) {
            this.errors = errors;
        }

        public Invalid(E error) {
            this.errors = List.of(error);
        }

        @Override
        public Boolean isValid() {
            return false;
        }

        @Override
        public Boolean isInvalid() {
            return true;
        }

        @Override
        public Rule<E> and(Rule<E> other) {
            if (other.isInvalid()) {
                Invalid<E> o = (Invalid<E>) other;
                return new Invalid<>(this.errors.toList().appendAll(o.errors));
            } else {
                return this;
            }
        }

        @Override
        public Rule<E> or(Rule<E> other) {
            if (other.isInvalid()) {
                Invalid<E> o = (Invalid<E>) other;
                return new Invalid<>(this.errors.toList().appendAll(o.errors));
            } else {
                return other;
            }
        }

        @Override
        public Rule<E> andThen(Supplier<Rule<E>> other) {
            return this;
        }

        @Override
        public <A> A fold(Function<Seq<E>, A> onError, Supplier<A> ok) {
            return onError.apply(this.errors);
        }

        @Override
        public <E1> Rule<E1> mapError(Function<E, E1> func) {
            return new Invalid<>(this.errors.map(func));
        }

        @Override
        public Either<Seq<E>, Tuple0> toEither() {
            return Either.left(this.errors);
        }

        @Override
        public <A> Either<Seq<E>, A> toEither(A ok) {
            return Either.left(this.errors);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Invalid<?> invalid = (Invalid<?>) o;
            return Objects.equals(errors, invalid.errors);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errors);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Invalid.class.getSimpleName() + "[", "]")
                    .add("errors=" + errors)
                    .toString();
        }

        @Override
        public Seq<E> getErrors() {
            return this.errors;
        }
    }

}

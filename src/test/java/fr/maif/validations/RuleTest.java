package fr.maif.validations;

import io.vavr.collection.List;
import io.vavr.control.Option;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.xml.transform.Source;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class RuleTest {


    @Test
    public void andValidation() {
        Rule<String> valid1 = Rule.valid();
        Rule<String> valid2 = Rule.valid();

        assertThat(valid1.and(valid2)).isEqualTo(Rule.valid());

        Rule<String> oups = Rule.invalid("Oups1");
        Rule<String> oups2 = Rule.invalid("Oups2");


        assertThat(valid1.and(oups)).isEqualTo(Rule.invalid("Oups1"));

        Rule<String> and2 = valid1.and(oups).and(oups2);
        assertThat(and2).isEqualTo(Rule.invalid("Oups1", "Oups2"));
    }

    @Test
    public void orValidation() {
        Rule<String> valid1 = Rule.valid();
        Rule<String> valid2 = Rule.valid();

        assertThat(valid1.or(valid2)).isEqualTo(Rule.valid());

        Rule<String> oups = Rule.invalid("Oups1");
        Rule<String> oups2 = Rule.invalid("Oups2");

        assertThat(valid1.or(oups)).isEqualTo(Rule.valid());
        assertThat(oups.or(valid1)).isEqualTo(Rule.valid());

        Rule<String> and2 = oups.or(oups2);
        assertThat(and2).isEqualTo(Rule.invalid("Oups1", "Oups2"));
    }

    @Test
    public void beanValidation() {
        Rule<ConstraintViolation<MyPojo>> rule = Rule.validate(new MyPojo());
        Rule<AppError> appErrorRule = rule.mapError(c -> AppError.fromConstraintError(c, "obj"));
        assertThat(appErrorRule.getErrors()).containsExactlyInAnyOrder(
                AppError.error("obj.email", "email.not.null"),
                AppError.error("obj.innerPojo.field", "field.not.null"),
                AppError.error("obj.pojos[0].field", "field.not.null")
        );
    }


    @Test
    public void beanValidationInner() {
        Rule<ConstraintViolation<MyPojo>> rule = Rule.validate(new MyPojo("test@gmail.com", new InnerPojo("test"), List.empty(), Option.of(-1)));
        Rule<AppError> appErrorRule = rule.mapError(c -> AppError.fromConstraintError(c, "obj"));
        assertThat(appErrorRule.getErrors()).containsExactlyInAnyOrder(
                AppError.error("obj.valueInside", "positive")
        );
    }

    public static class MyPojo {

        @NotNull(message = "email.not.null")
        String email;

        @Valid
        InnerPojo innerPojo = new InnerPojo();

        @Valid
        List<InnerPojo> pojos = List.of(new InnerPojo());

        Option<@Positive(message = "positive") Integer> valueInside;

        public MyPojo() {
        }

        public MyPojo(String email, InnerPojo innerPojo, List<InnerPojo> pojos, Option<Integer> valueInside) {
            this.email = email;
            this.innerPojo = innerPojo;
            this.pojos = pojos;
            this.valueInside = valueInside;
        }
    }

    public static class InnerPojo {

        @NotNull(message = "field.not.null")
        String field;

        public InnerPojo() {
        }

        public InnerPojo(@NotNull(message = "field.not.null") String field) {
            this.field = field;
        }
    }


    static class AppError {

        public String message;
        public String rawMessage;
        public Object[] args;
        public Option<String> path;

        public AppError(String message, Object[] args, Option<String> path) {
            if (args != null && args.length > 0) {
                this.message = MessageFormat.format(message, args);
            } else {
                this.message = rawMessage;
            }
            this.rawMessage = message;
            this.args = args;
            this.path = path;
        }

        public static AppError error(String message) {
            return new AppError(message, new Object[]{}, Option.none());
        }

        public static AppError error(String path, String message) {
            return new AppError(message, new String[]{}, Option.of(path));
        }
        public static AppError error(String path, String message, String... args) {
            return new AppError(message, args, Option.of(path));
        }

        public static AppError fromConstraintError(ConstraintViolation<?> constraintViolation, String rootPath) {
            String path = List.ofAll(constraintViolation.getPropertyPath())
                    .map(node -> node.toString())
                    .mkString(".");

            Option<String> finalPath = Option.of(rootPath).map(p -> Option.of(p + "." + path)).getOrElse(Option.of(path));

            Object[] executableParameters = constraintViolation.getExecutableParameters();
            if (executableParameters != null && executableParameters.length > 0) {
                return new AppError(constraintViolation.getMessage(), executableParameters, finalPath);
            } else {
                return new AppError(constraintViolation.getMessage(), new Object[]{}, finalPath);
            }
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", AppError.class.getSimpleName() + "[", "]")
                    .add("message='" + message + "'")
                    .add("rawMessage='" + rawMessage + "'")
                    .add("args=" + Arrays.toString(args))
                    .add("path=" + path)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AppError appError = (AppError) o;
            return Objects.equals(message, appError.message) &&
                    Objects.equals(rawMessage, appError.rawMessage) &&
                    Arrays.equals(args, appError.args) &&
                    Objects.equals(path, appError.path);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(message, rawMessage, path);
            result = 31 * result + Arrays.hashCode(args);
            return result;
        }
    }
}
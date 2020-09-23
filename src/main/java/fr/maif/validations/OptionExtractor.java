package fr.maif.validations;

import io.vavr.control.Option;

import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.ValueExtractor;

public class OptionExtractor implements ValueExtractor<Option<@ExtractedValue ?>> {

    @Override
    public void extractValues(Option<?> originalValue, ValueReceiver receiver) {
        originalValue.forEach(v ->
            receiver.value(null, v)
        );
    }
}

package fr.maif.validations;

import io.vavr.collection.Seq;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

public class SeqExtractorInner implements ValueExtractor<Seq<@ExtractedValue ?>> {

    @Override
    public void extractValues(Seq<?> originalValue, ValueReceiver receiver) {
        originalValue.zipWithIndex().forEach(t ->
                receiver.indexedValue("<seq element>", t._2, t._1)
        );
    }
}


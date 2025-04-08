package dev.ikm.maven;

import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.TinkarTerm;

import java.util.HashMap;
import java.util.Map;

public class TransformationHelper {
    private static final Map<String, Object> SNOMED_MAPPINGS = new HashMap<>();

    static {
        // language
        SNOMED_MAPPINGS.put("en", TinkarTerm.ENGLISH_LANGUAGE);
        SNOMED_MAPPINGS.put("es", TinkarTerm.SPANISH_LANGUAGE);
        SNOMED_MAPPINGS.put("900000000000509007", TinkarTerm.US_DIALECT_PATTERN);
        SNOMED_MAPPINGS.put("900000000000508004", TinkarTerm.GB_DIALECT_PATTERN);
    }

    private static <T> T getMapping(String code, Class<T> expectedType) {
        Object value = SNOMED_MAPPINGS.get(code);
        if(value == null) {
            throw new RuntimeException("Unrecognized Code: " + code);
        }
        return expectedType.cast(value);
    }
    /**
     * transforms languageCode in concept
     * @param languageCode String representation of english or spanish
     * @return language concept
     */
    public static EntityProxy.Concept getLanguageConcept(String languageCode){
        return getMapping(languageCode, EntityProxy.Concept.class);
    }

    /**
     * assigns dialect pattern to TinkarTerm depending on dialectRefSetId
     * @param dialectRefsetId GB or US dialect semantic
     * @return dialect pattern
     */
    public static EntityProxy.Pattern getDialectPattern(String dialectRefsetId) {
        return getMapping(dialectRefsetId, EntityProxy.Pattern.class);
    }
}

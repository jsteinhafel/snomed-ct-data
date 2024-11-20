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
        // case sensitivity
        SNOMED_MAPPINGS.put("900000000000448009", TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE);
        SNOMED_MAPPINGS.put("900000000000017005", TinkarTerm.DESCRIPTION_CASE_SENSITIVE);
        SNOMED_MAPPINGS.put("900000000000020002", TinkarTerm.DESCRIPTION_INITIAL_CHARACTER_CASE_SENSITIVE);
        // description
        SNOMED_MAPPINGS.put("900000000000550004", TinkarTerm.DEFINITION_DESCRIPTION_TYPE);
        SNOMED_MAPPINGS.put("900000000000003001", TinkarTerm.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE);
        SNOMED_MAPPINGS.put("900000000000013009", TinkarTerm.REGULAR_NAME_DESCRIPTION_TYPE);
        // dialect pattern
        SNOMED_MAPPINGS.put("900000000000509007", TinkarTerm.US_DIALECT_PATTERN);
        SNOMED_MAPPINGS.put("900000000000508004", TinkarTerm.GB_DIALECT_PATTERN);
        // dialect acceptability
        SNOMED_MAPPINGS.put("900000000000548007", TinkarTerm.PREFERRED);
        SNOMED_MAPPINGS.put("900000000000549004", TinkarTerm.ACCEPTABLE);
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
     * transforms caseSensitivity code into concept
     * @param caseSensitivityCode represents case sensitivity of a description
     * @return case sensitivity concept
     */
    public static EntityProxy.Concept getDescriptionCaseSignificanceConcept(String caseSensitivityCode){
        return getMapping(caseSensitivityCode, EntityProxy.Concept.class);
    }

    /**
     * transform descriptionType into concept
     * @param descriptionTypeCode String representation the type of descriptions
     * @return description type concept
     */
    public static EntityProxy.Concept getDescriptionType(String descriptionTypeCode){
        return getMapping(descriptionTypeCode, EntityProxy.Concept.class);
    }

    /**
     * assigns dialect pattern to TinkarTerm depending on dialectRefSetId
     * @param dialectRefsetId GB or US dialect semantic
     * @return dialect pattern
     */
    public static EntityProxy.Pattern getDialectPattern(String dialectRefsetId) {
        return getMapping(dialectRefsetId, EntityProxy.Pattern.class);
    }

    /**
     * assigns acceptability code to TinkarTerm based on acceptabilityCode
     * @param acceptabilityCode preferred or acceptable
     * @return acceptability concept
     */
    public static EntityProxy.Concept getDialectAccceptability(String acceptabilityCode){
        return getMapping(acceptabilityCode, EntityProxy.Concept.class);
    }
}

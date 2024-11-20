/*
 * Copyright © 2015 IKM (support@ikm.dev)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class SnomedUtility {

    private static final Logger LOG = LoggerFactory.getLogger(SnomedUtility.class.getSimpleName());

    /**
     * constructor for SnomedUtility
     * @param uuidUtility used to create uuid's
     */

    /**
     * taking time stamp and making it an epoch
     * @param effectiveTime String representation dates in yyyyMMdd format
     * @return long value of epochTime
     */
    public static long snomedTimestampToEpochSeconds(String effectiveTime) {
        long epochTime;
        try {
            epochTime = new SimpleDateFormat("yyyyMMdd").parse(effectiveTime).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return epochTime;
    }

    /**
     * transforms caseSensitivity code into concept
     * @param caseSensitivityCode represents case sensitivity of a description
     * @return case sensitivity concept
     */
    public static EntityProxy.Concept getDescriptionCaseSignificanceConcept(String caseSensitivityCode){
        EntityProxy.Concept caseSensitivityConcept = null;
        switch (caseSensitivityCode) {
            case "900000000000448009" -> caseSensitivityConcept = TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE;
            case "900000000000017005" -> caseSensitivityConcept = TinkarTerm.DESCRIPTION_CASE_SENSITIVE;
            case "900000000000020002" -> caseSensitivityConcept = TinkarTerm.DESCRIPTION_INITIAL_CHARACTER_CASE_SENSITIVE;
            default -> throw new RuntimeException("UNRECOGNIZED CASE SENSITIVITY CODE");
        }
        return caseSensitivityConcept;
    }

    /**
     * transform descriptionType into concept
     * @param descriptionTypeCode String representation the type of descriptions
     * @return description type concept
     */
     public static EntityProxy.Concept getDescriptionType(String descriptionTypeCode){
        EntityProxy.Concept descriptionTypeConcept = null;
        switch (descriptionTypeCode) {
            case "900000000000550004" -> descriptionTypeConcept = TinkarTerm.DEFINITION_DESCRIPTION_TYPE;
            case "900000000000003001" -> descriptionTypeConcept = TinkarTerm.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE;
            case "900000000000013009" -> descriptionTypeConcept = TinkarTerm.REGULAR_NAME_DESCRIPTION_TYPE;
            default -> throw new RuntimeException("UNRECOGNIZED DESCRIPTION TYPE CODE");
        }
        return descriptionTypeConcept;
    }

}

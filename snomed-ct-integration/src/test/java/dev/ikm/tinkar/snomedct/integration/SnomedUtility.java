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


import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.io.FileUtil;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.EntityVersion;
import dev.ikm.tinkar.entity.export.ExportEntitiesController;
import dev.ikm.tinkar.starterdata.UUIDUtility;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.EntityProxy.Concept;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SnomedUtility {

    private static final Logger LOG = LoggerFactory.getLogger(SnomedUtility.class.getSimpleName());
    private final UUIDUtility UUID_UTILITY;
    private final List<Entity<? extends EntityVersion>> STAMP_LIST = new ArrayList<>();

    /**
     * constructor for SnomedUtility
     * @param uuidUtility used to create uuid's
     */
    public SnomedUtility(UUIDUtility uuidUtility) {
        this.UUID_UTILITY = uuidUtility;
    }

    /**
     * creating a stamp
     * @param status String that represents active or inactive
     * @param effectiveTime String that represents dates in yyyyMMdd format
     * @param authorConcept {@link dev.ikm.tinkar.terms.EntityProxy.Concept}
     * @param moduleId String to represent a module
     * @param pathConcept {@link dev.ikm.tinkar.terms.EntityProxy.Concept}
     * @return new stamp entity
     */


    /**
     * writes stamps
     */
    public void writeSTAMPs() {
        Set<Entity<? extends EntityVersion>> stampSet = new HashSet<>(this.STAMP_LIST);
        stampSet.forEach(stampEntity -> EntityService.get().putEntity(stampEntity));
    }

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
     * transforms languageCode in concept
     * @param languageCode String representation of english or spanish
     * @return language concept
     */
    public static EntityProxy.Concept getLanguageConcept(String languageCode){
        EntityProxy.Concept languageConcept = null;
        switch (languageCode) {
            case "en" -> languageConcept = TinkarTerm.ENGLISH_LANGUAGE;
            case "es" -> languageConcept = TinkarTerm.SPANISH_LANGUAGE;
            default -> throw new RuntimeException("UNRECOGNIZED LANGUAGE CODE");
        }
        return languageConcept;
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

    /**
     * assigns dialect pattern to TinkarTerm depending on dialectRefSetId
     * @param dialectRefsetId GB or US dialect semantic
     * @return dialect pattern
     */
    public static EntityProxy.Pattern getDialectPattern(String dialectRefsetId) {
        EntityProxy.Pattern dialectPattern = null;
        switch (dialectRefsetId) {
            case "900000000000509007" -> dialectPattern = TinkarTerm.US_DIALECT_PATTERN;
            case "900000000000508004" -> dialectPattern = TinkarTerm.GB_DIALECT_PATTERN;
            default -> throw new RuntimeException("UNRECOGNIZED ACCEPTABILITY CODE");
        }
        return dialectPattern;
    }

    /**
     * assigns acceptability code to TinkarTerm based on acceptabilityCode
     * @param acceptabilityCode preferred or acceptable
     * @return acceptability concept
     */
    public static EntityProxy.Concept getDialectAccceptability(String acceptabilityCode){
        EntityProxy.Concept acceptabilityConcept = null;
        switch (acceptabilityCode) {
            case "900000000000548007" -> acceptabilityConcept = TinkarTerm.PREFERRED;
            case "900000000000549004" -> acceptabilityConcept = TinkarTerm.ACCEPTABLE;
            default -> throw new RuntimeException("UNRECOGNIZED ACCEPTABILITY CODE");
        }
        return acceptabilityConcept;
    }

    /**
     * retrieves user concept
     * @return the snomed author
     */
        public static EntityProxy.Concept getUserConcept(){
        EntityProxy.Concept snomedAuthor = EntityProxy.Concept.make("IHTSDO SNOMED CT Author", new UUIDUtility().createUUID("IHTSDO SNOMED CT Author"));
        return snomedAuthor;
//        return TinkarTerm.USER;
    }

    /**
     * retrieve a path concept
     * @return TinkarTerm from Concept class
     */
    public static EntityProxy.Concept getPathConcept(){
        return TinkarTerm.DEVELOPMENT_PATH;
    }

    /**
     * retrieves identifier concept
     * @return snomedIntId from Concept class
     */
    public static EntityProxy.Concept getIdentifierConcept(){
        EntityProxy.Concept snomedIntID = EntityProxy.Concept.make(PublicIds.of(UuidUtil.fromSNOMED("900000000000294009")));
        return snomedIntID;
    }

    public static void startDatabase(File dataStore, String controllerName) {
        LOG.info("Starting database");
        LOG.info("Loading data from " + dataStore.getAbsolutePath());
        CachingService.clearAll();
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, dataStore);
        PrimitiveData.selectControllerByName(controllerName);
        PrimitiveData.start();
    }

    public static void stopDatabase() {
        PrimitiveData.stop();
    }

    public static void exportEntities(File exportFile) {
        ExportEntitiesController exportEntitiesController = new ExportEntitiesController();
        FileUtil.recursiveDelete(exportFile);
        try {
            exportEntitiesController.export(exportFile).get();
        } catch (ExecutionException | InterruptedException ex){
            ex.printStackTrace();
        }
    }

    public List<Entity<? extends EntityVersion>> getStampList() {
        return this.STAMP_LIST;
    }

    public Set<Entity<? extends EntityVersion>> getStampSet() {
        return new HashSet<>(this.STAMP_LIST);
    }

    private static Pattern getIdPattern() {
        // Expecting a Snomed identifier following a colon as shown below
        // :609096000
        // Pattern of at least one numeric character after colon
        return Pattern.compile("(?<=:)([0-9]+)");
    }

    private static String idToPublicId(MatchResult id) {
        String idString = id.group();
        String publicIdString = PublicIds.of(UuidUtil.fromSNOMED(idString)).toString();
        return publicIdString.replaceAll("\"", "");
    }

    private static Pattern getUrlPattern() {
        // Expecting URL formatted as shown below
        // <http://www.w3.org/2002/07/owl#>
        // Pattern of characters between less-than and greater-than characters
        return Pattern.compile("<[^>]+>");
    }

    private static String urlToPublicId(MatchResult id) {
        String urlString = id.group();
        // remove beginning less-than character and ending greater-than character
        String idString = urlString.substring(1, urlString.length()-1);
        // Generate UUID from URL bytes
        String publicIdString = PublicIds.of(UUID.nameUUIDFromBytes(idString.getBytes())).toString();
        return publicIdString.replaceAll("\"", "");
    }

    public static String owlAxiomIdsToPublicIds(String owlExpression) {
        String publicIdOwlExpression = owlExpression;
        // Replace URLs with a UUID representation
        if (owlExpression.contains("<") & owlExpression.contains(">")) {
            Matcher urlMatcher = getUrlPattern().matcher(publicIdOwlExpression);
            publicIdOwlExpression = urlMatcher.replaceAll(SnomedUtility::urlToPublicId);
        }
        // Replace Snomed identifiers with a UUID representation
        Matcher idMatcher = getIdPattern().matcher(publicIdOwlExpression);
        publicIdOwlExpression = idMatcher.replaceAll(SnomedUtility::idToPublicId);
        return publicIdOwlExpression;
    }

}

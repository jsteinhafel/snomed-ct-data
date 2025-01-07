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
package dev.ikm.maven;

import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.util.uuid.UuidT5Generator;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityVersion;
import dev.ikm.tinkar.terms.EntityProxy.Concept;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SnomedUtility {

    private static final Logger LOG = LoggerFactory.getLogger(SnomedUtility.class.getSimpleName());
    private final List<Entity<? extends EntityVersion>> STAMP_LIST = new ArrayList<>();

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
     * retrieves user concept
     * @return the snomed author
     */
        public static Concept getUserConcept(UUID namespace){
        Concept snomedAuthor = Concept.make("IHTSDO SNOMED CT Author", UuidT5Generator.get(namespace,("IHTSDO SNOMED CT Author")));
        return snomedAuthor;
    }

    /**
     * retrieve a path concept
     * @return TinkarTerm from Concept class
     */
    public static Concept getPathConcept(){
        return TinkarTerm.DEVELOPMENT_PATH;
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

    private static Pattern getFullIriPattern() {
        // A full IRI is an IRI between "<" and ">"
        // A Prefix declaration is a full IRI
        // An Ontology declaration is a full IRI or an abbreviated IRI
        // but Snomed only uses full IRI, so...
        // <http://www.w3.org/2002/07/owl#>
        // Pattern of characters between less-than and greater-than characters
        return Pattern.compile("<[^>]+>");
    }

    private static String fullIriToPublicId(MatchResult id) {
        String fullIriString = id.group();
        // the IRI for the public id does not include "<" or ">"
        String iriString = fullIriString.substring(1, fullIriString.length() - 1);
        // Generate UUID from URL bytes
        String publicIdString = PublicIds.of(UUID.nameUUIDFromBytes(iriString.getBytes())).toString();
        return "<" + publicIdString.replaceAll("\"", "") + ">";
    }

    public static String owlAxiomIdsToPublicIds(String owlExpression) {
        String publicIdOwlExpression = owlExpression;
        // Replace IRIs with a UUID representation
        Matcher urlMatcher = getFullIriPattern().matcher(publicIdOwlExpression);
        publicIdOwlExpression = urlMatcher.replaceAll(SnomedUtility::fullIriToPublicId);
        // Replace Snomed identifiers with a UUID representation
        Matcher idMatcher = getIdPattern().matcher(publicIdOwlExpression);
        publicIdOwlExpression = idMatcher.replaceAll(SnomedUtility::idToPublicId);
        return publicIdOwlExpression;
    }
}

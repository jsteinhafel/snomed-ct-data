package dev.ikm.snomedct.starter.data;


public class StarterData implements Runnable{

    @Override
    public void run() {
        System.out.println("Hello World");


        //Grab namespace from origin manifest


        //Compose starter data
//        final Composer composer = new Composer("SNOMED CT Starter Data");
//        Session session = composer.open(State.ACTIVE, System.currentTimeMillis(), TinkarTerm.USER, TinkarTerm.PRIMORDIAL_MODULE, TinkarTerm.PRIMORDIAL_PATH);
//        session.compose((ConceptAssembler conceptAssembler) -> conceptAssembler.publicId(UuidT5Generator.get()));



        /*
        Concept snomedAuthor = EntityProxy.Concept.make("IHTSDO SNOMED CT Author", uuidUtility.createUUID("IHTSDO SNOMED CT Author"));
        starterData.concept(snomedAuthor)
                .fullyQualifiedName("IHTSDO SNOMED CT Author", TinkarTerm.PREFERRED)
                .synonym("SNOMED CT Author", TinkarTerm.PREFERRED)
                .definition("International Health Terminology Standards Development Organisation (IHTSDO) SNOMED CT Author", TinkarTerm.PREFERRED)
                .identifier(TinkarTerm.UNIVERSALLY_UNIQUE_IDENTIFIER, snomedAuthor.asUuidArray()[0].toString())
                .statedDefinition(List.of(TinkarTerm.USER))
                .build();

        Concept snomedIdentifier = EntityProxy.Concept.make("SNOMED CT Identifier", UuidUtil.fromSNOMED("900000000000294009"));
        starterData.concept(snomedIdentifier)
                .statedDefinition(List.of(TinkarTerm.IDENTIFIER_SOURCE))
                .build();

         */
    }
}

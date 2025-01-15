### Team Ownership - Product Owner
Data Team

## Getting Started

Follow these instructions to generate a snomedct dataset:

1. Clone the [snomed-ct-data repository](https://github.com/ikmdev/snomed-ct-data)

```bash
git clone [Rep URL]
```

2. Change local directory to `snomed-ct-data`

3. Download US or International RF2 Files from SNOMED CT: https://www.nlm.nih.gov/healthit/snomedct/index.html

4. Place the downloaded SnomedCT_*_.zip in your snomed-ct-data/snomed-ct-origin directory.

5. Ensure the snomed-ct-data/pom.xml <source.zip></source.zip> tag contains the proper filename for the downloaded file.

6. Create a ~/Solor directory and ensure ~/Solor/generated-data does not exist or is empty.

7. Enter the following command to build the dataset:

```bash
mvn clean install
```

8. You can create a reasoned or unreasoned dataset by either including or commenting out the snomed-ct-data/pom.xml <module>snomed-ct-reasoner</module>


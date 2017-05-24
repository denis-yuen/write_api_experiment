package io.dockstore.client.cli;

import java.io.File;

/**
 * @author gluu
 * @since 24/05/17
 */
public class TestData {
    public static final File descriptor = new File("src/test/resources/imports.cwl");
    public static final String descriptorPath = descriptor.getAbsolutePath();
    public static final File dockerfile = new File("src/test/resources/Dockerfile2");
    public static final String dockerfilePath = dockerfile.getAbsolutePath();
    public static final File testJson = new File("src/test/resources/Test.json");
    public static final String testJsonPath = testJson.getAbsolutePath();
    public static final File configFile = new File("src/test/resources/write.api.config.properties");
    public static final String configFilePath = configFile.getAbsolutePath();
    public static final File secondaryDescriptor = new File("src/test/resources/envvar-global.yml");
    public static final String secondaryDescriptorPath = secondaryDescriptor.getAbsolutePath();
    public static final String id = "dockstore-testing/travis-test";
    public static final String version = "3.0";
}

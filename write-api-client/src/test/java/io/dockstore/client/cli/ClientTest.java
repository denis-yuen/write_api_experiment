package io.dockstore.client.cli;

import java.io.File;

import com.beust.jcommander.ParameterException;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.ga4gh.reference.ServerApplication;
import io.ga4gh.reference.ServerConfiguration;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

/**
 * @author gluu
 * @since 22/03/17
 */
public class ClientTest {

    @ClassRule
    public static final DropwizardAppRule<ServerConfiguration> RULE = new DropwizardAppRule<>(ServerApplication.class,
            ResourceHelpers.resourceFilePath("ref.yml"));
    private static final File descriptor = new File("src/test/resources/Dockstore.cwl");
    private static final String descriptorPath = descriptor.getAbsolutePath();
    private static final File dockerfile = new File("src/test/resources/Dockerfile");
    private static final String dockerfilePath = dockerfile.getAbsolutePath();
    private static final File testJson = new File("src/test/resources/Test.json");
    private static final String testJsonPath = testJson.getAbsolutePath();
    private static final File configFile = new File("src/test/resources/write.api.config.properties");
    private static final String configFilePath = configFile.getAbsolutePath();
    private static final File secondaryDescriptor = new File("src/test/resources/Dockstore.wdl");
    private static final String secondaryDescriptorPath = secondaryDescriptor.getAbsolutePath();
    private static final String id = "dockstore-testing/travis-test";
    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Test
    public void main() {
        Client.main(new String[] {});
        String log = systemOutRule.getLog();
        checkUsage(log);
    }

    @Test
    public void mainHelp() {
        Client.main(new String[] { "--help" });
        String log = systemOutRule.getLog();
        checkUsage(log);
    }

    @Test(expected = ParameterException.class)
    public void add() {
        String[] argv = { "add" };
        Client.main(argv);
    }

    @Test(expected = ParameterException.class)
    public void addDockerfile() {
        String[] argv = { "add", "--Dockerfile" };
        Client.main(argv);
    }

    @Test(expected = ParameterException.class)
    public void addDockerfileWithDockerfile() {
        String[] argv = { "add", "--Dockerfile", "dockerfile" };
        Client.main(argv);
    }

    @Test(expected = ParameterException.class)
    public void addDescriptor() {
        String[] argv = { "add", "--cwl-file" };
        Client.main(argv);
    }

    @Test(expected = ParameterException.class)
    public void addDescriptorWithDescriptor() {
        String[] argv = { "add", "--cwl-file", "descriptor" };
        Client.main(argv);
    }

    @Test(expected = ParameterException.class)
    public void addDockerfileAndDescriptorWithDescriptor() {
        String[] argv = { "add", "--Dockerfile", "--cwl-file", "descriptor" };
        Client.main(argv);
    }

    @Test(expected = ParameterException.class)
    public void addDockerfileWithDockerfileAndDescriptor() {
        String[] argv = { "add", "--Dockerfile", "dockerfile", "--cwl-file" };
        Client.main(argv);
    }

    @Ignore("Ignoring until we can delete repos.")
    @Test
    public void addDockerfileWithDockerfileAndDescriptorWithDescriptor() {
        String[] argv = { "add", "--id", id, "--Dockerfile", dockerfilePath, "--cwl-file", descriptorPath };
        Client.main(argv);
        String log = systemOutRule.getLog();
        Assert.assertTrue(log.contains("Handling add"));
        Assert.assertTrue(log.contains("Successfully added."));
    }

    @Test(expected = ParameterException.class)
    public void addDockerfileWithDockerfileAndDescriptorWithDescriptorAndVersion() {
        String[] argv = { "add", "--Dockerfile", "dockerfile", "--cwl-file", "descriptor", "--version" };
        Client.main(argv);
    }

    @Test(expected = ParameterException.class)
    public void addDockerfileWithDockerfileAndDescriptorWithDescriptorAndSecondaryDescriptor() {
        String[] argv = { "add", "--Dockerfile", "dockerfile", "--cwl-file", "descriptor", "--cwl-secondary-file" };
        Client.main(argv);
    }

    @Ignore("Ignoring until we can delete repos.")
    @Test
    public void addDockerfileWithDockerfileAndDescriptorWithDescriptorAndSecondaryDescriptorWithSecondaryDescriptor() {
        String[] argv = { "--config", configFilePath, "add", "--id", id, "--Dockerfile", dockerfilePath, "--cwl-file", descriptorPath,
                "--cwl-secondary-file", secondaryDescriptorPath };
        Client.main(argv);
        String log = systemOutRule.getLog();
        Assert.assertTrue(log.contains("Handling add"));
        Assert.assertTrue(log.contains("Successfully added."));
    }

    @Ignore("Ignoring until we can delete repos.")
    @Test
    public void addDockerfileWithDockerfileAndDescriptorWithDescriptorAndVersionWithVersion() {
        String[] argv = { "--config", configFilePath, "add", "--id", id, "--Dockerfile", dockerfilePath, "--cwl-file", descriptorPath,
                "--version", "version" };
        Client.main(argv);
        String log = systemOutRule.getLog();
        Assert.assertTrue(log.contains("Handling add"));
    }

    @Test
    public void addEverything() {
        String[] argv = { "--config", configFilePath, "add", "--id", id, "--Dockerfile", dockerfilePath, "--cwl-file", descriptorPath,
                "--cwl-secondary-file", secondaryDescriptorPath, "--version", "3.0" };
        Client.main(argv);
        String log = systemOutRule.getLog();
        Assert.assertTrue(log.contains("Handling add"));
    }

    @Test
    public void addHelp() {
        String[] argv = { "add", "--help" };
        Client.main(argv);
        String log = systemOutRule.getLog();
        checkUsage(log);
    }

    @Test(expected = ParameterException.class)
    public void publish() {
        String[] argv = { "publish" };
        Client.main(argv);
    }

    @Test
    public void publishHelp() {
        String[] argv = { "publish", "--help" };
        Client.main(argv);
        String log = systemOutRule.getLog();
        checkUsage(log);
    }

    @Test(expected = ParameterException.class)
    public void publishTool() {
        String[] argv = { "publish", "--tool" };
        Client.main(argv);
    }

    @Test
    public void publishToolWithTool() {
        String[] argv = { "--config", configFilePath, "publish", "--tool", testJsonPath };
        Client.main(argv);
        String log = systemOutRule.getLog();
        Assert.assertTrue(log.contains("Handling publish"));
    }

    private void checkUsage(String log) {
        Assert.assertTrue(log.contains("Usage"));
    }

}

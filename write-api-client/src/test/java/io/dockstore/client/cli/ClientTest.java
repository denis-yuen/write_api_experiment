package io.dockstore.client.cli;

import java.io.File;

import com.beust.jcommander.ParameterException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

/**
 * @author gluu
 * @since 22/03/17
 */
public class ClientTest {
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

    @Ignore("Test is ignored until there are valid or mocked github and quay.io tokens")
    @Test
    public void addDockerfileWithDockerfileAndDescriptorWithDescriptor() {
        File descriptor = new File("src/test/resources/Dockstore.cwl");
        String descriptorPath = descriptor.getAbsolutePath();
        File dockerfile = new File("src/test/resources/Dockerfile");
        String dockerfilePath = dockerfile.getAbsolutePath();
        String[] argv = { "add", "--Dockerfile", dockerfilePath, "--cwl-file", descriptorPath };
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

    @Ignore("Test is ignored until there are valid or mocked github and quay.io tokens")
    @Test
    public void addDockerfileWithDockerfileAndDescriptorWithDescriptorAndSecondaryDescriptorWithSecondaryDescriptor() {
        File descriptor = new File("src/test/resources/Dockstore.cwl");
        String descriptorPath = descriptor.getAbsolutePath();
        File dockerfile = new File("src/test/resources/Dockerfile");
        String dockerfilePath = dockerfile.getAbsolutePath();
        File secondaryDescriptor = new File("src/test/resources/Dockstore.wdl");
        String secondaryDescriptorPath = secondaryDescriptor.getAbsolutePath();
        String[] argv = { "add", "--Dockerfile", dockerfilePath, "--cwl-file", descriptorPath, "--cwl-secondary-file",
                secondaryDescriptorPath };
        Client.main(argv);
        String log = systemOutRule.getLog();
        Assert.assertTrue(log.contains("Handling add"));
        Assert.assertTrue(log.contains("Successfully added."));
    }

    @Ignore("Test is ignored until there are valid or mocked github and quay.io tokens")
    @Test
    public void addDockerfileWithDockerfileAndDescriptorWithDescriptorAndVersionWithVersion() {
        String[] argv = { "add", "--Dockerfile", "dockerfile", "--cwl-file", "descriptor", "--version", "version" };
        Client.main(argv);
        String log = systemOutRule.getLog();
        Assert.assertTrue(log.contains("Handling add"));
    }

    @Ignore("Test is ignored until there are valid or mocked github and quay.io tokens")
    @Test
    public void addEverything() {
        File descriptor = new File("src/test/resources/Dockstore.cwl");
        String descriptorPath = descriptor.getAbsolutePath();
        File dockerfile = new File("src/test/resources/Dockerfile");
        String dockerfilePath = dockerfile.getAbsolutePath();
        File secondaryDescriptor = new File("src/test/resources/Dockstore.wdl");
        String secondaryDescriptorPath = secondaryDescriptor.getAbsolutePath();
        String[] argv = { "add", "--Dockerfile", dockerfilePath, "--cwl-file", descriptorPath, "--cwl-secondary-file",
                secondaryDescriptorPath, "--version", "3.0" };
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
        File testJson = new File("src/test/resources/Test.json");
        String testJsonPath = testJson.getAbsolutePath();
        File configFile = new File("src/test/resources/write.api.config.properties");
        String configFilePath = configFile.getAbsolutePath();
        String[] argv = { "--config", configFilePath, "publish", "--tool", testJsonPath };
        Client.main(argv);
        String log = systemOutRule.getLog();
        Assert.assertTrue(log.contains("Handling publish"));
    }

    private void checkUsage(String log) {
        Assert.assertTrue(log.contains("Usage"));
    }

}

package io.dockstore.client.cli;

import java.util.concurrent.TimeUnit;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.ga4gh.reference.ServerApplication;
import io.ga4gh.reference.ServerConfiguration;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import static io.dockstore.client.cli.TestData.configFilePath;
import static io.dockstore.client.cli.TestData.descriptorPath;
import static io.dockstore.client.cli.TestData.dockerfilePath;
import static io.dockstore.client.cli.TestData.id;
import static io.dockstore.client.cli.TestData.secondaryDescriptorPath;
import static io.dockstore.client.cli.TestData.testJsonPath;
import static io.dockstore.client.cli.TestData.version;

/**
 * @author gluu
 * @since 24/05/17
 */
public class ClientTestIT {

    @ClassRule
    public static final DropwizardAppRule<ServerConfiguration> RULE = new DropwizardAppRule<>(ServerApplication.class,
            ResourceHelpers.resourceFilePath("ref.yml"));

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    private void addEverything() {
        String[] argv = { "--config", configFilePath, "add", "--id", id, "--Dockerfile", dockerfilePath, "--cwl-file", descriptorPath,
                "--cwl-secondary-file", secondaryDescriptorPath, "--version", version };
        Client.main(argv);
        String log = systemOutRule.getLog();
        Assert.assertTrue(log.contains("Handling add"));
    }

    private void publishToolWithTool() {
        String[] argv = { "--config", configFilePath, "publish", "--tool", testJsonPath };
        Client.main(argv);
        String log = systemOutRule.getLog();
        Assert.assertTrue("Expecting \"Successfully published tool\" but got " + log, log.contains("Successfully published tool"));
    }

    private void check() {
        String[] argv = { "--config", configFilePath, "check", "--id", id, "--version", version };
        Client.main(argv);
        String log = systemOutRule.getLog();
        Assert.assertTrue(log.contains("Tool properly registered and version is valid"));
        Assert.assertTrue(log.contains("Docker image available"));
    }

    @Test
    public void integrationTest() throws InterruptedException {
        addEverything();
        // Sleeping because publish does not work until the image is built
        System.out.println("Sleeping for 5 minutes while Quay.io is building the image");
        TimeUnit.MINUTES.sleep((long)1);
        System.out.println("Sleeping for 4 more minutes while Quay.io is building the image");
        TimeUnit.MINUTES.sleep((long)1);
        System.out.println("Sleeping for 3 more minutes while Quay.io is building the image");
        TimeUnit.MINUTES.sleep((long)1);
        System.out.println("Sleeping for 2 more minutes while Quay.io is building the image");
        TimeUnit.MINUTES.sleep((long)1);
        System.out.println("Sleeping for 1 more minute while Quay.io is building the image");
        TimeUnit.MINUTES.sleep((long)1);
        System.out.println("Done sleeping, attempting to publish tool");
        publishToolWithTool();
        check();
    }
}

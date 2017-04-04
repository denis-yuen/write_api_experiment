package io.dockstore.tooltester.client;

import java.io.IOException;

import com.google.common.collect.Lists;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.ga4gh.reference.ServerApplication;
import io.ga4gh.reference.ServerConfiguration;
import io.swagger.client.write.ApiClient;
import io.swagger.client.write.ApiException;
import io.swagger.client.write.api.GAGHApi;
import io.swagger.client.write.api.GAGHoptionalwriteApi;
import io.swagger.client.write.model.Metadata;
import io.swagger.client.write.model.Tool;
import io.swagger.client.write.model.ToolDockerfile;
import io.swagger.client.write.model.ToolVersion;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

public class ClientTest {

    // these constants should be changed if you wish to run tests.
    // TODO: pre-emptively check that these are valid names for both quay.io and github

    // an organization for both GitHub and Quay.io where repos will be created (and deleted)
    public static final String ORGANIZATION_NAME = "dockstore-testing";
    // repo name for GitHub and Quay.io, this repo will be created and deleted
    public static final String REPO_NAME = "test_repo2";

    @ClassRule
    public static final DropwizardAppRule<ServerConfiguration> RULE = new DropwizardAppRule<>(
            ServerApplication.class, ResourceHelpers.resourceFilePath("ref.yml"));

    @Test
    public void validateGA4GH() throws IOException, ApiException {
        GAGHApi api = getGaghApi();
        Metadata metadata = api.metadataGet();
        Assert.assertTrue(!metadata.getVersion().isEmpty());
        Assert.assertTrue(!metadata.getCountry().isEmpty());
    }

    @Ignore("Test is ignored until there are valid or mocked quay.io and github tokens")
    @Test
    public void postDescriptorAndCreateRepo() throws IOException, ApiException {
        // watch out, versions can't start with a "v"
        final String toolVersionNumber = "1.0";

        GAGHoptionalwriteApi api = getGaghOptionalApi();
        Tool tool = new Tool();
        tool.setId(ORGANIZATION_NAME + "/" + REPO_NAME);
        tool.setOrganization(ORGANIZATION_NAME);
        tool.setToolname(REPO_NAME);
        Tool createdTool = api.toolsPost(tool);
        Assert.assertTrue(createdTool.getOrganization().equals(ORGANIZATION_NAME));

        // github repo has been created by now
        // next create release
        ToolVersion version = new ToolVersion();
        version.setId("id");
        version.setName(toolVersionNumber);
        version.setDescriptorType(Lists.newArrayList(ToolVersion.DescriptorTypeEnum.CWL));
        ToolVersion toolVersion = api.toolsIdVersionsPost(ORGANIZATION_NAME + "/" +REPO_NAME, version);
        Assert.assertTrue(toolVersion != null);
        // create files, this should trigger a quay.io build
        ToolDockerfile toolDockerfile = new ToolDockerfile();
        toolDockerfile.setDockerfile("FROM ubuntu:12.04");
        ToolDockerfile returnedDockerfile = api.toolsIdVersionsVersionIdDockerfilePost(ORGANIZATION_NAME + "/" +REPO_NAME, toolVersionNumber, toolDockerfile);
        Assert.assertTrue(returnedDockerfile != null);
    }

    private GAGHApi getGaghApi() {
        final int localPort = RULE.getLocalPort();
        ApiClient client = new ApiClient();
        client.setBasePath("http://localhost:" + localPort + "/api/ga4gh/v1");
        return new GAGHApi(client);
    }

    private GAGHoptionalwriteApi getGaghOptionalApi() {
        final int localPort = RULE.getLocalPort();
        ApiClient client = new ApiClient();
        client.setBasePath("http://localhost:" + localPort + "/api/ga4gh/v1");
        return new GAGHoptionalwriteApi(client);
    }

}

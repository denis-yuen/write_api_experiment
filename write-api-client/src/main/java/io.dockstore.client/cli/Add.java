package io.dockstore.client.cli;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import io.swagger.client.ApiException;
import io.swagger.client.api.GAGHoptionalwriteApi;
import io.swagger.client.model.Tool;
import io.swagger.client.model.ToolDockerfile;
import io.swagger.client.model.ToolVersion;
import json.Output;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gluu
 * @since 23/03/17
 */
public class Add {
    // an organization for both GitHub and Quay.io where repos will be created (and deleted)
    public static final String ORGANIZATION_NAME = "dockstore-testing";
    // repo name for GitHub and Quay.io, this repo will be created and deleted
    public static final String REPO_NAME = "test_repo2";
    private static final Logger LOGGER = LoggerFactory.getLogger(Add.class);

    public Add() {
    }

    public void handleAdd(String dockerfile, String descriptor, String secondaryDescriptor, String version) {
        // watch out, versions can't start with a "v"

        if (version == null) {
            version = "1.0";
        }
        LOGGER.info("Handling add");
        GAGHoptionalwriteApi api = WriteAPIServiceHelper.getGaghOptionalApi();
        Tool tool = createTool();
        Tool responseTool;
        try {
            responseTool = api.toolsPost(tool);
            Assert.assertTrue(responseTool.getOrganization().equals(ORGANIZATION_NAME));
            LOGGER.info("Created git repo");
        } catch (ApiException e) {
            LOGGER.error("ApiException: " + e.getMessage());
            return;
        }

        // github repo has been created by now
        // next create release
        ToolVersion toolVersion = createToolVersion(version);
        try {
            ToolVersion responseToolVersion = api.toolsIdVersionsPost(ORGANIZATION_NAME + "/" + REPO_NAME, toolVersion);
            Assert.assertTrue(responseToolVersion != null);
            LOGGER.info("Created git tag");
        } catch (ApiException e) {
            LOGGER.error("ApiException: " + e.getMessage());
            return;
        }

        // create files, this should trigger a quay.io build
        ToolDockerfile toolDockerfile = createToolDockerfile();
        ToolDockerfile responseDockerfile;
        try {
            responseDockerfile = api.toolsIdVersionsVersionIdDockerfilePost(ORGANIZATION_NAME + "/" + REPO_NAME, version, toolDockerfile);
            Assert.assertTrue(responseDockerfile != null);
        } catch (ApiException e) {
            LOGGER.error("ApiException: " + e.getMessage());
            return;
        }

        // Building the URLs myself because
        Output output = new Output();
        output.setGithubURL(responseTool.getUrl());
        output.setQuayioURL(responseDockerfile.getUrl());
        output.setVersion(version);
        Gson gson = new Gson();
        String json = gson.toJson(output);
        System.out.println(json);
    }

    private Tool createTool() {
        Tool tool = new Tool();
        tool.setId(ORGANIZATION_NAME + "/" + REPO_NAME);
        tool.setOrganization(ORGANIZATION_NAME);
        tool.setToolname(REPO_NAME);
        return tool;
    }

    private ToolVersion createToolVersion(String version) {
        ToolVersion toolVersion = new ToolVersion();
        toolVersion.setId("id");
        toolVersion.setName(version);
        toolVersion.setDescriptorType(Lists.newArrayList(ToolVersion.DescriptorTypeEnum.CWL));
        return toolVersion;
    }

    private ToolDockerfile createToolDockerfile() {
        ToolDockerfile toolDockerfile = new ToolDockerfile();
        toolDockerfile.setDockerfile("FROM ubuntu:12.04");
        return toolDockerfile;
    }
}

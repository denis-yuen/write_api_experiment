package io.dockstore.client.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.swagger.client.write.ApiException;
import io.swagger.client.write.api.GAGHoptionalwriteApi;
import io.swagger.client.write.model.Tool;
import io.swagger.client.write.model.ToolDescriptor;
import io.swagger.client.write.model.ToolDockerfile;
import io.swagger.client.write.model.ToolVersion;
import json.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.dockstore.client.cli.ConfigFileHelper.getIniConfiguration;

/**
 * @author gluu
 * @since 23/03/17
 */
class Add {
    // an organization for both GitHub and Quay.io where repos will be created (and deleted)
    private static final Properties PROPERTIES = getIniConfiguration();
    private static final String ORGANIZATION_NAME = PROPERTIES.getProperty("organization", "dockstore-testing");
    // repo name for GitHub and Quay.io, this repo will be created and deleted
    private static final String REPO_NAME = PROPERTIES.getProperty("repo", "test_repo3");
    private static final Logger LOGGER = LoggerFactory.getLogger(Add.class);

    Add() {
    }

    void handleAdd(String dockerfile, String descriptor, String secondaryDescriptor, String version) {
        // watch out, versions can't start with a "v"
        if (version == null) {
            version = "1.0";
        }
        LOGGER.info("Handling add...");
        ToolDockerfile toolDockerfile = createToolDockerfile(dockerfile);
        ToolDescriptor toolDescriptor = createDescriptor(descriptor);
        if (toolDockerfile == null) {
            ExceptionHelper.errorMessage("Dockerfile is empty.", ExceptionHelper.CLIENT_ERROR);
        } else if (toolDescriptor == null) {
            ExceptionHelper.errorMessage("Descriptor is empty.", ExceptionHelper.CLIENT_ERROR);
        }
        GAGHoptionalwriteApi api = WriteAPIServiceHelper.getGaghOptionalApi();
        Tool tool = createTool();
        Tool responseTool = null;
        try {
            responseTool = api.toolsPost(tool);
            assert (responseTool != null);
            assert (responseTool.getOrganization().equals(ORGANIZATION_NAME));
            LOGGER.info("Created repository on git.");
        } catch (ApiException e) {
            ExceptionHelper.errorMessage("Could not create repository: " + e.getMessage(), ExceptionHelper.CLIENT_ERROR);
        }

        // github repo has been created by now
        // next create release
        ToolVersion toolVersion = createToolVersion(version);
        ToolVersion responseToolVersion;
        try {
            responseToolVersion = api.toolsIdVersionsPost(ORGANIZATION_NAME + "/" + REPO_NAME, toolVersion);
            assert (responseToolVersion != null);
            LOGGER.info("Created branch, tag, and release on git.");
        } catch (ApiException e) {
            ExceptionHelper.errorMessage("Could not create tag/release: " + e.getMessage(), ExceptionHelper.CLIENT_ERROR);
        }

        // create dockerfile, this should trigger a quay.io build

        ToolDockerfile responseDockerfile = null;
        try {
            responseDockerfile = api.toolsIdVersionsVersionIdDockerfilePost(ORGANIZATION_NAME + "/" + REPO_NAME, version, toolDockerfile);
            assert (responseDockerfile != null);
            LOGGER.info("Created dockerfile on git.");
        } catch (ApiException e) {
            ExceptionHelper.errorMessage("Could not create Dockerfile: " + e.getMessage(), ExceptionHelper.CLIENT_ERROR);
        }

        // Create descriptor file
        ToolDescriptor responseDescriptor;
        try {
            assert toolDescriptor != null;
            responseDescriptor = api
                    .toolsIdVersionsVersionIdTypeDescriptorPost(toolDescriptor.getType().toString(), ORGANIZATION_NAME + "/" + REPO_NAME,
                            version, toolDescriptor);
            assert (responseDescriptor != null);
            LOGGER.info("Created descriptor on git.");
        } catch (ApiException e) {
            ExceptionHelper.errorMessage("Could not create descriptor file. " + e.getMessage(), ExceptionHelper.CLIENT_ERROR);
        }

        // Create secondary descriptor file
        if (secondaryDescriptor != null) {
            ToolDescriptor secondaryToolDescriptor = createDescriptor(secondaryDescriptor);
            ToolDescriptor responseSecondaryDescriptor;
            try {
                responseSecondaryDescriptor = api.toolsIdVersionsVersionIdTypeDescriptorPost(secondaryToolDescriptor.getType().toString(),
                        ORGANIZATION_NAME + "/" + REPO_NAME, version, secondaryToolDescriptor);
                assert (responseSecondaryDescriptor != null);
                LOGGER.info("Created secondary descriptor on git.");
            } catch (ApiException e) {
                ExceptionHelper.errorMessage("Could not create secondary descriptor file" + e.getMessage(), ExceptionHelper.CLIENT_ERROR);
            }
        }

        // Building the URLs myself because life is hard
        Output output = new Output();
        assert responseTool != null;
        output.setGithubURL(responseTool.getUrl());
        assert responseDockerfile != null;
        output.setQuayioURL(responseDockerfile.getUrl());
        output.setVersion(version);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(output);
        LOGGER.info("Successfully added tool.");
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

    private ToolDockerfile createToolDockerfile(String stringPath) {
        ToolDockerfile toolDockerfile = new ToolDockerfile();
        Path path = Paths.get(stringPath);
        String fileName = path.getFileName().toString();
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            toolDockerfile.setDockerfile(content);
            // Temporarily setting the url to the filename
            toolDockerfile.setUrl(fileName);
        } catch (IOException e) {
            ExceptionHelper.errorMessage("Could not read dockerfile" + e.getMessage(), ExceptionHelper.CLIENT_ERROR);
        }
        return toolDockerfile;
    }

    private ToolDescriptor createDescriptor(String stringPath) {
        ToolDescriptor toolDescriptor = new ToolDescriptor();
        try {
            Path path = Paths.get(stringPath);
            String fileName = path.getFileName().toString();
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            toolDescriptor.setDescriptor(content);
            toolDescriptor.setType(ToolDescriptor.TypeEnum.CWL);
            // Temporarily setting the url to the filename, otherwise there's no way to pass it
            toolDescriptor.setUrl(fileName);
        } catch (IOException e) {
            ExceptionHelper.errorMessage("Could not read descriptor file" + e.getMessage(), ExceptionHelper.CLIENT_ERROR);
        }
        return toolDescriptor;
    }
}

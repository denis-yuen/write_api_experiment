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
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.dockstore.client.cli.ConfigFileHelper.getIniConfiguration;

/**
 * @author gluu
 * @since 23/03/17
 */
class Add {
    private static final Logger LOGGER = LoggerFactory.getLogger(Add.class);
    private Properties properties;
    private String config;

    Add(String config) {
        setConfig(config);
        properties = getIniConfiguration(getConfig());
    }

    private String getConfig() {
        return config;
    }

    private void setConfig(String config) {
        this.config = config;
    }

    /**
     * Handles the add command
     *
     * @param dockerfile          The dockerfile path
     * @param descriptor          The descriptor path
     * @param secondaryDescriptor The secondary descriptor path
     * @param version             The version of the tool
     */
    @Transaction
    void handleAdd(String dockerfile, String descriptor, String secondaryDescriptor, String version, String id) {
        LOGGER.info("Handling add...");
        String[] idParts = id.split("/");
        if (idParts.length != 2) {
            throw new RuntimeException("--id parameter is invalid.  It must contain organization and repo separated by a slash");
        }
        String organizationName = idParts[0];
        String repoName = idParts[1];
        ToolDockerfile toolDockerfile = createToolDockerfile(dockerfile);
        ToolDescriptor toolDescriptor = createDescriptor(descriptor);
        GAGHoptionalwriteApi api = WriteAPIServiceHelper.getGaghOptionalApi(properties);
        Tool tool = createTool(organizationName, repoName);
        Tool responseTool = null;
        try {
            responseTool = api.toolsPost(tool);
            assert (responseTool != null);
            assert (responseTool.getOrganization().equals(organizationName));
            LOGGER.info("Created repository on git.");
        } catch (ApiException e) {
            ExceptionHelper.errorMessage(LOGGER, "Could not create repository: " + e.getMessage(), ExceptionHelper.API_ERROR);
        }

        // github repo has been created by now
        // next create release
        ToolVersion toolVersion = createToolVersion(version);
        ToolVersion responseToolVersion;
        try {
            responseToolVersion = api.toolsIdVersionsPost(organizationName + "/" + repoName, toolVersion);
            assert (responseToolVersion != null);
            LOGGER.info("Created branch, tag, and release on git.");
        } catch (ApiException e) {
            ExceptionHelper.errorMessage(LOGGER, "Could not create tag/release: " + e.getMessage(), ExceptionHelper.API_ERROR);
        }

        // create dockerfile, this should trigger a quay.io build

        ToolDockerfile responseDockerfile = null;
        try {
            responseDockerfile = api.toolsIdVersionsVersionIdDockerfilePost(organizationName + "/" + repoName, version, toolDockerfile);
            assert (responseDockerfile != null);
            LOGGER.info("Created dockerfile on git.");
        } catch (ApiException e) {
            ExceptionHelper.errorMessage(LOGGER, "Could not create Dockerfile: " + e.getMessage(), ExceptionHelper.API_ERROR);
        }

        // Create descriptor file
        ToolDescriptor responseDescriptor;
        try {
            assert toolDescriptor != null;
            responseDescriptor = api
                    .toolsIdVersionsVersionIdTypeDescriptorPost(toolDescriptor.getType().toString(), organizationName + "/" + repoName,
                            version, toolDescriptor);
            assert (responseDescriptor != null);
            LOGGER.info("Created descriptor on git.");
        } catch (ApiException e) {
            ExceptionHelper.errorMessage(LOGGER, "Could not create descriptor file. " + e.getMessage(), ExceptionHelper.API_ERROR);
        }

        // Create secondary descriptor file
        if (secondaryDescriptor != null) {
            ToolDescriptor secondaryToolDescriptor = createDescriptor(secondaryDescriptor);
            ToolDescriptor responseSecondaryDescriptor;
            try {
                responseSecondaryDescriptor = api.toolsIdVersionsVersionIdTypeDescriptorPost(secondaryToolDescriptor.getType().toString(),
                        organizationName + "/" + repoName, version, secondaryToolDescriptor);
                assert (responseSecondaryDescriptor != null);
                LOGGER.info("Created secondary descriptor on git.");
            } catch (ApiException e) {
                ExceptionHelper
                        .errorMessage(LOGGER, "Could not create secondary descriptor file" + e.getMessage(), ExceptionHelper.API_ERROR);
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

    private Tool createTool(String organizationName, String repoName) {
        Tool tool = new Tool();
        tool.setId(organizationName + "/" + repoName);
        tool.setOrganization(organizationName);
        tool.setToolname(repoName);
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
        if (path == null) {
            throw new RuntimeException("Could not get file path.");
        }
        Path filePath = path.getFileName();
        if (filePath == null) {
            throw new RuntimeException("Could not get file path.");
        }
        String fileName = filePath.toString();
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            toolDockerfile.setDockerfile(content);
            // Temporarily setting the url to the filename
            toolDockerfile.setUrl(fileName);
        } catch (IOException e) {
            ExceptionHelper.errorMessage(LOGGER, "Could not read dockerfile" + e.getMessage(), ExceptionHelper.IO_ERROR);
        }
        return toolDockerfile;
    }

    private ToolDescriptor createDescriptor(String stringPath) {
        ToolDescriptor toolDescriptor = new ToolDescriptor();
        try {
            Path path = Paths.get(stringPath);
            if (path == null) {
                throw new RuntimeException("Could not get file path.");
            }
            Path filePath = path.getFileName();
            if (filePath == null) {
                throw new RuntimeException("Could not get file path.");
            }
            String fileName = filePath.toString();
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            toolDescriptor.setDescriptor(content);
            toolDescriptor.setType(ToolDescriptor.TypeEnum.CWL);
            // Temporarily setting the url to the filename, otherwise there's no way to pass it
            toolDescriptor.setUrl(fileName);
        } catch (IOException e) {
            ExceptionHelper.errorMessage(LOGGER, "Could not read descriptor file" + e.getMessage(), ExceptionHelper.IO_ERROR);
        }
        return toolDescriptor;
    }
}

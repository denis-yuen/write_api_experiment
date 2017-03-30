package io.dockstore.client.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.Configuration;
import io.swagger.client.api.ContainersApi;
import io.swagger.client.model.DockstoreTool;
import io.swagger.client.model.PublishRequest;
import json.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.dockstore.client.cli.ExceptionHelper.CLIENT_ERROR;
import static io.dockstore.client.cli.ExceptionHelper.errorMessage;

/**
 * @author gluu
 * @since 23/03/17
 */
class Publish {
    private static final Logger LOGGER = LoggerFactory.getLogger(Publish.class);

    Publish() {
    }

    private static Output getJson(String filePath) {
        Output output = null;
        try {
            Path path = Paths.get(filePath);
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            output = gson.fromJson(content, Output.class);
        } catch (IOException e) {
            errorMessage("Could not read json file" + e.getMessage(), CLIENT_ERROR);
        }
        return output;
    }

    void handlePublish(String tool) {
        String token = "60d4f75197f3f2a1b3e70a0f56d901afa6991851b3d51c1df11efcbb62617649";
        String serverUrl = "http://10.0.29.120:8080";
        ApiClient defaultApiClient;
        defaultApiClient = Configuration.getDefaultApiClient();
        defaultApiClient.addDefaultHeader("Authorization", "Bearer " + token);
        defaultApiClient.setBasePath(serverUrl);

        ContainersApi containersApi = new ContainersApi(defaultApiClient);
        Output output = getJson(tool);
        String gitURL = output.getGithubURL();

        String[] bits = gitURL.split("/");
        String name = bits[bits.length - 1];
        String namespace = bits[bits.length - 2];

        final String dockerfilePath = "/Dockerfile";
        final String cwlPath = "/Dockstore.cwl";
        final String wdlPath = "/Dockstore.wdl";
        final String gitReference = output.getVersion();
        final String toolMaintainerEmail = null;
        final String registry = DockstoreTool.RegistryEnum.QUAY_IO.name();
        final String privateAccess = "false";
        final String customDockerPath = null;

        // Check that registry is valid
        boolean validRegistry = true;

        // Determine if chosen registry has special conditions
        boolean isPrivateRegistry = false;
        boolean hasCustomDockerPath = false;

        // Check if registry needs to override the docker path
        if (hasCustomDockerPath) {
            // Ensure that customDockerPath is not null
            // TODO: add validity checker for given path
            if (Strings.isNullOrEmpty(customDockerPath)) {
                errorMessage(registry + " requires a custom Docker path to be set.", Client.CLIENT_ERROR);
            }
        }

        // Check for correct private access
        if (!("false".equalsIgnoreCase(privateAccess) || "true".equalsIgnoreCase(privateAccess))) {
            errorMessage("The possible values for --private are 'true' and 'false'.", Client.CLIENT_ERROR);
        }

        DockstoreTool dockstoreTool = new DockstoreTool();
        dockstoreTool.setMode(DockstoreTool.ModeEnum.MANUAL_IMAGE_PATH);
        dockstoreTool.setName(name);
        dockstoreTool.setNamespace(namespace);
        dockstoreTool.setRegistry(DockstoreTool.RegistryEnum.QUAY_IO);

        // Registry path used (ex. quay.io)
        String registryPath = "quay.io";

        dockstoreTool.setPath(output.getQuayioURL());
        dockstoreTool.setDefaultDockerfilePath(dockerfilePath);
        dockstoreTool.setDefaultCwlPath(cwlPath);
        dockstoreTool.setDefaultWdlPath(wdlPath);
        dockstoreTool.setIsPublished(false);
        dockstoreTool.setDefaultVersion(output.getVersion());
        dockstoreTool.setGitUrl(gitURL);
        dockstoreTool.setPrivateAccess(false);
        dockstoreTool.setToolMaintainerEmail(toolMaintainerEmail);

        // Check that tool has at least one default path
        if (Strings.isNullOrEmpty(cwlPath) && Strings.isNullOrEmpty(wdlPath)) {
            errorMessage("A tool must have at least one descriptor default path.", Client.CLIENT_ERROR);
        }

        // Register new tool
        final String fullName = Joiner.on("/").skipNulls().join(registryPath, namespace, name);
        try {
            dockstoreTool = containersApi.registerManual(dockstoreTool);
            if (dockstoreTool != null) {
                // Refresh to update validity
                containersApi.refresh(dockstoreTool.getId());
            } else {
                errorMessage("Unable to register " + fullName, Client.COMMAND_ERROR);
            }
        } catch (final ApiException ex) {
            LOGGER.error("Unable to register " + fullName);
        }

        // If registration is successful then attempt to publish it
        if (dockstoreTool != null) {
            PublishRequest pub = new PublishRequest();
            pub.setPublish(true);
            DockstoreTool publishedTool;
            try {
                publishedTool = containersApi.publish(dockstoreTool.getId(), pub);
                if (publishedTool.getIsPublished()) {
                    LOGGER.info("Successfully published " + fullName);
                } else {
                    LOGGER.info("Successfully registered " + fullName + ", however it is not valid to publish."); // Should this throw an
                    // error?
                }
            } catch (ApiException ex) {
                LOGGER.error("Successfully registered " + fullName + ", however it is not valid to publish.");
            }
        }

        LOGGER.info("Handling publish");
    }
}

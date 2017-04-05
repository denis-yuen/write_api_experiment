package io.dockstore.client.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import com.google.gson.Gson;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.Configuration;
import io.swagger.client.api.ContainersApi;
import io.swagger.client.api.ContainertagsApi;
import io.swagger.client.api.UsersApi;
import io.swagger.client.model.DockstoreTool;
import io.swagger.client.model.PublishRequest;
import io.swagger.client.model.Tag;
import io.swagger.client.model.User;
import json.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.dockstore.client.cli.ConfigFileHelper.getIniConfiguration;

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
            ExceptionHelper.errorMessage("Could not read json file" + e.getMessage(), ExceptionHelper.CLIENT_ERROR);
        }
        return output;
    }

    void handlePublish(String tool) {
        LOGGER.info("Handling publish");
        Properties prop = getIniConfiguration();
        String token = prop.getProperty("token", "");
        String serverUrl = prop.getProperty("server-url", "https://www.dockstore.org:8443");
        ApiClient defaultApiClient;
        defaultApiClient = Configuration.getDefaultApiClient();
        defaultApiClient.addDefaultHeader("Authorization", "Bearer " + token);
        defaultApiClient.setBasePath(serverUrl);

        ContainersApi containersApi = new ContainersApi(defaultApiClient);
        UsersApi usersApi = new UsersApi(defaultApiClient);
        try {
            User user = usersApi.getUser();
            Long userId = user.getId();
            usersApi.refresh(userId);
        } catch (ApiException e) {
            LOGGER.info(e.getMessage());
        }
        Output output = getJson(tool);
        String gitURL = output.getGithubURL();

        String[] bits = gitURL.split("/");
        String name = bits[bits.length - 1];
        String namespace = bits[bits.length - 2];

        DockstoreTool dockstoreTool;
        try {
            dockstoreTool = containersApi.getContainerByToolPath("quay.io" + "/" + namespace + "/" + name);
            ContainertagsApi containertagsApi = new ContainertagsApi(defaultApiClient);
            List<Tag> tagsByPath = containertagsApi.getTagsByPath(dockstoreTool.getId());
            Tag first = tagsByPath.parallelStream().filter(tag -> tag.getName().equals(output.getVersion())).findFirst().get();
            first.setReference(output.getVersion());
            containertagsApi.updateTags(dockstoreTool.getId(), tagsByPath);
            containersApi.refresh(dockstoreTool.getId());
        } catch (ApiException e) {
            LOGGER.info(e.getMessage());
            return;
        }
        try {
            PublishRequest pub = new PublishRequest();
            pub.setPublish(true);
            containersApi.publish(dockstoreTool.getId(), pub);
        } catch (ApiException e) {
            LOGGER.info(e.getMessage());
            return;
        }
        LOGGER.info("Successfully published tool.");
    }
}

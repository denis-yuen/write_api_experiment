package io.dockstore.client.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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
import io.swagger.client.model.Token;
import io.swagger.client.model.User;
import json.Output;
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.dockstore.client.cli.ConfigFileHelper.getIniConfiguration;

/**
 * @author gluu
 * @since 23/03/17
 */
class Publish {
    private static final Logger LOGGER = LoggerFactory.getLogger(Publish.class);
    private static String config;
    private static final int ONE_HUNDRED = 100;

    Publish(String config) {
        setConfig(config);
    }

    /**
     * Gets the Output object from the filepath
     *
     * @param filePath The path to the file containing json output from the add command
     * @return The output object
     */
    private static Output getJson(String filePath) {
        Output output = null;
        try {
            Path path = Paths.get(filePath);
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            output = gson.fromJson(content, Output.class);
        } catch (IOException e) {
            ExceptionHelper.errorMessage(LOGGER, "Could not read json file" + e.getMessage(), ExceptionHelper.IO_ERROR);
        }
        return output;
    }

    public static String getConfig() {
        return config;
    }

    public static void setConfig(String config) {
        Publish.config = config;
    }

    /**
     * Handles the publish command
     *
     * @param tool The path of the file containing the json outputted by the add command
     */
    @Transaction
    void handlePublish(String tool) {
        LOGGER.info("Handling publish");
        Properties prop = getIniConfiguration(config);
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
            try {
                TimeUnit.SECONDS.sleep(ONE_HUNDRED);
            } catch (InterruptedException e) {
                throw new RuntimeException("Could not sleep", e);
            }
        } catch (ApiException e) {
            LOGGER.info(e.getMessage());
            return;
        }
        Output output = getJson(tool);
        String gitURL = output.getGithubURL();

        String[] bits = gitURL.split("/");
        String name = bits[bits.length - 1];
        String namespace = bits[bits.length - 2];

        DockstoreTool dockstoreTool;

        try {
            dockstoreTool = containersApi.getContainerByToolPath("quay.io" + "/" + namespace + "/" + name);
        } catch (ApiException e) {
            e.printStackTrace();
            LOGGER.error("Could not get tool by tool path");
            try {
                User user = usersApi.getUser();
                Long id = user.getId();
                List<DockstoreTool> dockstoreTools = usersApi.userContainers(id);

                dockstoreTools.parallelStream().forEach(dockstoreTool1 -> LOGGER.info(dockstoreTool1.getPath()));
                List<Token> dockstoreUserTokens = usersApi.getDockstoreUserTokens(id);
                List<Token> quayUserTokens = usersApi.getQuayUserTokens(id);
                List<Token> githubUserTokens = usersApi.getGithubUserTokens(id);
                dockstoreUserTokens.parallelStream().forEach(token1 -> LOGGER.info("Dockstore token: " + token1.getContent()));
                quayUserTokens.parallelStream().forEach(token1 -> LOGGER.info("Quay token: " + token1.getId()));
                githubUserTokens.parallelStream().forEach(token1 -> LOGGER.info("GitHub token: " + token1.getId()));
            } catch (ApiException e1) {
                e1.printStackTrace();
                LOGGER.error("Could not get all tools");
            }
            return;
        }
        ContainertagsApi containertagsApi = new ContainertagsApi(defaultApiClient);

        List<Tag> tagsByPath;
        try {
            tagsByPath = containertagsApi.getTagsByPath(dockstoreTool.getId());
        } catch (ApiException e) {
            e.printStackTrace();
            LOGGER.error("Could not get tool tags by path");
            return;
        }
        try {
            Tag first = tagsByPath.parallelStream().filter(tag -> tag.getName().equals(output.getVersion())).findFirst().orElse(null);
            first.setReference(output.getVersion());
            containertagsApi.updateTags(dockstoreTool.getId(), tagsByPath);
            containersApi.refresh(dockstoreTool.getId());
        } catch (ApiException e) {
            e.printStackTrace();
            LOGGER.error("Could not refresh tool" + e.getMessage());
            return;
        }
        try {
            PublishRequest pub = new PublishRequest();
            pub.setPublish(true);
            containersApi.publish(dockstoreTool.getId(), pub);
        } catch (ApiException e) {
            LOGGER.error("Could not publish tool" + e.getMessage());
            return;
        }
        LOGGER.info("Successfully published tool.");
    }
}

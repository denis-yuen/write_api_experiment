package io.dockstore.client.cli;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import io.swagger.client.Configuration;
import io.swagger.client.api.ContainersApi;
import io.swagger.client.model.DockstoreTool;
import io.swagger.client.model.Tag;
import io.swagger.client.write.ApiException;
import io.swagger.client.write.api.GAGHApi;
import io.swagger.client.write.model.ToolVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.dockstore.client.cli.ConfigFileHelper.getIniConfiguration;

/**
 * @author gluu
 * @since 05/05/17
 */
public class Check {
    private static final Logger LOGGER = LoggerFactory.getLogger(Check.class);
    private Properties properties;
    private String config;

    Check(String config) {
        setConfig(config);
        properties = getIniConfiguration(getConfig());
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public void handleCheck(String id, String version) {
        LOGGER.info("Handling check");
        String token = properties.getProperty("token", "");
        String serverUrl = properties.getProperty("server-url", "https://www.dockstore.org:8443");
        io.swagger.client.ApiClient defaultApiClient;
        defaultApiClient = Configuration.getDefaultApiClient();
        defaultApiClient.addDefaultHeader("Authorization", "Bearer " + token);
        defaultApiClient.setBasePath(serverUrl);
        ContainersApi containersApi = new ContainersApi(defaultApiClient);
        try {
            DockstoreTool containerByToolPath = containersApi.getContainerByToolPath("quay.io/" + id);
            if (containerByToolPath == null) {
                System.out.println("Tool is not registered");
            } else {
                List<Tag> tags = containerByToolPath.getTags();
                Optional<Tag> first = tags.parallelStream().filter(e -> e.getName().equals(version)).findFirst();
                if (first.isPresent()) {
                    Tag tag = first.get();
                    Boolean valid = tag.getValid();
                    if (valid) {
                        System.out.println("Tool properly registered and version is valid");
                    } else {
                        System.out.println("Tool properly registered but version is not valid");
                    }
                } else {
                    System.out.println("Tool version does not exist");
                }
            }
        } catch (io.swagger.client.ApiException e) {
            System.err.println(e.getMessage());
        }
        GAGHApi api = WriteAPIServiceHelper.getGAGHApi(properties);
        try {
            ToolVersion toolVersion = api.toolsIdVersionsVersionIdGet(id, version);
            boolean imageReady = toolVersion.getImageReady();
            if (imageReady) {
                System.out.println("Docker image available");
            } else {
                System.out.println("Docker image is not available");
            }
        } catch (ApiException e) {
            throw new RuntimeException("Could not get tool version.", e);
        }
    }
}

package io.dockstore.client.cli;

import java.io.File;
import java.util.Properties;

import io.swagger.client.write.ApiClient;
import io.swagger.client.write.api.GAGHoptionalwriteApi;

import static io.dockstore.client.cli.ConfigFileHelper.getIniConfiguration;

/**
 * @author gluu
 * @since 23/03/17
 */
final class WriteAPIServiceHelper {
    private static final Properties PROPERTIES = getIniConfiguration(
            System.getProperty("user.home") + File.separator + ".dockstore" + File.separator + "write.api.config.properties");
    private static final String URL = PROPERTIES.getProperty("write-api-url", "http://localhost:8080/api/ga4gh/v1");

    private WriteAPIServiceHelper() {
    }
  
    /**
     * Gets the write-api-service
     *
     * @return The write-api-service
     */
    static GAGHoptionalwriteApi getGaghOptionalApi() {
        ApiClient client = new ApiClient();
        client.setBasePath(URL);
        return new GAGHoptionalwriteApi(client);
    }
}

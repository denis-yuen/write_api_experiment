package io.dockstore.client.cli;

import java.util.Properties;

import io.swagger.client.write.ApiClient;
import io.swagger.client.write.api.GAGHoptionalwriteApi;

/**
 * @author gluu
 * @since 23/03/17
 */
final class WriteAPIServiceHelper {
    private WriteAPIServiceHelper() {
    }

    /**
     * Gets the write-api-service
     *
     * @param properties
     * @return The write-api-service
     */
    static GAGHoptionalwriteApi getGaghOptionalApi(Properties properties) {
        String url = properties.getProperty("write-api-url", "http://localhost:8080/api/ga4gh/v1");
        ApiClient client = new ApiClient();
        client.setBasePath(url);
        return new GAGHoptionalwriteApi(client);
    }
}

package io.dockstore.client.cli;

import java.util.Properties;

import io.swagger.client.write.ApiClient;
import io.swagger.client.write.api.GAGHoptionalwriteApi;

import static io.dockstore.client.cli.ConfigFileHelper.getIniConfiguration;

/**
 * @author gluu
 * @since 23/03/17
 */
public final class WriteAPIServiceHelper {
    private static final Properties PROPERTIES = getIniConfiguration();
    private static final String URL = PROPERTIES.getProperty("write-api-url", "http://localhost:8080/api/ga4gh/v1");

    private WriteAPIServiceHelper() {
    }

    public static GAGHoptionalwriteApi getGaghOptionalApi() {
        ApiClient client = new ApiClient();
        client.setBasePath(URL);
        return new GAGHoptionalwriteApi(client);
    }
}

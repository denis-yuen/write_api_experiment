package io.dockstore.client.cli;

import io.swagger.client.ApiClient;
import io.swagger.client.api.GAGHoptionalwriteApi;

/**
 * @author gluu
 * @since 23/03/17
 */
public final class WriteAPIServiceHelper {
    public static final String LOCALPORT = "8080";
    public static final String HOST = "http://localhost:";
    public static final String URI = "/api/ga4gh/v1";

    private WriteAPIServiceHelper() {
    }

    public static GAGHoptionalwriteApi getGaghOptionalApi() {
        ApiClient client = new ApiClient();
        client.setBasePath(HOST + LOCALPORT + URI);
        return new GAGHoptionalwriteApi(client);
    }
}

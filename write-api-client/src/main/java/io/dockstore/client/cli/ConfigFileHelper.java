package io.dockstore.client.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.dockstore.client.cli.ExceptionHelper.IO_ERROR;
import static io.dockstore.client.cli.ExceptionHelper.errorMessage;

/**
 * @author gluu
 * @since 30/03/17
 */
final class ConfigFileHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    private ConfigFileHelper() {
    }

    /**
     * Gets the properties from the default location
     *
     * @return The configuration properties
     */
    static Properties getIniConfiguration(String configFilePath) {
        Properties prop = new Properties();
        try (InputStream inputStream = new FileInputStream(configFilePath)) {
            prop.load(inputStream);
        } catch (IOException e) {
            errorMessage(LOGGER, e.getMessage(), IO_ERROR);
        }
        return prop;
    }
}

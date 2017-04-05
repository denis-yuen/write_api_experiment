package io.dockstore.client.cli;

import java.io.File;
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
public final class ConfigFileHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    private ConfigFileHelper() {
    }

    static Properties getIniConfiguration() {
        Properties prop = new Properties();
        String userHome = System.getProperty("user.home");
        String configFilePath = userHome + File.separator + ".dockstore" + File.separator + "write.api.config.properties";
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(configFilePath);
            prop.load(inputStream);
        } catch (IOException e) {
            errorMessage(e.getMessage(), IO_ERROR);
        }
        return prop;
    }
}

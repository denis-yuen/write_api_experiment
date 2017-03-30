package io.dockstore.client.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String configFilePath = userHome + File.separator + ".dockstore" + File.separator + "config.properties";
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configFilePath);
            prop.load(inputStream);
        } catch (FileNotFoundException e) {
            LOGGER.info(e.getMessage());
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
        return prop;
    }
}

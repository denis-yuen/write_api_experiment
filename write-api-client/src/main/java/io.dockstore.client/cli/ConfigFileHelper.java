package io.dockstore.client.cli;

import java.io.File;

import io.dockstore.common.Utilities;
import org.apache.commons.configuration2.INIConfiguration;

/**
 * @author gluu
 * @since 30/03/17
 */
public final class ConfigFileHelper {
    static INIConfiguration getIniConfiguration() {
        String userHome = System.getProperty("user.home");
        String configFilePath = userHome + File.separator + ".dockstore" + File.separator + "config";
        return Utilities.parseConfig(configFilePath);
    }
}

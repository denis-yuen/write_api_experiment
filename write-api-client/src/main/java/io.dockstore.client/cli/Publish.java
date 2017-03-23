package io.dockstore.client.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gluu
 * @since 23/03/17
 */
public final class Publish {
    private static final Logger LOGGER = LoggerFactory.getLogger(Publish.class);

    private Publish() {
    }

    static void handlePublish(String tool) {
        LOGGER.info("Handling publish");
    }
}

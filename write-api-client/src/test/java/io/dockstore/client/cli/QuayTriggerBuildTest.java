package io.dockstore.client.cli;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import io.ga4gh.reference.api.QuayIoBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @author gluu
 * @since 31/03/17
 */

/**
 * This class tests quay.io because its API sometimes changes.
 */
public class QuayTriggerBuildTest {
    // Username is travisquayoicr
    // Email address is gary.luu+quay.io@oicr.on.ca
    // Password...ask Gary Luu

    private final static String NAMESPACE = "dockstore-testing";
    private final static String NAME = "travis-test";
    private final static String RELEASE = "3.0";
    private static final Logger LOGGER = LoggerFactory.getLogger(QuayIoBuilder.class);
    static String quayToken = System.getProperty("quayToken");
    static QuayIoBuilder quayIoBuilder = new QuayIoBuilder(quayToken);

    @Ignore("Test is ignored until there is a valid or mocked quay.io token")
    @Test
    public void withFilename() {
        quayIoBuilder.triggerBuild(NAMESPACE, NAMESPACE, NAME, NAME, RELEASE, true);
        String phase = "waiting";
        int max = 100;
        int start = 0;
        while (!phase.equals("complete") && !phase.equals("error") && start < max) {
            try {
                TimeUnit.SECONDS.sleep(start);
            } catch (InterruptedException e) {
                LOGGER.error("Could not sleep");
            }
            Optional<String> s = quayIoBuilder.buildResults(NAMESPACE + "/" + NAME);
            Gson gson = new Gson();
            MyJAXBean myJAXBean = gson.fromJson(s.get(), MyJAXBean.class);
            phase = myJAXBean.getBuilds().get(0).getPhase();
            LOGGER.info(phase);
            start = start + 5;
        }
        assertTrue(phase.equals("complete"));
    }

    @Ignore("Test is ignored until there is a valid or mocked quay.io token")
    @Test
    public void withoutFilename() {
        quayIoBuilder.triggerBuild(NAMESPACE, NAMESPACE, NAME, NAME, RELEASE, false);
        String phase = "waiting";
        int max = 100;
        int start = 0;
        while (!phase.equals("complete") && !phase.equals("error") && start < max) {
            try {
                TimeUnit.SECONDS.sleep(start);
            } catch (InterruptedException e) {
                LOGGER.error("Could not sleep");
            }
            Optional<String> s = quayIoBuilder.buildResults(NAMESPACE + "/" + NAME);
            Gson gson = new Gson();
            MyJAXBean myJAXBean = gson.fromJson(s.get(), MyJAXBean.class);
            phase = myJAXBean.getBuilds().get(0).getPhase();
            LOGGER.info(phase);
            start = start + 5;
        }
        assertTrue(phase.equals("error"));
    }

    private class MyJAXBean {
        List<Build> builds;

        public List<Build> getBuilds() {
            return builds;
        }

        private class Build {
            String phase;

            public String getPhase() {
                return phase;
            }
        }
    }
}

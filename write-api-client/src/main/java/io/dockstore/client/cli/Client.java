/*
 *    Copyright 2016 OICR
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.dockstore.client.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gluu
 * @since 22/03/17
 */

public final class Client {
    public static final int PADDING = 3;
    public static final int GENERIC_ERROR = 1; // General error, not yet described by an error type
    public static final int CONNECTION_ERROR = 150; // Connection exception
    public static final int IO_ERROR = 3; // IO throws an exception
    public static final int API_ERROR = 6; // API throws an exception
    public static final int CLIENT_ERROR = 4; // Client does something wrong (ex. input validation)
    public static final int COMMAND_ERROR = 10; // Command is not successful, but not due to errors
    public static final int ENTRY_NOT_FOUND = 12; // Entry could not be found locally or remotely
    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    private Client() {
    }

    /*
     * Main Method
     * --------------------------------------------------------------------------------------------------------------------------
     * --------------
     */

    /**
     * Used for integration testing
     *
     * @param argv arguments provided match usage in the dockstore script (i.e. tool launch ...)
     */
    public static void main(String[] argv) {
        CommandMain commandMain = new CommandMain();

        CommandAdd commandAdd = new CommandAdd();
        CommandPublish commandPublish = new CommandPublish();

        JCommander jc = new JCommander(commandMain);

        jc.addCommand("add", commandAdd);
        jc.addCommand("publish", commandPublish);

        jc.setProgramName("client");
        try {
            jc.parse(argv);
        } catch (MissingCommandException e) {
            LOGGER.warn(e.getMessage());
            jc.usage();
        }
        if (commandMain.help) {
            jc.usage();
        } else {
            String command = jc.getParsedCommand();
            if (command == null) {
                LOGGER.warn("Expecting 'publish' or 'add' command");
                jc.usage();
                return;
            }
            switch (command) {
            case "add":
                if (commandAdd.help) {
                    jc.usage("add");
                } else {
                    Add add = new Add();
                    add.handleAdd(commandAdd.dockerfile, commandAdd.descriptor, commandAdd.secondaryDescriptor, commandAdd.version);
                }
                break;
            case "publish":
                if (commandPublish.help) {
                    jc.usage("publish");
                } else {
                    Publish publish = new Publish();
                    publish.handlePublish(commandPublish.tool);
                }
                break;
            default:
                // JCommander should've caught this, this should never execute
                LOGGER.warn("Unknown command");
                jc.usage();
            }
        }
    }

    @Parameters(separators = "=", commandDescription = "Add the Dockerfile and CWL file(s) using the write API.")
    private static class CommandAdd {
        @Parameter(names = "--Dockerfile", description = "The Dockerfile to upload", required = true)
        private String dockerfile;
        @Parameter(names = "--cwl-file", description = "The cwl descriptor to upload", required = true)
        private String descriptor;
        @Parameter(names = "--cwl-secondary-file", description = "The optional secondary cwl descriptor to upload")
        private String secondaryDescriptor;
        @Parameter(names = "--version", description = "The version of the tool to upload to")
        private String version;
        @Parameter(names = "--help", description = "Prints help for the add command", help = true)
        private boolean help = false;
    }

    @Parameters(separators = "=", commandDescription = "Publish tool to dockstore using the output of the 'add' command.")
    private static class CommandPublish {
        @Parameter(names = "--tool", description = "The json output from the 'add' command.", required = true)
        private String tool;
        @Parameter(names = "--help", description = "Prints help for the publish command.", help = true)
        private boolean help = false;
    }

    @Parameters(separators = "=", commandDescription = "Publish or add tools")
    private static class CommandMain {
        @Parameter(names = "--help", description = "Prints help for the client.", help = true)
        private boolean help = false;
    }

}

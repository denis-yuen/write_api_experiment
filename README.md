![Build Status](https://travis-ci.org/dockstore/write_api_service.svg?branch=develop)

# Write API Service and Client

This is an experimental service aimed at two tasks
1. Providing a concrete reference implementation of a proposed GA4GH [Write API](https://github.com/ga4gh/tool-registry-schemas/blob/feature/write_api_presentation/src/main/resources/swagger/ga4gh-tool-discovery.yaml).
2. Providing a utility for developers to convert plain CWL/WDL files and Dockerfiles into [GitHub](https://github.com) repos storing those plain CWL/WDL files and [Quay.io](https://quay.io) repos storing Docker images built from those Dockerfiles. This can be used by those converting tools described in other formats into "Dockstore-friendly" tools that can be quickly registered and published in Dockstore by using the Write API Client's publish command or programmatically via the Dockstore API.

## End Users
This is intended to be used by:
- Tool Migrators

  Developers that have access to a large number of tools in some different format and wants to migrate them all programmatically to Dockstore with minimal effort.
- Tool Developers

  Developers of a single tool that wants a quick and simple way of creating one without spending a large amount of time to post a single Dockerfile and CWL to implement a single tool.

## Write API Components

This contains two parts:
- The Write API web service that handles creation of GitHub and Quay.io repositories
- The Write API client that interacts with the Write API web service to create GitHub and Quay.io repositories and can also handle publishing of tools to Dockstore.


## Web Service Prerequisites
- [GitHub token](https://github.com)

  Learn how to create tokens on GitHub [here](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/).  You will need the scope "repo".

- A GitHub organization

  Your GitHub token must have access to an existing GitHub organization.  The Write API web service currently does not create GitHub organizations.  The name of this organization must match the Quay.io organization.
- [Quay.io token](https://quay.io)

  Learn how to create a token on Quay.io for your organization [here](https://docs.quay.io/api/) under the heading "Generating a Token (for internal application use)". You will need to provide these permissions:

  - Create Repositories
  - View all visible repositories
  - Read/Write to any accessible repository
  - Administer User

- A Quay.io organization

  Your Quay.io token msut have access to an existing Quay.io organization.  The Write API web service currently does not create Quay.io organiztaions.  The name of this organization must match the GitHub organization.

## Web Service Usage

The web service alone only requires a GitHub and Quay.io token.  There are two ways to specify your tokens.
1.  Environmental variables.  
  You can set them in Bash like the following:
```
export quayioToken=<your token here>
export githubToken=<your token here>
```
2.  The YAML configuration file.  The tokens can be entered in the top two lines.  An example of a YAML configuration file can be seen [here](https://github.com/dockstore/write_api_service/blob/develop/write-api-service/src/main/resources/example.yml).

To run the service, build it and then run it using a configuration file:
```
cd write_api/write-api-service
mvn clean install -DskipTests
java -jar target/write-api-service-*.jar server example.yml
```
The example.yml shown previously uses port 8082 by default, this can be changed.  Note this port number, it will later be used for the Write API Client properties file.

After running the webservice, you can check out the web service endpoints through swagger.  By default, it is available at http://localhost:8082/static/swagger-ui/index.html.  Then http://localhost:8082/api/ga4gh/v1/swagger.json will need to be entered in the first text field (top left).

The basic workflow is that GitHub repos are created when posting a new tool. When files are posted or put to a version of a tool, we will create or delete and re-create a GitHub release/branch/tag with a matching name. When Dockerfiles are added, the tool will be created and built as a Quay.io repo. After adding both Dockerfiles and descriptors, you basically have a tool that is ready to be quickly registered and published under a Dockstore 1.2 web service. Go to Dockstore, do a refresh, and then hit quick register on the repos that you wish to publish. You can also do this programmatically through the write api client.

## Limitations

This service is aimed at developers familiar with Dockstore (and have at least gone through Dockstore tutorials).

It also has the following limitations

1. The service lacks a GUI and is purely a tool provided for developers doing conversion
2. A full implementation awaits testing
3. It is not possible to create build triggers in Quay.io programmatically at this time. So new refresh code in Dockstore 1.2 is required.

## Client Prerequisites
- Write API web service and all its prerequisites

  By now, then web service should be up and running with valid GitHub and Quay.io tokens.  If not, please return to the web service usage section to get that running first.  It is advised to ensure that the Write API web service is functioning correctly before using the client.
- [Dockstore token](https://dockstore.org/docs/getting-started-with-dockstore)

  Follow the "Getting Started with Dockstore" tutorial to get a Dockstore token.  Note this down, it will later be used in the Write API client properties file.
- Dockstore server-url

  The Dockstore tutorial earlier would've specified the server-url alongside the token.  Unless you're running your own dockstore webservice, the Dockstore production server-url is "https://www.dockstore.org:8443" and the Dockstore staging server-url is "https://staging.dockstore.org:8443".  Note this down, it will also later be used in the Write API client properties file.
- Quay.io integration

  In order to publish to Dockstore, Quay.io must be linked to Dockstore.

- Write API web service URL

  You will need to know the URL of the Write API web service you ran previously.  If you've been using the example.yml, it should be "http://localhost:8082/api/ga4gh/v1"

## Client Usage
To use the write api client, the properties file must exist and contain the necessary information.
Here is a sample properties file:
```
token=imamafakedockstoretoken
server-url=https://www.dockstore.org:8443
write-api-url=http://localhost:8082/api/ga4gh/v1
```
These three properties refers to the 2nd, 3rd, and 4th prerequisites mentioned in the previous section.
By default, the client will look for the properties file at the following location:
```
~/.dockstore/write.api.config.properties
```
otherwise, you can specify your own with the --config option.

Here is the general usage information for the client:
```
$ java -jar write-api-client-*-shaded.jar --help
Usage: client [options] [command] [command options]
  Options:
    --config
      Config file location.
      Default: ~/.dockstore/write.api.config.properties
    --help
      Prints help for the client.
      Default: false
  Commands:
    add      Add the Dockerfile and CWL file(s) using the write API.
      Usage: add [options]
        Options:
        * --Dockerfile
            The Dockerfile to upload
        * --cwl-file
            The cwl descriptor to upload
          --cwl-secondary-file
            The optional secondary cwl descriptor to upload
          --help
            Prints help for the add command
            Default: false
        * --id
            The organization and repo name (e.g. ga4gh/dockstore).
          --version
            The version of the tool to upload to
            Default: 1.0

    publish      Publish tool to dockstore using the output of the 'add'
            command.
      Usage: publish [options]
        Options:
          --help
            Prints help for the publish command.
            Default: false
        * --tool
            The json output from the 'add' command.
```
There are two main commands that will be used: the Add command and then the Publish Command
### Add command

The Add command has 3 required parameters:
- --cwl-file (the absolute path to the cwl descriptor that you want to upload to GitHub)
- --dockerfile (the Dockerfile that you want to upload to GitHub and build on Quay.io)
- --id (the GitHub organization and repository to upload the Dockerfile and CWL descriptor to which is also the same name as the Quay.io repository)

This command interacts with the write API web service to perform several operations:
1.  Create GitHub and Quay.io repository if it doesn't exist based on the --id
2.  Create/recreate a new GitHub branch/tag/release (1.0 if version is not specified)
3.  Upload the Dockerfile to the that version on GitHub and build the image on Quay.io
4.  Upload the CWL descriptor files to that version on GitHub
5.  Upload secondary CWL descriptor to that version on GitHub if it was specified
6.  Output JSON object to stdout that contains the GitHub repo, Quay.io repo, and version number

Sample Add Command Output:
```
$ java -jar write-api-client-*-shaded.jar add --Dockerfile Dockerfile --cwl-file Dockstore.cwl --id dockstore-testing/travis-test
15:51:55.511 [main] INFO io.dockstore.client.cli.Add - Handling add...
15:51:56.509 [main] INFO io.dockstore.client.cli.Add - Created repository on git.
15:51:59.108 [main] INFO io.dockstore.client.cli.Add - Created branch, tag, and release on git.
15:52:04.799 [main] INFO io.dockstore.client.cli.Add - Created dockerfile on git.
15:52:06.037 [main] INFO io.dockstore.client.cli.Add - Created descriptor on git.
15:52:06.061 [main] INFO io.dockstore.client.cli.Add - Successfully added tool.
{
  "githubURL": "https://github.com/dockstore-testing/travis-test",
  "quayioURL": "https://quay.io/repository/dockstore-testing/travis-test",
  "version": "1.0"
}
```

You can pipe this command to an output file like "> test.json" and you can then use this output file for the publish command.

#### Result:

After running the Add command, you should now have a GitHub repository in your organization that contains a new branch/tag/release containing a Dockerfile and CWL descriptor.  In addition, there would be a Quay.io repository currently building the Dockerfile with version tag.  You also have a JSON Object in stdout containing information needed for the Publish command.

### Publish command

The Publish Command has one required parameter:
- --tool (the absolute path to the file containing the output from the add command) which contains something like this:
```
{
  "githubURL": "https://github.com/dockstore-testing/travis-test",
  "quayioURL": "https://quay.io/repository/dockstore-testing/travis-test",
  "version": "1.0"
}
```

This command interacts with the Dockstore web service to perform several operations:
1. Refresh all of the user's tools (based on the token present in the properties file) which will register it on Dockstore
2. Add Quay.io tags and its associated GitHub reference to that tool on Dockstore
3. If that tool is valid, it will attempt to publish that tool on Dockstore for others to see

Sample Publish Command Output:
```
$ java -jar write-api-client-*-shaded.jar publish --tool test.json
INFO  [2017-05-04 20:29:40,088] io.dockstore.client.cli.Publish: Handling publish
INFO  [2017-05-04 20:29:58,637] io.dockstore.client.cli.Publish: Successfully published tool.
```

#### Result:

After successfully running the Publish command, the tool should be marked as valid and available on Dockstore for everyone to use.

## Tests

If you wish to run the tests, you must ensure all configuration stated in both the Write API client and web service is completed.  Additionally there are a few files that must also be modified.   
1.  write_api/write-api-client/src/test/java/io/dockstore/client/cli/ClientTest.java contains a line with:
  ```
  private static final String id = "dockstore-testing/travis-test";
  ```
  In this example, "dockstore-testing" is the GitHub organization and Quay.io namespace, "travis-test" is the Quay.io and GitHub repository.
  Modify this line so that:
  - Your GitHub and Quay.io tokens have access to the organization/namespace.
  - The repository is something you want created.
2.  If you're not exporting Quay.io and GitHub tokens, you'll have to modify write_api/write-api-client/src/test/resources/ref.yml and write_api/write-api-service/src/test/resources/ref.yml by replacing its dummy tokens with proper GitHub and Quay.io tokens.

You can now run the tests with:
```
mvn clean install
```

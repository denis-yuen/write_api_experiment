[![Build Status](https://travis-ci.org/dockstore/write_api_service.svg?branch=develop)](https://travis-ci.org/dockstore/write_api_service)

# Write API Service and Client

This is a service aimed at two tasks
1. Providing a concrete reference implementation of a proposed GA4GH [Write API](https://github.com/ga4gh/tool-registry-schemas/blob/feature/write_api_presentation/src/main/resources/swagger/ga4gh-tool-discovery.yaml).
2. Providing a utility for developers to convert plain CWL/WDL files and Dockerfiles into [GitHub](https://github.com) repos storing those plain CWL/WDL files and [Quay.io](https://quay.io) repos storing Docker images built from those Dockerfiles. This can be used by those converting tools described in other formats into "Dockstore-friendly" tools that can be quickly registered and published in Dockstore by using the Write API Client's publish command or programmatically via the Dockstore API.  It is an alternative to using a GUI to register tools on Dockstore.

## End Users
This is intended to be used by:
- Tool Migrators

  Developers that have access to a large number of tools in some different format and wants to migrate them all programmatically to Dockstore with minimal effort.
- Tool Developers

  Developers of tools that wants a quick and simple way of creating one without spending a large amount of time to post a single Dockerfile and CWL descriptor to implement each tool.


## Write API Components

This contains two parts:
- The Write API web service that handles creation of GitHub and Quay.io repositories
- The Write API client that interacts with the Write API web service to create GitHub and Quay.io repositories and can also handle publishing of tools to Dockstore.

## Building the Write API jars

The README will assume you're building the jars.  You can build the jars from source using:

```
git clone https://github.com/dockstore/write_api_service.git
cd write_api_service
mvn clean install -DskipTests
```

The built jars will be available as:
- `write-api-service/target/write-api-service*.jar`
- `write-api-client/target/write-api-client*shaded.jar`

Note that the client one is a shaded jar.

## Downloading the Write API jars

Additionally, you can download the Write API jars using the following:
```
wget https://artifacts.oicr.on.ca/artifactory/collab-release/io/dockstore/write-api-client/1.0.2/write-api-client-1.0.2-shaded.jar
wget https://artifacts.oicr.on.ca/artifactory/collab-release/io/dockstore/write-api-service/1.0.2/write-api-service-1.0.2.jar
```

Note that the client one is a shaded jar.

## Web Service Prerequisites
- [GitHub token](https://github.com)

  Learn how to create tokens on GitHub [here](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/).  You will need the scope "repo".

- GitHub organization(s)

  Your GitHub token must have access to at least a single existing GitHub organization.  The organization can be changed as long as the the GitHub token still has access to it.  The Write API web service currently does not create GitHub organizations.  The name of this organization must match the Quay.io organization.  This organization will contain the repository that will be created.

- [Quay.io organization and Quay.io token](https://quay.io)

  You will need an existing Quay.io organization.  The Write API currently does not create Quay.io organizations.  The name of this organization must match the GitHub organization.  Once you have an organization, a token must be created for it.  

  This organization will contain the repository that will be created.  Changing the Quay.io organization requires the token to be recreated/changed too.

  Learn how to create a token on Quay.io for your organizations [here](https://docs.quay.io/api/) under the heading "Generating a Token (for internal application use)". You will need to provide these permissions:

  - Create Repositories
  - View all visible repositories
  - Read/Write to any accessible repository



## Web Service Usage

The web service alone only requires a GitHub and Quay.io token.  There are two ways to specify your tokens.
1.  Environmental variables.  
  You can set them in Bash like the following:
```
export quayioToken=<your token here>
export githubToken=<your token here>
```
2.  The YAML configuration file.  The tokens can be entered in the top two lines.  An example of a YAML configuration file can be seen [here](https://github.com/dockstore/write_api_service/blob/develop/write-api-service/src/main/resources/example.yml).

Run the service using a configuration file:
```
java -jar write-api-service-*.jar server example.yml
```
The example.yml shown previously uses port 8082 by default, this can be changed.  Note this port number, it will later be used for the Write API Client properties file.

After running the webservice, you can check out the web service endpoints through swagger.  By default, it is available at http://localhost:8082/static/swagger-ui/index.html.

The basic workflow is that GitHub repos are created when posting a new tool. When files are posted or put to a version of a tool, we will create or delete and re-create a GitHub release/branch/tag with a matching name. When Dockerfiles are added, the tool will be created and built as a Quay.io repo. After adding both Dockerfiles and descriptors, you basically have a tool that is ready to be quickly registered and published under a Dockstore 1.2 web service. Go to Dockstore, do a refresh, and then hit quick register on the repos that you wish to publish. You can also do this programmatically through the write api client.

## Limitations

This service is aimed at developers familiar with Dockstore (and have at least gone through Dockstore tutorials).

It also has the following limitations

1. The service lacks a GUI and is purely a tool provided for developers doing conversion
2. It is not possible to create build triggers in Quay.io programmatically at this time. So new refresh code in Dockstore 1.2 was added to detect metadata added to quay.io repos.
3. The service and client only handles local files.  It currently does not handle file provisioning.
4. The Dockerfile uploaded will be renamed to "Dockerfile" on GitHub.  This is due to a Quay.io limitation, it will only build the "Dockerfile" in a GitHub archive.  
5. The CWL descriptor uploaded will be renamed to "Dockstore.cwl" on GitHub.  This is due to the Write-API-Client Publish command and Dockstore limitation.  The input json for the Publish command does not contain any information regarding the descriptor name.  The Dockstore automatic refresh code will only try to import "Dockstore.cwl".

## Client Prerequisites
- Write API web service and all its prerequisites

  By now, then web service should be up and running with valid GitHub and Quay.io tokens.  If not, please return to the web service usage section to get that running first.  It is advised to ensure that the Write API web service is functioning correctly before using the client.
- [Dockstore token](https://dockstore.org/docs/getting-started-with-dockstore)

  Follow the "Getting Started with Dockstore" tutorial to get a Dockstore token.  Note this down, it will later be used in the Write API client properties file.
- Dockstore server-url

  The Dockstore tutorial earlier would've specified the server-url alongside the token.  Unless you're running your own dockstore webservice, the Dockstore production server-url is "https://www.dockstore.org:8443" and the Dockstore staging server-url is "https://staging.dockstore.org:8443".  Note this down, it will also later be used in the Write API client properties file.
- Quay.io integration

  In order to publish to Dockstore, Quay.io must be linked to Dockstore.  See [Dockstore](https://dockstore.org/docs/getting-started-with-dockstore) on how to link your Quay.io account to Dockstore.

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
These three properties refers to the prerequisites mentioned in the previous section.
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

    check      Checks if the tool is properly registered and Docker image is
            available.
      Usage: check [options]
        Options:
          --help
            Prints help for the check command.
            Default: false
        * --id
            The organization and repo name (e.g. ga4gh/dockstore).
          --version
            The version of the tool to upload to
            Default: 1.0

```
There are two main commands that will be used: the Add command and then the Publish Command
### Add command

The Add command has 3 required parameters:
- --cwl-file (the local path to the cwl descriptor that you want to upload to GitHub)
- --Dockerfile (the Dockerfile that you want to upload to GitHub and build on Quay.io)
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
{
  "githubURL": "https://github.com/dockstore-testing/travis-test",
  "quayioURL": "https://quay.io/repository/dockstore-testing/travis-test",
  "version": "1.0"
}
```

You can pipe this command to an output file like "> test.json" and you can then use this output file for the publish command.

#### Result:

After running the Add command, you should now have a GitHub repository in your organization that contains a new branch/tag/release containing a Dockerfile and CWL descriptor.  In addition, there would be a Quay.io repository currently building the Dockerfile with version tag.  

The Quay.io repositories created will contain metadata like:

```
GA4GH auto-managed repo


This is an example repository.

----------  
[GA4GH-generated-do-not-edit]: <> ({"repo":"review_repo","namespace":"dockstore-testing"})
```


You also have a JSON Object in stdout containing information needed for the Publish command.

### Publish command

The Publish Command has one required parameter:
- --tool (the local path to the file containing the output from the add command) which contains something like this:
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
Refreshing user...
Refreshed user
Refreshing tool...
Refreshed tool
Successfully published tool.
```

#### Result:

After successfully running the Publish command, the tool should be marked as valid and available on Dockstore for everyone to use.

### Check command

The check command checks if the tool is properly registered on Dockstore and Docker image is available on Quay.io.

It has one required parameter:
- --id (The organization and repo name (e.g. ga4gh/dockstore))

Sample Check Command Output:
```
$ java -jar write-api-client-*-shaded.jar check --id dockstore-testing/travis-test
Tool properly registered and version is valid
Docker image available
```

#### Result:

After successfully running the Check command, you will definitively know that the tool was registered on Dockstore and that the image is available to be pulled from Quay.io

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
Afterwards, you should have a GitHub and Quay.io repository at the organization/namespace specified previously.  The GitHub repo will have a Dockerfile, and CWL descriptor, and secondary CWL descriptor and a built/building image on the new Quay.io repository.  

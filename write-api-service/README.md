# Write API Service

This is an experimental service aimed at two tasks
1) Providing a concrete reference implementation of a proposed GA4GH [Write API](https://github.com/ga4gh/tool-registry-schemas/blob/feature/write_api_presentation/src/main/resources/swagger/ga4gh-tool-discovery.yaml)
2) Providing a utility for developers to convert plain CWL/WDL files and Dockerfiles into [GitHub](https://github.com) repos storing those plain CWL/WDL files and [quay.io](https://quay.io) repos storing Docker images built from those Dockerfiles. This can be used by those converting tools described in other formats into "Dockstore-friendly" tools that can be quickly registered in Dockstore by logging in, doing a refresh, and doing [quick registration via the API](https://dockstore.org/docs/getting-started-with-dockstore#register-your-tool-in-dockstore) or programmatically via the Dockstore API.


## Usage

Currently, spin up the web service after providing github and quay.io tokens via a [configuration file](https://github.com/dockstore/write_api_service/blob/develop/write-api-service/src/main/resources/example.yml) or environment variables. For environment variables you can do the following in Bash
```
export quayioToken=<your token here>
export githubToken=<your token here>
```

Learn how to create tokens on GitHub [here](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/). You will need the scope "repo".

Learn how to create a token on Quay.io for your organization [here](https://docs.quay.io/api/) under the heading "Generating a Token (for internal application use)".  You will need to provide these permissions:
- Create Repositories
- View all visible repositories
- Read/Write to any accessible repository

To run the service, build it and then run it using a configuration file:
```
mvn clean install -DskipTests
java -jar target/tool-registry-reference-*.jar server example.yml
```

You can also run tests to create GitHub and Quay.io repos while also scheduling a build. You can do this by
```
mvn clean install
```
but this will require the proper environment variables above and you will also need to customize the desired github and quay.io repos.


The basic workflow is that GitHub repos are created when posting a new tool. When files are posted or put to a version of a tool, we will create or delete and re-create a GitHub release with a matching name. When Dockerfiles are added, the tool will be created and built as a quay.io repo. After adding both dockerfiles and descriptors, you basically have a tool that is ready to be quick registered under a Dockstore 1.2 web service. Go to Dockstore, do a refresh, and then hit quick register on the repos that you wish to publish. You can also do this programmatically.

## Limitations

This service is aimed at developers familiar with Dockstore (and have at least gone through Dockstore tutorials).

It also has the following limitations

1. The service lacks a GUI and is purely a tool provided for developers doing conversion
2. A full implementation awaits testing
3. It is not possible to create build triggers in Quay.io programmatically at this time. So new refresh code in Dockstore 1.2 is required.

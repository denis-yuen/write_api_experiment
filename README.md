![Build Status](https://travis-ci.org/dockstore/write_api_service.svg?branch=develop)

The Write-API allows developer to automatically create GitHub and Quay repositories for any tool encapsulated in Docker and described with a CWL descriptor on behalf of the user.
It implements the proposed GA4GH [Write API](https://github.com/ga4gh/tool-registry-schemas/blob/feature/write_api_presentation/src/main/resources/swagger/ga4gh-tool-discovery.yaml)

This contains two parts:
- The Write API web service that handles creation of GitHub and Quay repositories
- The client that interacts with the web service and handles publishing of tools to Dockstore.

To build the service and the client use:
```
mvn clean install -DskipTests
```
Running tests will require configuration/properties files for both client and service.  Do not try to run "mvn clean install" without setting up those files first.  Additionally, both client and service tests will require Quay.io and GitHub tokens.
See their respective READMEs in the [write-api-client](write-api-client) and [write-api-service](write-api-service) directories for more details.


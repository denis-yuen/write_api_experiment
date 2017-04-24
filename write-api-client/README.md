# Write API Client

The Write API Service must be up and running before using the client.  This client uses the service to create GitHub and Quay repositories through a CLI.  It also handles the publishing of tools to Dockstore.
It requires a properties file at $HOME/.dockstore/write.api.config.properties that contains:
- token (your Dockstore token)
- server-url (the url of Dockstore)
- organization (your GitHub/Quay organization that you have access to)
- repo (the repository to create)
- write-api-url (the url of the write-api service)

Below is a sample properties file:
```
token=abcdefghijklmnopqrstuvwxyz
server-url=https://www.dockstore.org:8443
organization=test-organization
repo=test-repository
write-api-url=http://localhost:8082/api/ga4gh/v1
```


The following is the CLI's usage:
```
Usage: client [options] [command] [command options]
  Options:
    --config
      Config file location.
      Default: $HOME/.dockstore/write.api.config.properties
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
          --version
            The version of the tool to upload to

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

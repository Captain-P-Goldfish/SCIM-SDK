# Create POJO from SCIM schema file

This module is a spring-shell application that can be used to create the SCIM POJO objects from its schemas.

## How to use

Start the artifact with JDK 8 or higher. You can find the artifact within the release section:
https://github.com/Captain-P-Goldfish/SCIM-SDK/releases

*IMPORTANT*:

start the jar directly from a console like powershell or a linux shell
```
java -jar scim-sdk-schema-pojo-creator-${version}.jar
```

afterwards a shell will be opened that can be used to generate the required POJOs. A help command is available 
to display the possible commands 
                                
```
shell:>help
AVAILABLE COMMANDS

Built-In Commands
       help: Display help about available commands
       stacktrace: Display the full stacktrace of the last error.
       clear: Clear the shell screen.
       quit, exit: Exit the shell.
       history: Display or save the history of previously run commands
       version: Show version info
       script: Read and execute commands from a file.

Shell Controller
       translate: Translate SCIM schemas to Java POJOs for SCIM SDK
```

The important part here is the `translate command` you can print the available options by using the help-command

```
shell:>help translate
NAME
       translate - Translate SCIM schemas to Java POJOs for SCIM SDK

SYNOPSIS
       translate [--location String] --recursive boolean --output String --package String --useLombok boolean --override boolean --create-config boolean

OPTIONS
       --location or -l String
       a directory containing resource-schemas and resource-types or a direct file location of a resource-schema
       [Mandatory]

       --recursive or -r boolean
       if the given directory should be searched recursively
       [Optional, default = false]

       --output or -o String
       the output directory where the java POJOs will be placed. This directory should point to your 'src/main/java' directory
       [Optional, default = .]

       --package or -p String
       The name of the package for the generated POJOs
       [Optional, default = my.scim.sdk.app]

       --useLombok boolean
       Add lombok @Builder annotations to constructors
       [Optional, default = false]

       --override boolean
       Replace already existing files
       [Optional, default = false]

       --create-config boolean
       Creates a predefined SCIM configuration file with a ResourceEndpoint
       [Optional, default = false]
```
            
The `translate` command will process the following steps:

1. gather `resource-schema` files and `resource-type` files within the given location under option `-l` or `--location`
2. search for files recursively within the location under `-l` if the `recursive` option is enabled
3. validate the found `resource-schemas` and `resource-types` and analyze their relations with each other.
4. take the `output`-directory and add the `package`-path to it and this will be the actual output directory
5. create the resource files
6. write the files into their target directories

                            
if you e.g. execute the following command:

```
translate -r -l /opt/development/my-project/src/main/resources/scim -o /opt/development/my-project/src/main/java -p de.captaingoldfish.scim.example --useLombok --create-config
```

and the `location`-directory contains the following input:

```
├── resourcetypes
│   ├── group.json
│   ├── me.json
│   ├── service-provider.json
│   └── user.json
└── schemas
    ├── enterprise-user.json
    ├── groups.json
    ├── service-provider.json
    └── users.json
```


the generated output will look like this:
(In case that the `create-config` option is enabled a `ScimConfig` will be created that prepares a `ResourceEndpoint`
instance with all the generated classes already registered. See package `projectconfig`)

```
src
└── main
    └── java
        └── de
            └── captaingoldfish
                └── scim
                    └── example
                        ├── endpoints
                        │   ├── GroupEndpointDefinition.java
                        │   ├── MeEndpointDefinition.java
                        │   ├── ServiceProviderConfigEndpointDefinition.java
                        │   └── UserEndpointDefinition.java
                        ├── projectconfig
                        │   └── ScimConfig.java
                        ├── resourcehandler
                        │   ├── GroupResourceHandler.java
                        │   ├── MeResourceHandler.java
                        │   ├── ServiceProviderConfigResourceHandler.java
                        │   └── UserResourceHandler.java
                        └── resources
                            ├── EnterpriseUser.java
                            ├── Group.java
                            ├── ServiceProviderConfiguration.java
                            └── User.java
```
                       
## Expected resources are not generated

There are some rules that must be met in order for the source-code generation to work:

1. `ResourceHandler` and `EndpointDefinition` implementations can only be created if the corresponding `resource-type`
    json definition is present.
2. The schemas will be validated against their meta-schema-representations so invalid schemas will not work
3. `resource-schema`-filenames and `resource-type`-filenames must end with `.json` otherwise the files will be ignored.
4. `resource-type`-files that contain `resource-schema`-references in their `schema`-attribute that have not been found
    will be ignored and nothing will be generated because the underlying resource-base is missing.


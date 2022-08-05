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
       translate [--location String] --recursive boolean --output String --package String --useLombok boolean

OPTIONS
       --location or -l String
       a directory containing resource schemas and resource types or a direct file location of a resource schema
       [Mandatory]

       --recursive or -r boolean
       if the given directory should be searched recursively
       [Optional, default = false]

       --output or -o String
       the output directory where the java POJOs will be placed
       [Optional, default = .]

       --package or -p String
       The name of the package that will be added to the generated POJOs
       [Optional, default = my.scim.sdk.app]

       --useLombok boolean
       Add lombok @Builder annotations to constructors
       [Optional, default = false]
```
                                        
now execute the `translate` command e.g.:

```
translate -r -l /opt/development/my-project/src/main/resources/scim -o /opt/development/my-project/src/main/java/de/captaingoldfish/myproject/scim -p de.captaingoldfish.myproject.scim --useLombok
```

The given directory will be recursively searched and json-resources of the following types will be gathered:

 1. resources
 2. resource-types

The resource-types define the extensions bound to other resources so make sure that the resource-types are also
available under the given location. The program will then try its best to construct all resource-pojos together with its
extensions if present.

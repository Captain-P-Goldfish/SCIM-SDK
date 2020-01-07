# Springboot SCIM example

this example will show how this framework can be used within a Springboot environment.

### deploying this sample project

simply build the jar-archive with 

`mvn clean package`

start the jar with a console with `java -jar scim-sdk-springboot-example-${project.version}.jar`

* http://localhost:8080/scim/v2/ServiceProviderConfig
* http://localhost:8080/scim/v2/Schemas
* http://localhost:8080/scim/v2/ResourceTypes
* http://localhost:8080/scim/v2/Users
* http://localhost:8080/scim/v2/Groups

All operation types _[list, get, create, update, delete, patch, bulk, filtering, sorting]_ are available so test this
 sample to your hearts will. 

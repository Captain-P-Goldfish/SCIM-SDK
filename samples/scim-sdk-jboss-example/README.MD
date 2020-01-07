# JBoss SCIM example

this example will show how this framework can be used within a JBoss environment. The scim-sdk-server uses a newer
version of the jackson library which is incompatible with the version within the modules directory of the JBoss. So
we need to prevent that the JBoss jackson module is loaded into our application. This is done with the 
**_jboss-deployment-structure.xml_** within WEB-INF directory.

### deploying this sample project

simply build the war-archive with 

`mvn clean package`

add the file with the name `scim-sdk-jboss-example-${project.version}.war` into the `deployments` directory of the
 JBoss. Now call either one of the following URLs

* http://localhost:8080/scim-jboss-example/scim/v2/ServiceProviderConfig
* http://localhost:8080/scim-jboss-example/scim/v2/Schemas
* http://localhost:8080/scim-jboss-example/scim/v2/ResourceTypes
* http://localhost:8080/scim-jboss-example/scim/v2/Users
* http://localhost:8080/scim-jboss-example/scim/v2/Groups

All operation types _[list, get, create, update, delete, patch, bulk, filtering, sorting]_ are available so test this
 sample to your hearts will. 

---
**tested with:**

wildfly 18.0.1

---

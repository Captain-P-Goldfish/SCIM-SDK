# JBoss SCIM example

this example will show how this framework can be used within a JBoss environment. the same endpoint is reachable either
by a servlet implementation or a JAXRS implementation. Both URLs are published under different paths (see below).

### deploying this sample project

simply build the war-archive with

`mvn clean package`

add the file with the name `scim-sdk-jboss-example-${project.version}.war` into the `deployments` directory of the
JBoss. Now call either one of the following URLs

### Servlet example

* http://localhost:8080/scim-jboss-example/servlet/scim/v2/ServiceProviderConfig
* http://localhost:8080/scim-jboss-example/servlet/scim/v2/Schemas
* http://localhost:8080/scim-jboss-example/servlet/scim/v2/ResourceTypes
* http://localhost:8080/scim-jboss-example/servlet/scim/v2/Users
* http://localhost:8080/scim-jboss-example/servlet/scim/v2/Groups

### JAXRS example

* http://localhost:8080/scim-jboss-example/jaxrs/scim/v2/ServiceProviderConfig
* http://localhost:8080/scim-jboss-example/jaxrs/scim/v2/Schemas
* http://localhost:8080/scim-jboss-example/jaxrs/scim/v2/ResourceTypes
* http://localhost:8080/scim-jboss-example/jaxrs/scim/v2/Users
* http://localhost:8080/scim-jboss-example/jaxrs/scim/v2/Groups

All operation types _[list, get, create, update, delete, patch, bulk, filtering, sorting]_ are available so test this
sample to your hearts will.

---
**tested with:**

wildfly 19.1.0 wildfly 22.0.0

---

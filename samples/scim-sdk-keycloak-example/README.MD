# How to use Keycloak example

1. execute `mvn clean install` on the parent project
2. start a new keycloak instance and initialize the `master realm`
3. deploy the ear-file `keycloak-scim-ear.ear` into `$JBOSS_HOME/standalone/deployments`
4. open the class `ScimClient.java` within the module `keycloak-scim-client` and execute the `main-method`. This will
 create almost 5000 users and 5 groups with 10 random members for each of these groups
5. open the web admin console of keycloak within your browser and check for the created users and groups. Any
 created group and any created user will have an additional attribute `scim-user` which is used by the 
 scim-implementation to identify users that have been created with scim. This will prevent that the admin-user is
 deleted by accident.
 
The following resource endpoints are registered:

* http://localhost:8080/auth/realms/master/scim/v2/ServiceProviderConfig
* http://localhost:8080/auth/realms/master/scim/v2/Schemas
* http://localhost:8080/auth/realms/master/scim/v2/ResourceTypes
* http://localhost:8080/auth/realms/master/scim/v2/Users
* http://localhost:8080/auth/realms/master/scim/v2/Groups

please note that filtering is done by the `autoFiltering` feature and not on database level. Filtering on database level
 must be implemented manually.
 
 ### Authentication
 
 authentication is disabled for this example but is easily enabled by uncommenting the following lines in class
  `ScimEndpoint`
```  
 /**
    * the authentication and authorization details of the current user
    */
   // private final AdminAuth authResult;
 
   public ScimEndpoint(KeycloakSession keycloakSession)
   {
     this.keycloakSession = keycloakSession;
     // authResult = Authentication.authenticate(keycloakSession);
   }
```



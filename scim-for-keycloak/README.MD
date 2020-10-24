# What is SCIM-for-keycloak?

It is a full fleshed SCIM implementation that is easily integrated into keycloak and is also easily configurable
within the web admin console. You'll be able to restrict access of endpoints with roles and to specific clients
. Select the option `scim` on entry `Admin Console Theme` and reload the page (Press F5).

## Keycloak support matrix 

|  version | supported          | easily adapted         |
| :------- | :----------------- | :--------------------- |
| lower    | :x:                | :heavy_check_mark:     |
| 8.0.x    | :x:                | :heavy_check_mark:     |
| 9.0.x    | :heavy_check_mark: |                        |
| 10.0.x   | :heavy_check_mark: |                        |
| 11.0.0   | :heavy_check_mark: |                        |

## Deploy into a standalone keycloak server?

Switch into the parent directory "SCIM-SDK" and build the project once with `mvn package -DskipTests`.
Copy the file `SCIM-SDK/scim-for-keycloak/scim-for-keycloak-deployment/target/scim-for-keycloak.ear` into the
directory `${keycloak.home}/standalone/deployments` of a keycloak server.  
**NOTE:** *If the server is running during deployment you need to restart it!*

## build a docker image with scim-for-keycloak deployed

Execute `mvn clean package -P docker -DskipTests`. The build will create a docker image and installs it into your local
docker registry with the name `scim-for-keycloak-${project.version}`.  
This docker-image requires a mariadb database with username "root" and password "123456". This can easily be changed
by manipulating the `Dockerfile` in
`SCIM-SDK/scim-for-keycloak/scim-for-keycloak-deployment/src/main/docker/Dockerfile`. Simply execute the maven profile
again after manipulating the Dockerfile with `mvn clean package -P docker -DskipTests`

**NOTE:** *If the build with `mvn clean package -P docker -DskipTests` does not work please execute it from the
parent-directory `SCIM-SDK`*

After the image has been pushed into your local registry you can start a fast setup with the docker-compose file in
directory `SCIM-SDK/scim-for-keycloak/docker-compose` with the command 
`docker-compose -p scim-for-keycloak up`. Please note that docker might ask you to share the directory because the
 file `wait-for-it.sh` - that ensures successful starting of the containers - is mounted into the keycloak-container.


## How do I configure the SCIM endpoints?

1. Deploy the `scim-for-keycloak.ear` file into your server. If the server is currently running you need to restart it!
2. Now open the web admin console of keycloak open the realm settings and select the `Themes` tab and select the `scim`
 theme for the admin console. Now reload the browser page and the `SCIM` menu link should be visible. 
  ![theme-settings](images/theme-setting.png)

## What is configurable?

1. Select the `SCIM` link and you should see the following view:   
  ![theme-settings](images/service-provider-config.png)
2. You can disable SCIM for each realm separately and the `ServiceProvider` configurations as defined in RFC7643
3. You may restrict access to the SCIM-endpoints for each separate realm by allowing users only access if they have
 been identified by a specific client. If no clients are selected any client is authorized to access the SCIM
 endpoints:  
  ![theme-settings](images/service-provider-auth-config.png)
4. You get an overview for all registered `ResourceTypes` and may configure them separately.  
  ![theme-settings](images/resource-type-overview.png)  
You may have noticed that a 3rd `ResourceType` with name `RealmRole` is present that is not defined by RFC7643. This
`ResourceType` is a custom endpoint that acts as showcase to demonstrate how easy it is to extend the API with new
endpoints. This endpoint provides support for adding new `RealmRoles` and associate them with either `Groups` or
`Users`.
5. Each endpoint provides several settings that can be configured:  
  ![theme-settings](images/resource-type-config.png)  
  You'll be able to change the description of an endpoint, disable a specific `ResourceType` or just prevent that new
  resources are created, deleted, updated or read.
6. You can turn off the requirement for authentication on each `ResourceType` but not for specific 
`ResourceType`-endpoints.
7. You'll be able to set required roles for each `ResourceType` and each `ResourceType`-endpoint [create, get
, list, update, delete]  
  ![theme-settings](images/resource-type-auth-config.png)  
  The role settings are of course only respected if authentication is enabled.  
   
## Disadvantages

1. I didn't find a really good way in storing multi-valued-complex structures in the user-attributes table so I stored
such data structures in pure json in this table. Please note that the values in the screenshot are simply 
auto-generated test-values. 
![theme-settings](images/user-storage.png)
2. User loading and filtering is not implemented on database level meaning that a "list-Users"-request will currently
 retrieve all Users of the used realm from the database. Filtering, sorting and limiting of the returned entries will be
  done on API level by the SCIM-SDK. (personal tests have shown that this is still pretty fast with at least 
  5000 Users. I never tried more)  


## Grant management-access for configuration

In some cases you might want to have several administration users that will only be able to configure the SCIM
environment of a single realm. 
In such a case you must give the user a specific role that will grant access to configure the SCIM environment. If 
the user does not have the corresponding role the "SCIM"-menu entry will not be visible to this user. 

For each realm that gets created you will find a client named "realm-management". A specific *client-role* named
*scim-admin* can be found under this client. If you want a user to be able to configure the SCIM environment in a 
specific realm you need to grant this role to the user. In order to do this you must create a composite *realm-role*
that you can name as you want but is also named *scim-admin* in the example below. Simply assign the created 
*realm-role* that itself got the *client-role* *scim-admin* assigned to it to the user, and the user 
should be able to configure the SCIM environment for the specific realm
![theme-settings](images/restricted-scim-access.png)  

# SCIM 2.0 Open Source Implementation


|  Branch | Circle CI Status | Code Coverage |
| :------ | :--------------- | :------------ | 
| master  | [![CircleCI](https://circleci.com/gh/Captain-P-Goldfish/SCIM-SDK/tree/master.svg?style=shield)](https://circleci.com/gh/Captain-P-Goldfish/SCIM-SDK/tree/master) | [![codecov](https://codecov.io/gh/Captain-P-Goldfish/SCIM-SDK/branch/master/graph/badge.svg?token=z8URG24FO3)](https://codecov.io/gh/Captain-P-Goldfish/SCIM-SDK) |

This is an open source implementation of the SCIM (System for Cross-domain Identity Management) protocol that is
 defined by RFC7643 and RFC7644. 

A simple description of SCIM can be found here http://www.simplecloud.info/

### Create RESTful services with SCIM

SCIM is more than just users and groups. SCIM could be seen as a standardization of RESTful services and that is exactly
what this library does offer: An API for defining dynamic endpoints on a RESTful basis compatible with any
REST-framework on the market.

Define some JSON-schemas that describe the endpoint you want to create and get a lot of features that will support you
during development.

**This lib supports the full feature set of both client and server side of the SCIM specification.**

#### What exactly is a RESTful service?

RESTful is not a standard but a convention for services that define the HTTP-Methods [POST, GET, UPDATE, DELETE] to
manage resources on the server. Use this API to create such services and benefit from several features that make your
life easier.

### Documentation

for a documentation take a look into the github wiki https://github.com/Captain-P-Goldfish/SCIM/wiki

You will find a pretty detailed explanation on how to use the server implementation
here: https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/How-to-use-the-server-implementation

And a pretty detailed explanation on how to use the client can be found
here: https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/How-to-use-the-client-implementation

**Note:** You do not need to fork this project in order to make adjustments to the `User`-schema or the `User`-class
implementation. Find a more detailed explanation in the
wiki: [best practices](https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Best-practices)

---
**This project requires JDK 8 or higher**

```xml
<dependency>
    <groupId>de.captaingoldfish</groupId>
    <artifactId>scim-sdk-server</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>de.captaingoldfish</groupId>
    <artifactId>scim-sdk-client</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

---
**Note:**
If the project does not compile within your IDE install the "lombok" plugin for your IDE and restart it.

---

## Release Notes

To see the current release-notes take a look into the release
section: https://github.com/Captain-P-Goldfish/SCIM-SDK/releases

## Server implementation

This API is highly generic and allows the definition of your own schema representations for resources. You will even be
able to override the default User or Group core schemata if you want to. This might be useful if you do not want to
support attributes like "*ims*", "*phoneNumbers*" etc. All you need to do is simply to write two json documents.

1. a schema definition that describes the resource you want to supply
2. a resource type definition that describes the endpoint and the additional schema extensions that may be used within
   the schema definition

### The following features are supported

- [x] create
- [x] read
- [x] list
  * filtering (@see [additional features](https://github.com/Captain-P-Goldfish/SCIM/wiki/Additional-Features))
  * sorting (@see [additional features](https://github.com/Captain-P-Goldfish/SCIM/wiki/Additional-Features))
- [x] update
- [x] patch (@see [patching resources](https://github.com/Captain-P-Goldfish/SCIM/wiki/Patching-resources))
- [x] delete
- [x] bulk
  * resolving of bulkId references (@see [bulkId reference resolving](https://github.com/Captain-P-Goldfish/SCIM/wiki/BulkId-reference-resolving))
- [x] resource versioning (@see [resource versioning](https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Resource-versioning))
- [x] attributes-parameter
- [x] excludedAttributes-parameter

for any registered resource

### Additional supported features

- [x] authentication for specific endpoints 
(@see [authentication](https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Authentication-and-Authorization#authentication))
- [x] authorization for specific endpoints 
(@see [authorization](https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Authentication-and-Authorization#authorization))
- [x] attribute validation (@see [attribute validation](https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Attribute-validation)) 
- [x] custom resource validation [similiar to bean validation in JEE] (@see [resource validation](https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Validation-of-resources))
- [x] automatic POJO creation from SCIM schema resources. (@see [schema pojo creator](https://github.com/Captain-P-Goldfish/SCIM-SDK/tree/master/scim-sdk-schema-pojo-creator))

## Client implementation

The client implementation supports the following SCIM requests

- [x] create
- [x] read
- [x] list
- [x] update
- [x] delete
- [x] bulk
- [x] patch

there is also direct native support for easy use of basic authentication and X509 client authentication. All other
authentication types must be implemented manually.
 
 
---
**Automatic-Module-Names for Jigsaw:**

de.captaingoldfish.scim.sdk.common  
de.captaingoldfish.scim.sdk.server  
de.captaingoldfish.scim.sdk.client

---

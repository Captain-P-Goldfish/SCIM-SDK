# SCIM 2.0 Open Source Implementation

This is an open source implementation of the SCIM (System for Cross-domain Identity Management) protocol that is
 defined by RFC7643 and RFC7644. 

A simple description of SCIM can be found here http://www.simplecloud.info/

This API is highly generic and allows the definition of your own schema representations for resources. You will even be
able to override the default User or Group core schemata if you want to. This might be useful if you do not want to 
support attributes like "*ims*", "*phoneNumbers*" etc. All you need to do is simply to write two json documents.

1. a schema definition that describes the resource you want to supply
2. a resource type definition that describes the endpoint and the additional schema extensions that may be used within
 the shema definition  

For more detailed information take a look into the [wiki](https://github.com/Captain-P-Goldfish/SCIM/wiki)

The following features are supported

- [x] create
- [x] read
- [x] list (including filtering) see [additional features](https://github.com/Captain-P-Goldfish/SCIM/wiki/Additional-Features)
- [x] update
- [x] patch
- [x] delete
- [x] bulk
- [x] attributes-parameter
- [x] excludedAttributes-parameter

for any registered resource

the following features will be developed in the future:

Missing features defined by SCIM
- [ ] resource versioning
- [ ] resolving and ordering of bulkIds

Upcoming features not defined by SCIM
- [ ] auto sorting  => see also [additional features](https://github.com/Captain-P-Goldfish/SCIM/wiki/Additional-Features)
- [ ] authorization for specific endpoints
- [ ] authorization for specific attributes
- [ ] attribute validation (e.g. array-max-length=2 or number-min-value=10)
 

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

### additional features

- values with a returned value of "request" would normally only be returned if present in the *attributes* parameter in
 a query request. If you want to access the full data set add the resource URI to the *attributes* parameter e.g.: 
 **urn:ietf:params:scim:schemas:core:2.0:User**
 
### restrictions
 
- the *attributes* parameter does not accept sub-attribute definitions for multivalued complex types. For example you
 cannot put a value like "*emails.value*" into the *attributes* parameter and expect that only the value key for
  emails is returned in the response. But at least it works for simple complex types e.g. "*name.givenName*"  

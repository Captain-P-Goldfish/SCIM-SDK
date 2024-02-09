package de.captaingoldfish.scim.sdk.server.patch;

import java.util.List;
import java.util.function.Supplier;

import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.ETagConfig;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexMultivaluedSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexSimpleSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedSimpleAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.RemoveComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.RemoveExtensionRefOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.SimpleAttributeOperation;


/**
 * This interface is used to handle single patch operations that are prepared by the SCIM-SDK implementation
 *
 * @author Pascal Knueppel
 * @since 06.01.2024
 */
public interface PatchOperationHandler<T extends ResourceNode>
{

  /**
   * must retrieve a supplier that is able of retrieving the original resource state before any patch operation
   * was applied. This is needed in case that the {@link ETagConfig#isSupported()} value is set to true
   * otherwise it will not be needed otherwise.
   *
   * @param id the id of the resource
   * @param attributes optional request-attributes
   * @param excludedAttributes optional attributes to exclude from the resource
   * @param context the current request context
   * @return a supplier that retrieves the original state of the resource
   */
  public Supplier<T> getOldResourceSupplier(String id,
                                            List<SchemaAttribute> attributes,
                                            List<SchemaAttribute> excludedAttributes,
                                            Context context);

  /**
   * this method is called after the last call of {@link #handleOperation(PreparedPatchOperation)}. It adds
   * {@link de.captaingoldfish.scim.sdk.common.resources.complex.Meta}-infos like the new lastModified value to
   * it and passes the result of this method eventually to
   * {@link #getUpdatedResource(String, ResourceNode, boolean, Context)}. <br />
   * <br/>
   * <b>The easiest way to use this method in custom implementations:</b><br/>
   * Return an empty resource that contains nothing but its ID-value. The resource will be augmented by its
   * meta-attribute and a new lastModified value and then be passed to the
   * {@link #getUpdatedResource(String, ResourceNode, boolean, Context)}-method.
   *
   * @return the full patched resource
   */
  public T getPatchedResource(String id);

  /**
   * This method is called after all patch-operations were applied. It is assumed that all operations were
   * processed successfully and the result of this method is being passed to the
   * {@link de.captaingoldfish.scim.sdk.server.schemas.validation.ResponseResourceValidator}.
   *
   * @param id the id of the patched-resource that should be retrieved
   * @param wasResourceChanged if an effective change was applied to the resource or not
   * @return the full patched resource as it would be returned by
   *         {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler#getResource(String, List, List, Context)}
   */
  public T getUpdatedResource(String id,
                              T validatedPatchedResource,
                              boolean wasResourceChanged,
                              List<SchemaAttribute> attributes,
                              List<SchemaAttribute> excludedAttributes,
                              Context context);

  /**
   * this method gets patch-requests assigned that want to remove all attributes of an extension e.g.
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
   *      "op" : "remove"
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "op" : "replace",
   *      "value": {
   *        ...,
   *        "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": null
   *      }
   *    }
   *  }
   * </pre>
   *
   * <b>NOTE:</b><br />
   * This use-case should never have a filter-expression. So the {@link AttributePathRoot#getChild()} should
   * always be null.
   *
   * @param id the id of the resource that is being patched
   * @param patchOperation the validated patch-operation that contains the schema-reference to the extension for
   *          which all attributes should be removed
   * @return true if the resource was effectively changed or false if the values of the patch request did match
   *         the existing state of the resource
   */
  public boolean handleOperation(String id, RemoveExtensionRefOperation patchOperation);

  /**
   * this method gets patch-requests assigned that have simple attribute references. This includes references on
   * extension schema-attributes e.g.:
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "preferredLanguage",
   *      "op" : "add/replace",
   *      "value": "de"
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "preferredLanguage",
   *      "op" : "remove"
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "costCenter",
   *      "op" : "add/replace",
   *      "value": "costCenter"
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "op" : "add/replace",
   *      "value": {
   *        "nickName": "Chuck",       <--- handled by this method
   *        "timezone": null,          <--- handled by this method (treated as REMOVE operation)
   *        "name": {                  <--- NEVER handled directly (sub-attributes are handled)
   *          "givenName": "Carlos"    <--- handled by this method
   *        },
   *        "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
   *          "costCenter": null       <--- handled by this method (treated as REMOVE operation)
   *          "manager": {             <--- NEVER handled directly (sub-attributes are handled)
   *            "value": "123456789"   <--- handled by this method
   *          }
   *        }
   *      }
   *    }
   *  }
   * </pre>
   *
   * complex sub-attributes like <em>name.givenName</em> will not be assigned to this method<br />
   * <br />
   * <b>NOTE:</b><br />
   * This use-case should never have a filter-expression. So the {@link AttributePathRoot#getChild()} should
   * always be null. <br />
   * <br />
   * The direct attributes definition is accessible by {@link SimpleAttributeOperation#schemaAttribute}
   *
   * @param id the id of the resource that is being patched
   * @param patchOperation the validated patch-operation with all necessary infos
   * @return true if the attribute was effectively changed, false else
   */
  public boolean handleOperation(String id, SimpleAttributeOperation patchOperation);

  /**
   * this method gets patch-requests assigned that have simple array-attribute references. This includes
   * references on extension schema-attributes. The schemas defined in RFC7643 do not define simple-multivalued
   * attributes and have no examples. Therefore, we are using custom-attributes in the following examples:
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "realmRoleNames",
   *      "op" : "add/replace",
   *      "value": ["admin", "user", "developer"]
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "realmRoleNames",
   *      "op" : "add/replace",
   *      "value": "admin"         <--- array-notation is not required for single-values
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "departementAssignmentNumbers",
   *      "op" : "add/replace",
   *      "value": [1, 2, 3, 4, 5, 6]
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "op" : "remove",
   *      "path" : "departementAssignmentNumbers[value eq 6 or value eq 5]"
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "op" : "add/replace",
   *      "value": {
   *        "realmRoles": ["admin", "user"],      <--- handled by this method
   *        "spokenLanguages": null,              <--- handled by this method (treated as REMOVE operation)
   *        "name": {                             <--- NEVER handled directly (sub-attributes are handled)
   *          "middleNames": ["Darkwing", "Duck"] <--- handled by this method
   *        },
   *        "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
   *          "customArray": null       <--- handled by this method (treated as REMOVE operation)
   *          "manager": {              <--- NEVER handled directly (sub-attributes are handled)
   *            "subArray": "123456789" <--- handled by this method (array-notation not required for single-values)
   *          }
   *        }
   *      }
   *    }
   *  }
   * </pre>
   *
   * <b>NOTE:</b><br />
   * This use-case may have a filter-expression, in case of <em>REMOVE</em>-operations. It should be used to
   * remove a single element from a simple array. See <a href=
   * "https://github.com/Captain-P-Goldfish/SCIM-SDK/wiki/Patching-resources#support-for-special-filter-expression-since-1200">special-filter-expressions</a>
   * <br />
   * The direct attributes definition is accessible by
   * {@link MultivaluedSimpleAttributeOperation#schemaAttribute}
   *
   * @param id the id of the resource that is being patched
   * @param patchOperation the validated patch-operation with all necessary infos
   * @return true if the attribute was effectively changed, false else
   */
  public boolean handleOperation(String id, MultivaluedSimpleAttributeOperation patchOperation);

  /**
   * this method is called if a direct remove operation on a complex-attribute should be executed:
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "op" : "remove",
   *      "path" : "name"
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "op" : "add/replace",
   *      "value": {
   *        "name": null,
   *        ...
   *      }
   *    }
   *  }
   * </pre>
   *
   * <b>NOTE:</b><br />
   * This use-case should never have a filter-expression. So the {@link AttributePathRoot#getChild()} should
   * always be null.
   *
   * @param id the id of the resource that is being patched
   * @param patchOperation the validated patch-operation with all necessary infos
   * @return true if the resource was effectively changed or false if the values of the patch request did match
   *         the existing state of the resource
   */
  public boolean handleOperation(String id, RemoveComplexAttributeOperation patchOperation);

  /**
   * this method gets patch-requests assigned that directly reference a multivalued-complex-attribute. This is
   * the most difficult and complex request to handle. The following examples represent what might expect you
   * here:
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "op" : "remove",
   *      "path" : "emails"
   *    }]
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "op" : "remove",
   *      "path" : "emails[value eq "max@mustermann.de"]"
   *    }]
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "op" : "add/replace",
   *      "path" : "emails",
   *      "value": [
   *        {
   *          "value": "max@mustermann.de",
   *          "primary": true,
   *          "type": "home"
   *        },
   *        {
   *          "value": "max@muster.de",
   *          "primary": false,
   *          "type": "work"
   *        },
   *      ]
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *      {
   *        "op" : "add",
   *        "path" : "emails",
   *        "value": [
   *          {
   *            "value": "erika@mustermann.de",
   *            "primary": false,
   *            "type": "second-home"
   *          }
   *        ]
   *      }
   *    ]
   *  }
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "op" : "replace/add",
   *      "path" : "emails[value eq \"max@mustermann.de\"]",
   *      "value": [
   *        {
   *          "value": "erika@mustermann.de",
   *          "primary": false,
   *          "type": "second-home"
   *        }
   *      ]
   *    }]
   *  }
   * </pre>
   *
   * <b>NOTE:</b><br />
   * This use-case might have a filter-expression. The filter-expression is represented by
   * {@link AttributePathRoot#getChild()}.<br />
   * <br />
   * get the attributes you need to handle from the given {@link com.fasterxml.jackson.databind.node.ArrayNode}
   * and try to make sure that they match their definition<br />
   * The direct complex-attributes definition is accessible by
   * {@link MultivaluedComplexAttributeOperation#schemaAttribute}
   *
   * @param id the id of the resource that is being patched
   * @param patchOperation the validated patch-operation with all necessary infos
   * @return true if the resource was effectively changed or false if the values of the patch request did match
   *         the existing state of the resource
   */
  public boolean handleOperation(String id, MultivaluedComplexAttributeOperation patchOperation);

  /**
   * this method gets patch-requests assigned that directly reference a
   * multivalued-complex-[non-array]-sub-attribute e.g.:
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "emails[type eq "work"].display",
   *      "op" : "remove"
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "emails[type eq "work"].value",
   *      "op" : "add/replace",
   *      "value": "max@mustermann.de"
   *    }
   *  }
   * </pre>
   *
   * <b>NOTE:</b><br />
   * Expect this use-case to have a filter-expression in any case. If no filter-expression is present you are
   * free to handle it as you want because this case is not defined in RFC7644. Throw an error or change the
   * values in all array-elements according to the new value. The filter-expression is represented by
   * {@link AttributePathRoot#getChild()}. <br />
   * <br />
   * get the attributes you need to handle from the given {@link com.fasterxml.jackson.databind.node.ArrayNode}
   * and try to make sure that they match their definition<br />
   * The direct sub-attributes definition is accessible by
   * {@link MultivaluedComplexMultivaluedSubAttributeOperation#schemaAttribute} and
   * {@link MultivaluedComplexMultivaluedSubAttributeOperation#subAttribute} and the parent-attribute can be
   * addressed by {@link MultivaluedComplexMultivaluedSubAttributeOperation#parentAttribute}
   *
   * @param id the id of the resource that is being patched
   * @param patchOperation the validated patch-operation with all necessary infos
   * @return true if the resource was effectively changed or false if the values of the patch request did match
   *         the existing state of the resource
   */
  public boolean handleOperation(String id, MultivaluedComplexSimpleSubAttributeOperation patchOperation);

  /**
   * this method gets patch-requests assigned that directly reference a
   * multivalued-complex-[array]-sub-attribute e.g.:
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "myMultivaluedComplex[value eq \"some-identifier\"].mySubArray",
   *      "op" : "replace",
   *      "value": ["hello", "world"]
   *    }
   *  }
   * </pre>
   *
   * or
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "op" : "remove",
   *      "path" : "myMultivaluedComplex[value eq \"some-identifier\"].mySubArray"
   *    }
   *  }
   * </pre>
   *
   * <b>NOTE:</b><br />
   * Expect this use-case to have a filter-expression in any case. If no filter-expression is present you are
   * free to handle it as you want because this case is not defined in RFC7644. Throw an error or change the
   * values in all array-elements according to the new value. The filter-expression is represented by
   * {@link AttributePathRoot#getChild()}. <br />
   * <br />
   * get the attributes you need to handle from the given {@link com.fasterxml.jackson.databind.node.ArrayNode}
   * and try to make sure that they match their definition<br />
   * The direct sub-attributes definition is accessible by
   * {@link MultivaluedComplexMultivaluedSubAttributeOperation#schemaAttribute} and
   * {@link MultivaluedComplexMultivaluedSubAttributeOperation#subAttribute} and the parent-attribute can be
   * addressed by {@link MultivaluedComplexMultivaluedSubAttributeOperation#parentAttribute}
   *
   * @param id the id of the resource that is being patched
   * @param patchOperation the validated patch-operation with all necessary infos
   * @return true if the resource was effectively changed or false if the values of the patch request did match
   *         the existing state of the resource
   */
  public boolean handleOperation(String id, MultivaluedComplexMultivaluedSubAttributeOperation patchOperation);

}

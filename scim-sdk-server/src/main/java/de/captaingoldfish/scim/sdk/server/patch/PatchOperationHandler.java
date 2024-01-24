package de.captaingoldfish.scim.sdk.server.patch;

import java.util.List;
import java.util.function.Supplier;

import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.ETagConfig;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexSimpleSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.RemoveComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.RemoveExtensionRefOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexMultivaluedSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedSimpleAttributeOperation;
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
   * @return a supplier that retrieves the original state of the resource
   */
  public Supplier<T> getOldResourceSupplier(String id,
                                            List<SchemaAttribute> attributes,
                                            List<SchemaAttribute> excludedAttributes);

  /**
   * this method gets patch-requests assigned that directly reference an extension e.g.
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
   *      "path" : "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
   *      "op" : "add/replace",
   *      "value": {
   *        "costCenter": "...",
   *        ...
   *      }
   *    }
   *  }
   * </pre>
   *
   * <b>NOTE:</b><br />
   * This use-case should never have a filter-expression. So the {@link AttributePathRoot#getChild()} should
   * always be null. <br />
   * <br />
   * get the attributes you need to handle from the given {@link com.fasterxml.jackson.databind.node.ObjectNode}
   * and try to make sure that they match their definition
   *
   * @param id the id of the resource that is being patched
   * @param patchOperation the validated patch-operation with all necessary infos
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
   *      "path" : "costCenter",
   *      "op" : "add/replace",
   *      "value": "costCenter"
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
   * references on extension schema-attributes e.g.:
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
   *      "path" : "departementAssignmentNumbers",
   *      "op" : "add/replace",
   *      "value": [1, 2, 3, 4, 5, 6]
   *    }
   *  }
   * </pre>
   *
   * complex sub-attributes like <em>complex.myArray</em> will not be assigned to this method<br />
   * <br />
   * <b>NOTE:</b><br />
   * This use-case may have a filter-expression, in case of <em>REMOVE</em>-operations. This case is a custom
   * feature of the SCIM-SDK though and not defined in SCIM RFC7644. It should be used to remove a single
   * element from a simple array. See <a href=
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
   * this method gets patch-requests assigned that directly reference an extension e.g.
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "name",
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
   *      "path" : "name",
   *      "op" : "add/replace",
   *      "value": {
   *        "givenName": "...",
   *        "familyName": "..."
   *        ...
   *      }
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
   *      "path" : "manager",
   *      "op" : "add/replace",
   *      "value": {
   *        "value": "123456789"
   *      }
   *    }
   *  }
   * </pre>
   *
   * <b>NOTE:</b><br />
   * This use-case should never have a filter-expression. So the {@link AttributePathRoot#getChild()} should
   * always be null. <br />
   * <br />
   * get the attributes you need to handle from the given {@link com.fasterxml.jackson.databind.node.ObjectNode}
   * and try to make sure that they match their definition<br />
   * The direct complex-attributes definition is accessible by
   * {@link RemoveComplexAttributeOperation#schemaAttribute}
   *
   * @param id the id of the resource that is being patched
   * @param patchOperation the validated patch-operation with all necessary infos
   * @return true if the resource was effectively changed or false if the values of the patch request did match
   *         the existing state of the resource
   */
  public boolean handleOperation(String id, RemoveComplexAttributeOperation patchOperation);

  /**
   * this method gets patch-requests assigned that directly reference a multivalued-complex-attribute e.g.
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "emails",
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
   *      "path" : "emails",
   *      "op" : "add/replace",
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
   *    {
   *      "path" : "emails[value eq \"max@mustermann.de\"]",
   *      "op" : "replace",
   *      "value": [
   *        {
   *          "value": "erika@mustermann.de",
   *          "primary": false,
   *          "type": "second-home"
   *        }
   *      ]
   *    }
   *  }
   * </pre>
   *
   * <b>NOTE:</b><br />
   * This use-case might have a filter-expression in case of <em>REPLACE</em>-operations that can be used to
   * replace a specific element from the array. The filter-expression is represented by
   * {@link AttributePathRoot#getChild()}. Note that it is basically possible to replace a single element with
   * several elements. So the <em>value</em>-attribute might have more than one email assigned to it if a filter
   * is present.<br />
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
   * this method gets patch-requests assigned that directly reference a multivalued-complex-sub-attribute. This
   * includes also array-sub-attributes e.g.
   *
   * <pre>
   * {
   *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *    "Operations" : [
   *    {
   *      "path" : "emails[type eq "work"]",
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
   * or
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

  public boolean handleOperation(String id, MultivaluedComplexSimpleSubAttributeOperation patchOperation);

  /**
   * this method is called after the last call of {@link #handleOperation(PreparedPatchOperation)}. It assumes
   * that all patch operations were applied and will pass the result of this method into the
   * {@link de.captaingoldfish.scim.sdk.server.schemas.validation.RequestResourceValidator} to validate that the
   * applied patch-operations did not produce an invalid result.
   *
   * @return the full patched resource
   */
  public T getPatchedResource(String id);

  /**
   * This method is called after the result {@link #getPatchedResource()} was validated by the
   * {@link de.captaingoldfish.scim.sdk.server.schemas.validation.RequestResourceValidator}. It can be used to
   * apply additional custom-operations or to call the
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler#updateResource(ResourceNode, Context)}.
   * The result of this method will eventually be passed into the
   * {@link de.captaingoldfish.scim.sdk.server.schemas.validation.ResponseResourceValidator}
   *
   * @param id the id of the patched-resource that should be retrieved
   * @param wasResourceChanged if an effective change was applied to the resource or not
   * @return the full patched resource as it would be returned by
   *         {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler#getResource(String, List, List, Context)}
   */
  public T getUpdatedResource(String id, T validatedPatchedResource, boolean wasResourceChanged);

}

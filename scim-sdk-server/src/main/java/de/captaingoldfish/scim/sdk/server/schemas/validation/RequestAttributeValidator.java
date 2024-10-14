package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 10.04.2021
 */
@Slf4j
public class RequestAttributeValidator
{

  /**
   * validates a schema attribute in the context of a client-request that is either a POST or a PUT request
   *
   * @param schemaAttribute the attributes definition
   * @param attribute the attribute to validate
   * @param httpMethod the current request type that is either POST or PUT
   * @return the validated json node or an empty if the attribute is not present or should be ignored
   * @throws AttributeValidationException if the client has sent an invalid attribute that does not match its
   *           definition
   */
  public static Optional<JsonNode> validateAttribute(ServiceProvider serviceProvider,
                                                     SchemaAttribute schemaAttribute,
                                                     JsonNode attribute,
                                                     HttpMethod httpMethod)
  {
    ContextValidator requestContextValidator = getContextValidator(serviceProvider, httpMethod);
    Optional<JsonNode> validatedNode = ValidationSelector.validateNode(schemaAttribute,
                                                                       attribute,
                                                                       requestContextValidator);
    // checking once more for required is necessary for complex attributes and multivalued complex attributes
    // that have been evaluated to an empty.
    if (Type.COMPLEX.equals(schemaAttribute.getType()))
    {
      try
      {
        validateRequiredAttribute(httpMethod, schemaAttribute, !validatedNode.isPresent());
      }
      catch (AttributeValidationException ex)
      {
        String errorMessage = String.format("The required attribute '%s' was evaluated to an empty during "
                                            + "schema validation but the attribute is required '%s'",
                                            schemaAttribute.getFullResourceName(),
                                            attribute);
        throw new AttributeValidationException(schemaAttribute, errorMessage, ex);
      }
    }
    return validatedNode;
  }

  /**
   * the validation implementation that must only be executed in the context of a client-request
   *
   * @param httpMethod the http method that should either be POST or PUT since this method should only be called
   *          in case of creating and updating objects
   * @return the context validation for client requests
   */
  private static ContextValidator getContextValidator(final ServiceProvider serviceProvider,
                                                      final HttpMethod httpMethod)
  {
    return new ContextValidator(serviceProvider, ContextValidator.ValidationContextType.REQUEST)
    {

      @Override
      public boolean validateContext(SchemaAttribute schemaAttribute, JsonNode attribute)
        throws AttributeValidationException
      {
        // read only attributes are not accepted on request so we will simply ignore this attribute
        if (Mutability.READ_ONLY.equals(schemaAttribute.getMutability()))
        {
          if (attribute != null && !attribute.isNull())
          {
            log.debug("Removing '{}' attribute '{}' from request document",
                      Mutability.READ_ONLY,
                      schemaAttribute.getScimNodeName());
          }
          return false;
        }
        final boolean isNodeNull = attribute == null || attribute.isNull();

        if (!schemaAttribute.isRequired())
        {
          // if the node is not required and null the context validator needs to return false to ignore the validation
          // for this attribute since its definition says it is ignorable.
          return !isNodeNull;
        }

        validateRequiredAttribute(httpMethod, schemaAttribute, isNodeNull);
        return true;
      }
    };
  }

  /**
   * validates a required attribute
   *
   * @param httpMethod the http method that is necessary for immutable required attribute validation
   * @param schemaAttribute the attributes definition
   * @param isNodeNull if the attribute is null or not
   */
  private static void validateRequiredAttribute(HttpMethod httpMethod,
                                                SchemaAttribute schemaAttribute,
                                                boolean isNodeNull)
  {
    if (!schemaAttribute.isRequired())
    {
      return;
    }
    // null nodes are not allowed for required attributes that have a mutability of readOnly or writeOnly
    if ((Mutability.READ_WRITE.equals(schemaAttribute.getMutability())
         || Mutability.WRITE_ONLY.equals(schemaAttribute.getMutability()))
        && isNodeNull)
    {
      String errorMessage = String.format("Required '%s' attribute '%s' is missing",
                                          schemaAttribute.getMutability(),
                                          schemaAttribute.getFullResourceName());
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }

    // immutable required attributes must be set on object creation therefore we check for http-method POST
    if (Mutability.IMMUTABLE.equals(schemaAttribute.getMutability()) && HttpMethod.POST.equals(httpMethod)
        && isNodeNull)
    {
      String errorMessage = String.format("Required '%s' attribute '%s' must be set on object creation",
                                          schemaAttribute.getMutability(),
                                          schemaAttribute.getFullResourceName());
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }

    if (HttpMethod.PATCH.equals(httpMethod) && schemaAttribute.getParent() != null && isNodeNull)
    {
      String errorMessage = String.format("Required sub-attribute '%s' is missing in patch object.",
                                          schemaAttribute.getFullResourceName());
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }
  }

}

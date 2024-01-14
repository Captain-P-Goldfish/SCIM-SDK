package de.captaingoldfish.scim.sdk.server.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.Returned;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 15.10.2019 - 12:57 <br>
 * <br>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestHelper
{

  /**
   * will add a simple meta attribute to the given document
   */
  public static void addMetaToDocument(JsonNode document)
  {
    JsonHelper.addAttribute(document,
                            AttributeNames.RFC7643.META,
                            Meta.builder()
                                .resourceType("User")
                                .created(LocalDateTime.now())
                                .lastModified(LocalDateTime.now())
                                .location("/Users")
                                .build());
  }

  /**
   * creates a simple attribute definition with the given values
   */
  public static String getAttributeString(String name,
                                          Type type,
                                          boolean multiValued,
                                          boolean required,
                                          boolean caseExact,
                                          Mutability mutability,
                                          Returned returned,
                                          Uniqueness uniqueness)
  {
    // @formatter:off
    return "{" +
           "   \"name\": \"" + name + "\",\n" +
           "   \"type\": \"" + type.getValue() + "\",\n" +
           "   \"multiValued\": " + multiValued+ ",\n" +
           "   \"description\": \"some description\",\n" +
           "   \"required\": " + required + ",\n" +
           "   \"caseExact\": " + caseExact + ",\n" +
           "   \"mutability\": \"" + mutability.getValue() + "\",\n" +
           "   \"returned\": \"" + returned.getValue() + "\",\n" +
           "   \"uniqueness\": \"" + uniqueness.getValue() + "\"\n" +
           "}";
    // @formatter:on
  }

  /**
   * creates a new simple attribute definition with the given values and adds it to the attributes element of
   * the given schema node
   *
   * @return the new attribute definition
   */
  public static JsonNode addNewAttributeToSchema(JsonNode schemaNode,
                                                 String name,
                                                 Type type,
                                                 boolean multiValued,
                                                 boolean required,
                                                 boolean caseExact,
                                                 Mutability mutability,
                                                 Returned returned,
                                                 Uniqueness uniqueness)
  {
    JsonNode newAttribute = JsonHelper.readJsonDocument(TestHelper.getAttributeString(name,
                                                                                      type,
                                                                                      multiValued,
                                                                                      required,
                                                                                      caseExact,
                                                                                      mutability,
                                                                                      returned,
                                                                                      uniqueness));
    JsonHelper.getArrayAttribute(schemaNode, AttributeNames.RFC7643.ATTRIBUTES).ifPresent(arrayNode -> {
      arrayNode.add(newAttribute);
    });
    return newAttribute;
  }

  /**
   * this method extracts the attribute with the given name from the meta schema and modifies its attributes
   * with the given values
   *
   * @param metaSchema the meta schema that must contain an attribute with the given attributeName parameter
   * @param attributeName the name of the attribute that will should be modified. This attribute must exist
   */
  public static void modifyAttributeMetaData(JsonNode metaSchema,
                                             String attributeName,
                                             Type type,
                                             Mutability mutability,
                                             Returned returned,
                                             Uniqueness uniqueness,
                                             Boolean multiValued,
                                             Boolean required,
                                             Boolean caseExact,
                                             List<String> canonicalTypes)
  {
    String[] attributeNameParts = attributeName.split("\\.");
    JsonNode attributes = JsonHelper.getArrayAttribute(metaSchema, AttributeNames.RFC7643.ATTRIBUTES).get();
    JsonNode attributeDefinition = null;
    for ( JsonNode attribute : attributes )
    {
      String name = JsonHelper.getSimpleAttribute(attribute, AttributeNames.RFC7643.NAME).get();
      if (name.equals(attributeNameParts[0]))
      {
        attributeDefinition = attribute;
        break;
      }
    }
    if (attributeNameParts.length == 2)
    {
      JsonNode subAttributes = JsonHelper.getArrayAttribute(attributeDefinition, AttributeNames.RFC7643.SUB_ATTRIBUTES)
                                         .get();
      for ( JsonNode attribute : subAttributes )
      {
        String name = JsonHelper.getSimpleAttribute(attribute, AttributeNames.RFC7643.NAME).get();
        if (name.equals(attributeNameParts[1]))
        {
          attributeDefinition = attribute;
          break;
        }
      }
    }
    Assertions.assertNotNull(attributeDefinition);
    JsonNode finalAttributeDefinition = attributeDefinition;
    Optional.ofNullable(type).ifPresent(t -> {
      JsonHelper.addAttribute(finalAttributeDefinition, AttributeNames.RFC7643.TYPE, new TextNode(t.getValue()));
    });
    Optional.ofNullable(mutability).ifPresent(m -> {
      JsonHelper.addAttribute(finalAttributeDefinition, AttributeNames.RFC7643.MUTABILITY, new TextNode(m.getValue()));
    });
    Optional.ofNullable(returned).ifPresent(r -> {
      JsonHelper.addAttribute(finalAttributeDefinition, AttributeNames.RFC7643.RETURNED, new TextNode(r.getValue()));
    });
    Optional.ofNullable(uniqueness).ifPresent(u -> {
      JsonHelper.addAttribute(finalAttributeDefinition, AttributeNames.RFC7643.UNIQUENESS, new TextNode(u.getValue()));
    });
    Optional.ofNullable(multiValued).ifPresent(multi -> {
      JsonHelper.addAttribute(finalAttributeDefinition,
                              AttributeNames.RFC7643.MULTI_VALUED,
                              BooleanNode.valueOf(multi));
    });
    Optional.ofNullable(required).ifPresent(r -> {
      JsonHelper.addAttribute(finalAttributeDefinition, AttributeNames.RFC7643.REQUIRED, BooleanNode.valueOf(r));
    });
    Optional.ofNullable(caseExact).ifPresent(c -> {
      JsonHelper.addAttribute(finalAttributeDefinition, AttributeNames.RFC7643.CASE_EXACT, BooleanNode.valueOf(c));
    });
    Optional.ofNullable(canonicalTypes).ifPresent(canonical -> {
      ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
      arrayNode.addAll(canonical.stream().map(TextNode::new).collect(Collectors.toList()));
      JsonHelper.addAttribute(finalAttributeDefinition, AttributeNames.RFC7643.REFERENCE_TYPES, arrayNode);
    });
  }

  /**
   * overrides the registered resource type in a way that the enterprise user extension gets an attribute that
   * has a naming conflict with an attribute in the user schema
   *
   * @param ambigiousAttributeName the name of the attribute that should be added to the enterprise user schema
   * @param type the type of the new attribute
   * @param resourceType the resource type definition
   * @param resource the resource schema definition
   * @param extension the extension that will be extended by the new attribute
   * @param extensions additional extensions that must be present in the resource type
   * @return the new created {@link ResourceType}
   */
  public static ResourceType addAttributeToSchema(ResourceEndpoint resourceEndpoint,
                                                  ResourceHandler resourceHandler,
                                                  String ambigiousAttributeName,
                                                  Type type,
                                                  JsonNode resourceType,
                                                  JsonNode resource,
                                                  JsonNode extension,
                                                  JsonNode... extensions)
  {

    TestHelper.addNewAttributeToSchema(extension,
                                       ambigiousAttributeName,
                                       type,
                                       false,
                                       false,
                                       false,
                                       Mutability.READ_WRITE,
                                       Returned.DEFAULT,
                                       Uniqueness.NONE);
    List<JsonNode> extensionList = extensions == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(extensions));
    extensionList.add(extension);
    return resourceEndpoint.registerEndpoint(new EndpointDefinition(resourceType, resource, extensionList,
                                                                    resourceHandler));
  }
}

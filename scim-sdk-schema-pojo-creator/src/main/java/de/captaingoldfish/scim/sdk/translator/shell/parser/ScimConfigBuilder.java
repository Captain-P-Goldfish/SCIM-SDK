package de.captaingoldfish.scim.sdk.translator.shell.parser;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.SchemaRelation;
import de.captaingoldfish.scim.sdk.translator.shell.utils.UtilityMethods;
import freemarker.template.Template;
import lombok.Getter;
import lombok.SneakyThrows;


/**
 * author Pascal Knueppel <br>
 * created at: 08.08.2022 - 22:12 <br>
 * <br>
 */
public class ScimConfigBuilder extends AbstractPojoBuilder
{

  /**
   * the template to create a predefined scim configuration with a
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint}
   */
  private final Template scimConfigTemplate;

  @SneakyThrows
  public ScimConfigBuilder()
  {
    this.scimConfigTemplate = FREEMARKER_CONFIGURATION.getTemplate("scim-config.ftl");
  }

  /**
   * creates a scim configuration
   *
   * @param packageName the base package name of the source file
   * @param schemaRelations the relations that are necessary to register the created endpoints with their
   *          resource-handlers
   * @return the created scim configuration pojo
   */
  @SneakyThrows
  public String createScimConfigPojo(String packageName, List<SchemaRelation> schemaRelations, boolean useLombok)
  {
    List<SchemaRelation> resourceTypeRelations = schemaRelations.stream().filter(schemaRelation -> {
      return schemaRelation.getResourceType() != null;
    }).collect(Collectors.toList());

    List<String> resourceImports = resourceTypeRelations.stream().map(relation -> {
      final String resourceName = UtilityMethods.getResourceName(relation.getResourceSchema()
                                                                         .getJsonNode()
                                                                         .get(AttributeNames.RFC7643.NAME)
                                                                         .textValue());
      return String.format("%s.%s", UtilityMethods.getResourcesPackage(packageName, false), resourceName);
    }).collect(Collectors.toList());

    List<String> resourceHandlerImports = resourceTypeRelations.stream().map(relation -> {
      final String resourceName = UtilityMethods.getResourceHandlerName(relation.getResourceType()
                                                                                .getJsonNode()
                                                                                .get(AttributeNames.RFC7643.NAME)
                                                                                .textValue());
      return String.format("%s.%s", UtilityMethods.getResourceHandlerPackage(packageName, false), resourceName);
    }).collect(Collectors.toList());

    List<String> endpointDefinitionImports = resourceTypeRelations.stream().map(relation -> {
      final String resourceName = UtilityMethods.getEndpointDefinitionName(relation.getResourceType()
                                                                                   .getJsonNode()
                                                                                   .get(AttributeNames.RFC7643.NAME)
                                                                                   .textValue());
      return String.format("%s.%s", UtilityMethods.getEndpointsPackage(packageName, false), resourceName);
    }).collect(Collectors.toList());


    Map<String, Object> input = new HashMap<>();
    input.put("packageName", UtilityMethods.getScimConfigPackage(packageName, false));
    input.put("useLombok", useLombok);
    input.put("resourceImports", resourceImports);
    input.put("resourceHandlerImports", resourceHandlerImports);
    input.put("endpointDefinitionImports", endpointDefinitionImports);
    input.put("typeRelations",
              schemaRelations.stream()
                             .filter(relation -> relation.getResourceType() != null)
                             .map(TypeRelations::new)
                             .collect(Collectors.toList()));

    final String processedTemplate;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Writer fileWriter = new OutputStreamWriter(outputStream))
    {
      scimConfigTemplate.process(input, fileWriter);
      processedTemplate = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }
    return processedTemplate;
  }

  /**
   * a class representing the relations between a
   * {@link de.captaingoldfish.scim.sdk.server.schemas.ResourceType} a
   * {@link de.captaingoldfish.scim.sdk.common.resources.ResourceNode} and a
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler}
   */
  @Getter
  public static class TypeRelations
  {

    private final String resourceTypeClassName;

    private final String resourceNodeClassName;

    private final String resourceHandlerClassName;

    public TypeRelations(SchemaRelation schemaRelation)
    {
      final String resourceTypeName = schemaRelation.getResourceType()
                                                    .getJsonNode()
                                                    .get(AttributeNames.RFC7643.NAME)
                                                    .textValue();
      final String resourceNodeName = schemaRelation.getResourceSchema()
                                                    .getJsonNode()
                                                    .get(AttributeNames.RFC7643.NAME)
                                                    .textValue();
      this.resourceTypeClassName = UtilityMethods.getEndpointDefinitionName(resourceTypeName);
      this.resourceNodeClassName = UtilityMethods.getResourceName(resourceNodeName);
      this.resourceHandlerClassName = UtilityMethods.getResourceHandlerName(resourceTypeName);
    }
  }
}

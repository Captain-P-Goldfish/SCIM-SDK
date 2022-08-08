package de.captaingoldfish.scim.sdk.translator.shell.parser;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.FileInfoWrapper;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.SchemaRelation;
import de.captaingoldfish.scim.sdk.translator.shell.utils.UtilityMethods;
import freemarker.template.Template;
import lombok.SneakyThrows;


/**
 * author Pascal Knueppel <br>
 * created at: 05.08.2022 - 09:13 <br>
 * <br>
 */
public class EndpointDefinitionBuilder extends AbstractPojoBuilder
{

  /**
   * the template to parse resource-types into java
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition}s
   */
  private final Template endpointDefinitionTemplate;

  /**
   * creates a freemarker template to create ResourceNode implementations
   */
  @SneakyThrows
  public EndpointDefinitionBuilder()
  {
    this.endpointDefinitionTemplate = FREEMARKER_CONFIGURATION.getTemplate("endpoint-definition.ftl");
  }

  /**
   * will create the endpoint definitions based on the found resource-types
   *
   * @param packageName the base package name of the source file
   * @param schemaRelations all schema relations that were found. Processed are only those that contain a
   *          resource type and a resource schema
   * @return the created endpoint definitions
   */
  public Map<JsonNode, String> createEndpointDefinitions(String packageName, List<SchemaRelation> schemaRelations)
  {
    Map<JsonNode, String> pojoMap = new HashMap<>();
    for ( SchemaRelation schemaRelation : schemaRelations )
    {
      if (schemaRelation.getResourceType() == null)
      {
        continue;
      }
      String pojoString = createEndpointDefinitionJavaClass(packageName, schemaRelation);
      JsonNode resourceType = schemaRelation.getResourceType().getJsonNode();
      pojoMap.put(resourceType, pojoString);
    }
    return pojoMap;
  }

  /**
   * creates the endpoint definition java pojo by translating the given schemaRelation object with a freemarker
   * template
   *
   * @param packageName the packageName of the endpoint definition
   * @param schemaRelation the schemaRelation that is the base of the endpoint definition
   * @return the string representation of the created endpoint definition java pojo
   */
  @SneakyThrows
  private String createEndpointDefinitionJavaClass(String packageName, SchemaRelation schemaRelation)
  {
    JsonNode resourceTypeNode = schemaRelation.getResourceType().getJsonNode();
    String resourceTypeName = StringUtils.capitalize(resourceTypeNode.get(AttributeNames.RFC7643.NAME).textValue())
                                         .replace("\\s", "");
    resourceTypeName = String.format("%s", resourceTypeName);
    String resourceName = StringUtils.capitalize(new Schema(schemaRelation.getResourceSchema().getJsonNode()).getName()
                                                                                                             .get())
                                     .replaceAll("\\s", "");
    String resourceImport = String.format("%s.%s", packageName, resourceName);

    Map<String, Object> input = new HashMap<>();
    input.put("packageName", packageName);
    input.put("resourceName", resourceName);
    input.put("resourceImport", resourceImport);
    input.put("resourceTypeName", resourceTypeName);
    input.put("resourceTypeClasspath", UtilityMethods.toClasspath(schemaRelation.getResourceType().getResourceFile()));
    input.put("resourceTypeSchemaClasspath",
              UtilityMethods.toClasspath(schemaRelation.getResourceSchema().getResourceFile()));
    input.put("extensionPaths",
              schemaRelation.getExtensions()
                            .stream()
                            .map(FileInfoWrapper::getResourceFile)
                            .map(UtilityMethods::toClasspath)
                            .collect(Collectors.toList()));

    final String processedTemplate;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Writer fileWriter = new OutputStreamWriter(outputStream))
    {
      endpointDefinitionTemplate.process(input, fileWriter);
      processedTemplate = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }
    return processedTemplate;
  }
}

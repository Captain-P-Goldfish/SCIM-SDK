package de.captaingoldfish.scim.sdk.translator.shell.parser;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
public class ResourceNodeBuilder extends AbstractPojoBuilder
{

  /**
   * the template to parse resources into {@link de.captaingoldfish.scim.sdk.common.resources.ResourceNode}s
   */
  private final Template resourceNodeTemplate;

  /**
   * if the lombok builder annotations should be used
   */
  private final boolean useLombok;

  /**
   * creates a freemarker template to create ResourceNode implementations
   */
  @SneakyThrows
  public ResourceNodeBuilder(boolean useLombok)
  {
    this.useLombok = useLombok;
    this.resourceNodeTemplate = FREEMARKER_CONFIGURATION.getTemplate("resource-node.ftl");
  }

  /**
   * creates the java pojos and keeps them as string representation within the map
   *
   * @param packageName the packagename of the class
   * @param schemaRelations the schema relation representation from which the resources should be created
   * @return the created pojos as string representation
   */
  @SneakyThrows
  public Map<Schema, String> createResourceSchemaPojos(String packageName, List<SchemaRelation> schemaRelations)
  {
    SchemaHolder schemaHolder = SchemaHolder.PartSchemas(schemaRelations);

    Map<Schema, String> javaPojos = new HashMap();
    // now create the resource node java objects
    for ( Map.Entry<String, Schema> idSchemaEntry : schemaHolder.getResourceNodesToParse().entrySet() )
    {
      Schema schema = idSchemaEntry.getValue();
      List<Schema> extensions = schemaRelations.stream().filter(relation -> {
        return new Schema(relation.getResourceSchema().getJsonNode()).getNonNullId().equals(schema.getNonNullId());
      })
                                               .map(SchemaRelation::getExtensions)
                                               .findAny()
                                               .map(fileInfoWrappers -> fileInfoWrappers.stream()
                                                                                        .map(FileInfoWrapper::getJsonNode)
                                                                                        .map(Schema::new))
                                               .get()
                                               .collect(Collectors.toList());
      final String javaPojo = createResourceSchemaJavaClassFromSchema(packageName, schema, extensions, false);
      javaPojos.put(schema, javaPojo);
    }
    // now create the extension node java objects
    for ( Map.Entry<String, Schema> idSchemaEntry : schemaHolder.getExtensionNodesToParse().entrySet() )
    {
      Schema schema = idSchemaEntry.getValue();
      final String javaPojo = createResourceSchemaJavaClassFromSchema(packageName,
                                                                      schema,
                                                                      Collections.emptyList(),
                                                                      true);
      javaPojos.put(schema, javaPojo);
    }
    return javaPojos;
  }

  /**
   * builds the java class for a specific schema representation
   *
   * @param packageName the package where the java class is being located
   * @param schema the schema that is being translated into a java pojo
   * @param extensions the extensions that are related to this schema
   * @param isExtension if the schema itself is an extension or a resource node
   * @return the java pojo class representation as string
   */
  @SneakyThrows
  private String createResourceSchemaJavaClassFromSchema(String packageName,
                                                         Schema schema,
                                                         List<Schema> extensions,
                                                         boolean isExtension)
  {
    Map<String, Object> input = new HashMap<>();
    input.put("packageName", UtilityMethods.getResourcesPackage(packageName, false));
    input.put("resource", schema);
    input.put("extensionList", extensions);
    input.put("lombok", useLombok);
    input.put("isExtension", isExtension);

    final String processedTemplate;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Writer fileWriter = new OutputStreamWriter(outputStream))
    {
      resourceNodeTemplate.process(input, fileWriter);
      processedTemplate = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }
    return processedTemplate;
  }
}

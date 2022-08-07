package de.captaingoldfish.scim.sdk.translator.shell.parser;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.SchemaRelation;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import lombok.SneakyThrows;


/**
 * author Pascal Knueppel <br>
 * created at: 05.08.2022 - 09:13 <br>
 * <br>
 */
public class FreemarkerParser
{

  /**
   * the template to parse resources into java classes
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
  public FreemarkerParser(boolean useLombok)
  {
    this.useLombok = useLombok;
    // 1. Configure FreeMarker
    //
    // You should do this ONLY ONCE, when your application starts,
    // then reuse the same Configuration object elsewhere.

    Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);

    // Where do we load the templates from:
    final String templatePackage = "/de/captaingoldfish/scim/sdk/translator/freemarker/templates";
    configuration.setClassForTemplateLoading(FreemarkerParser.class, templatePackage);

    // Some other recommended settings:
    configuration.setIncompatibleImprovements(new Version(2, 3, 20));
    configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
    configuration.setLocale(Locale.ENGLISH);
    configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

    // 2.2. Get the template
    this.resourceNodeTemplate = configuration.getTemplate("resource-node.ftl");
  }

  /**
   * creates the java pojos and keeps them as string representation within the map
   * 
   * @param packageName the packagename of the class
   * @param schemaRelations the schema relation representation from which the resources should be created
   * @return the created pojos as string representation
   */
  @SneakyThrows
  public Map<Schema, String> createJavaResourcePojos(String packageName, List<SchemaRelation> schemaRelations)
  {
    SchemaHolder schemaHolder = SchemaHolder.PartSchemas(schemaRelations);

    Map<Schema, String> javaPojos = new HashMap();
    // now create the resource node java objects
    for ( Map.Entry<String, Schema> idSchemaEntry : schemaHolder.getResourceNodesToParse().entrySet() )
    {
      Schema schema = idSchemaEntry.getValue();
      List<Schema> extensions = schemaRelations.stream()
                                               .filter(relation -> relation.getResourceSchema()
                                                                           .getNonNullId()
                                                                           .equals(schema.getNonNullId()))
                                               .map(SchemaRelation::getExtensions)
                                               .findAny()
                                               .get();
      final String javaPojo = createResourceJavaClassFromSchema(packageName, schema, extensions, false);
      javaPojos.put(schema, javaPojo);
    }
    // now create the extension node java objects
    for ( Map.Entry<String, Schema> idSchemaEntry : schemaHolder.getExtensionNodesToParse().entrySet() )
    {
      Schema schema = idSchemaEntry.getValue();
      final String javaPojo = createResourceJavaClassFromSchema(packageName, schema, Collections.emptyList(), true);
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
  private String createResourceJavaClassFromSchema(String packageName,
                                                   Schema schema,
                                                   List<Schema> extensions,
                                                   boolean isExtension)
  {
    Map<String, Object> input = new HashMap<>();
    input.put("packageName", packageName);
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

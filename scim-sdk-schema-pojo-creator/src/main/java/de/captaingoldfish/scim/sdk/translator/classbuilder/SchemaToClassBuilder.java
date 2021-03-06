package de.captaingoldfish.scim.sdk.translator.classbuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * @author Pascal Knueppel
 * @since 03.05.2021
 */
public class SchemaToClassBuilder
{

  public String generateClassFromSchema(Schema schema)
  {
    String packageName = "package ???";
    String imports = getImports();
    String javadoc = schema.getDescription().orElse("");
    String className = StringUtils.capitalize(schema.getName().orElse("Unknown"));

    String classStructure = buildClassStructure(schema);
    return String.format("%s\n\n%s\n\n/** %s */\npublic class %s extends ResourceNode\n{\n  %s\n  %s\n}",
                         packageName,
                         imports,
                         javadoc,
                         className,
                         classStructure,
                         getFieldNames(schema));
  }

  private String getImports()
  {
    // @formatter:off
    return "import java.util.List;\n" +
           "import java.util.Optional;\n" +
           "import java.util.Set;\n" +
           "import java.util.Collections;\n" +
           "import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;\n" +
           "import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;\n" +
           "import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;";
    // @formatter:on
  }

  private String buildClassStructure(Schema schema)
  {
    List<String> constructorAttributes = new ArrayList<>();
    List<String> setterMethodCalls = new ArrayList<>();
    List<String> getterAndSetterMethodDefinitions = new ArrayList<>();
    List<String> complexAttributeDefinitions = new ArrayList<>();

    for ( SchemaAttribute attribute : schema.getAttributes() )
    {
      if ("id".equals(attribute.getName()))
      {
        continue;
      }
      if (Type.COMPLEX.equals(attribute.getType()))
      {
        final ComplexAttributeToInnerClassBuilder complexBuilder = new ComplexAttributeToInnerClassBuilder();
        String innerClassDefinition = complexBuilder.generateComplexAttributeClass(attribute);
        complexAttributeDefinitions.add(innerClassDefinition);
        setterMethodCalls.add(complexBuilder.getSetterMethodCall());
        constructorAttributes.add(complexBuilder.getSetterParameter());
      }
      else
      {
        final GetterMethodBuilder getterMethodBuilder = new GetterMethodBuilder();
        final SetterMethodBuilder setterMethodBuilder = new SetterMethodBuilder();
        String getterMethod = getterMethodBuilder.generateSimpleGetterMethod(attribute);
        String setterMethod = setterMethodBuilder.generateSimpleSetterMethod(attribute);

        getterAndSetterMethodDefinitions.add(String.format("%s\n%s", getterMethod, setterMethod));
        setterMethodCalls.add(setterMethodBuilder.getSetterCall());
        constructorAttributes.add(setterMethodBuilder.getSetterParameter());
      }
    }

    String noArgsConstructor = getNoArgsConstructor(schema);
    String allArgsConstructor = getAllArgsConstructor(schema, constructorAttributes, setterMethodCalls);
    StringBuilder getterAndSetterMethodCalls = new StringBuilder();
    for ( int i = 0 ; i < getterAndSetterMethodDefinitions.size() ; i++ )
    {
      String getterAndSetterDefinition = getterAndSetterMethodDefinitions.get(i);
      getterAndSetterMethodCalls.append(getterAndSetterDefinition).append('\n');
    }

    StringBuilder complexAttributeStringBuilder = new StringBuilder();
    for ( String complexAttributeDefinition : complexAttributeDefinitions )
    {
      complexAttributeStringBuilder.append(complexAttributeDefinition);
    }

    return String.format("%s%s%s%s",
                         noArgsConstructor,
                         allArgsConstructor,
                         getterAndSetterMethodCalls,
                         complexAttributeStringBuilder);
  }

  private String getNoArgsConstructor(Schema schema)
  {
    String constructorName = StringUtils.capitalize(schema.getName().orElse(null));
    return String.format("public %s() { }\n", constructorName);
  }

  private String getAllArgsConstructor(Schema schema,
                                       List<String> constructorAttributes,
                                       List<String> setterMethodCalls)
  {
    constructorAttributes.add(0, "String id");
    constructorAttributes.add("Meta meta");
    setterMethodCalls.add("setMeta(meta);");

    String constructorParams = String.join(", ", constructorAttributes);
    setterMethodCalls.add(0, "setId(id);");
    setterMethodCalls.add(0, "setSchemas(Collections.singletonList(FieldNames.SCHEMA_ID));");
    String setterCalls = String.join("\n  ", setterMethodCalls);
    String constructorName = StringUtils.capitalize(schema.getName().orElse(null));
    return String.format("public %s(%s) \n  {\n  %s\n  }\n", constructorName, constructorParams, setterCalls);
  }

  private String getFieldNames(Schema schema)
  {
    Set<String> attributeNames = getAttributeNames(schema);
    final String fieldNamesString = attributeNames.stream()
                                                  .map(name -> String.format("public static final String %s = \"%s\";",
                                                                             attributeNameToFieldName(name),
                                                                             name))
                                                  .collect(Collectors.joining("\n"));
    final String schemaIdField = String.format("public static final String SCHEMA_ID = \"%s\";\n",
                                               schema.getNonNullId());
    return String.format("public static class FieldNames \n{\n%s%s\n}", schemaIdField, fieldNamesString);
  }

  private Set<String> getAttributeNames(Schema schema)
  {
    Set<String> attributeNames = new HashSet<>();
    for ( SchemaAttribute attribute : schema.getAttributes() )
    {
      attributeNames.add(attribute.getName());
      if (Type.COMPLEX.equals(attribute.getType()))
      {
        for ( SchemaAttribute subAttribute : attribute.getSubAttributes() )
        {
          attributeNames.add(subAttribute.getName());
        }
      }
    }
    return attributeNames;
  }

  private String attributeNameToFieldName(String name)
  {
    return name.replaceAll("([A-Z])", "_$1").toUpperCase(Locale.ROOT);
  }
}

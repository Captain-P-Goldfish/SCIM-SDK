package de.captaingoldfish.scim.sdk.translator.classbuilder;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.translator.classbuilder.fieldnames.FieldnamesClassBuilder;
import de.captaingoldfish.scim.sdk.translator.classbuilder.setter.SetterMethod;
import de.captaingoldfish.scim.sdk.translator.classbuilder.setter.SetterMethodBuilder;
import de.captaingoldfish.scim.sdk.translator.utils.SharedMethods;
import lombok.RequiredArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 03.05.2021
 */
@RequiredArgsConstructor
public class SchemaToClassBuilder
{

  private final Schema resourceSchema;

  private final List<Schema> schemaExtensions;

  private final String packageDir;

  public String generateClassFromSchema()
  {
    String packageName = "package " + packageDir;
    String imports = getImports();
    String javadoc = resourceSchema.getDescription().orElse("");
    String className = SharedMethods.getSchemaClassname(resourceSchema);
    FieldnamesClassBuilder fieldnamesClassBuilder = new FieldnamesClassBuilder(resourceSchema);

    String classStructure = buildClassStructure(resourceSchema);
    return String.format("%s\n\n%s\n\n/** %s */\npublic class %s extends ResourceNode\n{\n  %s\n  %s\n}",
                         packageName,
                         imports,
                         javadoc,
                         className,
                         classStructure,
                         fieldnamesClassBuilder);
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
        SetterMethod setterMethod = setterMethodBuilder.generateSimpleSetterMethod(attribute);

        getterAndSetterMethodDefinitions.add(String.format("%s\n%s", getterMethod, setterMethod.toString()));
        setterMethodCalls.add(setterMethod.getCallSetter());
        constructorAttributes.add(setterMethod.getMethodParameter());
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

}

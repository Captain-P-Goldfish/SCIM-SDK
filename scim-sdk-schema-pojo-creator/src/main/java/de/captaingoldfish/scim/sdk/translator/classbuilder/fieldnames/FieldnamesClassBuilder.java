package de.captaingoldfish.scim.sdk.translator.classbuilder.fieldnames;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import lombok.RequiredArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 29.01.2022
 */
@RequiredArgsConstructor
public class FieldnamesClassBuilder
{

  private final Schema schema;

  @Override
  public String toString()
  {
    List<Fieldname> fieldnames = getFieldnames();
    fieldnames.add(0, new Fieldname("schemaId", schema.getNonNullId()));

    final String fieldNamesString = fieldnames.stream().map(Fieldname::toString).collect(Collectors.joining("\n"));

    return String.format("public static class FieldNames \n{\n%s\n}", fieldNamesString);
  }

  private List<Fieldname> getFieldnames()
  {
    Set<String> attributeNames = getAttributeNames(schema);
    List<Fieldname> fieldnames = new ArrayList<>();
    attributeNames.forEach(attributeName -> fieldnames.add(new Fieldname(attributeName)));
    return fieldnames;
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
}

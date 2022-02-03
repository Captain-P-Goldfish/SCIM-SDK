package de.captaingoldfish.scim.sdk.translator.classbuilder.fieldnames;

import java.util.Locale;


/**
 * @author Pascal Knueppel
 * @since 29.01.2022
 */
public class Fieldname
{

  private final String name;

  private final String value;

  public Fieldname(String name)
  {
    this.name = attributeNameToFieldName(name);
    this.value = name;
  }

  public Fieldname(String name, String value)
  {
    this.name = attributeNameToFieldName(name);
    this.value = value;
  }

  @Override
  public String toString()
  {
    return String.format("public static final String %s = \"%s\";", name, value);
  }

  private String attributeNameToFieldName(String name)
  {
    return name.replaceAll("([A-Z])", "_$1").toUpperCase(Locale.ROOT);
  }
}

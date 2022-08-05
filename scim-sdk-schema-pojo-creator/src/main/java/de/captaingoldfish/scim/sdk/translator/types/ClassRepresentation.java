package de.captaingoldfish.scim.sdk.translator.types;

import java.util.List;


/**
 * author Pascal Knueppel <br>
 * created at: 05.08.2022 - 08:46 <br>
 * <br>
 */
public class ClassRepresentation
{

  private String packageDir;

  private List<String> imports;

  private String className;

  private List<String> staticFinalFields;

  private List<ConstructorRepresentation> constructorRepresentations;

  private List<MethodRepresentations> methodRepresentations;

  private List<ClassRepresentation> innerClasses;


  public ClassRepresentation()
  {

  }
}

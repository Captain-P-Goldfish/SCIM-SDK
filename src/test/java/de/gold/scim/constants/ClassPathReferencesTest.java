package de.gold.scim.constants;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 01:06 <br>
 * <br>
 */
@Slf4j
public class ClassPathReferencesTest
{

  /**
   * will get all class path references from the class {@link ClassPathReferences} to verify that the given file
   * does exist
   */
  private static Stream<Arguments> getClassPathReferences() throws IllegalAccessException
  {
    List<Arguments> argumentList = new ArrayList<>();
    for ( Field field : ClassPathReferences.class.getFields() )
    {
      if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())
          && Modifier.isFinal(field.getModifiers()) && field.getType().equals(String.class))
      {
        argumentList.add(Arguments.of(field.get(null)));
      }
    }
    return Stream.of(argumentList.toArray(new Arguments[0]));
  }

  /**
   * will test that the classpath references do exist
   */
  @ParameterizedTest
  @MethodSource("getClassPathReferences")
  public void testVerifyThatClassPathReferencesDoExist(String classPathReference) throws IOException
  {
    log.trace("reading classpath resource: {}", classPathReference);
    try (InputStream inputStream = getClass().getResourceAsStream(classPathReference))
    {
      Assertions.assertNotNull(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
    }
  }
}

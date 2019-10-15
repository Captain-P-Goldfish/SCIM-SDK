package de.gold.scim.utils;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.resources.complex.Meta;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 15.10.2019 - 12:57 <br>
 * <br>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestHelper
{

  /**
   * will add a simple meta attribute to the given document
   */
  public static void addMetaToDocument(JsonNode document)
  {
    JsonHelper.addAttribute(document,
                            AttributeNames.META,
                            Meta.builder()
                                .resourceType("User")
                                .created(LocalDateTime.now())
                                .lastModified(LocalDateTime.now())
                                .location("/Users")
                                .build());
  }
}

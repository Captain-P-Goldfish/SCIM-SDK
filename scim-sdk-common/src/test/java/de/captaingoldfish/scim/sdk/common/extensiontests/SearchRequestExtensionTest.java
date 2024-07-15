package de.captaingoldfish.scim.sdk.common.extensiontests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.request.SearchRequest;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 17.09.2022 - 12:58 <br>
 * <br>
 */
public class SearchRequestExtensionTest
{

  @Test
  public void testExtendObject()
  {
    Assertions.assertDoesNotThrow(() -> SearchRequestExtension.builder().filter("/test").test("hello").build());
  }

  public static class SearchRequestExtension extends SearchRequest
  {

    @Builder
    public SearchRequestExtension(Long startIndex,
                                  Integer count,
                                  String filter,
                                  String sortBy,
                                  SortOrder sortOrder,
                                  String attributes,
                                  String excludedAttributes,
                                  String test)
    {
      super(startIndex, count, filter, sortBy, sortOrder, attributes, null, excludedAttributes, null);
    }



    /**
     * override lombok builder
     */
    public static class SearchRequestExtensionBuilder extends SearchRequest.SearchRequestBuilder
    {}
  }
}

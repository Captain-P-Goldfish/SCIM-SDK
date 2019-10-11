package de.gold.scim.resources.complex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:09 <br>
 * <br>
 */
public class AddressTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    Address instance = Assertions.assertDoesNotThrow(() -> Address.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new Address().isEmpty());
  }

  /**
   * will test if the attributes are correctly added into the json object
   */
  @Test
  public void testSetAndGetAttributes()
  {
    final String formatted = "Heidestraße 17\n51147 Köln\nDeutschland";
    final String streetAddress = "Heidestraße 17";
    final String postalCode = "51147";
    final String locality = "Köln";
    final String region = "Nordrhein-Westfalen";
    final String country = "DE";
    Address address = Address.builder()
                             .formatted(formatted)
                             .streetAddress(streetAddress)
                             .postalCode(postalCode)
                             .locality(locality)
                             .region(region)
                             .country(country)
                             .build();
    Assertions.assertEquals(formatted, address.getFormatted().get());
    Assertions.assertEquals(streetAddress, address.getStreetAddress().get());
    Assertions.assertEquals(postalCode, address.getPostalCode().get());
    Assertions.assertEquals(locality, address.getLocality().get());
    Assertions.assertEquals(region, address.getRegion().get());
    Assertions.assertEquals(country, address.getCountry().get());
  }
}

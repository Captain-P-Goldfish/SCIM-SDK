package de.gold.scim.common.resources;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.common.resources.complex.Manager;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 00:05 <br>
 * <br>
 */
public class EnterpriseUserTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    EnterpriseUser instance = Assertions.assertDoesNotThrow(() -> EnterpriseUser.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new EnterpriseUser().isEmpty());
  }

  /**
   * will test if the attributes are correctly added into the json object
   */
  @Test
  public void testSetAndGetAttributes()
  {
    final String employeeNumber = UUID.randomUUID().toString();
    final String costCenter = UUID.randomUUID().toString();
    final String organization = UUID.randomUUID().toString();
    final String division = UUID.randomUUID().toString();
    final String department = UUID.randomUUID().toString();
    final Manager manager = Manager.builder()
                                   .value(UUID.randomUUID().toString())
                                   .displayName(UUID.randomUUID().toString())
                                   .build();
    EnterpriseUser enterpriseUser = EnterpriseUser.builder()
                                                  .employeeNumber(employeeNumber)
                                                  .costCenter(costCenter)
                                                  .organization(organization)
                                                  .division(division)
                                                  .department(department)
                                                  .manager(manager)
                                                  .build();
    Assertions.assertEquals(employeeNumber, enterpriseUser.getEmployeeNumber().get());
    Assertions.assertEquals(costCenter, enterpriseUser.getCostCenter().get());
    Assertions.assertEquals(organization, enterpriseUser.getOrganization().get());
    Assertions.assertEquals(division, enterpriseUser.getDivision().get());
    Assertions.assertEquals(department, enterpriseUser.getDepartment().get());
    Assertions.assertEquals(manager, enterpriseUser.getManager().get());
  }
}

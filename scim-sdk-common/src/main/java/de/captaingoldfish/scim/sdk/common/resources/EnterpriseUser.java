package de.captaingoldfish.scim.sdk.common.resources;

import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 22:54 <br>
 * <br>
 * The following SCIM extension defines attributes commonly used in representing users that belong to, or act
 * on behalf of, a business or enterprise. The enterprise User extension is identified using the following
 * schema URI: "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User".
 */
public class EnterpriseUser extends ScimObjectNode
{

  public EnterpriseUser()
  {
    super(null);
  }

  @Builder
  public EnterpriseUser(String employeeNumber,
                        String costCenter,
                        String organization,
                        String division,
                        String department,
                        Manager manager)
  {
    this();
    setEmployeeNumber(employeeNumber);
    setCostCenter(costCenter);
    setOrganization(organization);
    setDivision(division);
    setDepartment(department);
    setManager(manager);
  }

  /**
   * A string identifier, typically numeric or alphanumeric, assigned to a person, typically based on order of
   * hire or association with an organization.
   */
  public Optional<String> getEmployeeNumber()
  {
    return getStringAttribute(AttributeNames.RFC7643.EMPLOYEE_NUMBER);
  }

  /**
   * A string identifier, typically numeric or alphanumeric, assigned to a person, typically based on order of
   * hire or association with an organization.
   */
  public void setEmployeeNumber(String employeeNumber)
  {
    setAttribute(AttributeNames.RFC7643.EMPLOYEE_NUMBER, employeeNumber);
  }

  /**
   * Identifies the name of a cost center.
   */
  public Optional<String> getCostCenter()
  {
    return getStringAttribute(AttributeNames.RFC7643.COST_CENTER);
  }

  /**
   * Identifies the name of a cost center.
   */
  public void setCostCenter(String costCenter)
  {
    setAttribute(AttributeNames.RFC7643.COST_CENTER, costCenter);
  }

  /**
   * Identifies the name of an organization.
   */
  public Optional<String> getOrganization()
  {
    return getStringAttribute(AttributeNames.RFC7643.ORGANIZATION);
  }

  /**
   * Identifies the name of an organization.
   */
  public void setOrganization(String organization)
  {
    setAttribute(AttributeNames.RFC7643.ORGANIZATION, organization);
  }

  /**
   * Identifies the name of a division.
   */
  public Optional<String> getDivision()
  {
    return getStringAttribute(AttributeNames.RFC7643.DIVISION);
  }

  /**
   * Identifies the name of a division.
   */
  public void setDivision(String division)
  {
    setAttribute(AttributeNames.RFC7643.DIVISION, division);
  }

  /**
   * Identifies the name of a department.
   */
  public Optional<String> getDepartment()
  {
    return getStringAttribute(AttributeNames.RFC7643.DEPARTMENT);
  }

  /**
   * Identifies the name of a department.
   */
  public void setDepartment(String department)
  {
    setAttribute(AttributeNames.RFC7643.DEPARTMENT, department);
  }

  /**
   * The user's manager. A complex type that optionally allows service providers to represent organizational
   * hierarchy by referencing the "id" attribute of another User.
   */
  public Optional<Manager> getManager()
  {
    return getObjectAttribute(AttributeNames.RFC7643.MANAGER, Manager.class);
  }

  /**
   * The user's manager. A complex type that optionally allows service providers to represent organizational
   * hierarchy by referencing the "id" attribute of another User.
   */
  public void setManager(Manager manager)
  {
    setAttribute(AttributeNames.RFC7643.MANAGER, manager);
  }


}

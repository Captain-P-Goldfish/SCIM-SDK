package de.captaingoldfish.scim.sdk.common.utils;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames.RFC7643;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * @author Pascal Knueppel
 * @since 26.02.2024
 */
public class ResourceComparatorTest implements FileReferences
{

  @DisplayName("No Values in attributes-member")
  @Nested
  class EmptyAttributesTests
  {

    private Schema userSchema;

    private Schema enterpriseUserSchema;

    private ResourceComparator resourceComparator;

    @BeforeEach
    public void initializeComparator()
    {
      userSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
      enterpriseUserSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON));

      resourceComparator = new ResourceComparator(userSchema, Arrays.asList(enterpriseUserSchema),
                                                  // the excluded attributes are set in the different test-methods
                                                  Collections.emptyList(),
                                                  ResourceComparator.AttributeHandlingType.EXCLUDE);
    }

    @DisplayName("success: resources are identical")
    @Test
    public void testIdenticalResources()
    {
      User user1 = JsonHelper.loadJsonDocument(USER_RESOURCE_WITH_EXTENSION, User.class);
      User user2 = JsonHelper.loadJsonDocument(USER_RESOURCE_WITH_EXTENSION, User.class);

      Assertions.assertTrue(resourceComparator.equals(user1, user2));
    }

    @DisplayName("success: null-resources are equal")
    @Test
    public void testNullValues()
    {
      Assertions.assertTrue(resourceComparator.equals(null, null));
    }

    @DisplayName("failure: one resources is null")
    @Test
    public void testOneResourceNull()
    {
      Assertions.assertFalse(resourceComparator.equals(User.builder().userName("goldfish").build(), null));
      Assertions.assertFalse(resourceComparator.equals(null, User.builder().userName("goldfish").build()));
    }

    @DisplayName("failure: different attribute count")
    @Test
    public void testDifferentAttributesCount()
    {
      Assertions.assertFalse(resourceComparator.equals(User.builder().userName("goldfish").build(),
                                                       User.builder()
                                                           .userName("goldfish")
                                                           .nickName("goldfish")
                                                           .build()));
    }

    @DisplayName("failure: nickName is different")
    @Test
    public void testSimpleAttributeComparison()
    {
      User user1 = User.builder().userName("goldfish").nickName("hello").build();
      User user2 = User.builder().userName("goldfish").nickName("world").build();

      Assertions.assertFalse(resourceComparator.equals(user1, user2));
    }
  }

  @DisplayName("Compare with excluded attributes")
  @Nested
  class ExcludedAttributeTests
  {

    private Schema userSchema;

    private Schema enterpriseUserSchema;

    private ResourceComparator resourceComparator;

    @BeforeEach
    public void initializeComparator()
    {
      userSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
      enterpriseUserSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON));

      resourceComparator = new ResourceComparator(userSchema, Arrays.asList(enterpriseUserSchema),
                                                  // the excluded attributes are set in the different test-methods
                                                  Collections.emptyList(),
                                                  ResourceComparator.AttributeHandlingType.EXCLUDE);
    }

    @DisplayName("attribute 'nickName' excluded from comparison")
    @Nested
    class NickNameExcludedTests
    {

      @BeforeEach
      public void initializeComparator()
      {
        resourceComparator.addAttributes(userSchema.getSchemaAttribute(RFC7643.NICK_NAME));
      }

      @DisplayName("Compare 'nickName' excluded")
      @Test
      public void testSimpleAttributeComparison()
      {

        User user1 = User.builder().userName("goldfish").nickName("hello").build();
        User user2 = User.builder().userName("goldfish").nickName("world").build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("success: resources are identical")
      @Test
      public void testIdenticalResources()
      {
        User user1 = JsonHelper.loadJsonDocument(USER_RESOURCE_WITH_EXTENSION, User.class);
        User user2 = JsonHelper.loadJsonDocument(USER_RESOURCE_WITH_EXTENSION, User.class);

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("success: null-resources are equal")
      @Test
      public void testNullValues()
      {
        Assertions.assertTrue(resourceComparator.equals(null, null));
      }

      @DisplayName("failure: one resources is null")
      @Test
      public void testOneResourceNull()
      {
        Assertions.assertFalse(resourceComparator.equals(User.builder().userName("goldfish").build(), null));
        Assertions.assertFalse(resourceComparator.equals(null, User.builder().userName("goldfish").build()));
      }

      @DisplayName("success: different attribute count")
      @Test
      public void testDifferentAttributesCount()
      {
        Assertions.assertTrue(resourceComparator.equals(User.builder().userName("goldfish").build(),
                                                        User.builder()
                                                            .userName("goldfish")
                                                            .nickName("goldfish")
                                                            .build()));
      }

      @DisplayName("success: different attribute count (part 2)")
      @Test
      public void testDifferentAttributesCount2()
      {
        Assertions.assertTrue(resourceComparator.equals(User.builder()
                                                            .userName("goldfish")
                                                            .nickName("goldfish")
                                                            .build(),
                                                        User.builder().userName("goldfish").build()));
      }
      /* ****************************************************************************************** */
    }

    @DisplayName("attribute 'name' excluded from comparison")
    @Nested
    class NameExcludedTests
    {

      @BeforeEach
      public void initializeComparator()
      {
        resourceComparator.addAttributes(userSchema.getSchemaAttribute(RFC7643.NAME));
      }

      @DisplayName("Compare 'name' excluded")
      @Test
      public void testSimpleAttributeComparison()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .name(Name.builder()
                                   .formatted("Sir Carlos Ray Norris")
                                   .honorificPrefix("Mr.")
                                   .honorificSuffix("-")
                                   .givenName("Carlos")
                                   .middlename("Ray")
                                   .familyName("Norris")
                                   .build())
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .name(Name.builder()
                                   .formatted("Sir Carlos Ray Norris-2")
                                   .honorificPrefix("Mr.-2")
                                   .honorificSuffix("-2")
                                   .givenName("Carlos-2")
                                   .middlename("Ray-2")
                                   .familyName("Norris-2")
                                   .build())
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }


      /* ****************************************************************************************** */
    }

    @DisplayName("sub-attributes of 'name' excluded from comparison")
    @Nested
    class NameSubAttributesExcludedTests
    {

      @BeforeEach
      public void initializeComparator()
      {
        SchemaAttribute nameAttribute = userSchema.getSchemaAttribute(RFC7643.NAME);
        resourceComparator.addAttributes(nameAttribute.getSubAttribute(RFC7643.GIVEN_NAME),
                                         nameAttribute.getSubAttribute(RFC7643.FAMILY_NAME));
      }

      @DisplayName("success: Compare 'name.givenName' and 'name.familyName' excluded")
      @Test
      public void testSimpleAttributeComparison()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .name(Name.builder()
                                   .formatted("Sir Carlos Ray Norris")
                                   .honorificPrefix("Mr.")
                                   .honorificSuffix("-")
                                   .givenName("Carlos")
                                   .middlename("Ray")
                                   .familyName("Norris")
                                   .build())
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .name(Name.builder()
                                   .formatted("Sir Carlos Ray Norris")
                                   .honorificPrefix("Mr.")
                                   .honorificSuffix("-")
                                   .givenName("Carlos-2")
                                   .middlename("Ray")
                                   .familyName("Norris-2")
                                   .build())
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("failure: Other than 'name.givenName' and 'name.familyName' are different")
      @Test
      public void testFailIfOtherAttributesAreUnequal()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .name(Name.builder()
                                   .formatted("Sir Carlos Ray Norris")
                                   .honorificPrefix("Mr.")
                                   .honorificSuffix("-")
                                   .givenName("Carlos")
                                   .middlename("Ray")
                                   .familyName("Norris")
                                   .build())
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .name(Name.builder()
                                   .formatted("Another name")
                                   .honorificPrefix("Mr.")
                                   .honorificSuffix("-")
                                   .givenName("Carlos-2")
                                   .middlename("Ray")
                                   .familyName("Norris-2")
                                   .build())
                         .build();

        Assertions.assertFalse(resourceComparator.equals(user1, user2));
      }
      /* ****************************************************************************************** */
    }

    @DisplayName("attribute 'emails' excluded from comparison")
    @Nested
    class EmailsAttributesExcludedTests
    {

      @BeforeEach
      public void initializeComparator()
      {
        SchemaAttribute emailsAttribute = userSchema.getSchemaAttribute(RFC7643.EMAILS);
        resourceComparator.addAttributes(emailsAttribute);
      }

      @DisplayName("success: Compare resources are equals")
      @Test
      public void testCompareSingleEmail()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build()))
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("success: Compare 'emails' (single emails) - also exclude externalId")
      @Test
      public void testCompareEmailsAlsoExcludeExternalId()
      {
        resourceComparator.addAttributes(userSchema.getSchemaAttribute(RFC7643.EXTERNAL_ID));

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .externalId("123456")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build()))
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("success: Compare 'emails' (single emails) - also exclude externalId - part 2")
      @Test
      public void testCompareEmailsAlsoExcludeExternalId2()
      {
        resourceComparator.addAttributes(userSchema.getSchemaAttribute(RFC7643.EXTERNAL_ID));

        User user1 = User.builder()
                         .userName("chuck")
                         .externalId("123456")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build()))
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("success: Compare emails have differences")
      @Test
      public void testEmailHasDifferences()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("home").build()))
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("success: Compare emails have different lengths")
      @Test
      public void testEmailHaveDifferentLengths()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build(),
                                               Email.builder().value("chucky@norris.de").type("home").build()))
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      /* ****************************************************************************************** */
    }

    @DisplayName("sub-attributes of 'emails' excluded from comparison")
    @Nested
    class SubAttributesOfEmailsExcludedTests
    {

      @BeforeEach
      public void initializeComparator()
      {
        SchemaAttribute emailsAttribute = userSchema.getSchemaAttribute(RFC7643.EMAILS);
        resourceComparator.addAttributes(emailsAttribute.getSubAttribute(RFC7643.PRIMARY),
                                         emailsAttribute.getSubAttribute(RFC7643.DISPLAY));
      }

      @DisplayName("success: Compared resources are equals")
      @Test
      public void testSimpleAttributeComparison()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder()
                                                    .value("chuck@norris.de")
                                                    .display("test-value")
                                                    .primary(true)
                                                    .type("work")
                                                    .build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder()
                                                    .value("chuck@norris.de")
                                                    .display("test-value")
                                                    .primary(true)
                                                    .type("work")
                                                    .build()))
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("success: Compared resources are equals (different order)")
      @Test
      public void testEmailsDifferentOrder()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chucky@norris.de").type("home").build(),
                                               Email.builder()
                                                    .value("chuck@norris.de")
                                                    .display("test-value")
                                                    .primary(true)
                                                    .type("work")
                                                    .build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder()
                                                    .value("chuck@norris.de")
                                                    .display("test-value")
                                                    .primary(true)
                                                    .type("work")
                                                    .build(),
                                               Email.builder().value("chucky@norris.de").type("home").build()))
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("success: Compared resources are equals (different order) (primary ignored)")
      @Test
      public void testEmailsDifferentOrderPrimaryIgnored()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chucky@norris.de").type("home").build(),
                                               Email.builder()
                                                    .value("chuck@norris.de")
                                                    .display("test-value")
                                                    .primary(true)
                                                    .type("work")
                                                    .build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder()
                                                    .value("chuck@norris.de")
                                                    .display("test-value")
                                                    .primary(false)
                                                    .type("work")
                                                    .build(),
                                               Email.builder().value("chucky@norris.de").type("home").build()))
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("success: Compared resources are equals (different order) (display ignored)")
      @Test
      public void testEmailsDifferentOrderDisplayIgnored()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chucky@norris.de").type("home").build(),
                                               Email.builder()
                                                    .value("chuck@norris.de")
                                                    .display("test-value")
                                                    .primary(true)
                                                    .type("work")
                                                    .build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder()
                                                    .value("chuck@norris.de")
                                                    .display("other-display")
                                                    .primary(true)
                                                    .type("work")
                                                    .build(),
                                               Email.builder().value("chucky@norris.de").type("home").build()))
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("success: Compared resources are equals (different order, different values)")
      @Test
      public void testEmailsDifferentOrderDifferentValues()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chucky@norris.de").type("home").build(),
                                               Email.builder()
                                                    .value("chuck@norris.de")
                                                    .display("test-value")
                                                    .primary(true)
                                                    .type("work")
                                                    .build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder()
                                                    .value("chucky-2@norris.de")
                                                    .display("test-value")
                                                    .primary(true)
                                                    .type("work")
                                                    .build(),
                                               Email.builder().value("chucky@norris.de").type("home").build()))
                         .build();

        Assertions.assertFalse(resourceComparator.equals(user1, user2));
      }

      @DisplayName("failure: Compare emails have differences")
      @Test
      public void testEmailHasDifferences()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder()
                                                    .value("chuck@norris.de")
                                                    .display("test-value")
                                                    .primary(true)
                                                    .type("work")
                                                    .build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder()
                                                    .value("chuck@norris.de")
                                                    .display("test-value-2")
                                                    .primary(false)
                                                    .type("work")
                                                    .build()))
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("failure: Compare emails have different lengths")
      @Test
      public void testEmailHaveDifferentLengths()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build(),
                                               Email.builder().value("chucky@norris.de").type("home").build()))
                         .build();

        Assertions.assertFalse(resourceComparator.equals(user1, user2));
      }

      @DisplayName("failure: Compare emails have different types")
      @Test
      public void testEmailHaveDifferentTypes()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("work").build()))
                         .build();
        User user2 = User.builder()
                         .userName("chuck")
                         .emails(Arrays.asList(Email.builder().value("chuck@norris.de").type("home").build()))
                         .build();

        Assertions.assertFalse(resourceComparator.equals(user1, user2));
      }

      /* ****************************************************************************************** */
    }

    /* ****************************************************************************************** */
  }

  @DisplayName("Compare with specific attributes")
  @Nested
  class IncludedAttributeTests
  {

    private Schema userSchema;

    private Schema enterpriseUserSchema;

    private ResourceComparator resourceComparator;

    @BeforeEach
    public void initializeComparator()
    {
      userSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
      enterpriseUserSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON));

      resourceComparator = new ResourceComparator(userSchema, Arrays.asList(enterpriseUserSchema),
                                                  // the excluded attributes are set in the different test-methods
                                                  Collections.emptyList(),
                                                  ResourceComparator.AttributeHandlingType.INCLUDE);
    }

    @DisplayName("Compare by nickName only")
    @Nested
    class CompareByNicknameOnlyTest
    {

      @BeforeEach
      public void initializeComparator()
      {
        resourceComparator.addAttributes(userSchema.getSchemaAttribute(RFC7643.NICK_NAME));
      }

      @DisplayName("success: Compare 'nickName' only")
      @Test
      public void testCompareNicknameOnly()
      {

        User user1 = User.builder().userName("chuck").nickName("hello").build();
        User user2 = User.builder().userName("chucky").nickName("hello").build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("failure: Compare 'nickName' only")
      @Test
      public void testCompareNicknameOnlyFailure()
      {

        User user1 = User.builder().userName("chuck").nickName("hello").build();
        User user2 = User.builder().userName("chucky").nickName("world").build();

        Assertions.assertFalse(resourceComparator.equals(user1, user2));
      }
      /* ******************************************************************************************** */
    }

    @DisplayName("Compare by givenName and familyName only")
    @Nested
    class CompareBySubAttributeGivenNameAndFamilyNameOnly
    {

      @BeforeEach
      public void initializeComparator()
      {
        SchemaAttribute nameAttribute = userSchema.getSchemaAttribute(RFC7643.NAME);
        resourceComparator.addAttributes(nameAttribute.getSubAttribute(RFC7643.GIVEN_NAME),
                                         nameAttribute.getSubAttribute(RFC7643.FAMILY_NAME));
      }

      @DisplayName("success: Compare 'givenName' only")
      @Test
      public void testCompareGivenName()
      {

        User user1 = User.builder().userName("chuck").name(Name.builder().givenName("Chuck").build()).build();
        User user2 = User.builder().userName("chucky").name(Name.builder().givenName("Chuck").build()).build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("failure: Compare 'givenName' only")
      @Test
      public void testCompareGivenNameFailure()
      {

        User user1 = User.builder().userName("chuck").name(Name.builder().givenName("Chuck").build()).build();
        User user2 = User.builder().userName("chucky").name(Name.builder().givenName("Chucky").build()).build();

        Assertions.assertFalse(resourceComparator.equals(user1, user2));
      }

      @DisplayName("success: Compare 'givenName' and 'familyName'")
      @Test
      public void testCompareGivenNameAndFamilyName()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .name(Name.builder().givenName("Chuck").familyName("Norris").build())
                         .build();
        User user2 = User.builder()
                         .userName("chucky")
                         .name(Name.builder().givenName("Chuck").familyName("Norris").build())
                         .build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("failure: Compare 'givenName' and 'familyName'")
      @Test
      public void testCompareGivenNameAndFamilyNameFailure()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .name(Name.builder().givenName("Chuck").familyName("Norris").build())
                         .build();
        User user2 = User.builder()
                         .userName("chucky")
                         .name(Name.builder().givenName("Chuck").familyName("Norris-2").build())
                         .build();

        Assertions.assertFalse(resourceComparator.equals(user1, user2));
      }

      @DisplayName("failure: Compare 'givenName' and 'familyName' part-2")
      @Test
      public void testCompareGivenNameAndFamilyNameFailure2()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .name(Name.builder().givenName("Chuck").familyName("Norris").build())
                         .build();
        User user2 = User.builder()
                         .userName("chucky")
                         .name(Name.builder().givenName("Chuck-2").familyName("Norris").build())
                         .build();

        Assertions.assertFalse(resourceComparator.equals(user1, user2));
      }

      @DisplayName("failure: Compare 'givenName' and 'familyName' part-3")
      @Test
      public void testCompareGivenNameAndFamilyNameFailure3()
      {

        User user1 = User.builder()
                         .userName("chuck")
                         .name(Name.builder().givenName("Chuck").familyName("Norris").build())
                         .build();
        User user2 = User.builder()
                         .userName("chucky")
                         .name(Name.builder().givenName("Chuck-2").familyName("Norris-2").build())
                         .build();

        Assertions.assertFalse(resourceComparator.equals(user1, user2));
      }
      /* ******************************************************************************************** */
    }

    @DisplayName("Compare by EnterpriseUser costCenter only")
    @Nested
    class CompareByEnterpriseUserCostCenter
    {

      @BeforeEach
      public void initializeComparator()
      {
        SchemaAttribute costCenterAttribute = enterpriseUserSchema.getSchemaAttribute(RFC7643.COST_CENTER);
        resourceComparator.addAttributes(costCenterAttribute);
      }

      @DisplayName("success: compare costCenter")
      @Test
      public void testCompareCostCenter()
      {
        EnterpriseUser enterpriseUser1 = EnterpriseUser.builder().costCenter("hello").build();
        User user1 = User.builder().userName("chuck").enterpriseUser(enterpriseUser1).build();

        EnterpriseUser enterpriseUser2 = EnterpriseUser.builder().costCenter("hello").build();
        User user2 = User.builder().userName("chucky").enterpriseUser(enterpriseUser2).build();

        Assertions.assertTrue(resourceComparator.equals(user1, user2));
      }

      @DisplayName("failure: compare costCenter")
      @Test
      public void testCompareCostCenterDifferentValues()
      {
        EnterpriseUser enterpriseUser1 = EnterpriseUser.builder().costCenter("hello").build();
        User user1 = User.builder().userName("chuck").enterpriseUser(enterpriseUser1).build();

        EnterpriseUser enterpriseUser2 = EnterpriseUser.builder().costCenter("world").build();
        User user2 = User.builder().userName("chucky").enterpriseUser(enterpriseUser2).build();

        Assertions.assertFalse(resourceComparator.equals(user1, user2));
      }

      /* ******************************************************************************************** */
    }

    @DisplayName("Compare by emails attribute")
    @Nested
    class CompareByEmailsAttribute
    {

      @BeforeEach
      public void initializeComparator()
      {
        SchemaAttribute emailsAttribute = userSchema.getSchemaAttribute(RFC7643.EMAILS);
        resourceComparator.addAttributes(emailsAttribute);
      }

      @DisplayName("Single emails Tests")
      @Nested
      class SingleEmailTest
      {

        @DisplayName("success: Compare 'emails' (single email)")
        @Test
        public void testCompareEmails()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build()))
                           .build();

          Assertions.assertTrue(resourceComparator.equals(user1, user2));
        }

        @DisplayName("failure: Compare 'emails' (single email)")
        @Test
        public void testCompareSingleEmailFailure()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris-2.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build()))
                           .build();

          Assertions.assertFalse(resourceComparator.equals(user1, user2));
        }

        @DisplayName("failure: Compare 'emails' (single email) part-2")
        @Test
        public void testCompareSingleEmailFailure2()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris-2.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build()))
                           .build();

          Assertions.assertFalse(resourceComparator.equals(user1, user2));
        }

        @DisplayName("failure: Compare 'emails' (single email) part-3")
        @Test
        public void testCompareSingleEmailFailure3()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home-2")
                                                      .primary(true)
                                                      .build()))
                           .build();

          Assertions.assertFalse(resourceComparator.equals(user1, user2));
        }

        @DisplayName("failure: Compare 'emails' (single email) part-4")
        @Test
        public void testCompareSingleEmailFailure4()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(false)
                                                      .build()))
                           .build();

          Assertions.assertFalse(resourceComparator.equals(user1, user2));
        }

        @DisplayName("failure: Compare 'emails' (single email) part-5")
        @Test
        public void testCompareSingleEmailFailure5()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .build()))
                           .build();

          Assertions.assertFalse(resourceComparator.equals(user1, user2));
        }
        /* ******************************************************************************************** */
      }
      /* ******************************************************************************************** */

      @DisplayName("Multiple emails Tests")
      @Nested
      class MultipleEmailsTests
      {

        @DisplayName("success: Compare two 'emails'")
        @Test
        public void testCompareTwoEmails()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();

          Assertions.assertTrue(resourceComparator.equals(user1, user2));
        }

        @DisplayName("success: Compare two 'emails' (different orders)")
        @Test
        public void testCompareTwoEmailsDifferentOrders()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build(),
                                                 Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();

          Assertions.assertTrue(resourceComparator.equals(user1, user2));
        }

        @DisplayName("failure: Compare two 'emails'")
        @Test
        public void testCompareTwoEmailsFailure()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris-2.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();

          Assertions.assertFalse(resourceComparator.equals(user1, user2));
        }

        @DisplayName("failure: Compare two 'emails' part-2")
        @Test
        public void testCompareTwoEmailsFailure2()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris-2.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();

          Assertions.assertFalse(resourceComparator.equals(user1, user2));
        }

        @DisplayName("failure: Compare two 'emails' part-3")
        @Test
        public void testCompareTwoEmailsFailure3()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home-2")
                                                      .primary(true)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();

          Assertions.assertFalse(resourceComparator.equals(user1, user2));
        }

        @DisplayName("failure: Compare two 'emails' part-4")
        @Test
        public void testCompareTwoEmailsFailure4()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(false)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();

          Assertions.assertFalse(resourceComparator.equals(user1, user2));
        }

        @DisplayName("failure: Compare two 'emails' part-4 -> different order")
        @Test
        public void testCompareTwoEmailsFailure4DifferentOrder()
        {

          User user1 = User.builder()
                           .userName("chuck")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build(),
                                                 Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(true)
                                                      .build()))
                           .build();
          User user2 = User.builder()
                           .userName("chucky")
                           .emails(Arrays.asList(Email.builder()
                                                      .value("chuck@norris.com")
                                                      .display("chucky@norris.com")
                                                      .type("home")
                                                      .primary(false)
                                                      .build(),
                                                 Email.builder()
                                                      .value("mario@nintendo.com")
                                                      .display("super@mario.com")
                                                      .type("work")
                                                      .primary(false)
                                                      .build()))
                           .build();

          Assertions.assertFalse(resourceComparator.equals(user1, user2));
        }

        /* ******************************************************************************************** */
      }

      /* ******************************************************************************************** */
    }
    /* ******************************************************************************************** */
  }
}

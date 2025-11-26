package de.captaingoldfish.scim.sdk.server.filter.resources;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.enums.Comparator;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidFilterException;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 20.10.2019 - 21:56 <br>
 * <br>
 */
@Slf4j
public class FilterResourceResolverTest implements FileReferences
{

  /**
   * needed to extract the {@link ResourceType}s which are necessary to check if the given
   * filter-attribute-names are valid or not
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the user resource type
   */
  private ResourceType userResourceType;

  /**
   * the resource type for all types definition. Contains data types of any possible scim representation
   */
  private ResourceType allTypesResourceType;

  /**
   * the service provider is used here to provide a thread pool for filtering
   */
  private ServiceProvider serviceProvider;

  /**
   * initializes a new {@link ResourceTypeFactory} for the following tests
   */
  @BeforeEach
  public void initialize()
  {
    this.resourceTypeFactory = new ResourceTypeFactory();
    JsonNode userResourceTypeJson = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    this.userResourceType = resourceTypeFactory.registerResourceType(null,
                                                                     userResourceTypeJson,
                                                                     userSchema,
                                                                     enterpriseUser);

    JsonNode allTypesResourceType = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    this.allTypesResourceType = resourceTypeFactory.registerResourceType(null,
                                                                         allTypesResourceType,
                                                                         allTypesSchema,
                                                                         enterpriseUser);
    serviceProvider = ServiceProvider.builder().forkJoinPool(new ForkJoinPool(6)).build();
  }

  /**
   * will test that the string comparison is working correctly on the resources for simple top level attributes.
   * In this case it is the attribute user
   */
  @TestFactory
  public List<DynamicTest> testStringComparison()
  {
    final List<User> userList = Arrays.asList(User.builder()
                                                  .userName("abc")
                                                  .name(Name.builder().givenName("abc").build())
                                                  .build(),
                                              User.builder()
                                                  .userName("bcd")
                                                  .name(Name.builder().givenName("bcd").build())
                                                  .build(),
                                              User.builder()
                                                  .userName("cde")
                                                  .name(Name.builder().givenName("cde").build())
                                                  .build());

    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getDynamicStringComparisonTest(userList, "bc", Comparator.LT, "abc"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "bcd", Comparator.LT, "abc"));

    dynamicTests.add(getDynamicStringComparisonTest(userList, "bc", Comparator.LE, "abc"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "bcd", Comparator.LE, "abc", "bcd"));

    dynamicTests.add(getDynamicStringComparisonTest(userList, "bc", Comparator.GT, "cde", "bcd"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "bcd", Comparator.GT, "cde"));

    dynamicTests.add(getDynamicStringComparisonTest(userList, "bc", Comparator.GE, "cde", "bcd"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "bcd", Comparator.GE, "cde", "bcd"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "bcde", Comparator.GE, "cde"));

    dynamicTests.add(getDynamicStringComparisonTest(userList, "abc", Comparator.EQ, "abc"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "bcd", Comparator.EQ, "bcd"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "cde", Comparator.EQ, "cde"));

    dynamicTests.add(getDynamicStringComparisonTest(userList, "abc", Comparator.NE, "cde", "bcd"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "bcd", Comparator.NE, "cde", "abc"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "cde", Comparator.NE, "bcd", "abc"));

    dynamicTests.add(getDynamicStringComparisonTest(userList, "e", Comparator.EW, "cde"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "d", Comparator.EW, "bcd"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "c", Comparator.EW, "abc"));

    dynamicTests.add(getDynamicStringComparisonTest(userList, "a", Comparator.SW, "abc"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "b", Comparator.SW, "bcd"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "c", Comparator.SW, "cde"));

    dynamicTests.add(getDynamicStringComparisonTest(userList, "a", Comparator.CO, "abc"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "b", Comparator.CO, "abc", "bcd"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "c", Comparator.CO, "abc", "bcd", "cde"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "d", Comparator.CO, "bcd", "cde"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "e", Comparator.CO, "cde"));
    dynamicTests.add(getDynamicStringComparisonTest(userList, "f", Comparator.CO));

    dynamicTests.add(getStringCompareUsernameTest(userList, true, "f", Comparator.CO, "abc", "bcd", "cde"));
    dynamicTests.add(getStringCompareUsernameTest(userList, true, "c", Comparator.CO));
    dynamicTests.add(getStringCompareUsernameTest(userList, true, "a", Comparator.SW, "bcd", "cde"));

    dynamicTests.add(getDynamicStringComparisonTest(userList, null, Comparator.PR, "abc", "bcd", "cde"));
    dynamicTests.add(getStringCompareUsernameTest(userList, true, null, Comparator.PR));
    return dynamicTests;
  }

  /**
   * builds a test that compares the username with the given value and comparator and verifies that the expected
   * results are correct
   */
  private DynamicTest getDynamicStringComparisonTest(List<User> userList,
                                                     String value,
                                                     Comparator comparator,
                                                     String... expectedResults)
  {
    return getStringCompareUsernameTest(userList, false, value, comparator, expectedResults);
  }

  /**
   * builds a test that compares the username with the given value and comparator and verifies that the expected
   * results are correct.
   */
  private DynamicTest getStringCompareUsernameTest(List<User> userList,
                                                   boolean useNot,
                                                   String value,
                                                   Comparator comparator,
                                                   String... expectedResults)
  {
    String filter = (useNot ? "not (" : "") + "userName " + comparator.name()
                    + (value == null ? "" : " \"" + value + "\"") + (useNot ? ")" : "");
    return DynamicTest.dynamicTest(filter, () -> {
      final FilterNode filterNode = RequestUtils.parseFilter(userResourceType, filter);
      List<User> filteredUsers = FilterResourceResolver.filterResources(serviceProvider, userList, filterNode);
      List<String> userNames = filteredUsers.stream()
                                            .map(user -> user.getUserName().orElse(null))
                                            .collect(Collectors.toList());
      Assertions.assertEquals(Optional.ofNullable(expectedResults).map(strings -> strings.length).orElse(0),
                              filteredUsers.size(),
                              "result list was: [" + String.join(",", userNames) + "]");
      MatcherAssert.assertThat(userNames, Matchers.hasItems(expectedResults));
    });
  }

  /**
   * verifies that filtering works correctly for simple complex types
   */
  @TestFactory
  public List<DynamicTest> testFilterOnSimpleComplexTypeForStringValues()
  {
    final List<User> userList = getUserList();
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getComplexTypeTest(userList, "name.givenName", "abc", Comparator.EQ, userList.get(0)));
    dynamicTests.add(getComplexTypeTest(userList, "name.givenName", "bcd", Comparator.EQ, userList.get(1)));
    dynamicTests.add(getComplexTypeTest(userList, "name.givenName", "cde", Comparator.EQ, userList.get(2)));

    dynamicTests.add(getComplexTypeTest(userList,
                                        "name.givenName",
                                        "abc",
                                        Comparator.NE,
                                        userList.get(1),
                                        userList.get(2)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "nAme.givenname",
                                        "bcd",
                                        Comparator.NE,
                                        userList.get(0),
                                        userList.get(2)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "nAme.givenName",
                                        "cde",
                                        Comparator.NE,
                                        userList.get(0),
                                        userList.get(1)));

    dynamicTests.add(getComplexTypeTest(userList,
                                        "name.givenname",
                                        "bcde",
                                        Comparator.LT,
                                        userList.get(0),
                                        userList.get(1)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "name.givenName",
                                        "cde",
                                        Comparator.LT,
                                        userList.get(0),
                                        userList.get(1)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "name.givenname",
                                        "bcd",
                                        Comparator.LE,
                                        userList.get(0),
                                        userList.get(1)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "nAme.givenName",
                                        "bcd",
                                        Comparator.GE,
                                        userList.get(1),
                                        userList.get(2)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "naMe.givenname",
                                        "bc",
                                        Comparator.GT,
                                        userList.get(1),
                                        userList.get(2)));
    dynamicTests.add(getComplexTypeTest(userList, "name.givenName", "c", Comparator.EW, userList.get(0)));
    dynamicTests.add(getComplexTypeTest(userList, "name.givenName", "c", Comparator.SW, userList.get(2)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "name.givenName",
                                        "c",
                                        Comparator.CO,
                                        userList.get(0),
                                        userList.get(1),
                                        userList.get(2)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "name.givenname",
                                        "b",
                                        Comparator.CO,
                                        userList.get(0),
                                        userList.get(1)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "namE.givenName",
                                        "d",
                                        Comparator.CO,
                                        userList.get(1),
                                        userList.get(2)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "Name.givenname",
                                        null,
                                        Comparator.PR,
                                        userList.get(0),
                                        userList.get(1),
                                        userList.get(2)));
    return dynamicTests;
  }

  /**
   * verifies that filtering works correctly for simple complex types
   */
  @TestFactory
  public List<DynamicTest> testFilterOnMultiValuedComplexTypeForStringAndBooleanValues()
  {
    final List<User> userList = getUserList();
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getComplexTypeTest(userList, "emails.value", "abc1@goldfish.de", Comparator.EQ, userList.get(0)));
    dynamicTests.add(getComplexTypeTest(userList, "emails.value", "bcd2@goldfish.de", Comparator.EQ, userList.get(1)));
    dynamicTests.add(getComplexTypeTest(userList, "emails.value", "bcd2@goldfish.de", Comparator.EQ, userList.get(1)));
    dynamicTests.add(getComplexTypeTest(userList, "emails.value", "bcd2@goldfish.de", Comparator.CO, userList.get(1)));
    dynamicTests.add(getComplexTypeTest(userList, "emails.value", "ä", Comparator.CO));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "emails.value",
                                        "@",
                                        Comparator.CO,
                                        userList.get(0),
                                        userList.get(1),
                                        userList.get(2)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "emails.value",
                                        "de",
                                        Comparator.EW,
                                        userList.get(0),
                                        userList.get(1),
                                        userList.get(2)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "emails.primary",
                                        "false",
                                        Comparator.EQ,
                                        userList.get(0),
                                        userList.get(1),
                                        userList.get(2)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "emails.primary",
                                        "true",
                                        Comparator.NE,
                                        userList.get(0),
                                        userList.get(1),
                                        userList.get(2)));
    dynamicTests.add(getComplexTypeTest(userList,
                                        "emails.primary",
                                        "true",
                                        Comparator.EQ,
                                        userList.get(0),
                                        userList.get(1)));
    return dynamicTests;
  }

  /**
   * this method will create serveral tests for number comparison on simple types array types complex simple
   * types, multi valued complex array-types and complex-array types
   */
  @TestFactory
  public List<DynamicTest> testAllTypesNumberComparison()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 50.5, Comparator.EQ, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 50.5, Comparator.NE));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 50.5, Comparator.LT));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 50.6, Comparator.LT, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 50.5, Comparator.LE, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 50.4, Comparator.LE));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 50.5, Comparator.GT));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 50.5, Comparator.GE, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 50.6, Comparator.GE));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 5, Comparator.SW, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 5, Comparator.EW, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 0, Comparator.EW));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 0, Comparator.CO, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 5, Comparator.CO, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", 6, Comparator.CO));
    dynamicTests.add(getAllTypesTest(allTypesList, "decimal", null, Comparator.PR, allTypesArray));

    dynamicTests.add(getAllTypesTest(allTypesList,
                                     "numberArray",
                                     55,
                                     Comparator.EQ,
                                     allTypesList.get(0),
                                     allTypesList.get(1),
                                     allTypesList.get(2)));
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", 55, Comparator.NE));
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", 55, Comparator.LT, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", 44, Comparator.LT));
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", 55, Comparator.LE, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", 55, Comparator.GT, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", 66, Comparator.GT));
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", 66, Comparator.GE, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", 5, Comparator.SW, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", 6, Comparator.EW, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", 4, Comparator.CO, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", null, Comparator.PR, allTypesArray));


    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999999999)),
                                     allTypesList,
                                     "decimalArray",
                                     88.99999999999999,
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999999999)),
                                     allTypesList,
                                     "decimalArray",
                                     88.99999999999999,
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999)),
                                     allTypesList,
                                     "decimalArray",
                                     99.999,
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999)),
                                     allTypesList,
                                     "decimalArray",
                                     99.999,
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999)),
                                     allTypesList,
                                     "decimalArray",
                                     10,
                                     Comparator.GT,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999)),
                                     allTypesList,
                                     "decimalArray",
                                     6.2,
                                     Comparator.GE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999)),
                                     allTypesList,
                                     "decimalArray",
                                     6.3,
                                     Comparator.GE,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999)),
                                     allTypesList,
                                     "decimalArray",
                                     88,
                                     Comparator.LT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999)),
                                     allTypesList,
                                     "decimalArray",
                                     88.99999999,
                                     Comparator.LT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999)),
                                     allTypesList,
                                     "decimalArray",
                                     6.3,
                                     Comparator.LE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999)),
                                     allTypesList,
                                     "decimalArray",
                                     9999,
                                     Comparator.CO,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999)),
                                     allTypesList,
                                     "decimalArray",
                                     88,
                                     Comparator.SW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDecimalArray(Arrays.asList(99.999, 88.99999999)),
                                     allTypesList,
                                     "decimalArray",
                                     999,
                                     Comparator.EW,
                                     allTypesArray[0]));


    dynamicTests.add(getAllTypesTest(allTypesList,
                                     "complex.numberArray",
                                     55,
                                     Comparator.EQ,
                                     allTypesList.get(0),
                                     allTypesList.get(1),
                                     allTypesList.get(2)));
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", 55, Comparator.NE));
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", 55, Comparator.LT, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", 44, Comparator.LT));
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", 55, Comparator.LE, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", 55, Comparator.GT, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", 66, Comparator.GT));
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", 66, Comparator.GE, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", 5, Comparator.SW, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", 6, Comparator.EW, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", 4, Comparator.CO, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", null, Comparator.PR, allTypesArray));

    Runnable exchangeComplexDecimalArray = () -> allTypesArray[0].getComplex()
                                                                 .get()
                                                                 .setDecimalArray(Arrays.asList(99.999,
                                                                                                88.99999999999999));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     88.99999999999999,
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     88.99999999999999,
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     99.999,
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     99.999,
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     10,
                                     Comparator.GT,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     6.2,
                                     Comparator.GE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     6.3,
                                     Comparator.GE,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     88,
                                     Comparator.LT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     88.99999999,
                                     Comparator.LT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     6.3,
                                     Comparator.LE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     9999,
                                     Comparator.CO,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     88,
                                     Comparator.SW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeComplexDecimalArray,
                                     allTypesList,
                                     "complex.decimalArray",
                                     999,
                                     Comparator.EW,
                                     allTypesArray[0]));

    Runnable exchangeMultiComplexDecimalArray = () -> allTypesArray[0].getMultiComplex().forEach(allType -> {
      allType.setDecimalArray(Arrays.asList(99.999, 88.99999999999999));
    });
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     88.99999999999999,
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     88.99999999999999,
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     99.999,
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     99.999,
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     10,
                                     Comparator.GT,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     6.2,
                                     Comparator.GE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     6.3,
                                     Comparator.GE,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     88,
                                     Comparator.LT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     88.99999999,
                                     Comparator.LT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     6.3,
                                     Comparator.LE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     9999,
                                     Comparator.CO,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     88,
                                     Comparator.SW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(exchangeMultiComplexDecimalArray,
                                     allTypesList,
                                     "multiComplex.decimalArray",
                                     999,
                                     Comparator.EW,
                                     allTypesArray[0]));
    return dynamicTests;
  }

  /**
   * will create several comparison tests for string on simple types, array-types, complex types complex
   * array-types and multi valued complex array-types
   */
  @TestFactory
  public List<DynamicTest> testAllTypesStringComparison()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);
    List<DynamicTest> dynamicTests = new ArrayList<>();
    Function<String, Runnable> changeString = s -> () -> allTypesArray[0].setString(s);
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "hello world",
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "chuck",
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "hello world",
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "world",
                                     Comparator.EW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "hello",
                                     Comparator.SW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "lo wo",
                                     Comparator.CO,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "uc",
                                     Comparator.CO,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "hello world",
                                     Comparator.LT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "chuck",
                                     Comparator.LT));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "chuck",
                                     Comparator.LE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "hello world",
                                     Comparator.LE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "chuck",
                                     Comparator.GT,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeString.apply("hello world"),
                                     allTypesList,
                                     "string",
                                     "chuck",
                                     Comparator.GE,
                                     allTypesArray));


    Function<String, Runnable> changeComplexString = s -> () -> allTypesArray[0].getComplex().get().setString(s);
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "hello world",
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "chuck",
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "hello world",
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "world",
                                     Comparator.EW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "hello",
                                     Comparator.SW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "lo wo",
                                     Comparator.CO,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "uc",
                                     Comparator.CO,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "hello world",
                                     Comparator.LT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "chuck",
                                     Comparator.LT));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "chuck",
                                     Comparator.LE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "hello world",
                                     Comparator.LE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "chuck",
                                     Comparator.GT,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeComplexString.apply("hello world"),
                                     allTypesList,
                                     "complex.string",
                                     "chuck",
                                     Comparator.GE,
                                     allTypesArray));


    Function<String, Runnable> changeMultiComplexString = s -> () -> allTypesArray[0].getMultiComplex()
                                                                                     .forEach(allType -> allType.setString(s));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "hello world",
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "chuck",
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "hello world",
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "world",
                                     Comparator.EW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "hello",
                                     Comparator.SW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "lo wo",
                                     Comparator.CO,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "uc",
                                     Comparator.CO,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "hello world",
                                     Comparator.LT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "chuck",
                                     Comparator.LT));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "chuck",
                                     Comparator.LE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "hello world",
                                     Comparator.LE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "chuck",
                                     Comparator.GT,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexString.apply("hello world"),
                                     allTypesList,
                                     "multiComplex.string",
                                     "chuck",
                                     Comparator.GE,
                                     allTypesArray));


    Function<String[], Runnable> changeMultiString = s -> () -> allTypesArray[0].setStringArray(Arrays.asList(s));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "mommy",
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "world",
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "world",
                                     Comparator.NE,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "hello",
                                     Comparator.NE));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "ld",
                                     Comparator.EW,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "hello",
                                     Comparator.SW,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "mom",
                                     Comparator.SW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "o",
                                     Comparator.CO,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "om",
                                     Comparator.CO,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "mommy",
                                     Comparator.LT,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "hello",
                                     Comparator.LT));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "hello",
                                     Comparator.LE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "mommy",
                                     Comparator.LE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "world",
                                     Comparator.GT));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "mommy",
                                     Comparator.GT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeMultiString.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "stringArray",
                                     "mommy",
                                     Comparator.GE,
                                     allTypesArray));


    Function<String[], Runnable> changeComplexStringArray = s -> () -> allTypesArray[0].getComplex()
                                                                                       .get()
                                                                                       .setStringArray(Arrays.asList(s));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "mommy",
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "world",
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "world",
                                     Comparator.NE,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "hello",
                                     Comparator.NE));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "ld",
                                     Comparator.EW,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "hello",
                                     Comparator.SW,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "mom",
                                     Comparator.SW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "o",
                                     Comparator.CO,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "om",
                                     Comparator.CO,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "mommy",
                                     Comparator.LT,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "hello",
                                     Comparator.LT));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "hello",
                                     Comparator.LE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "mommy",
                                     Comparator.LE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "world",
                                     Comparator.GT));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "mommy",
                                     Comparator.GT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "complex.stringArray",
                                     "mommy",
                                     Comparator.GE,
                                     allTypesArray));


    Function<String[], Runnable> changeMultiComplexStringArray = s -> () -> {
      allTypesArray[0].getMultiComplex()
                      .forEach(allType -> allType.setStringArray(s == null ? null : Arrays.asList(s)));
    };
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "mommy",
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "world",
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "world",
                                     Comparator.NE,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "hello",
                                     Comparator.NE));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "ld",
                                     Comparator.EW,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "hello",
                                     Comparator.SW,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "mom",
                                     Comparator.SW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "o",
                                     Comparator.CO,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "om",
                                     Comparator.CO,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "mommy",
                                     Comparator.LT,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "hello",
                                     Comparator.LT));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "hello",
                                     Comparator.LE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "mommy",
                                     Comparator.LE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "world",
                                     Comparator.GT));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "mommy",
                                     Comparator.GT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(changeMultiComplexStringArray.apply(new String[]{"hello", "mommy"}),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     "mommy",
                                     Comparator.GE,
                                     allTypesArray));

    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].getMultiComplex()
                                                           .forEach(allType -> allType.setStringArray(null)),
                                     allTypesList,
                                     "multiComplex.stringArray",
                                     null,
                                     Comparator.EQ,
                                     allTypesArray[0]));

    dynamicTests.add(getAllTypesTest(() -> {
      allTypesArray[0].getMultiComplex().forEach(allType -> allType.setStringArray(null));
      allTypesArray[1].getMultiComplex().forEach(allType -> allType.setStringArray(null));
      allTypesArray[2].getMultiComplex().forEach(allType -> allType.setStringArray(null));
    }, allTypesList, "multiComplex.stringArray", null, Comparator.EQ, allTypesArray));


    return dynamicTests;
  }


  @TestFactory
  public List<DynamicTest> testCompareDateTimeValues()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getAllTypesTest(allTypesList, "date", "1940-03-10T00:00:00Z", Comparator.EQ, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "date", "1940-03-10T00:00:00Z", Comparator.NE));

    ZoneOffset zoneOffSet = ZoneOffset.of("+01:00");
    OffsetDateTime offsetDateTime = OffsetDateTime.now(zoneOffSet);
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.toString()),
                                     allTypesList,
                                     "date",
                                     offsetDateTime.toString(),
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.toString()),
                                     allTypesList,
                                     "date",
                                     offsetDateTime.toInstant().toString(),
                                     Comparator.EQ,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.toString()),
                                     allTypesList,
                                     "date",
                                     offsetDateTime.toInstant().toString(),
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.toString()),
                                     allTypesList,
                                     "date",
                                     "1940-03-10T00:00:00Z",
                                     Comparator.GT,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.toString()),
                                     allTypesList,
                                     "date",
                                     "1940-03-10T00:00:00Z",
                                     Comparator.GE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.toString()),
                                     allTypesList,
                                     "date",
                                     offsetDateTime.toString(),
                                     Comparator.LT,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.toString()),
                                     allTypesList,
                                     "date",
                                     offsetDateTime.toString(),
                                     Comparator.LE,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.toString()),
                                     allTypesList,
                                     "date",
                                     String.valueOf(offsetDateTime.getYear()),
                                     Comparator.SW,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.toString()),
                                     allTypesList,
                                     "date",
                                     "1940",
                                     Comparator.SW,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.withNano(0).toString()),
                                     allTypesList,
                                     "date",
                                     "194",
                                     Comparator.CO,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.toString()),
                                     allTypesList,
                                     "date",
                                     String.valueOf(offsetDateTime.getYear()),
                                     Comparator.CO,
                                     allTypesArray[0]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate(offsetDateTime.toString()),
                                     allTypesList,
                                     "date",
                                     offsetDateTime.getOffset().toString(),
                                     Comparator.EW,
                                     allTypesArray[0]));

    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate((String)null),
                                     allTypesList,
                                     "date",
                                     null,
                                     Comparator.PR,
                                     allTypesArray[1],
                                     allTypesArray[2]));

    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate((String)null),
                                     allTypesList,
                                     "date",
                                     offsetDateTime.toString(),
                                     Comparator.EQ));

    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDate((String)null),
                                     allTypesList,
                                     "date",
                                     null,
                                     Comparator.EQ,
                                     allTypesArray[0]));

    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setDateArray(Arrays.asList(offsetDateTime.toString(),
                                                                                       null)),
                                     allTypesList,
                                     "dateArray",
                                     null,
                                     Comparator.EQ,
                                     allTypesArray[0]));
    return dynamicTests;
  }

  @TestFactory
  public List<DynamicTest> testCompareBooleanValues()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);
    List<DynamicTest> dynamicTests = new ArrayList<>();

    dynamicTests.add(getAllTypesTest(allTypesList, "bool", false, Comparator.EQ, allTypesArray));
    dynamicTests.add(getAllTypesTest(allTypesList, "bool", false, Comparator.NE));
    dynamicTests.add(getAllTypesTest(allTypesList, "bool", true, Comparator.EQ));
    dynamicTests.add(getAllTypesTest(allTypesList, "bool", true, Comparator.NE, allTypesArray));

    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setBoolArray(Arrays.asList(true, true)),
                                     allTypesList,
                                     "boolArray",
                                     true,
                                     Comparator.EQ,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setBoolArray(Arrays.asList(true, true)),
                                     allTypesList,
                                     "boolArray",
                                     true,
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setBoolArray(Arrays.asList(null, false)),
                                     allTypesList,
                                     "boolArray",
                                     true,
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setBoolArray(Arrays.asList(null, null)),
                                     allTypesList,
                                     "boolArray",
                                     true,
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setBoolArray(Arrays.asList(null, null, true)),
                                     allTypesList,
                                     "boolArray",
                                     true,
                                     Comparator.EQ,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setBoolArray(Arrays.asList(null, null, true)),
                                     allTypesList,
                                     "complex.boolArray",
                                     true,
                                     Comparator.EQ,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].getComplex().get().setBoolArray(Arrays.asList(true)),
                                     allTypesList,
                                     "complex.boolArray",
                                     false,
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].getComplex().get().setBoolArray(Arrays.asList(true)),
                                     allTypesList,
                                     "complex.boolArray",
                                     true,
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].getMultiComplex()
                                                           .forEach(allType -> allType.setBoolArray(Arrays.asList(true))),
                                     allTypesList,
                                     "multiComplex.boolArray",
                                     false,
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].getMultiComplex()
                                                           .forEach(allType -> allType.setBoolArray(Arrays.asList(true))),
                                     allTypesList,
                                     "multiComplex.boolArray",
                                     true,
                                     Comparator.NE,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setBoolArray(null),
                                     allTypesList,
                                     "boolArray",
                                     true,
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> allTypesArray[0].setBoolArray(null),
                                     allTypesList,
                                     "boolArray",
                                     false,
                                     Comparator.EQ,
                                     allTypesArray[1],
                                     allTypesArray[2]));
    dynamicTests.add(getAllTypesTest(() -> {
      allTypesArray[0].setBool(null);
      allTypesArray[1].setBool(false);
      allTypesArray[2].setBool(false);
    }, allTypesList, "bool", false, Comparator.EQ, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> {
      allTypesArray[0].setBool(null);
      allTypesArray[1].setBool(true);
      allTypesArray[2].setBool(true);
    }, allTypesList, "bool", true, Comparator.EQ, allTypesArray[1], allTypesArray[2]));

    dynamicTests.add(getAllTypesTest(() -> {
      allTypesArray[0].getComplex().get().setBool(null);
      allTypesArray[1].getComplex().get().setBool(false);
      allTypesArray[2].getComplex().get().setBool(false);
    }, allTypesList, "complex.bool", false, Comparator.EQ, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> {
      allTypesArray[0].getComplex().get().setBool(null);
      allTypesArray[1].getComplex().get().setBool(true);
      allTypesArray[2].getComplex().get().setBool(true);
    }, allTypesList, "complex.bool", true, Comparator.EQ, allTypesArray[1], allTypesArray[2]));

    dynamicTests.add(getAllTypesTest(() -> {
      allTypesArray[0].getMultiComplex().forEach(allType -> allType.setBool(null));
      allTypesArray[1].getMultiComplex().forEach(allType -> allType.setBool(false));
      allTypesArray[2].getMultiComplex().forEach(allType -> allType.setBool(false));
    }, allTypesList, "multiComplex.bool", false, Comparator.EQ, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> {
      allTypesArray[0].getMultiComplex().forEach(allType -> allType.setBool(null));
      allTypesArray[1].getMultiComplex().forEach(allType -> allType.setBool(true));
      allTypesArray[2].getMultiComplex().forEach(allType -> allType.setBool(true));
    }, allTypesList, "multiComplex.bool", true, Comparator.EQ, allTypesArray[1], allTypesArray[2]));

    return dynamicTests;
  }


  @TestFactory
  public List<DynamicTest> testIsComplexPr()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getAllTypesTest(allTypesList, "complex", null, Comparator.PR, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.setComplex(null)),
                                     allTypesList,
                                     "complex",
                                     null,
                                     Comparator.PR));
    return dynamicTests;
  }

  @TestFactory
  public List<DynamicTest> testIsArrayPr()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getAllTypesTest(allTypesList, "numberArray", null, Comparator.PR, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.setNumberArray(null)),
                                     allTypesList,
                                     "numberArray",
                                     null,
                                     Comparator.PR));

    dynamicTests.add(getAllTypesTest(allTypesList, "complex.numberArray", null, Comparator.PR, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.getComplex()
                                                                                  .get()
                                                                                  .setNumberArray(null)),
                                     allTypesList,
                                     "complex.numberArray",
                                     null,
                                     Comparator.PR));

    dynamicTests.add(getAllTypesTest(allTypesList, "multiComplex.numberArray", null, Comparator.PR, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.getMultiComplex()
                                                                                  .forEach(subType -> subType.setNumberArray(null))),
                                     allTypesList,
                                     "multiComplex.numberArray",
                                     null,
                                     Comparator.PR));
    return dynamicTests;
  }

  @TestFactory
  public List<DynamicTest> testIsComplexStringPr()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getAllTypesTest(allTypesList, "complex.string", null, Comparator.PR, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.getComplex().get().setString(null)),
                                     allTypesList,
                                     "complex.string",
                                     null,
                                     Comparator.PR));
    return dynamicTests;
  }

  @TestFactory
  public List<DynamicTest> testIsMultiComplexPr()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getAllTypesTest(allTypesList, "multiComplex", null, Comparator.PR, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.setMultiComplex(null)),
                                     allTypesList,
                                     "multiComplex",
                                     null,
                                     Comparator.PR));
    return dynamicTests;
  }

  @TestFactory
  public List<DynamicTest> testIsMultiComplexStringPr()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getAllTypesTest(allTypesList, "multiComplex.string", null, Comparator.PR, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.getMultiComplex()
                                                                                  .forEach(multi -> multi.setString(null))),
                                     allTypesList,
                                     "multiComplex.string",
                                     null,
                                     Comparator.PR));

    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.setMultiComplex(null)),
                                     allTypesList,
                                     "multiComplex.string",
                                     null,
                                     Comparator.PR));
    return dynamicTests;
  }

  @TestFactory
  public List<DynamicTest> testCompareBooleanPr()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);
    List<DynamicTest> dynamicTests = new ArrayList<>();

    dynamicTests.add(getAllTypesTest(allTypesList, "bool", null, Comparator.PR, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.setBool(null)),
                                     allTypesList,
                                     "bool",
                                     null,
                                     Comparator.PR,
                                     allTypesArray));

    dynamicTests.add(getAllTypesTest(allTypesList, "boolArray", null, Comparator.PR, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.setBoolArray(null)),
                                     allTypesList,
                                     "boolArray",
                                     null,
                                     Comparator.PR));

    dynamicTests.add(getAllTypesTest(allTypesList, "complex.bool", null, Comparator.PR, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.getComplex().get().setBool(null)),
                                     allTypesList,
                                     "complex.bool",
                                     null,
                                     Comparator.PR,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.setComplex(null)),
                                     allTypesList,
                                     "complex.bool",
                                     null,
                                     Comparator.PR,
                                     allTypesArray));

    dynamicTests.add(getAllTypesTest(allTypesList, "multiComplex.bool", null, Comparator.PR, allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.getMultiComplex()
                                                                                  .forEach(multi -> multi.setBool(null))),
                                     allTypesList,
                                     "multiComplex.bool",
                                     null,
                                     Comparator.PR,
                                     allTypesArray));
    dynamicTests.add(getAllTypesTest(() -> allTypesList.forEach(allType -> allType.setMultiComplex(null)),
                                     allTypesList,
                                     "multiComplex.bool",
                                     null,
                                     Comparator.PR,
                                     allTypesArray));
    return dynamicTests;
  }

  @Test
  public void testOrExpression()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);

    allTypesArray[0].setNumber(1L);
    allTypesArray[1].setNumber(2L);
    allTypesArray[2].setNumber(3L);

    final String filter = "number eq 1 or number eq 3";

    final FilterNode filterNode = RequestUtils.parseFilter(allTypesResourceType, filter);
    List<AllTypes> filteredAllTypes = FilterResourceResolver.filterResources(serviceProvider, allTypesList, filterNode);
    Assertions.assertEquals(2, filteredAllTypes.size());
    MatcherAssert.assertThat(filteredAllTypes, Matchers.hasItems(allTypesArray[0], allTypesArray[2]));
  }

  @Test
  public void testAndExpression()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);

    allTypesArray[0].setNumber(1L);
    allTypesArray[0].setString("hello");
    allTypesArray[1].setNumber(2L);
    allTypesArray[1].setString("world");
    allTypesArray[2].setNumber(3L);
    allTypesArray[2].setString("somewhere else");

    final String filter = "number eq 1 and string eq \"hello\" or number co 2 and string sw \"world\"";

    final FilterNode filterNode = RequestUtils.parseFilter(allTypesResourceType, filter);
    List<AllTypes> filteredAllTypes = FilterResourceResolver.filterResources(serviceProvider, allTypesList, filterNode);
    Assertions.assertEquals(2, filteredAllTypes.size());
    MatcherAssert.assertThat(filteredAllTypes, Matchers.hasItems(allTypesArray[0], allTypesArray[1]));
  }

  /**
   * verifies that bracket filters are also working
   */
  @Test
  public void testFilterWithBracketNotation()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);

    allTypesArray[0].getMultiComplex().get(0).setNumber(1L);
    allTypesArray[0].getMultiComplex().get(0).setString("hello");
    allTypesArray[1].getMultiComplex().get(0).setNumber(2L);
    allTypesArray[1].getMultiComplex().get(0).setString("world");
    allTypesArray[2].getMultiComplex().get(0).setNumber(3L);
    allTypesArray[2].getMultiComplex().get(0).setString("somewhere else");

    final String filter = "multicomplex[number eq 1 and string eq \"hello\" or number eq 2 and string eq \"world\"]";
    final FilterNode filterNode = RequestUtils.parseFilter(allTypesResourceType, filter);
    List<AllTypes> filteredAllTypes = FilterResourceResolver.filterResources(serviceProvider, allTypesList, filterNode);
    Assertions.assertEquals(2, filteredAllTypes.size());
    MatcherAssert.assertThat(filteredAllTypes, Matchers.hasItems(allTypesArray[0], allTypesArray[1]));
  }

  /**
   * verifies that the given filter expression is correctly resolved as expected by MsAzure
   */
  @Test
  public void testFilterWithMsAzureBracketNotation()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);

    allTypesArray[0].getMultiComplex().get(0).setNumber(1L);
    allTypesArray[0].getMultiComplex().get(0).setString("hello");
    allTypesArray[1].getMultiComplex().get(0).setNumber(2L);
    allTypesArray[1].getMultiComplex().get(0).setString("world");
    allTypesArray[2].getMultiComplex().get(0).setNumber(3L);
    allTypesArray[2].getMultiComplex().get(0).setString("somewhere else");

    final String filter = "multicomplex[number eq 1 and string eq \"hello\" or number eq 2].string eq \"world\"";
    final FilterNode filterNode = RequestUtils.parseFilter(allTypesResourceType, filter);
    List<AllTypes> filteredAllTypes = FilterResourceResolver.filterResources(serviceProvider, allTypesList, filterNode);
    Assertions.assertEquals(1, filteredAllTypes.size());
    MatcherAssert.assertThat(filteredAllTypes, Matchers.hasItems(allTypesArray[1]));
  }

  /**
   * verifies that bracket filters can be resolved as they have to be for patch expressions
   */
  @Test
  public void testFilterWithBracketNotationAndSubattributeSuffix()
  {
    AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};
    List<AllTypes> allTypesList = Arrays.asList(allTypesArray);

    allTypesArray[0].getMultiComplex().get(0).setNumber(1L);
    allTypesArray[0].getMultiComplex().get(0).setString("hello");
    allTypesArray[1].getMultiComplex().get(0).setNumber(2L);
    allTypesArray[1].getMultiComplex().get(0).setString("world");
    allTypesArray[2].getMultiComplex().get(0).setNumber(3L);
    allTypesArray[2].getMultiComplex().get(0).setString("somewhere else");

    final String subAttributeName = "decimal";
    final String filter = "multicomplex[number eq 1 and string eq \"hello\" or number eq 2 and string eq \"world\"]"
                          + "." + subAttributeName;
    final FilterNode filterNode = RequestUtils.parseFilter(allTypesResourceType, filter);
    Assertions.assertEquals(subAttributeName, filterNode.getSubAttributeName());
    List<AllTypes> filteredAllTypes = FilterResourceResolver.filterResources(serviceProvider, allTypesList, filterNode);
    Assertions.assertEquals(2, filteredAllTypes.size());
    MatcherAssert.assertThat(filteredAllTypes, Matchers.hasItems(allTypesArray[0], allTypesArray[1]));
  }

  /**
   * verifies that bracket filters can be resolved as they have to be for patch expressions
   */
  @ParameterizedTest
  @ValueSource(strings = {"date", "complex.stringArray", "CoMpLeX.NuMbEr", "complex[string eq \"chuck\"]",
                          "multicomplex[stringArray eq \"hello\"].number",
                          "urn:gold:params:scim:schemas:custom:2.0:AllTypes:date",
                          "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.stringarray",
                          "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex[string eq \"chuck\"]",
                          "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multicomplex[stringArray eq \"hello\"]"
                                                                                                           + ".number"})
  public void testValuePathFilteringForPatch(String path)
  {
    final AttributePathRoot filterNode = RequestUtils.parsePatchPath(allTypesResourceType, path);
    Assertions.assertNotNull(filterNode);
    Assertions.assertNotNull(filterNode.getSchemaAttribute());
    Assertions.assertNotNull(filterNode.getFullName());
  }

  /**
   * verifies that filtering works also on a simple attribute on the user enterprise extension
   */
  @Test
  public void testFilterOnEnterpriseUserWithEmployeeId()
  {
    final String employeeNumber1 = UUID.randomUUID().toString();
    final String employeeNumber2 = UUID.randomUUID().toString();
    final String employeeNumber3 = UUID.randomUUID().toString();

    List<User> userList = Arrays.asList(User.builder()
                                            .enterpriseUser(EnterpriseUser.builder()
                                                                          .employeeNumber(employeeNumber1)
                                                                          .build())
                                            .build(),
                                        User.builder()
                                            .enterpriseUser(EnterpriseUser.builder()
                                                                          .employeeNumber(employeeNumber2)
                                                                          .build())
                                            .build(),
                                        User.builder()
                                            .enterpriseUser(EnterpriseUser.builder()
                                                                          .employeeNumber(employeeNumber3)
                                                                          .build())
                                            .build());
    final String filter = String.format("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber eq \"%s\"",
                                        employeeNumber1);
    final FilterNode filterNode = RequestUtils.parseFilter(userResourceType, filter);
    List<User> filteredUsers = FilterResourceResolver.filterResources(serviceProvider, userList, filterNode);
    Assertions.assertEquals(1, filteredUsers.size(), filteredUsers.toString());
    Assertions.assertEquals(userList.get(0), filteredUsers.get(0), filteredUsers.toString());
  }

  /**
   * verifies that filtering works also on a complex attribute on the user enterprise extension
   */
  @Test
  public void testFilterOnEnterpriseUserWithManagerValue()
  {
    final String managerId1 = UUID.randomUUID().toString();
    final String managerId2 = UUID.randomUUID().toString();
    final String managerId3 = UUID.randomUUID().toString();

    List<User> userList = Arrays.asList(User.builder()
                                            .enterpriseUser(EnterpriseUser.builder()
                                                                          .manager(Manager.builder()
                                                                                          .value(managerId1)
                                                                                          .build())
                                                                          .build())
                                            .build(),
                                        User.builder()
                                            .enterpriseUser(EnterpriseUser.builder()
                                                                          .manager(Manager.builder()
                                                                                          .value(managerId2)
                                                                                          .build())
                                                                          .build())
                                            .build(),
                                        User.builder()
                                            .enterpriseUser(EnterpriseUser.builder()
                                                                          .manager(Manager.builder()
                                                                                          .value(managerId3)
                                                                                          .build())
                                                                          .build())
                                            .build());
    final String filter = String.format("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value eq \"%s\"",
                                        managerId1);
    final FilterNode filterNode = RequestUtils.parseFilter(userResourceType, filter);
    List<User> filteredUsers = FilterResourceResolver.filterResources(serviceProvider, userList, filterNode);
    Assertions.assertEquals(1, filteredUsers.size(), filteredUsers.toString());
    Assertions.assertEquals(userList.get(0), filteredUsers.get(0), filteredUsers.toString());
  }

  @Test
  public void testFilterOnMetaValue()
  {
    final String id1 = UUID.randomUUID().toString();
    final String id2 = UUID.randomUUID().toString();
    final String id3 = UUID.randomUUID().toString();
    final Instant instant = Instant.parse("2000-01-01T00:00:00.000Z");

    final List<User> userList = Arrays.asList(User.builder()
                                                  .id(id1)
                                                  .meta(Meta.builder()
                                                            .created(instant.minus(Duration.ofMillis(100)))
                                                            .build())
                                                  .build(),
                                              User.builder()
                                                  .id(id2)
                                                  .meta(Meta.builder().created(instant).build())
                                                  .build(),
                                              User.builder()
                                                  .id(id3)
                                                  .meta(Meta.builder()
                                                            .created(instant.plus(Duration.ofMillis(100)))
                                                            .build())
                                                  .build());


    final String filter = String.format("meta.created eq \"%s\"", DateTimeFormatter.ISO_INSTANT.format(instant));
    final FilterNode filterNode = RequestUtils.parseFilter(userResourceType, filter);
    List<User> filteredUsers = FilterResourceResolver.filterResources(serviceProvider, userList, filterNode);
    Assertions.assertEquals(1, filteredUsers.size(), filteredUsers.toString());
    Assertions.assertEquals(userList.get(1), filteredUsers.get(0), filteredUsers.toString());
  }

  /**
   * this test reflects the issue: https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/309
   */
  @ParameterizedTest
  @ValueSource(strings = {")", "]"})
  public void testFilterWithEnclosingBrace(String brace)
  {
    final List<User> userList = Arrays.asList(User.builder()
                                                  .id(UUID.randomUUID().toString())
                                                  .userName(String.format("test%s", brace))
                                                  .build());


    final String filter = String.format("userName eq \"test%s\"", brace);
    final FilterNode filterNode = RequestUtils.parseFilter(userResourceType, filter);
    List<User> filteredUsers = FilterResourceResolver.filterResources(serviceProvider, userList, filterNode);
    Assertions.assertEquals(1, filteredUsers.size());
  }

  /**
   * it is rather hard to define a scenario that can be used to filter reliably for binary data. Therefor we
   * will simply reject filter-expressions on binaries
   */
  @Test
  public void testBinaryFiltersAreRejected()
  {
    final String filter = "binary eq \"aGVsbG8gd29ybGQ=\"";
    InvalidFilterException ex = Assertions.assertThrows(InvalidFilterException.class,
                                                        () -> RequestUtils.parseFilter(allTypesResourceType, filter));
    Assertions.assertEquals("binary types like 'urn:gold:params:scim:schemas:custom:2.0:AllTypes:binary' "
                            + "are not suitable for filter expressions",
                            ex.getMessage());
  }

  /**
   * creates a test for the allTypes representation
   */
  private DynamicTest getAllTypesTest(List<AllTypes> allTypesList,
                                      String attributeName,
                                      Object value,
                                      Comparator comparator,
                                      AllTypes... expectedValues)
  {
    return getAllTypesTest(null, allTypesList, attributeName, value, comparator, expectedValues);
  }

  /**
   * creates a test for the allTypes representation
   */
  private DynamicTest getAllTypesTest(Runnable doBefore,
                                      List<AllTypes> allTypesList,
                                      String attributeName,
                                      Object value,
                                      Comparator comparator,
                                      AllTypes... expectedValues)
  {
    final String filter = attributeName + " " + comparator + toFilterStringValue(comparator, value);
    return DynamicTest.dynamicTest(filter, () -> {
      if (doBefore != null)
      {
        doBefore.run();
      }
      final FilterNode filterNode = RequestUtils.parseFilter(allTypesResourceType, filter);
      List<AllTypes> filteredAllTypes = FilterResourceResolver.filterResources(serviceProvider,
                                                                               allTypesList,
                                                                               filterNode);
      if (expectedValues == null)
      {
        MatcherAssert.assertThat(filteredAllTypes, Matchers.empty());
      }
      else
      {
        Assertions.assertEquals(expectedValues.length, filteredAllTypes.size(), filter + "\n" + filteredAllTypes);
        MatcherAssert.assertThat(filteredAllTypes, Matchers.hasItems(expectedValues));
      }
    });
  }

  /**
   * converts the given value into a filter value
   *
   * @param comparator if null should be added to the filter expression as string or an empty string
   * @param value the value that must be entered as a filter value
   * @return an empty a simple or a quoted string
   */
  private String toFilterStringValue(Comparator comparator, Object value)
  {
    if (value == null)
    {
      if (Comparator.PR.equals(comparator))
      {
        return "";
      }
      else
      {
        return " null";
      }
    }
    else if (Integer.class.isAssignableFrom(value.getClass()) || Double.class.isAssignableFrom(value.getClass())
             || Boolean.class.isAssignableFrom(value.getClass()))
    {
      return " " + value;
    }
    else
    {
      return " \"" + value + "\"";
    }
  }

  /**
   * builds a complex type test
   *
   * @param userList the list of users that should be filtered
   * @param attributeName the name of the attribute that is used to build the filter
   * @param value the comparison value of the filter
   * @param comparator the comparator expression for the filter
   * @param expectedValues the expected results
   */
  private DynamicTest getComplexTypeTest(List<User> userList,
                                         String attributeName,
                                         String value,
                                         Comparator comparator,
                                         User... expectedValues)
  {
    boolean isBoolean = Strings.CS.equals(value, "true") || Strings.CS.equals(value, "false");
    final String filter = attributeName + " " + comparator
                          + (value == null ? "" : (isBoolean ? " " + value : " \"" + value + "\""));
    return DynamicTest.dynamicTest(filter, () -> {
      final FilterNode filterNode = RequestUtils.parseFilter(userResourceType, filter);
      List<User> users = FilterResourceResolver.filterResources(serviceProvider, userList, filterNode);
      if (expectedValues == null)
      {
        MatcherAssert.assertThat(users, Matchers.empty());
      }
      else
      {
        Assertions.assertEquals(expectedValues.length, users.size(), users.toString());
        MatcherAssert.assertThat(users, Matchers.hasItems(expectedValues));
      }
    });
  }

  /**
   * builds a list of users that can be used for simple filter tests
   */
  protected List<User> getUserList()
  {
    List<Email> emailList1 = Arrays.asList(Email.builder().value("abc1@goldfish.de").primary(true).build(),
                                           Email.builder().value("abc2@goldfish.de").build(),
                                           Email.builder().value("abc3@goldfish.de").build());
    List<Email> emailList2 = Arrays.asList(Email.builder().value("bcd2@goldfish.de").build(),
                                           Email.builder().value("bcd3@goldfish.de").primary(true).build(),
                                           Email.builder().value("bcd4@goldfish.de").build());
    List<Email> emailList3 = Arrays.asList(Email.builder().value("cde3@goldfish.de").build(),
                                           Email.builder().value("cde4@goldfish.de").build(),
                                           Email.builder().value("cde5@goldfish.de").build());
    List<User> userList = Arrays.asList(User.builder()
                                            .userName("abc")
                                            .name(Name.builder().givenName("abc").build())
                                            .emails(emailList1)
                                            .build(),
                                        User.builder()
                                            .userName("bcd")
                                            .name(Name.builder().givenName("bcd").build())
                                            .emails(emailList2)
                                            .build(),
                                        User.builder()
                                            .userName("cde")
                                            .name(Name.builder().givenName("cde").build())
                                            .emails(emailList3)
                                            .build());
    Assertions.assertNotEquals(userList.get(0), userList.get(1));
    Assertions.assertNotEquals(userList.get(0), userList.get(2));
    Assertions.assertNotEquals(userList.get(1), userList.get(2));
    return userList;
  }

  @Nested
  class FailureTests
  {

    /**
     * this test is based on <a href=
     * "https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/650">https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/650</a>
     */
    @DisplayName("Illegal comparator in simple filter does not cause NullPointer")
    @Test
    public void testIllegalComparator()
    {
      AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                  JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                  JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};

      allTypesArray[0].setString("hello-world");
      allTypesArray[2].setString("hello-world");

      final String filter = "string eq1 \"hello-world\"";

      BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                       () -> RequestUtils.parseFilter(allTypesResourceType, filter));
      Assertions.assertEquals(String.format("Failed to parse patch-filter expression '%s'", filter), ex.getMessage());
    }

    /**
     * this test is based on <a href=
     * "https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/650">https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/650</a>
     */
    @DisplayName("Illegal comparator in complex filter does not cause NullPointer")
    @Test
    public void testIllegalComparator2()
    {
      AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                  JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                  JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};

      allTypesArray[0].setString("hello-world");
      allTypesArray[2].setString("hello-world");

      final String filter = "string eq \"hello-world\" or string eq1 \"hello-world\"";

      BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                       () -> RequestUtils.parseFilter(allTypesResourceType, filter));
      Assertions.assertEquals(String.format("Failed to parse patch-filter expression '%s'", filter), ex.getMessage());
    }

    /**
     * this test is based on <a href=
     * "https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/650">https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/650</a>
     */
    @DisplayName("Illegal comparator in simple bracket-filter does not cause Nullpointer")
    @Test
    public void testIllegalComparator3()
    {
      AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                  JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                  JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};

      AllTypes complex = new AllTypes();
      complex.setString("hello-world");
      allTypesArray[0].setComplex(complex);
      allTypesArray[2].setComplex(complex);

      final String filter = "complex[string eq1 \"hello-world\"]";

      InvalidFilterException ex = Assertions.assertThrows(InvalidFilterException.class,
                                                          () -> RequestUtils.parseFilter(allTypesResourceType, filter));
      Assertions.assertEquals("The specified filter syntax was invalid, or the specified attribute and "
                              + "filter comparison combination is not supported: mismatched input 'eq1' expecting "
                              + "{'[', ']', '.', OR, AND}",
                              ex.getMessage());
    }

    /**
     * this test is based on <a href=
     * "https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/650">https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/650</a>
     */
    @DisplayName("Illegal comparator in complex bracket-filter does not cause Nullpointer")
    @Test
    public void testIllegalComparator4()
    {
      AllTypes[] allTypesArray = {JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                  JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class),
                                  JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class)};

      AllTypes complex = new AllTypes();
      complex.setString("hello-world");
      allTypesArray[0].setComplex(complex);
      allTypesArray[2].setComplex(complex);

      final String filter = "complex[string eq \"hello-world\" or string eq1 \"hello-world\"]";

      InvalidFilterException ex = Assertions.assertThrows(InvalidFilterException.class,
                                                          () -> RequestUtils.parseFilter(allTypesResourceType, filter));
      Assertions.assertEquals("The specified filter syntax was invalid, or the specified attribute and "
                              + "filter comparison combination is not supported: mismatched input 'eq1' expecting "
                              + "{'[', ']', '.', OR, AND}",
                              ex.getMessage());
    }
  }
}

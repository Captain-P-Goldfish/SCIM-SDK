package de.gold.scim.filter.antlr;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.constants.enums.Type;
import de.gold.scim.exceptions.InvalidFilterException;
import de.gold.scim.filter.AndExpressionNode;
import de.gold.scim.filter.AttributeExpressionLeaf;
import de.gold.scim.filter.FilterNode;
import de.gold.scim.filter.OrExpressionNode;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.schemas.ResourceTypeFactoryUtil;
import de.gold.scim.utils.JsonHelper;
import de.gold.scim.utils.RequestUtils;
import de.gold.scim.utils.TestHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 15.10.2019 - 21:33 <br>
 * <br>
 */
@Slf4j
public class FilterVisitorTest
{

  /**
   * needed to extract the {@link de.gold.scim.schemas.ResourceType}s which are necessary to check if the given
   * filter-attribute-names are valid or not
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the user resource type
   */
  private ResourceType userResourceType;

  /**
   * initializes a new {@link ResourceTypeFactory} for the following tests
   */
  @BeforeEach
  public void initialize()
  {
    this.resourceTypeFactory = ResourceTypeFactoryUtil.getUnitTestResourceTypeFactory();
    JsonNode userResourceTypeJson = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    this.userResourceType = resourceTypeFactory.registerResourceType(null,
                                                                     userResourceTypeJson,
                                                                     userSchema,
                                                                     enterpriseUser);
  }

  /**
   * this test will verify that the following complex scim filter expressions will be parsed successfully
   */
  @ParameterizedTest
  @ValueSource(strings = {"userName co \"chu\"", "userName eq \"chuck\"", "userName eq \"5.5\"",
                          "userName eq \"chu\" or name.givenName eq \"Carlos\"",
                          "userName eq \"chu\" and name.givenName eq \"null\"",
                          "userName eq \"chu\" and not( name.givenName eq null )",
                          "((userName eq \"5.5\") and not( name.givenName eq \"Carlos\" OR nickName eq \"blubb\"))",
                          "((userName eQ \"chu\") and not( name.givenName eq \"-6\" or nickName eq \"true\"))",
                          "((userName eq \"false\") and not( name.givenName eq \"6\" or nickName eq \"true\"))",
                          "((userName pR) and not( name.givenName Pr and nickName pr))", "emails.primary eq true",
                          "((userName ne \"false\") and not( name.givenName co \"-6\" or nickName sw \"true\"))",
                          "((userName ew \"false\") and not( name.givenName gt \"6\" or nickName GE \"true\"))",
                          "((userName lt \"false\") and not( name.givenName le \"-6\" or nickName gt \"true\"))",
                          "meta.lastModified ge \"2019-10-17T01:07:00Z\"",
                          "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter eq \"chuck\"",
                          "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value eq \"chuck\"",
                          "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value eq \"5.5\""})
  public void testParseAllKindOfFilters(String expression)
  {
    Assertions.assertNotNull(RequestUtils.parseFilter(userResourceType, expression));
  }

  /**
   * this test will show that the an expression with a fully qualified URI attribute name for an extension is
   * resolved correctly
   */
  @Test
  public void testResolveExpressionWithSimpleUri()
  {
    final String uri = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";
    final String attributeName = "costCenter";
    final String operation = "eq";
    final String value = "CostCenter GmbH";
    final String expression = uri + ":" + attributeName + " " + operation + " \"" + value + "\"";

    FilterNode filterNode = RequestUtils.parseFilter(userResourceType, expression);
    MatcherAssert.assertThat(filterNode.getClass(), Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
    AttributeExpressionLeaf expressionLeaf = (AttributeExpressionLeaf)filterNode;
    Assertions.assertEquals(Type.STRING, expressionLeaf.getType());
    Assertions.assertEquals(attributeName, expressionLeaf.getShortName());
    Assertions.assertEquals(uri, expressionLeaf.getResourceUri());
    Assertions.assertEquals(uri + ":" + attributeName, expressionLeaf.getFullName());
    Assertions.assertEquals(value, expressionLeaf.getValue());
    Assertions.assertEquals(Comparator.EQ, expressionLeaf.getComparator());
  }

  /**
   * this test will verify that the expression tree will be built correctly on a complex expression if a
   * parenthesis is used
   */
  @Test
  public void testResolveParanthesisExpression()
  {
    final String expression = "userName eq \"false\" and (name.givenName PR OR nickName eq \"blubb\") and "
                              + "displayName co \"chuck\" or meta.created gt \"2019-10-17T01:07:00Z\"";
    FilterNode filterNode = RequestUtils.parseFilter(userResourceType, expression);
    log.warn(filterNode.toString());
    MatcherAssert.assertThat(filterNode.getClass(), Matchers.typeCompatibleWith(OrExpressionNode.class));

    OrExpressionNode orExpressionNode = (OrExpressionNode)filterNode;
    MatcherAssert.assertThat(orExpressionNode.getLeftNode().getClass(),
                             Matchers.typeCompatibleWith(AndExpressionNode.class));
    MatcherAssert.assertThat(orExpressionNode.getRightNode().getClass(),
                             Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));

    AndExpressionNode andExpressionNode = (AndExpressionNode)orExpressionNode.getLeftNode();
    validateAndExpressionNode:
    {
      MatcherAssert.assertThat(andExpressionNode.getLeftNode().getClass(),
                               Matchers.typeCompatibleWith(AndExpressionNode.class));
      MatcherAssert.assertThat(andExpressionNode.getRightNode().getClass(),
                               Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
    }

    AndExpressionNode secondLevelAndNodeLeft = (AndExpressionNode)andExpressionNode.getLeftNode();
    validateSecondLevelAndNode:
    {
      MatcherAssert.assertThat(secondLevelAndNodeLeft.getLeftNode().getClass(),
                               Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
      MatcherAssert.assertThat(secondLevelAndNodeLeft.getRightNode().getClass(),
                               Matchers.typeCompatibleWith(OrExpressionNode.class));
    }

    OrExpressionNode thirdLevelOrNodeRight = (OrExpressionNode)secondLevelAndNodeLeft.getRightNode();
    validateSecondLevelAndNode:
    {
      MatcherAssert.assertThat(thirdLevelOrNodeRight.getLeftNode().getClass(),
                               Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
      MatcherAssert.assertThat(thirdLevelOrNodeRight.getRightNode().getClass(),
                               Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
    }
  }

  /**
   * this test will verify that the expression tree will be built correctly on a complex expression if a
   * parenthesis is used
   */
  @Test
  public void testResolveParanthesisExpressionSimplerExpression()
  {
    final String expression = "userName eq \"false\" and (name.givenName PR OR nickName eq \"blubb\") and "
                              + "displayName co \"chuck\"";
    FilterNode filterNode = RequestUtils.parseFilter(userResourceType, expression);
    log.warn(filterNode.toString());
    MatcherAssert.assertThat(filterNode.getClass(), Matchers.typeCompatibleWith(AndExpressionNode.class));

    AndExpressionNode andExpressionNode = (AndExpressionNode)filterNode;
    MatcherAssert.assertThat(andExpressionNode.getLeftNode().getClass(),
                             Matchers.typeCompatibleWith(AndExpressionNode.class));
    MatcherAssert.assertThat(andExpressionNode.getRightNode().getClass(),
                             Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));

    AndExpressionNode leftNode = (AndExpressionNode)andExpressionNode.getLeftNode();
    validateLeftOrNode:
    {
      MatcherAssert.assertThat(leftNode.getLeftNode().getClass(),
                               Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
      MatcherAssert.assertThat(leftNode.getRightNode().getClass(), Matchers.typeCompatibleWith(OrExpressionNode.class));

      OrExpressionNode rightNode = (OrExpressionNode)leftNode.getRightNode();
      validateRightNode:
      {
        MatcherAssert.assertThat(rightNode.getLeftNode().getClass(),
                                 Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
        MatcherAssert.assertThat(rightNode.getRightNode().getClass(),
                                 Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
      }
    }
  }

  /**
   * this test will verify that the expression tree will be built correctly on a complex expression if no
   * parentheses are used
   */
  @Test
  public void testResolveNonParanthesisExpression()
  {
    final String expression = "userName eq \"false\" and name.givenName PR OR nickName eq \"blubb\" and "
                              + "displayName co \"chuck\"";
    FilterNode filterNode = RequestUtils.parseFilter(userResourceType, expression);
    log.warn(filterNode.toString());
    MatcherAssert.assertThat(filterNode.getClass(), Matchers.typeCompatibleWith(OrExpressionNode.class));

    OrExpressionNode orExpressionNode = (OrExpressionNode)filterNode;
    MatcherAssert.assertThat(orExpressionNode.getLeftNode().getClass(),
                             Matchers.typeCompatibleWith(AndExpressionNode.class));
    MatcherAssert.assertThat(orExpressionNode.getRightNode().getClass(),
                             Matchers.typeCompatibleWith(AndExpressionNode.class));

    AndExpressionNode leftNode = (AndExpressionNode)orExpressionNode.getLeftNode();
    validateLeftOrNode:
    {
      MatcherAssert.assertThat(leftNode.getLeftNode().getClass(),
                               Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
      MatcherAssert.assertThat(leftNode.getRightNode().getClass(),
                               Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
    }

    AndExpressionNode rightNode = (AndExpressionNode)orExpressionNode.getRightNode();
    validateRightOrNode:
    {
      MatcherAssert.assertThat(rightNode.getLeftNode().getClass(),
                               Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
      MatcherAssert.assertThat(rightNode.getRightNode().getClass(),
                               Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
    }
  }

  /**
   * this test will show that the meta attribute can be used in filter expressions eventhough it was not
   * declared in the schema definition of user
   */
  @Test
  public void testFilterForMetaCreated()
  {
    final String dateTime = "2019-10-17T01:07:00Z";
    final String expression = "meta.created gt \"" + dateTime + "\"";
    FilterNode filterNode = RequestUtils.parseFilter(userResourceType, expression);
    log.warn(filterNode.toString());
    MatcherAssert.assertThat(filterNode.getClass(), Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
    AttributeExpressionLeaf expressionLeaf = (AttributeExpressionLeaf)filterNode;
    Assertions.assertEquals(Type.DATE_TIME, expressionLeaf.getType());
    Assertions.assertEquals(dateTime, expressionLeaf.getDateTime().get().toString());
    Assertions.assertEquals(dateTime, expressionLeaf.getStringValue().get());
  }

  /**
   * this method will add an ambiguous attribute
   */
  @Test
  public void testAmbiguousNameInUserResourceAndExtension()
  {
    JsonNode userResourceTypeJson = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);

    final String ambiguousAttributeName = "nickName";
    this.userResourceType = TestHelper.addAttributeToSchema(resourceTypeFactory,
                                                            ambiguousAttributeName,
                                                            Type.STRING,
                                                            userResourceTypeJson,
                                                            userSchema,
                                                            enterpriseUser);

    final String filterExpression = ambiguousAttributeName + " eq \"Number 768324\"";
    try
    {
      RequestUtils.parseFilter(userResourceType, filterExpression);
      Assertions.fail();
    }
    catch (InvalidFilterException ex)
    {
      log.debug(ex.getMessage(), ex);
      MatcherAssert.assertThat(ex.getMessage(), Matchers.containsString(ambiguousAttributeName));
    }
  }

  /**
   * this test will add an ambiguous attribute to the enterprise user schema and will then use the fully
   * qualified uris on the attribute name in the filter expression. This test must succeed
   *
   * @param resourceUri the resource uri of the schemas
   */
  @ParameterizedTest
  @ValueSource(strings = {SchemaUris.USER_URI, SchemaUris.ENTERPRISE_USER_URI})
  public void testAmbiguousNameInUserResourceAndExtensionButFullUriWasUsed(String resourceUri)
  {
    JsonNode userResourceTypeJson = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);

    final String ambiguousAttributeName = "nickName";
    this.userResourceType = TestHelper.addAttributeToSchema(resourceTypeFactory,
                                                            ambiguousAttributeName,
                                                            Type.STRING,
                                                            userResourceTypeJson,
                                                            userSchema,
                                                            enterpriseUser);

    final String filterExpression = resourceUri + ":" + ambiguousAttributeName + " eq \"Number 768324\"";
    Assertions.assertDoesNotThrow(() -> RequestUtils.parseFilter(userResourceType, filterExpression));
  }

  /**
   * this test will add a new attribute to the enterprise user and will then use filter expressions on these
   * types to verify that these types do not cause exceptions
   *
   * @param type the attribute type
   * @param value the value for the filter expression
   */
  @ParameterizedTest
  @CsvSource({"INTEGER,6", "INTEGER,", "DECIMAL,5.5", "DECIMAL,", "BOOLEAN, true", "BOOLEAN, false", "BOOLEAN,",
              "STRING,", "STRING, \"hello world\"", "DATE_TIME,", "DATE_TIME,\"2019-10-17T01:07:00Z\""})
  public void testAttributeFilterExpressionWithNumber(Type type, String value)
  {
    JsonNode userResourceTypeJson = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);

    final String newAttributeName = "helloWorld";
    this.userResourceType = TestHelper.addAttributeToSchema(resourceTypeFactory,
                                                            newAttributeName,
                                                            type,
                                                            userResourceTypeJson,
                                                            userSchema,
                                                            enterpriseUser);
    final String filterExpression = newAttributeName + " eq " + (StringUtils.isBlank(value) ? "null" : value);
    FilterNode filterNode = Assertions.assertDoesNotThrow(() -> RequestUtils.parseFilter(userResourceType,
                                                                                         filterExpression));
    MatcherAssert.assertThat(filterNode.getClass(), Matchers.typeCompatibleWith(AttributeExpressionLeaf.class));
    AttributeExpressionLeaf leaf = (AttributeExpressionLeaf)filterNode;
    String val = value;
    if ((Type.STRING.equals(type) || Type.DATE_TIME.equals(type)) && val != null)
    {
      val = val.replaceFirst("^\"", "").replaceFirst("\"$", "");
    }
    Assertions.assertEquals(val, leaf.getValue());
    if (val == null)
    {
      return;
    }
    switch (type)
    {
      case INTEGER:
        Assertions.assertTrue(leaf.getIntegerValue().isPresent());
        Assertions.assertTrue(leaf.getDoubleValue().isPresent());
        Assertions.assertFalse(leaf.getStringValue().isPresent());
        Assertions.assertFalse(leaf.getBooleanValue().isPresent());
        Assertions.assertFalse(leaf.getDateTime().isPresent());
        break;
      case DECIMAL:
        Assertions.assertTrue(leaf.getDoubleValue().isPresent());
        Assertions.assertFalse(leaf.getIntegerValue().isPresent());
        Assertions.assertFalse(leaf.getStringValue().isPresent());
        Assertions.assertFalse(leaf.getBooleanValue().isPresent());
        Assertions.assertFalse(leaf.getDateTime().isPresent());
        break;
      case BOOLEAN:
        Assertions.assertTrue(leaf.getBooleanValue().isPresent());
        Assertions.assertFalse(leaf.getIntegerValue().isPresent());
        Assertions.assertFalse(leaf.getDoubleValue().isPresent());
        Assertions.assertFalse(leaf.getStringValue().isPresent());
        Assertions.assertFalse(leaf.getDateTime().isPresent());
        break;
      case DATE_TIME:
        Assertions.assertTrue(leaf.getDateTime().isPresent());
        Assertions.assertTrue(leaf.getStringValue().isPresent());
        Assertions.assertFalse(leaf.getBooleanValue().isPresent());
        Assertions.assertFalse(leaf.getIntegerValue().isPresent());
        Assertions.assertFalse(leaf.getDoubleValue().isPresent());
        break;
      case REFERENCE:
      case STRING:
        Assertions.assertTrue(leaf.getStringValue().isPresent());
        Assertions.assertFalse(leaf.getBooleanValue().isPresent());
        Assertions.assertFalse(leaf.getIntegerValue().isPresent());
        Assertions.assertFalse(leaf.getDoubleValue().isPresent());
        Assertions.assertFalse(leaf.getDateTime().isPresent());
        break;
    }
  }

  /**
   * uses an unknown attribute in a filter expression and verifies that an {@link InvalidFilterException} is
   * thrown
   */
  @Test
  public void testUseUnknownAttributeInFilter()
  {
    String filterExpression = "unknownAttribute eq 5.5";
    try
    {
      RequestUtils.parseFilter(userResourceType, filterExpression);
      Assertions.fail();
    }
    catch (InvalidFilterException ex)
    {
      log.debug(ex.getMessage(), ex);
    }
  }

  /**
   * this test builds just a long filter string wraps the whole string into a parenthesis and checks if the
   * build {@link FilterNode}s are equal
   */
  @Test
  public void testFilterNodesAreEqual()
  {
    final String filterExpression = "(id eq \"123456\" or externalId sw \"hello world\") and "
                                    + "(userName eq \"hello world\" or nickName eq \"hello world\") and displayName co "
                                    + "\"hello world\" and emails pr and not (emails.primary eq true)";
    final String paranthesisFilterExpression = "(" + filterExpression + ")";

    FilterNode firstTree = Assertions.assertDoesNotThrow(() -> RequestUtils.parseFilter(userResourceType,
                                                                                        filterExpression));
    FilterNode secondTree = Assertions.assertDoesNotThrow(() -> RequestUtils.parseFilter(userResourceType,
                                                                                         paranthesisFilterExpression));

    Assertions.assertEquals(firstTree, secondTree);
    log.debug("{}\n{}", firstTree.toString(), secondTree.toString());
  }

  /**
   * this test will show that an exception is thrown if an invalid date time value is given in the attribute
   * value
   */
  @Test
  public void testFilterForMetaCreatedWithInvalidValue()
  {
    final String expression = "meta.created gt \"2019-10-17T\"";
    Assertions.assertThrows(InvalidFilterException.class, () -> RequestUtils.parseFilter(userResourceType, expression));
  }

  /**
   * this test will assure that the filter language is case insensitive
   *
   * @param attributeName the name of the attribute
   * @param comparator the comparator name
   */
  @ParameterizedTest
  @CsvSource({"userName, EQ", "username,eq", "UsErNaMe,Eq", "uSeRnAmE,eQ"})
  public void testFilterLanguageIsCaseInsensitive(String attributeName, String comparator)
  {
    final String expression = attributeName + " " + comparator + " \"hello world\"";
    Assertions.assertDoesNotThrow(() -> RequestUtils.parseFilter(userResourceType, expression));
  }

  /**
   * asserts that the given comparator types will cause {@link InvalidFilterException}s if used on boolean type
   * attributes
   */
  @ParameterizedTest
  @ValueSource(strings = {"GE", "GT", "LE", "LT"})
  public void testIllegalComparatorOnBoolean(Comparator comparator)
  {
    try
    {
      RequestUtils.parseFilter(userResourceType, "emails.primary " + comparator + " true");
      Assertions.fail();
    }
    catch (InvalidFilterException ex)
    {
      log.debug(ex.getMessage(), ex);
    }
  }

  /**
   * verifies that an empty filter does not cause any exceptions
   */
  @ParameterizedTest
  @ValueSource(strings = {"", " ", "  "})
  public void testFilterIsEmpty(String filter)
  {
    Assertions.assertNull(RequestUtils.parseFilter(userResourceType, filter));
  }

}

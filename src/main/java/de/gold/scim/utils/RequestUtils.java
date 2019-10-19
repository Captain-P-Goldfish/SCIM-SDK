package de.gold.scim.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.StringUtils;

import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.InvalidFilterException;
import de.gold.scim.filter.FilterNode;
import de.gold.scim.filter.antlr.FilterAttributeName;
import de.gold.scim.filter.antlr.FilterRuleErrorListener;
import de.gold.scim.filter.antlr.FilterVisitor;
import de.gold.scim.filter.antlr.ScimFilterLexer;
import de.gold.scim.filter.antlr.ScimFilterParser;
import de.gold.scim.resources.ServiceProvider;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.Schema;
import de.gold.scim.schemas.SchemaAttribute;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 20:08 <br>
 * <br>
 * this class will add some helper methods that can be used to validate or modify request based attributes
 * based on the SCIM specification RFC7643 and RFC7644
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestUtils
{

  /**
   * this method will parse either the attributes parameter or the excludedAttributes parameter into a list. The
   * expected form of the attributes list is: form (e.g., userName, name, emails)
   *
   * @param attributes the comma separated string of scim attribute names
   * @return the list of attributes
   */
  public static List<String> getAttributes(String attributes)
  {
    if (StringUtils.isBlank(attributes))
    {
      return Collections.emptyList();
    }
    if (!attributes.matches("(^[a-zA-Z0-9][:a-zA-Z0-9.,]+[a-zA-Z0-9]$)*"))
    {
      String errorMessage = "the attributes or excludedAttributes parameter '" + attributes + "' is malformed please "
                            + "check your syntax and please note that whitespaces are not allowed.";
      throw new BadRequestException(errorMessage, null, null);
    }
    String[] attributeNameArray = attributes.split(",");
    return Arrays.asList(attributeNameArray);
  }

  /**
   * From RFC7644 chapter 3.9:<br>
   *
   * <pre>
   *     Clients MAY request a partial resource representation on any
   *     operation that returns a resource within the response by specifying
   *     either of the mutually exclusive URL query parameters "attributes" or
   *     "excludedAttributes"
   * </pre>
   *
   * so only one these parameters are allowed to be specified in a request
   *
   * @param attributes the required attributes that should be present in the response
   * @param excludedAttributes the attributes that should not be returned in the response
   */
  public static void validateAttributesAndExcludedAttributes(String attributes, String excludedAttributes)
  {
    if (StringUtils.isNotBlank(attributes) && StringUtils.isNotBlank(excludedAttributes))
    {
      final String errorMessage = "the attributes and excludedAttributes parameter must not be set at the same time:"
                                  + "\n\tattributes: '" + attributes + "'\n\texcludedAttributes: '" + excludedAttributes
                                  + "'";
      throw new BadRequestException(errorMessage, null, ScimType.Custom.INVALID_PARAMETERS);
    }
  }

  /**
   * parsed the filter of a list request
   *
   * @param resourceType the resource type that describes the endpoint on which the filter is used so that the
   *          filter expression can be correctly resolved
   * @param filter the filter expression that must apply to the given resource type
   * @return The parsed filter expression as resolvable tree structure that might be used to resolve them to jpa
   *         predicates for example
   */
  public static FilterNode parseFilter(ResourceType resourceType, String filter)
  {
    if (StringUtils.isBlank(filter))
    {
      return null;
    }
    FilterRuleErrorListener filterRuleErrorListener = new FilterRuleErrorListener();
    ScimFilterLexer lexer = new ScimFilterLexer(CharStreams.fromString(filter));
    lexer.removeErrorListeners();
    lexer.addErrorListener(filterRuleErrorListener);
    CommonTokenStream commonTokenStream = new CommonTokenStream(lexer);
    ScimFilterParser scimFilterParser = new ScimFilterParser(commonTokenStream);
    scimFilterParser.removeErrorListeners();
    scimFilterParser.addErrorListener(filterRuleErrorListener);
    ScimFilterParser.FilterContext filterContext = scimFilterParser.filter();
    FilterVisitor filterVisitor = new FilterVisitor(resourceType);
    return filterVisitor.visit(filterContext);
  }

  /**
   * The 1-based index of the first query result. A value less than 1 SHALL be interpreted as 1.
   *
   * @param startIndex the index to start with to list the resources
   * @return number "1" or greater
   */
  public static int getEffectiveStartIndex(Integer startIndex)
  {
    if (startIndex == null || startIndex < 1)
    {
      return 1;
    }
    return startIndex;
  }

  /**
   * Will get the effective count value as described in RFC7644:<br>
   * <br>
   * Non-negative integer. Specifies the desired maximum number of query results per page, e.g., 10. A negative
   * value SHALL be interpreted as "0". A value of "0" indicates that no resource results are to be returned
   * except for "totalResults". <br>
   * <b>DEFAULT:</b> None<br>
   * When specified, the service provider MUST NOT return more results than specified, although it MAY return
   * fewer results. If unspecified, the maximum number of results is set by the service provider.
   */
  public static int getEffectiveCount(ServiceProvider serviceProvider, Integer count)
  {
    if (count == null)
    {
      return serviceProvider.getFilterConfig().getMaxResults();
    }
    if (count < 0)
    {
      return 0;
    }
    return Math.min(count, serviceProvider.getFilterConfig().getMaxResults());
  }

  /**
   * gets the {@link SchemaAttribute} from the given {@link ResourceType}
   *
   * @param resourceType the resource type from which the attribute definition should be extracted
   * @param sortBy this instance holds the attribute name to extract the {@link SchemaAttribute} from the
   *          {@link ResourceType}
   * @return the found {@link SchemaAttribute} definition
   * @throws de.gold.scim.exceptions.BadRequestException if no {@link SchemaAttribute} was found for the given
   *           name attribute
   */
  public static SchemaAttribute getSchemaAttributeForSortBy(ResourceType resourceType, String sortBy)
  {
    try
    {
      return StringUtils.isBlank(sortBy) ? null : getSchemaAttribute(resourceType, new FilterAttributeName(sortBy));
    }
    catch (BadRequestException ex)
    {
      ex.setScimType(ScimType.Custom.INVALID_PARAMETERS);
      throw ex;
    }
  }

  /**
   * gets the {@link SchemaAttribute} from the given {@link ResourceType}
   *
   * @param resourceType the resource type from which the attribute definition should be extracted
   * @param attributeName this instance holds the attribute name to extract the {@link SchemaAttribute} from the
   *          {@link ResourceType}
   * @return the found {@link SchemaAttribute} definition
   * @throws de.gold.scim.exceptions.InvalidFilterException if no {@link SchemaAttribute} was found for the
   *           given name attribute
   */
  public static SchemaAttribute getSchemaAttributeForFilter(ResourceType resourceType,
                                                            FilterAttributeName attributeName)
  {
    try
    {
      return getSchemaAttribute(resourceType, attributeName);
    }
    catch (BadRequestException ex)
    {
      throw new InvalidFilterException(ex.getMessage(), ex);
    }
  }

  /**
   * gets the {@link SchemaAttribute} from the given {@link ResourceType}
   *
   * @param resourceType the resource type from which the attribute definition should be extracted
   * @param attributeName this instance holds the attribute name to extract the {@link SchemaAttribute} from the
   *          {@link ResourceType}
   * @return the found {@link SchemaAttribute} definition
   * @throws de.gold.scim.exceptions.BadRequestException if no {@link SchemaAttribute} was found for the given
   *           name attribute
   */
  private static SchemaAttribute getSchemaAttribute(ResourceType resourceType, FilterAttributeName attributeName)
  {
    if (attributeName == null)
    {
      return null;
    }
    final boolean resourceUriPresent = StringUtils.isNotBlank(attributeName.getResourceUri());
    final String scimNodeName = attributeName.getShortName();
    List<Schema> resourceTypeSchemas = resourceType.getAllSchemas();

    List<SchemaAttribute> schemaAttributeList;
    if (resourceUriPresent)
    {
      schemaAttributeList = resourceTypeSchemas.stream()
                                               .filter(schema -> schema.getId().equals(attributeName.getResourceUri()))
                                               .map(schema -> schema.getSchemaAttribute(scimNodeName))
                                               .filter(Objects::nonNull)
                                               .collect(Collectors.toList());
    }
    else
    {
      schemaAttributeList = resourceTypeSchemas.stream()
                                               .map(schema -> schema.getSchemaAttribute(scimNodeName))
                                               .filter(Objects::nonNull)
                                               .collect(Collectors.toList());
    }
    if (schemaAttributeList.isEmpty())
    {
      throw new BadRequestException("the attribute with the name '" + attributeName.getShortName() + "' is "
                                    + "unknown to resource type '" + resourceType.getName() + "'", null, null);
    }
    else if (schemaAttributeList.size() > 1)
    {
      String schemaIds = schemaAttributeList.stream()
                                            .map(schemaAttribute -> schemaAttribute.getSchema().getId())
                                            .collect(Collectors.joining(","));
      String exampleAttributeName = schemaAttributeList.get(0).getSchema().getId() + ":" + attributeName.getShortName();
      throw new BadRequestException("the attribute with the name '" + attributeName.getShortName() + "' is "
                                    + "ambiguous it was found in the schemas with the ids [" + schemaIds + "]. "
                                    + "Please use the fully qualified Uri for this attribute e.g.: "
                                    + exampleAttributeName, null, null);
    }
    else
    {
      return schemaAttributeList.get(0);
    }
  }
}

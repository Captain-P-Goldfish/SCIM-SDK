package de.captaingoldfish.scim.sdk.server.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidFilterException;
import de.captaingoldfish.scim.sdk.common.request.BulkRequest;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.EncodingUtils;
import de.captaingoldfish.scim.sdk.server.exceptions.UnparseableFilterException;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.filter.antlr.FilterAttributeName;
import de.captaingoldfish.scim.sdk.server.filter.antlr.FilterRuleErrorListener;
import de.captaingoldfish.scim.sdk.server.filter.antlr.FilterVisitor;
import de.captaingoldfish.scim.sdk.server.filter.antlr.ScimFilterLexer;
import de.captaingoldfish.scim.sdk.server.filter.antlr.ScimFilterParser;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
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
   * this method will parse either the attributes parameter or the excludedAttributes parameter into a list of
   * {@link SchemaAttribute}s. The expected form of the attributes list is: form (e.g., userName, name, emails)
   *
   * @param attributes the comma separated string of scim attribute names
   * @return the list of attributes
   */
  public static List<SchemaAttribute> getAttributes(ResourceType resourceType, String attributes)
  {
    List<String> attributeNameArray = getAttributeList(attributes).map(Arrays::asList)
                                                                  .map(ArrayList::new)
                                                                  .orElse(new ArrayList<>());
    // this if-block is used in cases if the client might try to use the schema-reference uris as attribute-names
    List<SchemaAttribute> attributesList = new ArrayList<>();

    {
      List<String> schemaUris = new ArrayList<>();
      List<Schema> schemas = resourceType.getAllSchemas();
      for ( String attributeName : attributeNameArray )
      {
        for ( Schema schema : schemas )
        {
          if (schema.getNonNullId().equals(attributeName))
          {
            attributesList.addAll(schema.getAttributes());
            schemaUris.add(schema.getNonNullId());
            break;
          }
        }
      }
      attributeNameArray.removeAll(schemaUris);
    }

    return Stream.concat(attributesList.stream(),
                         attributeNameArray.stream().map(s -> getSchemaAttributeByAttributeName(resourceType, s)))
                 .collect(Collectors.toList());
  }

  /**
   * parses the given attributes into an array of strings
   *
   * @param attributes the attributes request parameter that is expected to be a comma separated string
   * @return the array of the separated attribute names or an empty
   */
  private static Optional<String[]> getAttributeList(String attributes)
  {
    if (StringUtils.isBlank(attributes))
    {
      return Optional.empty();
    }
    if (!attributes.matches("(^[a-zA-Z0-9$]([:a-zA-Z0-9.,$]+)?[a-zA-Z0-9$]$)*"))
    {
      String errorMessage = "the attributes or excludedAttributes parameter '" + attributes + "' is malformed please "
                            + "check your syntax and please note that whitespaces are not allowed.";
      throw new BadRequestException(errorMessage, null, null);
    }
    return Optional.of(attributes.split(","));
  }

  /**
   * parses the filter of a list request
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
    FilterVisitor filterVisitor = new FilterVisitor(resourceType, false);
    FilterNode filterNode;
    try
    {
      filterNode = filterVisitor.visit(filterContext);
    }
    catch (UnparseableFilterException ex)
    {
      throw new BadRequestException(String.format("Failed to parse patch-filter expression '%s'", filter), ex);
    }
    return filterNode;
  }

  /**
   * parses a value path context for patch path expressions
   *
   * @param resourceType the resource type that describes the endpoint on which the path expression is used
   * @param path the path expression that must apply to the given resource type
   * @return The parsed path expression as resolvable tree structure to find matching attributes within a single
   *         resource
   */
  public static AttributePathRoot parsePatchPath(ResourceType resourceType, String path)
  {
    if (StringUtils.isBlank(path))
    {
      return null;
    }
    FilterRuleErrorListener filterRuleErrorListener = new FilterRuleErrorListener();
    ScimFilterLexer lexer = new ScimFilterLexer(CharStreams.fromString(path));
    lexer.removeErrorListeners();
    lexer.addErrorListener(filterRuleErrorListener);
    CommonTokenStream commonTokenStream = new CommonTokenStream(lexer);
    ScimFilterParser scimFilterParser = new ScimFilterParser(commonTokenStream);
    scimFilterParser.removeErrorListeners();
    scimFilterParser.addErrorListener(filterRuleErrorListener);
    ScimFilterParser.ValuePathContext valuePathContext = scimFilterParser.valuePath();
    FilterVisitor filterVisitor = new FilterVisitor(resourceType, true);
    FilterNode filterNode = filterVisitor.visit(valuePathContext);
    if (filterNode == null || !AttributePathRoot.class.isAssignableFrom(filterNode.getClass()))
    {
      throw new BadRequestException("the path expression is invalid and not supported for patch operations: '" + path
                                    + "'", null, ScimType.RFC7644.INVALID_PATH);
    }
    AttributePathRoot attributePathRoot = (AttributePathRoot)filterNode;
    attributePathRoot.setOriginalExpressionString(path);
    return attributePathRoot;
  }

  /**
   * tries to parse the incoming startIndex value as long number
   *
   * @param countQueryParameter the query parameter that should be a number
   * @return the parsed startIndex value or an empty if the parameter is missing
   */
  public static Optional<Long> parseStartIndex(String startIndexQueryParameter)
  {
    if (StringUtils.isEmpty(startIndexQueryParameter))
    {
      return Optional.empty();
    }
    try
    {
      return Optional.of(Long.parseLong(startIndexQueryParameter));
    }
    catch (NumberFormatException ex)
    {
      throw new BadRequestException(String.format("Got invalid startIndex value '%s'. StartIndex must be a number",
                                                  startIndexQueryParameter));
    }
  }

  /**
   * tries to parse the incoming count value as integer number
   *
   * @param countQueryParameter the query parameter that should be a number
   * @return the parsed count value or an empty if the parameter is missing
   */
  public static Optional<Integer> parseCount(String countQueryParameter)
  {
    if (StringUtils.isEmpty(countQueryParameter))
    {
      return Optional.empty();
    }
    try
    {
      return Optional.of(Integer.parseInt(countQueryParameter));
    }
    catch (NumberFormatException ex)
    {
      throw new BadRequestException(String.format("Got invalid count value '%s'. Count must be a number",
                                                  countQueryParameter));
    }
  }

  /**
   * The 1-based index of the first query result. A value less than 1 SHALL be interpreted as 1.
   *
   * @param startIndex the index to start with to list the resources
   * @return number "1" or greater
   */
  public static long getEffectiveStartIndex(Long startIndex)
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
   * @param attributeName this instance holds the attribute name to extract the {@link SchemaAttribute} from the
   *          {@link ResourceType}
   * @return the found {@link SchemaAttribute} definition
   * @throws BadRequestException if no {@link SchemaAttribute} was found for the given name attribute
   */
  public static SchemaAttribute getSchemaAttributeByAttributeName(ResourceType resourceType, String attributeName)
  {
    try
    {
      return StringUtils.isBlank(attributeName) ? null
        : getSchemaAttribute(resourceType, new FilterAttributeName(attributeName));
    }
    catch (BadRequestException ex)
    {
      ex.setScimType(ScimType.RFC7644.INVALID_PATH);
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
   * @throws InvalidFilterException if no {@link SchemaAttribute} was found for the given name attribute
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
   * @throws BadRequestException if no {@link SchemaAttribute} was found for the given name attribute
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
                                               .filter(schema -> attributeName.getResourceUri()
                                                                              .equals(schema.getId().orElse(null)))
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
      String[] parts = scimNodeName.split("\\.");
      if (parts.length == 2 && AttributeNames.RFC7643.VALUE.equals(parts[1]))
      {
        schemaAttributeList = resourceTypeSchemas.stream()
                                                 .map(schema -> schema.getSchemaAttribute(parts[0]))
                                                 .filter(Objects::nonNull)
                                                 .collect(Collectors.toList());
        if (schemaAttributeList.size() == 1)
        {
          SchemaAttribute schemaAttribute = schemaAttributeList.get(0);
          if (schemaAttribute.isMultiValued() && !Type.COMPLEX.equals(schemaAttribute.getType()))
          {
            return schemaAttribute;
          }
        }
      }
      throw new BadRequestException(String.format("Attribute '%s' is unknown to resource type '%s'",
                                                  attributeName.getFullName(),
                                                  resourceType.getName()),
                                    null, ScimType.RFC7644.INVALID_PATH);
    }
    else if (schemaAttributeList.size() > 1)
    {
      String schemaIds = schemaAttributeList.stream()
                                            .map(schemaAttribute -> schemaAttribute.getSchema().getId().orElse(null))
                                            .collect(Collectors.joining(","));
      String exampleAttributeName = schemaAttributeList.get(0).getSchema().getId().orElse(null) + ":"
                                    + attributeName.getShortName();
      throw new BadRequestException("The attribute with the name '" + attributeName.getShortName() + "' is "
                                    + "ambiguous it was found in the schemas with the ids [" + schemaIds + "]. "
                                    + "Please use the fully qualified Uri for this attribute e.g.: "
                                    + exampleAttributeName, null, null);
    }
    else
    {
      return schemaAttributeList.get(0);
    }
  }

  /**
   * gets the query parameter from the given URL
   *
   * @param query the query string
   * @return the query parameters as a map
   */
  public static Map<String, String> getQueryParameters(String query)
  {
    Map<String, String> queryParameter = new HashMap<>();
    if (StringUtils.isBlank(query))
    {
      return Collections.emptyMap();
    }
    String[] pairs = query.split("&");
    for ( String pair : pairs )
    {
      if (StringUtils.isBlank(pair) || pair.charAt(0) == '=')
      {
        continue;
      }
      int index = pair.indexOf("=");
      String param;
      String value;
      if (index == -1)
      {
        param = pair;
        value = "";
      }
      else
      {
        param = pair.substring(0, index);
        value = pair.substring(index + 1);
      }
      queryParameter.put(EncodingUtils.urlDecode(param.toLowerCase()), EncodingUtils.urlDecode(value));

    }
    return queryParameter;
  }

  /**
   * will check the failOnErrors attribute in a bulk request and return a sanitized value.<br>
   * <br>
   * RFC7644 chapter 3.7.3 defines the minimum value of failOnErrors as 1
   *
   * <pre>
   *   The "failOnErrors" attribute is set to '1', indicating that the
   *   service provider will stop processing and return results after one
   *   error
   * </pre>
   *
   * @param bulkRequest the bulk request
   * @return a failOnErrors value that has been validated and sanitized
   */
  public static int getEffectiveFailOnErrors(BulkRequest bulkRequest)
  {
    Integer failOnErrors = bulkRequest.getFailOnErrors().orElse(null);
    if (failOnErrors == null)
    {
      return Integer.MAX_VALUE;
    }
    if (failOnErrors < 1)
    {
      return 1;
    }
    return failOnErrors;
  }
}

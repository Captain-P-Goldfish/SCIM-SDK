package de.captaingoldfish.scim.sdk.server.filter.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Comparator;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.utils.TimeUtils;
import de.captaingoldfish.scim.sdk.server.filter.AndExpressionNode;
import de.captaingoldfish.scim.sdk.server.filter.AttributeExpressionLeaf;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.filter.NotExpressionNode;
import de.captaingoldfish.scim.sdk.server.filter.OrExpressionNode;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 20.10.2019 - 20:17 <br>
 * <br>
 * This class is used to use a {@link FilterNode} tree on a resource list to filter all resources that match
 * the given filter expression
 */
@Slf4j
public class FilterResourceResolver
{

  /**
   * filters the given resources based on the filternode
   *
   * @param resources the resources that must be filtered
   * @param filterNode the filter node that holds the information how the resources should be filtered
   * @param <T> a {@link ResourceNode} type
   * @return the filtered resources
   */
  public static <T extends ResourceNode> List<T> filterResources(List<T> resources, FilterNode filterNode)
  {
    return resources.parallelStream().filter(getResourcePredicate(filterNode)).collect(Collectors.toList());
  }

  /**
   * creates a predicate that tells us if the given resource does match the filter or not
   *
   * @param filterNode the filter expression
   * @return the predicate that tells us if evaluated if the resource does match the filter or not
   */
  private static Predicate<ResourceNode> getResourcePredicate(FilterNode filterNode)
  {
    return resourceNode -> isResourceMatchingFilter(resourceNode, filterNode);
  }

  /**
   * checks the current filter node and evaluates it or does a recursive call to itself
   *
   * @param resourceNode the resource node to evaluate
   * @param filterNode the filternode to which the resource must match
   * @return true if the resource node does match to the filter
   */
  private static boolean isResourceMatchingFilter(ResourceNode resourceNode, FilterNode filterNode)
  {
    if (AndExpressionNode.class.isAssignableFrom(filterNode.getClass()))
    {
      AndExpressionNode andExpressionNode = (AndExpressionNode)filterNode;
      return isResourceMatchingFilter(resourceNode, andExpressionNode.getLeftNode())
             && isResourceMatchingFilter(resourceNode, andExpressionNode.getRightNode());
    }
    else if (OrExpressionNode.class.isAssignableFrom(filterNode.getClass()))
    {
      OrExpressionNode orExpressionNode = (OrExpressionNode)filterNode;
      return isResourceMatchingFilter(resourceNode, orExpressionNode.getLeftNode())
             || isResourceMatchingFilter(resourceNode, orExpressionNode.getRightNode());
    }
    else if (NotExpressionNode.class.isAssignableFrom(filterNode.getClass()))
    {
      NotExpressionNode notExpressionNode = (NotExpressionNode)filterNode;
      return !isResourceMatchingFilter(resourceNode, notExpressionNode.getRightNode());
    }
    else if (AttributeExpressionLeaf.class.isAssignableFrom(filterNode.getClass()))
    {
      return visitAttributeExpressionLeaf(resourceNode, (AttributeExpressionLeaf)filterNode);
    }
    else if (AttributePathRoot.class.isAssignableFrom(filterNode.getClass()))
    {
      AttributePathRoot attributePathRoot = (AttributePathRoot)filterNode;
      return isResourceMatchingFilter(resourceNode, attributePathRoot.getChild());
    }
    return false;
  }

  /**
   * evaluates a leaf node so a direct expression that must be evaluated
   *
   * @param resourceNode the resource node that must evaluate to the expression leaf
   * @param attributeExpressionLeaf the leaf node that holds the expressions information
   * @return true if the expression matches, false else
   */
  private static boolean visitAttributeExpressionLeaf(ResourceNode resourceNode,
                                                      AttributeExpressionLeaf attributeExpressionLeaf)
  {
    String[] nameParts = attributeExpressionLeaf.getShortName().split("\\.");
    if (nameParts.length == 1)
    {
      JsonNode simpleAttribute = retrieveSimpleAttributeNode(resourceNode, attributeExpressionLeaf);
      return checkValueEquality(simpleAttribute, attributeExpressionLeaf);
    }
    else
    {
      JsonNode complexNode = retrieveComplexAttributeNode(resourceNode, attributeExpressionLeaf);
      if (complexNode != null && complexNode.isArray())
      {
        return evaluateMultiComplexNode(complexNode, attributeExpressionLeaf);
      }
      else
      {
        return evaluateComplexNode(complexNode, attributeExpressionLeaf);
      }
    }
  }

  /**
   * retrieves the attribute from either the main schema or an schema extension
   * 
   * @param jsonNode the json document from which the attribute should be extracted
   * @param attributeExpressionLeaf the filter expression node that describes the node that should be extracted
   * @return the extracted json node
   */
  private static JsonNode retrieveSimpleAttributeNode(JsonNode jsonNode,
                                                      AttributeExpressionLeaf attributeExpressionLeaf)
  {
    if (attributeExpressionLeaf.isMainSchemaNode())
    {
      return jsonNode.get(attributeExpressionLeaf.getSchemaAttribute().getName());
    }
    else
    {
      JsonNode extensionNode = jsonNode.get(attributeExpressionLeaf.getSchemaAttribute().getSchema().getId().get());
      return extensionNode.get(attributeExpressionLeaf.getSchemaAttribute().getName());
    }
  }

  /**
   * retrieves the attribute from either the main schema or an schema extension
   * 
   * @param jsonNode the json document from which the attribute should be extracted
   * @param attributeExpressionLeaf the filter expression node that describes the node that should be extracted
   * @return the extracted json node
   */
  private static JsonNode retrieveComplexAttributeNode(JsonNode jsonNode,
                                                       AttributeExpressionLeaf attributeExpressionLeaf)
  {
    if (attributeExpressionLeaf.isMainSchemaNode())
    {
      return jsonNode.get(attributeExpressionLeaf.getSchemaAttribute().getParent().getName());
    }
    else
    {
      JsonNode extensionNode = jsonNode.get(attributeExpressionLeaf.getSchemaAttribute().getSchema().getId().get());
      return extensionNode.get(attributeExpressionLeaf.getSchemaAttribute().getParent().getName());
    }
  }

  /**
   * evaluates a multi valued complex node in the resource node
   *
   * @param multiComplexNode the multi valued complex node
   * @param attributeExpressionLeaf the expression leaf that describes the inner node
   * @return true if the multi valued complex node does match to the expression, false else
   */
  private static boolean evaluateMultiComplexNode(JsonNode multiComplexNode,
                                                  AttributeExpressionLeaf attributeExpressionLeaf)
  {
    for ( JsonNode complexType : multiComplexNode )
    {
      JsonNode simpleNode = complexType.get(attributeExpressionLeaf.getSchemaAttribute().getName());
      if (checkValueEquality(simpleNode, attributeExpressionLeaf))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * evaluates a simple complex node in the resource node
   *
   * @param complexNode the simple complex node
   * @param attributeExpressionLeaf the expression leaf that describes the inner node
   * @return true if the complex node does match to the expression, false else
   */
  private static boolean evaluateComplexNode(JsonNode complexNode, AttributeExpressionLeaf attributeExpressionLeaf)
  {
    if (complexNode == null)
    {
      return checkValueEquality(null, attributeExpressionLeaf);
    }
    JsonNode simpleNode = complexNode.get(attributeExpressionLeaf.getSchemaAttribute().getName());
    if (simpleNode == null)
    {
      return checkValueEquality(null, attributeExpressionLeaf);
    }
    return checkValueEquality(simpleNode, attributeExpressionLeaf);
  }

  /**
   * checks if the given simple attribute node does match the given filter expression
   *
   * @param attributeNode a simple attribute node with primitive values. This might be a json array with
   *          primitives or a json primitive
   * @param attributeExpressionLeaf the expression leaf that describes the node
   * @return true if the attribute matches the expression, false else
   */
  protected static boolean checkValueEquality(JsonNode attributeNode, AttributeExpressionLeaf attributeExpressionLeaf)
  {

    switch (attributeExpressionLeaf.getType())
    {
      case BOOLEAN:
        Boolean boolValue = attributeExpressionLeaf.getBooleanValue().orElse(null);
        return compareBooleanValue(attributeNode, boolValue, attributeExpressionLeaf);
      case INTEGER:
      case DECIMAL:
        return compareNumberValue(attributeNode,
                                  attributeExpressionLeaf.getNumberValue().orElse(null),
                                  attributeExpressionLeaf.getComparator());
      case DATE_TIME:
        return compareDateTimeValue(attributeNode, attributeExpressionLeaf);
      default:
        return compareStringTypeValue(attributeNode, attributeExpressionLeaf);
    }
  }

  /**
   * checks if the given simple attribute node (json array with boolean or json boolean) matches the given
   * expression
   *
   * @param attributeNode the simple attribute node with primitive values
   * @param boolValue the comparison value from the filter expression
   * @param attributeExpressionLeaf the expression leaf that describes the node
   * @return true if the node matches, false else
   */
  private static boolean compareBooleanValue(JsonNode attributeNode,
                                             Boolean boolValue,
                                             AttributeExpressionLeaf attributeExpressionLeaf)
  {
    Comparator comparator = attributeExpressionLeaf.getComparator();
    List<Boolean> booleanValues = new ArrayList<>();
    if (attributeNode != null && attributeNode.isArray())
    {
      attributeNode.forEach(jsonNode -> booleanValues.add(jsonNode == null || jsonNode.isNull() ? null
        : jsonNode.asBoolean()));
    }
    else
    {
      booleanValues.add(attributeNode == null || attributeNode.isNull() ? null : attributeNode.booleanValue());
    }

    // if the attribute is a simple node and not present expect it to be false as default value
    if (booleanValues.size() == 1 && booleanValues.get(0) == null && !attributeExpressionLeaf.isMultiValued())
    {
      booleanValues.clear();
      booleanValues.add(false);
    }

    boolean anyMatch;
    for ( Boolean booleanValue : booleanValues )
    {
      switch (comparator)
      {
        case PR:
          anyMatch = booleanValue != null;
          break;
        case EQ:
          anyMatch = booleanValue == boolValue;
          break;
        case NE:
          anyMatch = booleanValue != boolValue;
          break;
        default:
          throw new InternalServerException("Illegal comparator '" + comparator + "' for boolean type", null, null);
      }
      if (anyMatch)
      {
        return true;
      }
    }
    return false;
  }

  /**
   * compares 2 strings based on the given filter {@link Comparator}
   *
   * @param jsonNode the json node that must be compared
   * @param attributeExpressionLeaf the filter node expression
   * @return true if the resource value is matching the filter node expression, false else
   */
  private static boolean compareStringTypeValue(JsonNode jsonNode, AttributeExpressionLeaf attributeExpressionLeaf)
  {
    Comparator comparator = attributeExpressionLeaf.getComparator();
    switch (comparator)
    {
      case PR:
        return jsonNode != null && !jsonNode.isNull();
      case EQ:
        return evaluateString(jsonNode, string -> {
          if (attributeExpressionLeaf.getSchemaAttribute().isCaseExact())
          {
            return StringUtils.equals(string, attributeExpressionLeaf.getValue());
          }
          else
          {
            return StringUtils.equalsIgnoreCase(string, attributeExpressionLeaf.getValue());
          }
        });
      case NE:
        return !evaluateString(jsonNode, string -> {
          if (attributeExpressionLeaf.getSchemaAttribute().isCaseExact())
          {
            return StringUtils.equals(string, attributeExpressionLeaf.getValue());
          }
          else
          {
            return StringUtils.equalsIgnoreCase(string, attributeExpressionLeaf.getValue());
          }
        });
      case EW:
        return evaluateString(jsonNode, string -> {
          if (attributeExpressionLeaf.getSchemaAttribute().isCaseExact())
          {
            return StringUtils.endsWith(string, attributeExpressionLeaf.getValue());
          }
          else
          {
            return StringUtils.endsWithIgnoreCase(string, attributeExpressionLeaf.getValue());
          }
        });
      case SW:
        return evaluateString(jsonNode, string -> {
          if (attributeExpressionLeaf.getSchemaAttribute().isCaseExact())
          {
            return StringUtils.startsWith(string, attributeExpressionLeaf.getValue());
          }
          else
          {
            return StringUtils.startsWithIgnoreCase(string, attributeExpressionLeaf.getValue());
          }
        });
      case CO:
        return evaluateString(jsonNode, string -> {
          if (attributeExpressionLeaf.getSchemaAttribute().isCaseExact())
          {
            return StringUtils.contains(string, attributeExpressionLeaf.getValue());
          }
          else
          {
            return StringUtils.containsIgnoreCase(string, attributeExpressionLeaf.getValue());
          }
        });
      case LT:
        return evaluateString(jsonNode, string -> {
          if (attributeExpressionLeaf.getSchemaAttribute().isCaseExact())
          {
            return StringUtils.compare(string, attributeExpressionLeaf.getValue()) < 0;
          }
          else
          {
            return StringUtils.compareIgnoreCase(string, attributeExpressionLeaf.getValue()) < 0;
          }
        });
      case LE:
        return evaluateString(jsonNode, string -> {
          if (attributeExpressionLeaf.getSchemaAttribute().isCaseExact())
          {
            return StringUtils.compare(string, attributeExpressionLeaf.getValue()) <= 0;
          }
          else
          {
            return StringUtils.compareIgnoreCase(string, attributeExpressionLeaf.getValue()) <= 0;
          }
        });
      case GT:
        return evaluateString(jsonNode, string -> {
          if (attributeExpressionLeaf.getSchemaAttribute().isCaseExact())
          {
            return StringUtils.compare(string, attributeExpressionLeaf.getValue()) > 0;
          }
          else
          {
            return StringUtils.compareIgnoreCase(string, attributeExpressionLeaf.getValue()) > 0;
          }
        });
      case GE:
        return evaluateString(jsonNode, string -> {
          if (attributeExpressionLeaf.getSchemaAttribute().isCaseExact())
          {
            return StringUtils.compare(string, attributeExpressionLeaf.getValue()) >= 0;
          }
          else
          {
            return StringUtils.compareIgnoreCase(string, attributeExpressionLeaf.getValue()) >= 0;
          }
        });
      default:
        throw new InternalServerException("Illegal comparator '" + comparator + "' for attribute type string", null,
                                          null);
    }
  }

  /**
   * evaluates if the given json string node does apply to the given comparison operation (this might also be an
   * array of json primitive strings)
   *
   * @param jsonNode the json string primitve
   * @param comparison a comparison operation that should be executed on the given json string
   * @return true if the json string applies to the given comparison operation
   */
  private static boolean evaluateString(JsonNode jsonNode, Function<String, Boolean> comparison)
  {
    List<String> stringValues = new ArrayList<>();
    if (jsonNode != null && jsonNode.isArray())
    {
      jsonNode.forEach(val -> stringValues.add(val == null || val.isNull() ? null : val.textValue()));
    }
    else
    {
      stringValues.add(jsonNode == null || jsonNode.isNull() ? null : jsonNode.textValue());
    }
    for ( String stringValue : stringValues )
    {
      if (comparison.apply(stringValue))
      {
        return true;
      }

    }
    return false;
  }

  /**
   * evaluates the given dateTime jsonNode that should be a primitve json string or array with primitive strings
   * applying to the dateTime syntax.
   *
   * @param jsonNode the dateTime node that should be represented as a string
   * @param attributeExpressionLeaf the expression leaf that describes the node
   * @return true if the given datetime does match to the given expression, false else
   */
  private static boolean compareDateTimeValue(JsonNode jsonNode, AttributeExpressionLeaf attributeExpressionLeaf)
  {
    List<String> dateTimes = new ArrayList<>();
    if (jsonNode != null && jsonNode.isArray())
    {
      jsonNode.forEach(dateNode -> dateTimes.add(dateNode.isNull() ? null : dateNode.textValue()));
    }
    else
    {
      dateTimes.add(jsonNode == null ? null : jsonNode.textValue());
    }
    if (Comparator.PR.equals(attributeExpressionLeaf.getComparator()))
    {
      return jsonNode != null && !jsonNode.isNull();
    }
    boolean matchFound = false;
    for ( String dateTimeString : dateTimes )
    {
      if (dateTimeString == null)
      {
        if (attributeExpressionLeaf.isNull())
        {
          return true;
        }
        else
        {
          continue;
        }
      }
      switch (attributeExpressionLeaf.getComparator())
      {
        case EQ:
        case NE:
        case GT:
        case GE:
        case LT:
        case LE:
          Instant dateTime = TimeUtils.parseDateTime(dateTimeString);
          long dateTimeLong = dateTime.toEpochMilli();
          BigDecimal compareNumber = attributeExpressionLeaf.getDateTime()
                                                            .map(instant -> new BigDecimal(instant.toEpochMilli()))
                                                            .orElse(null);
          if (compareNumber == null)
          {
            continue;
          }
          matchFound = compareNumberValue(new LongNode(dateTimeLong),
                                          compareNumber,
                                          attributeExpressionLeaf.getComparator());
          break;
        default:
          matchFound = compareStringTypeValue(jsonNode, attributeExpressionLeaf);
      }
      if (matchFound)
      {
        return true;
      }
    }
    return matchFound;
  }

  /**
   * checks if the given number json node matches the given filter expression. This might be a simple json
   * number or an array of json numbers
   *
   * @param number the json node that should be evaluated
   * @param compareNumber the number from the filter expression
   * @param comparator the comparator operation
   * @return true if the number matches the compareNumber based on the given comparator operator
   */
  private static boolean compareNumberValue(JsonNode number, BigDecimal compareNumber, Comparator comparator)
  {
    BiFunction<BigDecimal, Function<Integer, Boolean>, Boolean> compareDecimal = (bigDecimal, evaluateComparison) -> {
      if (bigDecimal == null)
      {
        return compareNumber == null;
      }
      int comparison = bigDecimal.compareTo(compareNumber);
      return evaluateComparison.apply(comparison);
    };
    switch (comparator)
    {
      case PR:
        return number != null && !number.isNull();
      case EQ:
        return evaluateNumber(number, bigDecimal -> compareDecimal.apply(bigDecimal, comparison -> comparison == 0));
      case NE:
        return !evaluateNumber(number, bigDecimal -> compareDecimal.apply(bigDecimal, comparison -> comparison == 0));
      case LT:
        return evaluateNumber(number, bigDecimal -> compareDecimal.apply(bigDecimal, comparison -> comparison < 0));
      case LE:
        return evaluateNumber(number, bigDecimal -> compareDecimal.apply(bigDecimal, comparison -> comparison <= 0));
      case GT:
        return evaluateNumber(number, bigDecimal -> compareDecimal.apply(bigDecimal, comparison -> comparison > 0));
      case GE:
        return evaluateNumber(number, bigDecimal -> compareDecimal.apply(bigDecimal, comparison -> comparison >= 0));
      case SW:
        return getNumberStringValues(number).stream().anyMatch(s -> s.startsWith(String.valueOf(compareNumber)));
      case EW:
        return getNumberStringValues(number).stream().anyMatch(s -> s.endsWith(String.valueOf(compareNumber)));
      case CO:
        return getNumberStringValues(number).stream().anyMatch(s -> s.contains(String.valueOf(compareNumber)));
      default:
        throw new InternalServerException("Illegal comparator '" + comparator
                                          + "' for attribute type number or dateTime", null, null);
    }
  }

  /**
   * turns the given json node to a list of strings that can be compared
   *
   * @param number a number node which is either a simple json node or a json array of numbers
   * @return a list of string representations of these numbers
   */
  private static List<String> getNumberStringValues(JsonNode number)
  {
    List<String> numberValues = new ArrayList<>();
    if (number.isArray())
    {
      for ( JsonNode jsonNode : number )
      {
        numberValues.add(String.valueOf(jsonNode.decimalValue().toString()));
      }
    }
    else
    {
      numberValues.add(String.valueOf(number.decimalValue().toString()));
    }
    return numberValues;
  }

  /**
   * evaluates that the given number node applies to the given comparison operation
   *
   * @param numberNode the number node that is either a primitive json number node or an array of numbers
   * @param comparison the comparison operation to which at least one of the numbers of the json node must apply
   * @return true if the json node applies to the given comparison operation, false else
   */
  private static boolean evaluateNumber(JsonNode numberNode, Function<BigDecimal, Boolean> comparison)
  {
    List<BigDecimal> decimals = new ArrayList<>();
    if (numberNode != null && numberNode.isArray())
    {
      numberNode.forEach(jsonNode -> decimals.add(jsonNode.decimalValue()));
    }
    else
    {
      decimals.add(numberNode == null ? null : numberNode.decimalValue());
    }

    for ( BigDecimal decimal : decimals )
    {
      if (comparison.apply(decimal))
      {
        return true;
      }
    }
    return false;
  }

}

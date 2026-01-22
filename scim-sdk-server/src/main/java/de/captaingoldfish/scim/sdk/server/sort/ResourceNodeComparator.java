package de.captaingoldfish.scim.sdk.server.sort;

import java.util.Comparator;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.TimeUtils;
import lombok.AllArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 04.11.2019 - 21:25 <br>
 * <br>
 */
@AllArgsConstructor
public class ResourceNodeComparator implements Comparator<ResourceNode>
{

  /**
   * the attribute that must be compared
   */
  private SchemaAttribute schemaAttribute;

  /**
   * the ordering direction
   */
  private SortOrder sortOrder;


  /**
   * {@inheritDoc}
   */
  @Override
  public int compare(ResourceNode resource1, ResourceNode resource2)
  {
    boolean ascending = SortOrder.ASCENDING.equals(sortOrder);
    Optional<JsonNode> attribute1Optional = resource1.getSortingAttribute(schemaAttribute);
    Optional<JsonNode> attribute2Optional = resource2.getSortingAttribute(schemaAttribute);
    if (!attribute1Optional.isPresent() && !attribute2Optional.isPresent())
    {
      return 0;
    }
    if (!attribute1Optional.isPresent())
    {
      return 1;
    }
    if (!attribute2Optional.isPresent())
    {
      return -1;
    }

    JsonNode attribute1 = attribute1Optional.get();
    JsonNode attribute2 = attribute2Optional.get();
    int compare;
    switch (schemaAttribute.getType())
    {
      case DATE_TIME:
        long dateTime1 = TimeUtils.parseDateTime(attribute1.textValue()).toEpochMilli();
        long dateTime2 = TimeUtils.parseDateTime(attribute2.textValue()).toEpochMilli();
        compare = NumberUtils.compare(dateTime1, dateTime2);
        return ascending ? compare : -compare;
      case INTEGER:
        compare = attribute1.bigIntegerValue().compareTo(attribute2.bigIntegerValue());
        return ascending ? compare : -compare;
      case DECIMAL:
        compare = attribute1.decimalValue().compareTo(attribute2.decimalValue());
        return ascending ? compare : -compare;
      default:
        if (schemaAttribute.isCaseExact())
        {
          compare = StringUtils.compare(attribute1.asText(), attribute2.asText());
          return ascending ? compare : -compare;
        }
        else
        {
          compare = StringUtils.compareIgnoreCase(attribute1.asText(), attribute2.asText());
          return ascending ? compare : -compare;
        }
    }
  }
}

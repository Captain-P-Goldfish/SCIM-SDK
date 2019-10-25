package de.gold.scim.endpoints.handler;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.gold.scim.constants.enums.SortOrder;
import de.gold.scim.endpoints.ResourceHandler;
import de.gold.scim.exceptions.NotImplementedException;
import de.gold.scim.exceptions.ResourceNotFoundException;
import de.gold.scim.filter.FilterNode;
import de.gold.scim.response.PartialListResponse;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.schemas.Schema;
import de.gold.scim.schemas.SchemaAttribute;
import lombok.AllArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 20.10.2019 - 12:16 <br>
 * <br>
 */
@AllArgsConstructor
public class SchemaHandler extends ResourceHandler<Schema>
{

  /**
   * creates the error message for the not supported operations
   */
  private static final Function<String, String> ERROR_MESSAGE_SUPPLIER = operation -> {
    return "the '" + operation + "'-operation is not supported for Schemas";
  };

  /**
   * needed for unit tests to prevent application context pollution
   */
  private final ResourceTypeFactory resourceTypeFactory;

  /**
   * creating of schemas not supported
   */
  @Override
  public Schema createResource(Schema resource)
  {
    throw new NotImplementedException(ERROR_MESSAGE_SUPPLIER.apply("create"));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Schema getResource(String id)
  {
    Schema schema = resourceTypeFactory.getAllResourceTypes()
                                       .stream()
                                       .map(ResourceType::getAllSchemas)
                                       .flatMap(Collection::stream)
                                       .distinct()
                                       .filter(s -> id.equals(s.getId().orElse(null)))
                                       .findAny()
                                       .orElse(null);
    if (schema == null)
    {
      throw new ResourceNotFoundException("a schema with the uri identifier '" + id + "' does not exist", null, null);
    }
    return schema;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PartialListResponse<Schema> listResources(long startIndex,
                                                   int count,
                                                   FilterNode filter,
                                                   SchemaAttribute sortBy,
                                                   SortOrder sortOrder)
  {
    List<Schema> allSchemas = resourceTypeFactory.getAllResourceTypes()
                                                 .stream()
                                                 .map(ResourceType::getAllSchemas)
                                                 .flatMap(Collection::stream)
                                                 .distinct()
                                                 .collect(Collectors.toList());
    return PartialListResponse.<Schema> builder().resources(allSchemas).totalResults(allSchemas.size()).build();
  }

  /**
   * updating of schemas not supported
   */
  @Override
  public Schema updateResource(Schema schema)
  {
    throw new NotImplementedException(ERROR_MESSAGE_SUPPLIER.apply("update"));
  }

  /**
   * deleting of schemas not supported
   */
  @Override
  public void deleteResource(String id)
  {
    throw new NotImplementedException(ERROR_MESSAGE_SUPPLIER.apply("delete"));
  }
}

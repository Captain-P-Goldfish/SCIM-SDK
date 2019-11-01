package de.gold.scim.server.constants.enums;

import org.apache.commons.lang3.StringUtils;

import de.gold.scim.server.constants.HttpStatus;
import de.gold.scim.server.constants.ScimType;
import de.gold.scim.server.exceptions.UnknownValueException;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 29.10.2019 - 08:37 <br>
 * <br>
 * HTTP PATCH is an OPTIONAL server function that enables clients to update one or more attributes of a SCIM
 * resource using a sequence of operations to "add", "remove", or "replace" values.Clients may discover
 * service provider support for PATCH by querying the service provider configuration
 */
public enum PatchOp
{

  ADD("add"), REPLACE("replace"), REMOVE("remove");

  @Getter
  private String value;

  PatchOp(String value)
  {
    this.value = value;
  }

  public static PatchOp getByValue(String value)
  {
    for ( PatchOp patchOp : PatchOp.values() )
    {
      if (StringUtils.equals(value, patchOp.getValue()))
      {
        return patchOp;
      }
    }
    throw new UnknownValueException("the value '" + value + "' is not a valid patch operation", null,
                                    HttpStatus.BAD_REQUEST, ScimType.Custom.INVALID_PARAMETERS);
  }
}

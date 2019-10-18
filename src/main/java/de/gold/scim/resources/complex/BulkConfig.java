package de.gold.scim.resources.complex;

import java.util.Optional;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.resources.base.ScimObjectNode;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 10:06 <br>
 * <br>
 * represents the bulk configuration for a {@link de.gold.scim.resources.ServiceProvider} object<br>
 * <br>
 * A complex type that specifies bulk configuration options. See Section 3.7 of [RFC7644]. REQUIRED.
 */
public class BulkConfig extends ScimObjectNode
{

  /**
   * the default maximum number of operations is 1. This will enforce the developer to modify the service
   * provider configuration to the applications requirements
   */
  protected static final int DEFAULT_MAX_OPERATIONS = 1;

  /**
   * the default maximum payload is set to 2MB
   */
  protected static final int DEFAULT_MAX_PAYLOAD_SIZE = (int)(Math.pow(1024, 2) * 2);

  @Builder
  public BulkConfig(Boolean supported, Integer maxOperations, Integer maxPayloadSize)
  {
    super(null);
    setSupported(supported);
    setMaxOperations(maxOperations);
    setMaxPayloadSize(maxPayloadSize);
  }

  /**
   * A Boolean value specifying whether or not the operation is supported. REQUIRED.
   */
  public boolean isSupported()
  {
    return getBooleanAttribute(AttributeNames.RFC7643.SUPPORTED).orElse(false);
  }

  /**
   * A Boolean value specifying whether or not the operation is supported. REQUIRED.
   */
  public void setSupported(Boolean supported)
  {
    setAttribute(AttributeNames.RFC7643.SUPPORTED, Optional.ofNullable(supported).orElse(false));
  }

  /**
   * An integer value specifying the maximum number of operations. REQUIRED.
   */
  public int getMaxOperations()
  {
    return getIntegerAttribute(AttributeNames.RFC7643.MAX_OPERATIONS).orElse(DEFAULT_MAX_OPERATIONS);
  }

  /**
   * An integer value specifying the maximum number of operations. REQUIRED.
   */
  public void setMaxOperations(Integer maxOperations)
  {
    setAttribute(AttributeNames.RFC7643.MAX_OPERATIONS,
                 Optional.ofNullable(maxOperations).orElse(DEFAULT_MAX_OPERATIONS));
  }

  /**
   * An integer value specifying the maximum payload size in bytes. REQUIRED.
   */
  public int getMaxPayloadSize()
  {
    return getIntegerAttribute(AttributeNames.RFC7643.MAX_PAYLOAD_SIZE).orElse(DEFAULT_MAX_PAYLOAD_SIZE);
  }

  /**
   * An integer value specifying the maximum payload size in bytes. REQUIRED.
   */
  public void setMaxPayloadSize(Integer maxPayloadSize)
  {
    setAttribute(AttributeNames.RFC7643.MAX_PAYLOAD_SIZE,
                 Optional.ofNullable(maxPayloadSize).orElse(DEFAULT_MAX_PAYLOAD_SIZE));
  }
}

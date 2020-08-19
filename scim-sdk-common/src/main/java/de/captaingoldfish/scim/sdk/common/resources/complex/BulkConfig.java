package de.captaingoldfish.scim.sdk.common.resources.complex;

import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 10:06 <br>
 * <br>
 * represents the bulk configuration for a {@link ServiceProvider} object<br>
 * <br>
 * A complex type that specifies bulk configuration options. See Section 3.7 of [RFC7644]. REQUIRED.
 */
@NoArgsConstructor
public class BulkConfig extends ScimObjectNode
{

  /**
   * the default maximum number of operations is 1. This will enforce the developer to modify the service
   * provider configuration to the applications requirements
   */
  protected static final Integer DEFAULT_MAX_OPERATIONS = 1;

  /**
   * the default maximum payload is set to 2MB
   */
  protected static final Long DEFAULT_MAX_PAYLOAD_SIZE = (long)(Math.pow(1024, 2) * 2);

  @Builder
  public BulkConfig(Boolean supported, Integer maxOperations, Long maxPayloadSize)
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
  public Integer getMaxOperations()
  {
    return getLongAttribute(AttributeNames.RFC7643.MAX_OPERATIONS).orElse(Long.valueOf(DEFAULT_MAX_OPERATIONS))
                                                                  .intValue();
  }

  /**
   * An integer value specifying the maximum number of operations. REQUIRED.
   */
  public void setMaxOperations(Integer maxOperations)
  {
    Long max = maxOperations == null ? null : Long.valueOf(maxOperations);
    setAttribute(AttributeNames.RFC7643.MAX_OPERATIONS,
                 Optional.ofNullable(max).orElse(Long.valueOf(DEFAULT_MAX_OPERATIONS)));
  }

  /**
   * An integer value specifying the maximum payload size in bytes. REQUIRED.
   */
  public Long getMaxPayloadSize()
  {
    return getLongAttribute(AttributeNames.RFC7643.MAX_PAYLOAD_SIZE).orElse(DEFAULT_MAX_PAYLOAD_SIZE);
  }

  /**
   * An integer value specifying the maximum payload size in bytes. REQUIRED.
   */
  public void setMaxPayloadSize(Long maxPayloadSize)
  {
    setAttribute(AttributeNames.RFC7643.MAX_PAYLOAD_SIZE,
                 Optional.ofNullable(maxPayloadSize).orElse(DEFAULT_MAX_PAYLOAD_SIZE));
  }
}

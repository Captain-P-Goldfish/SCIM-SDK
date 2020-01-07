package de.captaingoldfish.scim.sdk.common.resources.base;

import java.util.List;

import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.ReferenceTypes;
import de.captaingoldfish.scim.sdk.common.constants.enums.Returned;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * author Pascal Knueppel <br>
 * created at: 05.10.2019 - 16:37 <br>
 * <br>
 * an implementation with default methods to simulate multiple inheritance to jackson
 * {@link com.fasterxml.jackson.databind.JsonNode}s
 */
public interface ScimNode
{

  SchemaAttribute getSchemaAttribute();

  /**
   * @return the name of the node in SCIM representation e.g. "name.givenName" or "emails.primary" or "userName"
   */
  default String getScimNodeName()
  {
    return getSchemaAttribute() == null ? null : getSchemaAttribute().getScimNodeName();
  }

  /**
   * @return the simple name of this attribute e.g. "givenName" or "honoricPrefix" or "id"
   */
  default String getAttributeName()
  {
    return getSchemaAttribute() == null ? null : getSchemaAttribute().getName();
  }

  /**
   * @return represents the type of this node
   */
  default Type getValueType()
  {
    return getSchemaAttribute() == null ? null : getSchemaAttribute().getType();
  }


  /**
   * @return the meta description of this node
   */
  default String getAttributeDescription()
  {
    return getSchemaAttribute() == null ? null : getSchemaAttribute().getDescription();
  }


  /**
   * @return the mutability value of this node
   */
  default Mutability getMutability()
  {
    return getSchemaAttribute() == null ? null : getSchemaAttribute().getMutability();
  }


  /**
   * @return the returned value of this node
   */
  default Returned getReturned()
  {
    return getSchemaAttribute() == null ? null : getSchemaAttribute().getReturned();
  }

  /**
   * @return the uniqueness value of this node
   */
  default Uniqueness getUniqueness()
  {
    return getSchemaAttribute() == null ? null : getSchemaAttribute().getUniqueness();
  }

  /**
   * @return if this node is an {@link com.fasterxml.jackson.databind.node.ArrayNode} or not
   */
  default boolean isMultiValued()
  {
    return getSchemaAttribute() != null && getSchemaAttribute().isMultiValued();
  }

  /**
   * @return if this node is a required value in the resource
   */
  default boolean isRequired()
  {
    return getSchemaAttribute() != null && getSchemaAttribute().isRequired();
  }


  /**
   * @return if the value of this node must be handled case exact or case insensitive
   */
  default boolean isCaseExact()
  {
    return getSchemaAttribute() != null && getSchemaAttribute().isCaseExact();
  }

  /**
   * @return the canonical values of this node
   */
  default List<String> getCanonicalValues()
  {
    return getSchemaAttribute() == null ? null : getSchemaAttribute().getCanonicalValues();
  }


  /**
   * @return the reference types that are valid for this node. Only important if the {@link #getValueType()}
   *         method returns the value {@link Type#REFERENCE}
   */
  default List<ReferenceTypes> getReferenceTypes()
  {
    return getSchemaAttribute() == null ? null : getSchemaAttribute().getReferenceTypes();
  }

}

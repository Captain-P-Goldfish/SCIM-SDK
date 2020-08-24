package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Address;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Entitlement;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Ims;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.MultiComplexNode;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PersonRole;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PhoneNumber;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Photo;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.ScimX509Certificate;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.auth.ScimAuthorization;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 */
@Slf4j
public class UserHandler extends ResourceHandler<User>
{

  public static final String PRIMARY_SUFFIX = "_primary";

  private static final String SCIM_DEPARTMENT = "department";

  private static final String SCIM_DIVISION = "division";

  private static final String SCIM_DISPLAY_NAME = "displayName";

  private static final String SCIM_FORMATTED_NAME = "formattedName";

  private static final String SCIM_HONORIC_PREFIX = "honoricPrefix";

  private static final String SCIM_HONORIC_SUFFIX = "honoricSuffix";

  private static final String SCIM_LDAP_ID = "LDAP_ID";

  private static final String SCIM_MIDDLE_NAME = "middleName";

  private static final String SCIM_ORGANIZATION = "company";

  private static final String SCIM_TELPEHONE_NUMBER = "phoneNumber";

  private static final String SCIM_MOBILE_TELPEHONE_NUMBER = "mobileNumber";

  private static final String SCIM_TITLE = "title";

  /**
   * an attribute that is added to users created by the scim protocol
   */
  private static final String SCIM_USER = "scim-user";


  private static String KEYCLOAK_DEBUG = System.getProperty("keycloak.debug");

  /**
   * {@inheritDoc}
   */
  @Override
  public User createResource(User user, Authorization authorization)
  {
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    final String username = user.getUserName().get();
    log.info(this.getClass().getName() + " createResource: " + username + "\n" + user.toPrettyString());
    if (KEYCLOAK_DEBUG != null)
    {
      log.info(this.getClass().getName() + " createResource input: " + user.toPrettyString());
    }
    if (keycloakSession.users().getUserByUsername(username, keycloakSession.getContext().getRealm()) != null)
    {
      throw new ConflictException("the username '" + username + "' is already taken");
    }
    Optional<Email> emailOpt = user.getEmails()
                                   .stream()
                                   .filter(MultiComplexNode::isPrimary)
                                   .findAny()
                                   .filter(value -> value.getValue().isPresent());
    if (emailOpt.isPresent() && keycloakSession.users()
                                               .getUserByEmail(emailOpt.get().getValue().orElse("xx"),
                                                               keycloakSession.getContext().getRealm()) != null)
    {
      throw new ConflictException("the email '" + emailOpt.get().getValue().get() + "' is already taken");

    }
    UserModel userModel = keycloakSession.users().addUser(keycloakSession.getContext().getRealm(), username);
    userModel = userToModel(user, userModel);
    User ret = modelToUser(userModel);
    if (KEYCLOAK_DEBUG != null)
    {
      log.info(this.getClass().getName() + " createResource returns: " + ret.toPrettyString());
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public User getResource(String id, Authorization authorization)
  {
    log.info(this.getClass().getName() + " getResource: " + id);

    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    UserModel userModel = keycloakSession.users().getUserById(id, keycloakSession.getContext().getRealm());

    if (userModel == null || !Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    User ret = modelToUser(userModel);
    if (KEYCLOAK_DEBUG != null)
    {
      log.info(this.getClass().getName() + " getResource returns: " + ret.toPrettyString());
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PartialListResponse<User> listResources(long startIndex,
                                                 int count,
                                                 FilterNode filter,
                                                 SchemaAttribute sortBy,
                                                 SortOrder sortOrder,
                                                 List<SchemaAttribute> attributes,
                                                 List<SchemaAttribute> excludedAttributes,
                                                 Authorization authorization)
  {
    log.info(this.getClass().getName() + " listResources");

    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    // TODO in order to filter on database level the feature "autoFiltering" must be disabled and the JPA criteria
    // api should be used
    RealmModel realmModel = keycloakSession.getContext().getRealm();
    List<UserModel> userModels = keycloakSession.users().getUsers(realmModel);
    List<User> userList = new ArrayList<>();

    for ( UserModel userModel : userModels )
    {
      if (Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
      {
        User add = modelToUser(userModel);
        if (KEYCLOAK_DEBUG != null)
        {
          log.info(this.getClass().getName() + " listResources User " + add.toPrettyString());
        }
        userList.add(add);
      }
    }
    // List<User> userList = userModels.stream()
    // .filter(userModel -> Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
    // .map(this::modelToUser)
    // .collect(Collectors.toList());
    return PartialListResponse.<User> builder().totalResults(userList.size()).resources(userList).build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public User updateResource(User userToUpdate, Authorization authorization)
  {
    log.info(this.getClass().getName() + " updateResource " + userToUpdate.getName());
    if (KEYCLOAK_DEBUG != null)
    {
      log.info(this.getClass().getName() + " updateResource input: " + userToUpdate.toPrettyString());
    }
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    UserModel userModel = keycloakSession.users()
                                         .getUserById(userToUpdate.getId().get(),
                                                      keycloakSession.getContext().getRealm());

    if (userModel == null || !Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    userModel = userToModel(userToUpdate, userModel);
    User ret = modelToUser(userModel);
    log.info(this.getClass().getName() + " updateResource returns: " + ret.toPrettyString());
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteResource(String id, Authorization authorization)
  {
    log.info(this.getClass().getName() + " deleteResource " + id);

    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    UserModel userModel = keycloakSession.users().getUserById(id, keycloakSession.getContext().getRealm());
    if (userModel == null || !Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
    {
      throw new ResourceNotFoundException("resource with id '" + id + "' does not exist");
    }
    keycloakSession.users().removeUser(keycloakSession.getContext().getRealm(), userModel);
    log.info("deleted user with username: {}", userModel.getUsername());
  }

  /**
   * this method will set the password of the user
   *
   * @param password the password to set
   * @param userModel the model that should receive the password
   */
  private void setPassword(KeycloakSession keycloakSession, String password, UserModel userModel)
  {
    if (StringUtils.isEmpty(password))
    {
      return;
    }
    UserCredentialModel userCredential = UserCredentialModel.password(password);
    UserCredentialManager credentialManager = keycloakSession.userCredentialManager();
    RealmModel realm = keycloakSession.getContext().getRealm();
    try
    {
      credentialManager.updateCredential(realm, userModel, userCredential);
    }
    catch (ModelException ex)
    {
      // this exception is thrown if the password policy was not matched
      log.debug(ex.getMessage(), ex);
      throw new BadRequestException("password policy not matched");
    }
  }

  /**
   * writes the values of the scim user instance into the keycloak user instance
   *
   * @param user the scim user instance
   * @param userModel the keycloak user instance
   * @return the updated keycloak user instance
   */
  private UserModel userToModel(User user, UserModel userModel)
  {
    user.isActive().ifPresent(userModel::setEnabled);
    userModel.setSingleAttribute(SCIM_USER, String.valueOf(true));
    user.getName().ifPresent(name -> {
      name.getGivenName().ifPresent(userModel::setFirstName);
      name.getFamilyName().ifPresent(userModel::setLastName);
      name.getMiddleName()
          .ifPresent(middleName -> userModel.setSingleAttribute(AttributeNames.RFC7643.MIDDLE_NAME, middleName));
      name.getHonorificPrefix()
          .ifPresent(prefix -> userModel.setSingleAttribute(AttributeNames.RFC7643.HONORIFIC_PREFIX, prefix));
      name.getHonorificSuffix()
          .ifPresent(suffix -> userModel.setSingleAttribute(AttributeNames.RFC7643.HONORIFIC_SUFFIX, suffix));
      name.getFormatted()
          .ifPresent(formatted -> userModel.setSingleAttribute(AttributeNames.RFC7643.FORMATTED, formatted));
    });
    userModel.setSingleAttribute(AttributeNames.RFC7643.NICK_NAME, user.getNickName().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.TITLE, user.getTitle().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.DISPLAY_NAME, user.getDisplayName().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.USER_TYPE, user.getUserType().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.LOCALE, user.getLocale().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.PREFERRED_LANGUAGE, user.getPreferredLanguage().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.TIMEZONE, user.getTimezone().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.PROFILE_URL, user.getProfileUrl().orElse(null));

    user.getEmails()
        .stream()
        .filter(MultiComplexNode::isPrimary)
        .findAny()
        .flatMap(MultiComplexNode::getValue)
        .ifPresent(userModel::setEmail);
    for ( PhoneNumber number : user.getPhoneNumbers() )
    {
      if (number.getType().isPresent() && number.getValue().isPresent() && "work".equals(number.getType().get()))
      {
        userModel.setSingleAttribute(SCIM_TELPEHONE_NUMBER, number.getValue().get());
      }
      else if (number.getType().isPresent() && number.getValue().isPresent() && "mobile".equals(number.getType().get()))
      {
        userModel.setSingleAttribute(SCIM_MOBILE_TELPEHONE_NUMBER, number.getValue().get());
      }
    }

    if (user.getLdapId().isPresent())
    {
      userModel.setSingleAttribute(SCIM_LDAP_ID, user.getLdapId().get());
    }
    setMultiAttribute(user::getEmails, AttributeNames.RFC7643.EMAILS, userModel);
    setMultiAttribute(user::getPhoneNumbers, AttributeNames.RFC7643.PHONE_NUMBERS, userModel);
    setMultiAttribute(user::getAddresses, AttributeNames.RFC7643.ADDRESSES, userModel);
    setMultiAttribute(user::getIms, AttributeNames.RFC7643.IMS, userModel);
    setMultiAttribute(user::getEntitlements, AttributeNames.RFC7643.ENTITLEMENTS, userModel);
    setMultiAttribute(user::getPhotos, AttributeNames.RFC7643.PHOTOS, userModel);
    setMultiAttribute(user::getRoles, AttributeNames.RFC7643.ROLES, userModel);
    setMultiAttribute(user::getX509Certificates, AttributeNames.RFC7643.X509_CERTIFICATES, userModel);

    if (user.getExternalId().isPresent())
    {
      userModel.setSingleAttribute(AttributeNames.RFC7643.EXTERNAL_ID, user.getExternalId().get());
    }
    userModel.setSingleAttribute(AttributeNames.RFC7643.COST_CENTER,
                                 user.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.DEPARTMENT,
                                 user.getEnterpriseUser().flatMap(EnterpriseUser::getDepartment).orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.DIVISION,
                                 user.getEnterpriseUser().flatMap(EnterpriseUser::getDivision).orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.EMPLOYEE_NUMBER,
                                 user.getEnterpriseUser().flatMap(EnterpriseUser::getEmployeeNumber).orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.ORGANIZATION,
                                 user.getEnterpriseUser().flatMap(EnterpriseUser::getOrganization).orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.MANAGER,
                                 user.getEnterpriseUser()
                                     .flatMap(EnterpriseUser::getManager)
                                     .flatMap(Manager::getValue)
                                     .orElse(null));
    return userModel;
  }

  private void setMultiAttribute(Supplier<List<? extends MultiComplexNode>> getList,
                                 String attributeName,
                                 UserModel keycloakUser)
  {
    keycloakUser.setAttribute(attributeName,
                              getList.get()
                                     .stream()
                                     .filter(multiComplex -> !multiComplex.isPrimary())
                                     .map(MultiComplexNode::toPrettyString)
                                     .collect(Collectors.toList()));

    getList.get()
           .stream()
           .filter(MultiComplexNode::isPrimary)
           .findAny()
           .map(MultiComplexNode::toPrettyString)
           .ifPresent(multiNode -> keycloakUser.setSingleAttribute(attributeName + PRIMARY_SUFFIX, multiNode));
  }

  /**
   * converts a keycloak {@link UserModel} into a SCIM representation of {@link User}
   *
   * @param userModel the keycloak user representation
   * @return the SCIM user representation
   */
  private User modelToUser(UserModel userModel)
  {
    List<PhoneNumber> phoneNumbers = new ArrayList<PhoneNumber>();
    String telephoneNumber = userModel.getFirstAttribute(SCIM_TELPEHONE_NUMBER);
    String mobileNumber = userModel.getFirstAttribute(SCIM_MOBILE_TELPEHONE_NUMBER);
    if (!StringUtils.isEmpty(telephoneNumber))
    {
      phoneNumbers.add(new PhoneNumber("work", true, null, telephoneNumber, null));
    }
    if (!StringUtils.isEmpty(mobileNumber))
    {
      phoneNumbers.add(new PhoneNumber("mobile", false, null, mobileNumber, null));
    }
    User user = User.builder()
                    .id(userModel.getId())
                    .ldapId(userModel.getFirstAttribute(SCIM_LDAP_ID))
                    .userName(userModel.getUsername())
                    .name(Name.builder()
                              .givenName(userModel.getFirstName())
                              .familyName(userModel.getLastName())
                              .middlename(userModel.getFirstAttribute(AttributeNames.RFC7643.MIDDLE_NAME))
                              .honorificPrefix(userModel.getFirstAttribute(AttributeNames.RFC7643.HONORIFIC_PREFIX))
                              .honorificSuffix(userModel.getFirstAttribute(AttributeNames.RFC7643.HONORIFIC_SUFFIX))
                              .formatted(userModel.getFirstAttribute(AttributeNames.RFC7643.FORMATTED))
                              .build())
                    .active(userModel.isEnabled())
                    .nickName(userModel.getFirstAttribute(AttributeNames.RFC7643.NICK_NAME))
                    .title(userModel.getFirstAttribute(AttributeNames.RFC7643.TITLE))
                    .displayName(userModel.getFirstAttribute(AttributeNames.RFC7643.DISPLAY_NAME))
                    .userType(userModel.getFirstAttribute(AttributeNames.RFC7643.USER_TYPE))
                    .externalId(userModel.getFirstAttribute(AttributeNames.RFC7643.EXTERNAL_ID))
                    .locale(userModel.getFirstAttribute(AttributeNames.RFC7643.LOCALE))
                    .preferredLanguage(userModel.getFirstAttribute(AttributeNames.RFC7643.PREFERRED_LANGUAGE))
                    .timeZone(userModel.getFirstAttribute(AttributeNames.RFC7643.TIMEZONE))
                    .profileUrl(userModel.getFirstAttribute(AttributeNames.RFC7643.PROFILE_URL))
                    .emails(getAttributeList(Email.class, AttributeNames.RFC7643.EMAILS, userModel))
                    .phoneNumbers(phoneNumbers)
                    .addresses(getAttributeList(Address.class, AttributeNames.RFC7643.ADDRESSES, userModel))
                    .ims(getAttributeList(Ims.class, AttributeNames.RFC7643.IMS, userModel))
                    .entitlements(getAttributeList(Entitlement.class, AttributeNames.RFC7643.ENTITLEMENTS, userModel))
                    .photos(getAttributeList(Photo.class, AttributeNames.RFC7643.PHOTOS, userModel))
                    .roles(getAttributeList(PersonRole.class, AttributeNames.RFC7643.ROLES, userModel))
                    .x509Certificates(getAttributeList(ScimX509Certificate.class,
                                                       AttributeNames.RFC7643.X509_CERTIFICATES,
                                                       userModel))
                    .meta(Meta.builder()
                              .created(Instant.ofEpochMilli(userModel.getCreatedTimestamp()))
                              .lastModified(getLastModified(userModel))
                              .build())
                    .build();

    Manager manager = Manager.builder().value(userModel.getFirstAttribute(AttributeNames.RFC7643.MANAGER)).build();
    EnterpriseUser enterpriseUser = EnterpriseUser.builder()
                                                  .costCenter(userModel.getFirstAttribute(AttributeNames.RFC7643.COST_CENTER))
                                                  .department(userModel.getFirstAttribute(AttributeNames.RFC7643.DEPARTMENT))
                                                  .division(userModel.getFirstAttribute(AttributeNames.RFC7643.DIVISION))
                                                  .employeeNumber(userModel.getFirstAttribute(AttributeNames.RFC7643.EMPLOYEE_NUMBER))
                                                  .organization(userModel.getFirstAttribute(AttributeNames.RFC7643.ORGANIZATION))
                                                  .build();
    if (!manager.isEmpty())
    {
      enterpriseUser.setManager(manager);
    }
    if (!enterpriseUser.isEmpty())
    {
      user.setEnterpriseUser(enterpriseUser);
    }
    return user;
  }

  private <T extends MultiComplexNode> List<T> getAttributeList(Class<T> type,
                                                                String attributeName,
                                                                UserModel keycloakUser)
  {
    List<T> attributeList = new ArrayList<>();
    keycloakUser.getAttribute(attributeName).forEach(attribute -> {
      attributeList.add(JsonHelper.readJsonDocument(attribute, type));
    });
    keycloakUser.getAttribute(attributeName + PRIMARY_SUFFIX).forEach(attribute -> {
      attributeList.add(JsonHelper.readJsonDocument(attribute, type));
    });
    return attributeList;
  }

  /**
   * gets the lastModified value of the user
   *
   * @param userModel the user model from which the last modified value should be extracted
   * @return the last modified value of the given user
   */
  private Instant getLastModified(UserModel userModel)
  {
    String lastModifiedString = userModel.getFirstAttribute(AttributeNames.RFC7643.LAST_MODIFIED);
    if (StringUtils.isNotBlank(lastModifiedString))
    {
      return Instant.ofEpochMilli(Long.parseLong(lastModifiedString));
    }
    else
    {
      return Instant.ofEpochMilli(userModel.getCreatedTimestamp());
    }
  }
}

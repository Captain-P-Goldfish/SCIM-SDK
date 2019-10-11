package de.gold.scim.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.resources.complex.Address;
import de.gold.scim.resources.complex.Manager;
import de.gold.scim.resources.complex.Name;
import de.gold.scim.resources.multicomplex.Email;
import de.gold.scim.resources.multicomplex.Entitlement;
import de.gold.scim.resources.multicomplex.GroupNode;
import de.gold.scim.resources.multicomplex.Ims;
import de.gold.scim.resources.multicomplex.MultiComplexNode;
import de.gold.scim.resources.multicomplex.PersonRole;
import de.gold.scim.resources.multicomplex.PhoneNumber;
import de.gold.scim.resources.multicomplex.Photo;
import de.gold.scim.resources.multicomplex.ScimX509Certificate;
import de.gold.scim.utils.FileReferences;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 00:31 <br>
 * <br>
 */
public class UserTest implements FileReferences
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    User instance = Assertions.assertDoesNotThrow(() -> User.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new User().isEmpty());
  }

  /**
   * this test will set values into the user object and will read them again and test that the values have been
   * correctly set within the object
   */
  @Test
  public void testSetAndGetBasicStringValues()
  {
    Random random = new Random();
    final String userName = UUID.randomUUID().toString();
    final Name name = Name.builder().givenName("max").familyName("mustermann").build();
    final String displayName = UUID.randomUUID().toString();
    final String nickName = UUID.randomUUID().toString();
    final String profileUrl = UUID.randomUUID().toString();
    final String title = UUID.randomUUID().toString();
    final String userType = UUID.randomUUID().toString();
    final String preferredLanguage = UUID.randomUUID().toString();
    final String locale = UUID.randomUUID().toString();
    final String timeZone = UUID.randomUUID().toString();
    final Boolean active = random.nextBoolean();
    final String password = UUID.randomUUID().toString();
    final List<Email> emails = getRandomComplexNodeList(Email.class, random.nextInt(10) + 1);
    final List<PhoneNumber> phoneNumbers = getRandomComplexNodeList(PhoneNumber.class, random.nextInt(10) + 1);
    final List<Ims> imsList = getRandomComplexNodeList(Ims.class, random.nextInt(10) + 1);
    final List<Photo> photos = getRandomComplexNodeList(Photo.class, random.nextInt(10) + 1);
    final List<Address> addresses = getRandomAddresses(random.nextInt(10) + 1);
    final List<GroupNode> groups = getRandomComplexNodeList(GroupNode.class, random.nextInt(10) + 1);
    final List<Entitlement> entitlements = getRandomComplexNodeList(Entitlement.class, random.nextInt(10) + 1);
    final List<PersonRole> roles = getRandomComplexNodeList(PersonRole.class, random.nextInt(10) + 1);
    final List<ScimX509Certificate> x509Certificates = getRandomComplexNodeList(ScimX509Certificate.class,
                                                                                random.nextInt(10) + 1);
    final EnterpriseUser enterpriseUser = getRandomEnterpriseUser();

    User user = User.builder()
                    .userName(userName)
                    .name(name)
                    .displayName(displayName)
                    .nickName(nickName)
                    .profileUrl(profileUrl)
                    .title(title)
                    .userType(userType)
                    .preferredLanguage(preferredLanguage)
                    .locale(locale)
                    .timeZone(timeZone)
                    .active(active)
                    .password(password)
                    .emails(emails)
                    .phoneNumbers(phoneNumbers)
                    .ims(imsList)
                    .photos(photos)
                    .addresses(addresses)
                    .groups(groups)
                    .entitlements(entitlements)
                    .roles(roles)
                    .x509Certificates(x509Certificates)
                    .enterpriseUser(enterpriseUser)
                    .build();

    Email email = getRandomInstance(Email.class, false);
    user.addEmail(email);
    emails.add(email);

    PhoneNumber phoneNumber = getRandomInstance(PhoneNumber.class, false);
    user.addPhoneNumber(phoneNumber);
    phoneNumbers.add(phoneNumber);

    Ims ims = getRandomInstance(Ims.class, false);
    user.addIms(ims);
    imsList.add(ims);

    Photo photo = getRandomInstance(Photo.class, false);
    user.addPhoto(photo);
    photos.add(photo);

    Address address = getRandomAddresses(1).get(0);
    user.addAddress(address);
    addresses.add(address);

    GroupNode groupNode = getRandomInstance(GroupNode.class, false);
    user.addGroup(groupNode);
    groups.add(groupNode);

    Entitlement entitlement = getRandomInstance(Entitlement.class, false);
    user.addEntitlement(entitlement);
    entitlements.add(entitlement);

    PersonRole role = getRandomInstance(PersonRole.class, false);
    user.addRole(role);
    roles.add(role);

    ScimX509Certificate x509Certificate = getRandomInstance(ScimX509Certificate.class, false);
    user.addX509Certificate(x509Certificate);
    x509Certificates.add(x509Certificate);

    Assertions.assertEquals(userName, user.getUserName().get());
    Assertions.assertEquals(name, user.getNameNode().get());
    Assertions.assertEquals(displayName, user.getDisplayName().get());
    Assertions.assertEquals(nickName, user.getNickName().get());
    Assertions.assertEquals(profileUrl, user.getProfileUrl().get());
    Assertions.assertEquals(title, user.getTitle().get());
    Assertions.assertEquals(userType, user.getUserType().get());
    Assertions.assertEquals(preferredLanguage, user.getPreferredLanguage().get());
    Assertions.assertEquals(locale, user.getLocale().get());
    Assertions.assertEquals(timeZone, user.getTimezone().get());
    Assertions.assertEquals(active, user.isActive().get());
    Assertions.assertEquals(password, user.getPassword().get());
    Assertions.assertEquals(emails, user.getEmails());
    Assertions.assertEquals(phoneNumbers, user.getPhoneNumbers());
    Assertions.assertEquals(imsList, user.getIms());
    Assertions.assertEquals(photos, user.getPhotos());
    Assertions.assertEquals(addresses, user.getAddresses());
    Assertions.assertEquals(groups, user.getGroups());
    Assertions.assertEquals(entitlements, user.getEntitlements());
    Assertions.assertEquals(roles, user.getRoles());
    Assertions.assertEquals(x509Certificates, user.getX509Certificates());
    Assertions.assertEquals(enterpriseUser, user.getEnterpriseUser().get());
  }

  /**
   * creates an enterprise user with random values
   */
  private EnterpriseUser getRandomEnterpriseUser()
  {
    final String employeeNumber = UUID.randomUUID().toString();
    final String costCenter = UUID.randomUUID().toString();
    final String organization = UUID.randomUUID().toString();
    final String division = UUID.randomUUID().toString();
    final String department = UUID.randomUUID().toString();
    final Manager manager = Manager.builder()
                                   .value(UUID.randomUUID().toString())
                                   .displayName(UUID.randomUUID().toString())
                                   .build();
    return EnterpriseUser.builder()
                         .employeeNumber(employeeNumber)
                         .costCenter(costCenter)
                         .organization(organization)
                         .division(division)
                         .department(department)
                         .manager(manager)
                         .build();

  }

  /**
   * creates one or several addresses of {@link Address} with randomized values
   *
   * @param entries the number of new instances that should be present in the returned list
   * @return a list with instances that have randomized values
   */
  private List<Address> getRandomAddresses(int entries)
  {
    List<Address> addresses = new ArrayList<>();
    for ( int i = 0 ; i < entries ; i++ )
    {
      final String formatted = UUID.randomUUID().toString();
      final String streetAddress = UUID.randomUUID().toString();
      final String postalCode = UUID.randomUUID().toString();
      final String locality = UUID.randomUUID().toString();
      final String region = UUID.randomUUID().toString();
      final String country = UUID.randomUUID().toString();
      Address address = Address.builder()
                               .formatted(formatted)
                               .streetAddress(streetAddress)
                               .postalCode(postalCode)
                               .locality(locality)
                               .region(region)
                               .country(country)
                               .build();
      addresses.add(address);
    }
    return addresses;
  }

  /**
   * creates one or several instances of type T with randomized values
   *
   * @param type the type to create a random instance from
   * @param entries the number of new instances that should be present in the returned list
   * @return a list with instances that have randomized values
   */
  private <T extends MultiComplexNode> List<T> getRandomComplexNodeList(Class<T> type, int entries)
  {
    List<T> nodes = new ArrayList<>();
    boolean primarySet = false;
    Random random = new Random();
    for ( int i = 0 ; i < entries ; i++ )
    {
      boolean primary = random.nextBoolean();
      T t = getRandomInstance(type, !primarySet && primary);
      if (primary)
      {
        primarySet = true;
      }
      nodes.add(t);
    }
    return nodes;
  }

  /**
   * creates an instance of the given types with random values
   *
   * @param type the type of the new instance
   * @param primary if the given type should be the primary type or not
   * @return the instance with randomized values
   */
  private <T extends MultiComplexNode> T getRandomInstance(Class<T> type, boolean primary)
  {
    T multiComplexNode = createNewInstanceOfType(type);
    multiComplexNode.setValue(UUID.randomUUID().toString());
    multiComplexNode.setDisplay(UUID.randomUUID().toString());
    multiComplexNode.setScimType(UUID.randomUUID().toString());
    multiComplexNode.setRef(UUID.randomUUID().toString());
    multiComplexNode.setPrimary(primary);
    return multiComplexNode;
  }

  /**
   * creates a new instance of the given type
   *
   * @param type the type to create an instance from
   * @return the new instance of the given type
   */
  private <T extends MultiComplexNode> T createNewInstanceOfType(Class<T> type)
  {
    try
    {
      return type.newInstance();
    }
    catch (InstantiationException | IllegalAccessException e)
    {
      throw new IllegalStateException(e);
    }
  }
}

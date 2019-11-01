package de.gold.scim.server.resources.complex;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.server.utils.TimeUtils;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:22 <br>
 * <br>
 */
public class MetaTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    Meta instance = Assertions.assertDoesNotThrow(() -> Meta.builder().build());
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new Meta().isEmpty());
  }

  /**
   * will test if the attributes are correctly added into the json object
   */
  @Test
  public void testSetAndGetAttributes()
  {
    final LocalDateTime created = LocalDateTime.now().withNano(0);
    final LocalDateTime lastModified = LocalDateTime.now().withNano(0);
    final String resourceType = "User";
    final String location = "/Users/" + UUID.randomUUID().toString();
    final String version = "1";
    Meta meta = Meta.builder()
                    .created(created)
                    .lastModified(lastModified)
                    .resourceType(resourceType)
                    .location(location)
                    .version(version)
                    .build();
    Assertions.assertEquals(created.atZone(ZoneId.systemDefault()).toInstant(), meta.getCreated().get());
    Assertions.assertEquals(lastModified.atZone(ZoneId.systemDefault()).toInstant(), meta.getLastModified().get());
    Assertions.assertEquals(resourceType, meta.getResourceType().get());
    Assertions.assertEquals(location, meta.getLocation().get());
    Assertions.assertEquals(version, meta.getVersion().get());
  }

  /**
   * will test the setter for the date times with string parameter
   */
  @Test
  public void testSetCreatedWithString()
  {
    Meta meta = new Meta();

    final String created = "2019-09-29T24:00:00.0000000-10:00";
    final String lastModified = "2019-09-29T24:00:00Z";
    meta.setCreated(created);
    meta.setLastModified(lastModified);

    Assertions.assertEquals(TimeUtils.parseDateTime(created), meta.getCreated().get());
    Assertions.assertEquals(TimeUtils.parseDateTime(lastModified), meta.getLastModified().get());
  }

  /**
   * will test the setter for the date times with instant parameter
   */
  @Test
  public void testSetCreatedWithInstant()
  {
    Meta meta = new Meta();

    final Instant created = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    final Instant lastModified = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    meta.setCreated(created);
    meta.setLastModified(lastModified);

    Assertions.assertEquals(created, meta.getCreated().get());
    Assertions.assertEquals(lastModified, meta.getLastModified().get());
  }

  /**
   * will test the setter for the date times with LocalDateTime parameter
   */
  @Test
  public void testSetCreatedWithLocalDateTime()
  {
    Meta meta = new Meta();

    final LocalDateTime created = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    final LocalDateTime lastModified = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    meta.setCreated(created);
    meta.setLastModified(lastModified);

    Assertions.assertEquals(created.atZone(ZoneId.systemDefault()).toInstant(), meta.getCreated().get());
    Assertions.assertEquals(lastModified.atZone(ZoneId.systemDefault()).toInstant(), meta.getLastModified().get());
  }

  /**
   * will test the setter for the date times with OffsetDateTime parameter
   */
  @Test
  public void testSetCreatedWithOffsetDateTime()
  {
    Meta meta = new Meta();

    final OffsetDateTime created = Instant.now().atOffset(ZoneOffset.ofHours(14)).withNano(0);
    final OffsetDateTime lastModified = Instant.now().atOffset(ZoneOffset.ofHours(-14)).withNano(0);
    meta.setCreated(created);
    meta.setLastModified(lastModified);

    Assertions.assertEquals(created.toInstant(), meta.getCreated().get());
    Assertions.assertEquals(lastModified.toInstant(), meta.getLastModified().get());
  }
}

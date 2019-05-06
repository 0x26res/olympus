package org.aa.olympus.example;

import com.google.common.collect.ImmutableSet;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SubscriptionTypeExample {

  public static final EntityKey<String, String> MANDATORY_INPUT =
      Olympus.key("MANDATORY_INPUT", String.class, String.class);
  public static final EntityKey<String, String> MANDATORY =
      Olympus.key("MANDATORY", String.class, String.class);
  public static final EntityKey<String, String> OPTIONAL_INPUT =
      Olympus.key("OPTIONAL_INPUT", String.class, String.class);
  public static final EntityKey<String, String> OPTIONAL =
      Olympus.key("OPTIONAL", String.class, String.class);
  public static final EntityKey<String, String> RESULT =
      Olympus.key("RESULT", String.class, String.class);

  private Engine engine;

  @Before
  public void setUp() {
    engine =
        Olympus.builder()
            .registerSource(MANDATORY_INPUT)
            .registerEntity(
                MANDATORY, new FailOn42Manager(MANDATORY_INPUT), ImmutableSet.of(MANDATORY_INPUT))
            .registerSource(OPTIONAL_INPUT)
            .registerEntity(
                OPTIONAL, new FailOn42Manager(OPTIONAL_INPUT), ImmutableSet.of(OPTIONAL_INPUT))
            .registerEntity(RESULT, new MyElementManger(), ImmutableSet.of(MANDATORY, OPTIONAL))
            .build();
  }

  @Test
  public void testBothReady() {

    engine.setSourceState(MANDATORY_INPUT, "foo", "FOO");
    engine.setSourceState(OPTIONAL_INPUT, "foo", "FOO");
    engine.runOnce();
    Assert.assertEquals("FOO/FOO", engine.getState(RESULT, "foo"));
  }

  @Test
  public void testMandatoryReady() {

    engine.setSourceState(MANDATORY_INPUT, "foo", "FOO");
    engine.runOnce();
    Assert.assertEquals("FOO/no value", engine.getState(RESULT, "foo"));
  }

  @Test
  public void testOptionalReady() {
    engine.setSourceState(OPTIONAL_INPUT, "foo", "FOO");
    engine.runOnce();
    Assert.assertNull(engine.getState(RESULT, "foo"));
    Assert.assertEquals(ElementStatus.NOT_READY, engine.getElement(RESULT, "foo").getStatus());
  }

  @Test
  public void testNeitherReady() {
    engine.runOnce();
    Assert.assertEquals(ElementStatus.SHADOW, engine.getElement(RESULT, "foo").getStatus());
  }

  @Test
  public void testWithFailures() {
    // Mandatory missing, Optional error
    engine.setSourceState(OPTIONAL_INPUT, "foo", "42");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.ERROR, engine.getElement(OPTIONAL, "foo").getStatus());
    Assert.assertEquals(ElementStatus.NOT_READY, engine.getElement(RESULT, "foo").getStatus());
    // Mandatory present, Optional error
    engine.setSourceState(MANDATORY_INPUT, "foo", "foo");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.ERROR, engine.getElement(OPTIONAL, "foo").getStatus());
    Assert.assertEquals(ElementStatus.UPDATED, engine.getElement(RESULT, "foo").getStatus());
    Assert.assertEquals("foo/no value", engine.getElement(RESULT, "foo").getState());
    // Mandatory present, Optional present
    engine.setSourceState(OPTIONAL_INPUT, "foo", "FOO");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.UPDATED, engine.getElement(OPTIONAL, "foo").getStatus());
    Assert.assertEquals(ElementStatus.UPDATED, engine.getElement(MANDATORY, "foo").getStatus());
    Assert.assertEquals(ElementStatus.UPDATED, engine.getElement(RESULT, "foo").getStatus());
    Assert.assertEquals("foo/FOO", engine.getElement(RESULT, "foo").getState());
    // Mandatory fails, Optional present
    engine.setSourceState(MANDATORY_INPUT, "foo", "42");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.UPDATED, engine.getElement(OPTIONAL, "foo").getStatus());
    Assert.assertEquals(ElementStatus.ERROR, engine.getElement(MANDATORY, "foo").getStatus());
    Assert.assertEquals(ElementStatus.UPSTREAM_ERROR, engine.getElement(RESULT, "foo").getStatus());
    Assert.assertNull(engine.getElement(RESULT, "foo").getState());
    // Both presents
    engine.setSourceState(MANDATORY_INPUT, "foo", "FOO");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.UPDATED, engine.getElement(OPTIONAL, "foo").getStatus());
    Assert.assertEquals(ElementStatus.UPDATED, engine.getElement(MANDATORY, "foo").getStatus());
    Assert.assertEquals(ElementStatus.UPDATED, engine.getElement(RESULT, "foo").getStatus());
    Assert.assertEquals("FOO/FOO", engine.getElement(RESULT, "foo").getState());
  }

  public static class MyElementUpdater implements ElementUpdater<String> {

    ElementHandle<String, String> mandatory;
    ElementHandle<String, String> optional;

    MyElementUpdater(String key, Toolbox toolbox) {
      mandatory = toolbox.get(MANDATORY, key).subscribe(SubscriptionType.STRONG);
      optional = toolbox.get(OPTIONAL, key).subscribe(SubscriptionType.WEAK);
    }

    @Override
    public UpdateResult<String> update(
        String previous, UpdateContext updateContext, Toolbox toolbox) {
      return UpdateResult.maybe(mandatory.getState() + '/' + optional.getStateOrDefault("no value"));
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      // We don't care we've already subscribed in the constructor
      return true;
    }
  }

  public static class MyElementManger implements ElementManager<String, String> {

    @Override
    public ElementUpdater<String> create(String key, UpdateContext updateContext, Toolbox toolbox) {
      return new MyElementUpdater(key, toolbox);
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<String> toNotify) {
      toNotify.accept((String) key);
    }
  }

  public static class FailOn42Updater implements ElementUpdater<String> {

    private final ElementHandle<String, String> input;

    FailOn42Updater(EntityKey<String, String> sourceEntity, String key, Toolbox toolbox) {
      input = toolbox.get(sourceEntity, key).subscribe(SubscriptionType.STRONG);
    }

    @Override
    public UpdateResult<String> update(
        String previous, UpdateContext updateContext, Toolbox toolbox) {
      if (input.getState().equals("42")) {
        throw new IllegalArgumentException("42");
      } else {
        return UpdateResult.maybe(input.getState());
      }
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      return true;
    }
  }

  public static class FailOn42Manager implements ElementManager<String, String> {

    private final EntityKey<String, String> sourceEntity;

    public FailOn42Manager(EntityKey<String, String> sourceEntity) {
      this.sourceEntity = sourceEntity;
    }

    @Override
    public ElementUpdater<String> create(String key, UpdateContext updateContext, Toolbox toolbox) {
      return new FailOn42Updater(sourceEntity, key, toolbox);
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<String> toNotify) {
      toNotify.accept((String) key);
    }
  }
}

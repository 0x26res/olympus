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

  private static final EntityKey<String, String> MANDATORY_INPUT =
      Olympus.key("MANDATORY_INPUT", String.class, String.class);
  private static final EntityKey<String, String> MANDATORY =
      Olympus.key("MANDATORY", String.class, String.class);

  private static final EntityKey<String, String> OPTIONAL_INPUT =
      Olympus.key("OPTIONAL_INPUT", String.class, String.class);
  private static final EntityKey<String, String> OPTIONAL =
      Olympus.key("OPTIONAL", String.class, String.class);

  private static final EntityKey<String, String> WEAK_INPUT =
      Olympus.key("WEAK_INPUT", String.class, String.class);
  private static final EntityKey<String, String> WEAK =
      Olympus.key("WEAK", String.class, String.class);
  private static final EntityKey<String, String> RESULT =
      Olympus.key("RESULT", String.class, String.class);

  private Engine engine;

  @Before
  public void setUp() {
    engine =
        Olympus.builder()
            .registerSource(MANDATORY_INPUT)
            .registerInnerEntity(
                MANDATORY, new FailOn42Manager(MANDATORY_INPUT), ImmutableSet.of(MANDATORY_INPUT))
            .registerSource(OPTIONAL_INPUT)
            .registerInnerEntity(
                OPTIONAL, new FailOn42Manager(OPTIONAL_INPUT), ImmutableSet.of(OPTIONAL_INPUT))
            .registerSource(WEAK_INPUT)
            .registerInnerEntity(WEAK, new FailOn42Manager(WEAK_INPUT), ImmutableSet.of(WEAK_INPUT))
            .registerInnerEntity(
                RESULT, new MyElementManger(), ImmutableSet.of(MANDATORY, WEAK, OPTIONAL))
            .build();
  }

  @Test
  public void testAllReady() {

    engine.setSourceState(MANDATORY_INPUT, "foo", "FOO");
    engine.setSourceState(OPTIONAL_INPUT, "foo", "FOO");
    engine.setSourceState(WEAK_INPUT, "foo", "FOO");
    engine.runOnce();
    Assert.assertEquals("FOO/FOO/FOO", engine.getState(RESULT, "foo"));
  }

  @Test
  public void testMandatoryReady() {

    engine.setSourceState(MANDATORY_INPUT, "foo", "FOO");
    engine.runOnce();
    Assert.assertEquals("FOO/no value/no value", engine.getState(RESULT, "foo"));
  }

  @Test
  public void testWeakReady() {
    engine.setSourceState(WEAK_INPUT, "foo", "FOO");
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
    // Mandatory missing, weak error
    engine.setSourceState(WEAK_INPUT, "foo", "42");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.ERROR, engine.getElement(WEAK, "foo").getStatus());
    Assert.assertEquals(ElementStatus.NOT_READY, engine.getElement(RESULT, "foo").getStatus());
    Assert.assertEquals(1, engine.getElement(RESULT, "foo").getUpdateContext().getUpdateId());
    // Mandatory present, weak error
    engine.setSourceState(MANDATORY_INPUT, "foo", "foo");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.ERROR, engine.getElement(WEAK, "foo").getStatus());
    Assert.assertEquals(ElementStatus.OK, engine.getElement(RESULT, "foo").getStatus());
    Assert.assertEquals("foo/no value/no value", engine.getElement(RESULT, "foo").getState());
    Assert.assertEquals(2, engine.getElement(RESULT, "foo").getUpdateContext().getUpdateId());
    // Mandatory present, weak present
    engine.setSourceState(WEAK_INPUT, "foo", "FOO");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.OK, engine.getElement(WEAK, "foo").getStatus());
    Assert.assertEquals(ElementStatus.OK, engine.getElement(MANDATORY, "foo").getStatus());
    Assert.assertEquals(ElementStatus.OK, engine.getElement(RESULT, "foo").getStatus());
    Assert.assertEquals(2, engine.getElement(RESULT, "foo").getUpdateContext().getUpdateId());
    Assert.assertEquals(
        "No update because dependency is weak",
        "foo/no value/no value",
        engine.getElement(RESULT, "foo").getState());
    // Mandatory present, weak present + TICK
    engine.setSourceState(MANDATORY_INPUT, "foo", "FOO1");
    engine.runOnce();
    Assert.assertEquals(4, engine.getElement(RESULT, "foo").getUpdateContext().getUpdateId());
    Assert.assertEquals(
        "No update because dependency is weak",
        "FOO1/no value/FOO",
        engine.getElement(RESULT, "foo").getState());
    // Mandatory fails, Weak present
    engine.setSourceState(MANDATORY_INPUT, "foo", "42");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.OK, engine.getElement(WEAK, "foo").getStatus());
    Assert.assertEquals(ElementStatus.ERROR, engine.getElement(MANDATORY, "foo").getStatus());
    Assert.assertEquals(ElementStatus.UPSTREAM_ERROR, engine.getElement(RESULT, "foo").getStatus());
    Assert.assertNull(engine.getElement(RESULT, "foo").getState());
    // Mandatory OK, Weak present, Optional Present
    engine.setSourceState(MANDATORY_INPUT, "foo", "FOO");
    engine.setSourceState(OPTIONAL_INPUT, "foo", "FOO");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.OK, engine.getElement(WEAK, "foo").getStatus());
    Assert.assertEquals(ElementStatus.OK, engine.getElement(MANDATORY, "foo").getStatus());
    Assert.assertEquals(ElementStatus.OK, engine.getElement(RESULT, "foo").getStatus());
    Assert.assertEquals("FOO/FOO/FOO", engine.getElement(RESULT, "foo").getState());
    // Optional fails
    engine.setSourceState(OPTIONAL_INPUT, "foo", "42");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.ERROR, engine.getElement(OPTIONAL, "foo").getStatus());
    Assert.assertEquals("FOO/no value/FOO", engine.getElement(RESULT, "foo").getState());
    // Weak and Optional update, so everything updates
    engine.setSourceState(OPTIONAL_INPUT, "foo", "foo");
    engine.setSourceState(WEAK_INPUT, "foo", "foo");
    engine.runOnce();
    Assert.assertEquals(ElementStatus.OK, engine.getElement(OPTIONAL, "foo").getStatus());
    Assert.assertEquals("FOO/foo/foo", engine.getElement(RESULT, "foo").getState());
  }

  public static class MyElementUpdater implements ElementUpdater<String> {

    ElementHandle<String, String> mandatory;
    ElementHandle<String, String> optional;
    ElementHandle<String, String> weak;

    MyElementUpdater(String key, Toolbox toolbox) {
      mandatory = toolbox.get(MANDATORY, key).subscribe(SubscriptionType.STRONG);
      optional = toolbox.get(OPTIONAL, key).subscribe(SubscriptionType.OPTIONAL);
      weak = toolbox.get(WEAK, key).subscribe(SubscriptionType.WEAK);
    }

    @Override
    public UpdateResult<String> update(
        String previous, UpdateContext updateContext, Toolbox toolbox) {
      return UpdateResult.maybe(
          mandatory.getState()
              + '/'
              + optional.getStateOrDefault("no value")
              + '/'
              + weak.getStateOrDefault("no value"));
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

    FailOn42Manager(EntityKey<String, String> sourceEntity) {
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

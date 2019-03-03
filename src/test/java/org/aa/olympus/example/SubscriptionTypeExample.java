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

  public static final EntityKey<String, String> MANDATORY =
      Olympus.key("MANDATORY", String.class, String.class);
  public static final EntityKey<String, String> OPTIONAL =
      Olympus.key("OPTIONAL", String.class, String.class);
  public static final EntityKey<String, String> RESULT =
      Olympus.key("RESULT", String.class, String.class);

  private Engine engine;

  @Before
  public void setUp() {
    engine = Olympus.builder()
        .registerSource(MANDATORY)
        .registerSource(OPTIONAL)
        .registerEntity(
            RESULT,
            new MyElementManger(),
            ImmutableSet.of(MANDATORY, OPTIONAL))
        .build();
  }

  @Test
  public void testBothReady() {

    engine.setSourceState(MANDATORY, "foo", "FOO");
    engine.setSourceState(OPTIONAL, "foo", "FOO");
    engine.runOnce();
    Assert.assertEquals("FOO/FOO", engine.getState(RESULT, "foo"));

  }

  @Test
  public void testMandatoryReady() {

    engine.setSourceState(MANDATORY, "foo", "FOO");
    engine.runOnce();
    Assert.assertEquals("FOO/no value", engine.getState(RESULT, "foo"));

  }

  @Test
  public void testOptionalReady() {
    engine.setSourceState(OPTIONAL, "foo", "FOO");
    engine.runOnce();
    Assert.assertNull(engine.getState(RESULT, "foo"));
    Assert.assertEquals(ElementStatus.NOT_READY, engine.getElement(RESULT, "foo").getStatus());
  }

  public static class MyElementUpdater implements ElementUpdater<String> {

    ElementHandle<String, String> mandatory;
    ElementHandle<String, String> optional;

    MyElementUpdater(String key, Toolbox toolbox) {
      mandatory = toolbox.get(MANDATORY, key).subscribe(SubscriptionType.STRONG);
      optional = toolbox.get(OPTIONAL, key).subscribe(SubscriptionType.WEAK);

    }

    @Override
    public UpdateResult<String> update(String previous, UpdateContext updateContext,
        Toolbox toolbox) {
      return UpdateResult.maybe(previous,
          mandatory.getState() + '/' +
              optional.getStateOrDefault("no value"));
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


}

package org.aa.olympus.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.examples.KeyValuePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestMaybe {

  private static final EventChannel<KeyValuePair<String, String>> SOURCE_CHANNEL =
      Olympus.channel("SOURCE", new TypeToken<KeyValuePair<String, String>>() {});
  private static final EntityKey<String, String> SOURCE =
      Olympus.key("SOURCE", String.class, String.class);
  private static final EntityKey<String, String> ENTITY =
      Olympus.key("ENTITY", String.class, String.class);

  private Engine engine;

  @Before
  public void setUp() {

    engine =
        Olympus.builder()
            .registerEventChannel(SOURCE_CHANNEL)
            .eventToEntity(SOURCE_CHANNEL, SOURCE, KeyValuePair::getKey, KeyValuePair::getValue)
            .registerInnerEntity(ENTITY, new PassThroughManager(), ImmutableSet.of(SOURCE))
            .build();
  }

  private void inject(String key, String state) {
    engine.injectEvent(SOURCE_CHANNEL, KeyValuePair.of(key, state));
  }

  @Test
  public void testMaybe() {

    inject("foo", "FOO");
    engine.runOnce();

    ElementView<String, String> element = engine.getElement(ENTITY, "foo");
    Assert.assertEquals("FOO", element.getState());
    Assert.assertEquals(1, element.getUpdateContext().getUpdateId());

    inject("foo", "FOO");
    engine.runOnce();
    Assert.assertEquals("FOO", element.getState());
    Assert.assertEquals(1, element.getUpdateContext().getUpdateId());

    inject("foo", "FOO2");
    engine.runOnce();
    Assert.assertEquals("FOO2", element.getState());
    Assert.assertEquals(3, element.getUpdateContext().getUpdateId());
  }

  private static class PassThroughUpdater implements ElementUpdater<String> {

    final ElementHandle<String, String> input;

    private PassThroughUpdater(ElementHandle<String, String> input) {
      this.input = input;
    }

    @Override
    public UpdateResult<String> update(
        String previous, UpdateContext updateContext, Toolbox toolbox) {
      return UpdateResult.maybe(input.getState());
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      return true;
    }
  }

  private static class PassThroughManager implements ElementManager<String, String> {

    @Override
    public ElementUpdater<String> create(String key, UpdateContext updateContext, Toolbox toolbox) {
      return new PassThroughUpdater(toolbox.get(SOURCE, key).subscribe(SubscriptionType.STRONG));
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<String> toNotify) {
      toNotify.accept((String) key);
    }
  }
}

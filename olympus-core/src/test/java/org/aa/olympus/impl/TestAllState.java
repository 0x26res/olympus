package org.aa.olympus.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import java.util.function.Supplier;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.api.Notifier;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.examples.KeyValuePair;
import org.aa.olympus.utils.OlympusAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestAllState {

  private static final class Message {

    private final Supplier<UpdateResult<String>> supplier;

    static Message ofValue(UpdateResult<String> updateResult) {
      return new Message(() -> updateResult);
    }

    private Message(Supplier<UpdateResult<String>> supplier) {
      this.supplier = supplier;
    }
  }

  private static final EventChannel<KeyValuePair<String, Message>> SOURCE_CHANNEL =
      Olympus.channel("SOURCE", new TypeToken<KeyValuePair<String, Message>>() {});

  private static final EntityKey<String, Message> SOURCE =
      Olympus.key("SOURCE", String.class, Message.class);
  private static final EntityKey<String, String> ENTITY =
      Olympus.key("ENTITY", String.class, String.class);

  private Engine engine;
  ElementView<String, String> handle;

  @Before
  public void setUp() {
    engine =
        Olympus.builder()
            .registerEventChannel(SOURCE_CHANNEL)
            .eventToEntity(SOURCE_CHANNEL, SOURCE, KeyValuePair::getKey, KeyValuePair::getValue)
            .registerInnerEntity(ENTITY, new MessagePasserManager(), ImmutableSet.of(SOURCE))
            .build();
    handle = engine.getElement(ENTITY, "foo");
  }

  @Test
  public void testWorkflow() {

    Assert.assertSame(UpdateContextImpl.NONE, handle.getUpdateContext());
    Assert.assertEquals(ElementStatus.SHADOW, handle.getStatus());

    engine.runOnce();
    Assert.assertSame(UpdateContextImpl.NONE, handle.getUpdateContext());
    Assert.assertEquals(ElementStatus.SHADOW, handle.getStatus());

    inject("foo", Message.ofValue(UpdateResult.update("FOO")));
    engine.runOnce();
    Assert.assertEquals(2, handle.getUpdateContext().getUpdateId());
    Assert.assertEquals("FOO", handle.getState());
    Assert.assertEquals(ElementStatus.OK, handle.getStatus());

    inject("foo", new Message(new RaiseSupplier()));
    engine.runOnce();
    Assert.assertEquals(ElementStatus.ERROR, handle.getStatus());

    // TODO: test downstream fails

  }

  void inject(String key, Message message) {
    engine.injectEvent(SOURCE_CHANNEL, KeyValuePair.of(key, message));
  }

  @Test
  public void testNotReady() {

    OlympusAssert.assertElement(handle, ElementStatus.SHADOW, null, 0);

    inject("foo", Message.ofValue(UpdateResult.notReady()));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.NOT_READY, null, 1);

    inject("foo", Message.ofValue(UpdateResult.notReady()));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.NOT_READY, null, 1);

    inject("foo", Message.ofValue(UpdateResult.update("FOO")));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.OK, "FOO", 3);

    inject("foo", Message.ofValue(UpdateResult.notReady()));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.NOT_READY, null, 4);
  }

  @Test
  public void testFirstCycleNothing() {
    OlympusAssert.assertElement(handle, ElementStatus.SHADOW, null, 0);
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.SHADOW, null, 0);
  }

  @Test
  public void testDelete() {
    inject("foo", Message.ofValue(UpdateResult.notReady()));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.NOT_READY, null, 1);

    inject("foo", Message.ofValue(UpdateResult.delete()));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.DELETED, null, 2);
  }

  @Test
  public void testMaybe() {
    inject("foo", Message.ofValue(UpdateResult.maybe("FOO")));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.OK, "FOO", 1);

    inject("foo", Message.ofValue(UpdateResult.maybe("FOO")));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.OK, "FOO", 1);

    inject("foo", Message.ofValue(UpdateResult.maybe("BAR")));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.OK, "BAR", 3);

    inject("foo", Message.ofValue(UpdateResult.maybe("FOO")));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.OK, "FOO", 4);
  }

  static final class RaiseSupplier implements Supplier<UpdateResult<String>> {

    @Override
    public UpdateResult<String> get() {
      throw new RuntimeException("Not today");
    }
  }

  static final class MessagePasserUpdater implements ElementUpdater<String> {

    ElementHandle<String, Message> source;

    @Override
    public UpdateResult<String> update(
        String previous, UpdateContext updateContext, Toolbox toolbox) {
      return source.getState().supplier.get();
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      this.source = SOURCE.castHandle(handle.subscribe(SubscriptionType.STRONG));
      return true;
    }
  }

  public static final class MessagePasserManager implements ElementManager<String, String> {

    @Override
    public ElementUpdater<String> create(String key, UpdateContext updateContext, Toolbox toolbox) {
      return new MessagePasserUpdater();
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Notifier<String> notifier) {
      notifier.notifyElement((String) key);
    }
  }
}

package org.aa.olympus.impl;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.ELementToolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
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

  private static final EntityKey<String, Message> SOURCE =
      Olympus.key("SOURCE", String.class, Message.class);
  private static final EntityKey<String, String> ENTITY =
      Olympus.key("ENTITY", String.class, String.class);
  private static final EntityKey<String, String> SUBSCIRBER =
      Olympus.key("SUBSCRIBER", String.class, String.class);

  private Engine engine;
  ElementView<String, String> handle;
  ElementView<String, String> subscirber;

  @Before
  public void setUp() {
    engine =
        Olympus.builder()
            .registerSource(SOURCE)
            .registerInnerEntity(ENTITY, new MessagePasserManager(), ImmutableSet.of(SOURCE))
            .registerSimpleEntity(SUBSCIRBER, p -> new DummyConcatenator(), ImmutableSet.of(ENTITY))
            .build();
    handle = engine.getElement(ENTITY, "foo");
    subscirber = engine.getElement(SUBSCIRBER, "foo");
  }

  @Test
  public void testWorkflow() {

    OlympusAssert.assertElement(handle, ElementStatus.SHADOW, null, 0);

    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.SHADOW, null, 0);

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.update("FOO")));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.OK, "FOO", 2);
    OlympusAssert.assertElement(subscirber, ElementStatus.OK, "FOO", 2);

    engine.setSourceState(SOURCE, "foo", new Message(new RaiseSupplier()));
    engine.runOnce();

    OlympusAssert.assertElement(handle, ElementStatus.ERROR, null, 3);
    Assert.assertEquals("Not today", handle.getError().getMessage());

    OlympusAssert.assertElement(subscirber, ElementStatus.UPSTREAM_ERROR, null, 3);
  }

  @Test
  public void testNotReady() {

    OlympusAssert.assertElement(handle, ElementStatus.SHADOW, null, 0);

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.notReady()));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.NOT_READY, null, 1);

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.notReady()));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.NOT_READY, null, 1);

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.update("FOO")));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.OK, "FOO", 3);

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.notReady()));
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
    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.notReady()));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.NOT_READY, null, 1);

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.delete()));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.DELETED, null, 2);
  }

  @Test
  public void testMaybe() {
    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.maybe("FOO")));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.OK, "FOO", 1);

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.maybe("FOO")));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.OK, "FOO", 1);

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.maybe("BAR")));
    engine.runOnce();
    OlympusAssert.assertElement(handle, ElementStatus.OK, "BAR", 3);

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.maybe("FOO")));
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
        String previous, UpdateContext updateContext, ELementToolbox ELementToolbox) {
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
    public ElementUpdater<String> create(String key, UpdateContext updateContext, ELementToolbox ELementToolbox) {
      return new MessagePasserUpdater();
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<String> toNotify) {
      toNotify.accept((String) key);
    }
  }

  public static final class DummyConcatenator implements ElementUpdater<String> {

    final List<ElementHandle<?, String>> handles = new ArrayList<>();

    @Override
    public UpdateResult<String> update(
        String previous, UpdateContext updateContext, ELementToolbox ELementToolbox) {
      return UpdateResult.maybe(
          handles.stream().map(ElementView::getState).collect(Collectors.joining()));
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      handle.subscribe(SubscriptionType.STRONG);
      handles.add((ElementHandle<?, String>) handle);
      return true;
    }
  }
}

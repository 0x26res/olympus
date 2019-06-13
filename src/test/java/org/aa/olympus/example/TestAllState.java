package org.aa.olympus.example;

import com.google.common.collect.ImmutableSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.impl.UpdateContextImpl;
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

  @Before
  public void setUp() {
    engine =
        Olympus.builder()
            .registerSource(SOURCE)
            .registerEntity(ENTITY, new MessagePasserManager(), ImmutableSet.of(SOURCE))
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

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.update("FOO")));
    engine.runOnce();
    Assert.assertEquals(2, handle.getUpdateContext().getUpdateId());
    Assert.assertEquals("FOO", handle.getState());
    Assert.assertEquals(ElementStatus.UPDATED, handle.getStatus());

    engine.setSourceState(SOURCE, "foo", new Message(new RaiseSupplier()));
    engine.runOnce();
    Assert.assertEquals(ElementStatus.ERROR, handle.getStatus());

    // TODO: test downstream fails

  }

  @Test
  public void testNotReady() {

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.notReady()));
    engine.runOnce();
    Assert.assertEquals(ElementStatus.NOT_READY, handle.getStatus());

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.notReady()));
    engine.runOnce();
    Assert.assertEquals(ElementStatus.NOT_READY, handle.getStatus());

    engine.setSourceState(SOURCE, "foo", Message.ofValue(UpdateResult.update("FOO")));
    engine.runOnce();
    Assert.assertEquals(ElementStatus.UPDATED, handle.getStatus());
  }

  static final class RaiseSupplier implements Supplier<UpdateResult<String>> {

    @Override
    public UpdateResult<String> get() {
      throw new RuntimeException("Not today");
    }
  }

  static final class MessagePasserUpdater implements ElementUpdater<String> {

    ElementHandle<String, Message> source;

    MessagePasserUpdater() {
      this.source = source;
    }

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
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<String> toNotify) {
      toNotify.accept((String) key);
    }
  }
}

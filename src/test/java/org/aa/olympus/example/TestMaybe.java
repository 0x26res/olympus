package org.aa.olympus.example;

import com.google.common.collect.ImmutableSet;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
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

public class TestMaybe {

  private static final EntityKey<String, String> SOURCE =
      Olympus.key("SOURCE", String.class, String.class);
  private static final EntityKey<String, String> ENTITY =
      Olympus.key("ENTITY", String.class, String.class);

  private Engine engine;

  @Before
  public void setUp() {

    engine =
        Olympus.builder()
            .registerSource(SOURCE)
            .registerEntity(ENTITY, new PassThroughManager(), ImmutableSet.of(SOURCE))
            .build();
  }

  @Test
  public void testMaybe() {

    engine.setSourceState(SOURCE, "foo", "FOO");
    engine.runOnce();
    Assert.assertEquals("FOO", engine.getElement(ENTITY, "foo").getState());
    // TODO: run again and check that the entity did not update but the source did

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

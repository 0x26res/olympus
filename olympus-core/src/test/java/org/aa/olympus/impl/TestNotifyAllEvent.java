package org.aa.olympus.impl;

import com.google.common.collect.ImmutableSet;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Event;
import org.aa.olympus.api.Notifier;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.examples.WorldCountExample;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestNotifyAllEvent {

  static final class WildKeyCounterElementManager extends WorldCountExample.CounterElementManager {

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Notifier<String> notifier) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <E> void onEvent(Event<E> event, Notifier<String> notifier) {
      if (event.getChannel().equals(WorldCountExample.WORDS)) {
        String word = (String) event.getValue();
        if (word.equals("*")) {
          notifier.notifyAllElements();
        } else {
          notifier.notifyElement(word);
        }
      }
    }
  }

  static Engine createEngine() {
    return Olympus.builder()
        .registerEventChannel(WorldCountExample.WORDS)
        .registerInnerEntity(
            WorldCountExample.COUNTER,
            new WildKeyCounterElementManager(),
            ImmutableSet.of(),
            ImmutableSet.of(WorldCountExample.WORDS))
        .build();
  }

  private Engine engine;

  @Before
  public void setUp() {
    engine = createEngine();
  }

  @Test
  public void test() {
    engine.injectEvent(WorldCountExample.WORDS, "HELLO").runOnce();

    ElementView<String, Integer> hello = engine.getElement(WorldCountExample.COUNTER, "HELLO");
    Assert.assertEquals(1, hello.getState().intValue());

    engine
        .injectEvent(WorldCountExample.WORDS, "WORLD")
        .injectEvent(WorldCountExample.WORDS, "*")
        .runOnce();

    ElementView<String, Integer> world = engine.getElement(WorldCountExample.COUNTER, "WORLD");
    Assert.assertEquals(2, hello.getState().intValue());
    Assert.assertEquals(2, world.getState().intValue());

    engine
        .injectEvent(WorldCountExample.WORDS, "*")
        .injectEvent(WorldCountExample.WORDS, "DID_NOT_EXIST")
        .runOnce();
    // If the wild key event comes before the creation, we ignore it.
    ElementView<String, Integer> didNotExist =
        engine.getElement(WorldCountExample.COUNTER, "DID_NOT_EXIST");
    Assert.assertEquals(1, didNotExist.getState().intValue());
  }
}

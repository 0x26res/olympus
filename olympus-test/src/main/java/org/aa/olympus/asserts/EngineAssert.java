package org.aa.olympus.asserts;

import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;

public class EngineAssert {

  private final Engine engine;

  public EngineAssert(Engine engine) {
    this.engine = engine;
  }

  public <K, S> ElementAssert<K, S> createElement(EntityKey<K, S> entityKey, K key) {
    return new ElementAssert<>(engine.getElement(entityKey, key));
  }

  public <K, S> void assertElement(
      EntityKey<K, S> entityKey,
      K key,
      ElementStatus expectedStatus,
      S expectedState,
      int expectedUpdateId) {
    OlympusAssert.assertElement(
        engine.getElement(entityKey, key), expectedStatus, expectedState, expectedUpdateId);
  }
}

package org.aa.olympus.asserts;

import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.junit.Assert;

public final class OlympusAssert {

  private OlympusAssert() {}

  public static <K, S> void assertElement(
      ElementView<K, S> element,
      ElementStatus expectedStatus,
      S expectedState,
      int expectedUpdateId) {

    Assert.assertEquals(expectedStatus, element.getStatus());
    Assert.assertEquals(expectedState, element.getState());
    Assert.assertEquals(expectedUpdateId, element.getUpdateContext().getUpdateId());
  }

  public static <K, S> void assertElement(
      Engine engine,
      EntityKey<K, S> entityKey,
      K key,
      ElementStatus expectedStatus,
      S expectedState,
      int expectedUpdateId) {

    assertElement(
        engine.getElement(entityKey, key), expectedStatus, expectedState, expectedUpdateId);
  }
}

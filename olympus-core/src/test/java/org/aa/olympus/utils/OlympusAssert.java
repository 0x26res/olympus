package org.aa.olympus.utils;

import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementView;
import org.junit.Assert;

/** This is copied from the test library */
public final class OlympusAssert {

  private OlympusAssert() {}

  public static <K, S> void assertElement(
      ElementView<K, S> view, ElementStatus expectedStatus, S expectedState, int expectedUpdateId) {

    Assert.assertEquals(expectedStatus, view.getStatus());
    Assert.assertEquals(expectedState, view.getState());
    Assert.assertEquals(expectedUpdateId, view.getUpdateContext().getUpdateId());
  }
}

package org.aa.olympus.asserts;

import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementView;

public class ElementAssert<K, S> {

  private final ElementView<K, S> element;

  public ElementAssert(ElementView<K, S> element) {
    this.element = element;
  }

  public void assertElement(ElementStatus expectedStatus, S expectedState, int expectedUpdateId) {
    OlympusAssert.assertElement(element, expectedStatus, expectedState, expectedUpdateId);
  }
}

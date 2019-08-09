package org.aa.olympus.examples;

import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.Engine;
import org.aa.olympus.utils.OlympusAssert;
import org.junit.Test;

public class SimplesExampleTest {

  @Test
  public void simpleTest() {

    Engine engine = SimplestExample.createEngine();
    engine.injectEvent(SimplestExample.WORDS, "HELLO");
    engine.injectEvent(SimplestExample.WORDS, "HELLO");
    engine.injectEvent(SimplestExample.WORDS, "HELLO");
    engine.injectEvent(SimplestExample.WORDS, "WORLD");
    engine.injectEvent(SimplestExample.WORDS, "WORLD");

    engine.runOnce();

    OlympusAssert.assertElement(
        engine.getElement(SimplestExample.COUNTER, "HELLO"), ElementStatus.OK, 3, 1);
    OlympusAssert.assertElement(
        engine.getElement(SimplestExample.COUNTER, "WORLD"), ElementStatus.OK, 2, 1);

    engine.runOnce();

    OlympusAssert.assertElement(
        engine.getElement(SimplestExample.COUNTER, "HELLO"), ElementStatus.OK, 3, 1);
    OlympusAssert.assertElement(
        engine.getElement(SimplestExample.COUNTER, "WORLD"), ElementStatus.OK, 2, 1);

    engine.injectEvent(SimplestExample.WORDS, "WORLD");
    engine.runOnce();

    OlympusAssert.assertElement(
        engine.getElement(SimplestExample.COUNTER, "HELLO"), ElementStatus.OK, 3, 1);
    OlympusAssert.assertElement(
        engine.getElement(SimplestExample.COUNTER, "WORLD"), ElementStatus.OK, 3, 3);
  }
}

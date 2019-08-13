package org.aa.olympus.examples;

import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.Engine;
import org.aa.olympus.utils.OlympusAssert;
import org.junit.Test;

public class SimplesExampleTest {

  @Test
  public void simpleTest() {

    Engine engine = WorldCountExample.createEngine();
    engine.injectEvent(WorldCountExample.WORDS, "HELLO");
    engine.injectEvent(WorldCountExample.WORDS, "HELLO");
    engine.injectEvent(WorldCountExample.WORDS, "HELLO");
    engine.injectEvent(WorldCountExample.WORDS, "WORLD");
    engine.injectEvent(WorldCountExample.WORDS, "WORLD");

    engine.runOnce();

    OlympusAssert.assertElement(
        engine.getElement(WorldCountExample.COUNTER, "HELLO"), ElementStatus.OK, 3, 1);
    OlympusAssert.assertElement(
        engine.getElement(WorldCountExample.COUNTER, "WORLD"), ElementStatus.OK, 2, 1);

    engine.runOnce();

    OlympusAssert.assertElement(
        engine.getElement(WorldCountExample.COUNTER, "HELLO"), ElementStatus.OK, 3, 1);
    OlympusAssert.assertElement(
        engine.getElement(WorldCountExample.COUNTER, "WORLD"), ElementStatus.OK, 2, 1);

    engine.injectEvent(WorldCountExample.WORDS, "WORLD");
    engine.runOnce();

    OlympusAssert.assertElement(
        engine.getElement(WorldCountExample.COUNTER, "HELLO"), ElementStatus.OK, 3, 1);
    OlympusAssert.assertElement(
        engine.getElement(WorldCountExample.COUNTER, "WORLD"), ElementStatus.OK, 3, 3);
  }
}

package org.aa.olympus.examples;

import com.google.common.collect.ImmutableMap;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.Engine;
import org.aa.olympus.examples.IndexCalculatorExample.IndexComposition;
import org.aa.olympus.utils.OlympusAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

// TODO: add test for price being removed (set to null)
// TODO: use the lifecycle support (not ready)

/**
 * This example calculates index prices using 2 inputs:
 *
 * <ul>
 *   <li>Stock prices: for example GOOGLE is worth $135, IBM $230
 *   <li>Index composition (the weight of each index. For example the TECH index is made of 2 part
 *       GOOGLE and 3 part IBM
 * </ul>
 *
 * In this example the price of the TECH index is worth 960 (135*2 + 230*3). But we want to make
 * sure that the index price updates every time the price of it's constituents (GOOGLE, IBM) update.
 * Also we want to make sure that the index price updates when its composition changes. And last but
 * not least, if constituent are added/removed from the index, the index price should
 * subscribe/unsubscribe to constituent accordingly.
 */
public class IndexCalculatorExampleTest {

  private Engine engine;

  @Before
  public void setUp() {
    engine = IndexCalculatorExample.createEngine();
  }

  void feedPrice(String key, double value) {
    engine.injectEvent(IndexCalculatorExample.PRICE_CHANNEL, KeyValuePair.of(key, value));
  }

  void feedComposition(String key, IndexComposition composition) {
    engine.injectEvent(
        IndexCalculatorExample.COMPOSITION_CHANNEL, KeyValuePair.of(key, composition));
  }

  @Test
  public void testTechIndex() {

    feedPrice("GOOGLE", 130.0);
    feedPrice("IBM", 230.0);

    feedComposition("TECH", new IndexComposition(ImmutableMap.of("GOOGLE", 2.0, "IBM", 3.0)));
    engine.runOnce();
    Assert.assertEquals(
        130.0 * 2.0 + 230.0 * 3.0,
        engine.getState(IndexCalculatorExample.INDEX_PRICE, "TECH"),
        0.0);

    feedPrice("GOOGLE", 135.0);
    feedPrice("IBM", 230.0);
    engine.runOnce();
    Assert.assertEquals(
        135.0 * 2.0 + 230.0 * 3.0,
        engine.getState(IndexCalculatorExample.INDEX_PRICE, "TECH"),
        0.0);
  }

  @Test
  public void testCompositionUpdates() {

    feedPrice("A", 1.0);
    feedPrice("G", 2.0);
    feedPrice("IBM", 3.0);

    feedComposition("TECH", new IndexComposition(ImmutableMap.of("IBM", 1.0)));
    engine.runOnce();
    Assert.assertEquals(3.0, engine.getState(IndexCalculatorExample.INDEX_PRICE, "TECH"), 0.0);

    feedComposition("TECH", new IndexComposition(ImmutableMap.of("IBM", 0.5, "G", 0.5)));
    engine.runOnce();
    Assert.assertEquals(
        2.0 * 0.5 + 3.0 * 0.5, engine.getState(IndexCalculatorExample.INDEX_PRICE, "TECH"), 0.0);

    feedComposition("TECH", new IndexComposition(ImmutableMap.of("IBM", 0.5, "G", 1.0)));
    engine.runOnce();
    Assert.assertEquals(
        2.0 * 1.0 + 3.0 * 0.5, engine.getState(IndexCalculatorExample.INDEX_PRICE, "TECH"), 0.0);
  }

  @Test
  public void testPriceUpdate() {

    feedPrice("A", 2.0);
    feedComposition("NOT_READY", new IndexComposition(ImmutableMap.of("A", 1.0, "B", 1.0)));

    engine.runOnce();
    Assert.assertTrue(
        Double.isNaN(engine.getState(IndexCalculatorExample.INDEX_PRICE, "NOT_READY")));

    feedPrice("B", 4.0);
    engine.runOnce();
    Assert.assertEquals(6.0, engine.getState(IndexCalculatorExample.INDEX_PRICE, "NOT_READY"), 0.0);

    feedPrice("B", 3.0);
    engine.runOnce();
    Assert.assertEquals(5.0, engine.getState(IndexCalculatorExample.INDEX_PRICE, "NOT_READY"), 0.0);
  }

  @Test
  public void testWithElementView() {

    feedPrice("A", 2.0);
    feedPrice("B", 4.0);
    feedComposition("A+B", new IndexComposition(ImmutableMap.of("A", 1.0, "B", 1.0)));

    engine.runOnce();
    OlympusAssert.assertElement(
        engine.getElement(IndexCalculatorExample.INDEX_PRICE, "A+B"), ElementStatus.OK, 6.0, 1);

    feedPrice("B", 5.0);
    engine.runOnce();
    OlympusAssert.assertElement(
        engine.getElement(IndexCalculatorExample.INDEX_PRICE, "A+B"), ElementStatus.OK, 7.0, 2);

    engine.runOnce();
    OlympusAssert.assertElement(
        engine.getElement(IndexCalculatorExample.INDEX_PRICE, "A+B"), ElementStatus.OK, 7.0, 2);
  }

  @Test
  public void testUnsubscribe() {

    feedPrice("A", 2.0);
    feedPrice("B", 4.0);
    feedComposition("A+B", new IndexComposition(ImmutableMap.of("A", 1.0, "B", 1.0)));

    engine.runOnce();
    OlympusAssert.assertElement(
        engine.getElement(IndexCalculatorExample.INDEX_PRICE, "A+B"), ElementStatus.OK, 6.0, 1);

    feedComposition("A+B", new IndexComposition(ImmutableMap.of("A", 1.0)));
    engine.runOnce();
    OlympusAssert.assertElement(
        engine.getElement(IndexCalculatorExample.INDEX_PRICE, "A+B"), ElementStatus.OK, 2.0, 2);

    engine.runOnce();
    feedPrice("B", 3.0);
    OlympusAssert.assertElement(
        engine.getElement(IndexCalculatorExample.INDEX_PRICE, "A+B"), ElementStatus.OK, 2.0, 2);
  }
}

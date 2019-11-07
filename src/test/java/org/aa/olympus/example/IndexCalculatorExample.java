package org.aa.olympus.example;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EngineBuilder;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.ELementToolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
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
 * subscribe/unsubsccribe to constituent accordingly.
 */
public class IndexCalculatorExample {

  public static final EntityKey<String, IndexComposition> COMPOSITIONS =
      Olympus.key("COMPOSITIONS", String.class, IndexComposition.class);
  public static final EntityKey<String, Double> STOCK_PRICES =
      Olympus.key("STOCK_PRICES", String.class, Double.class);
  public static final EntityKey<String, Double> INDEX_PRICES =
      Olympus.key("INDEX_PRICES", String.class, Double.class);

  private Engine engine;

  @Test
  public void testTechIndex() {

    engine.setSourceState(STOCK_PRICES, "GOOGLE", 130.0);
    engine.setSourceState(STOCK_PRICES, "IBM", 230.0);

    engine.setSourceState(
        COMPOSITIONS, "TECH", new IndexComposition(ImmutableMap.of("GOOGLE", 2.0, "IBM", 3.0)));
    engine.runOnce();
    Assert.assertEquals(130.0 * 2.0 + 230.0 * 3.0, engine.getState(INDEX_PRICES, "TECH"), 0.0);

    engine.setSourceState(STOCK_PRICES, "GOOGLE", 135.0);
    engine.setSourceState(STOCK_PRICES, "IBM", 230.0);
    engine.runOnce();
    Assert.assertEquals(135.0 * 2.0 + 230.0 * 3.0, engine.getState(INDEX_PRICES, "TECH"), 0.0);
  }

  @Test
  public void testCompositionUpdates() {

    engine.setSourceState(STOCK_PRICES, "A", 1.0);
    engine.setSourceState(STOCK_PRICES, "G", 2.0);
    engine.setSourceState(STOCK_PRICES, "IBM", 3.0);

    engine.setSourceState(COMPOSITIONS, "TECH", new IndexComposition(ImmutableMap.of("IBM", 1.0)));
    engine.runOnce();
    System.out.println(engine.toString());
    Assert.assertEquals(3.0, engine.getState(INDEX_PRICES, "TECH"), 0.0);

    engine.setSourceState(
        COMPOSITIONS, "TECH", new IndexComposition(ImmutableMap.of("IBM", 0.5, "G", 0.5)));
    engine.runOnce();
    System.out.println(engine.toString());
    Assert.assertEquals(2.0 * 0.5 + 3.0 * 0.5, engine.getState(INDEX_PRICES, "TECH"), 0.0);

    engine.setSourceState(
        COMPOSITIONS, "TECH", new IndexComposition(ImmutableMap.of("IBM", 0.5, "G", 1.0)));
    engine.runOnce();
    System.out.println(engine.toString());
    Assert.assertEquals(2.0 * 1.0 + 3.0 * 0.5, engine.getState(INDEX_PRICES, "TECH"), 0.0);
  }

  @Test
  public void testPriceUpdate() {

    engine.setSourceState(STOCK_PRICES, "A", 2.0);
    engine.setSourceState(
        COMPOSITIONS, "NOT_READY", new IndexComposition(ImmutableMap.of("A", 1.0, "B", 1.0)));

    engine.runOnce();
    Assert.assertTrue(Double.isNaN(engine.getState(INDEX_PRICES, "NOT_READY")));

    engine.setSourceState(STOCK_PRICES, "B", 4.0);
    engine.runOnce();
    Assert.assertEquals(6.0, engine.getState(INDEX_PRICES, "NOT_READY"), 0.0);

    engine.setSourceState(STOCK_PRICES, "B", 3.0);
    engine.runOnce();
    Assert.assertEquals(5.0, engine.getState(INDEX_PRICES, "NOT_READY"), 0.0);
  }

  @Before
  public void setUp() {
    EngineBuilder engineBuilder = Olympus.builder();
    engineBuilder.registerSource(COMPOSITIONS);
    engineBuilder.registerSource(STOCK_PRICES);
    engineBuilder.registerInnerEntity(
        INDEX_PRICES, new IndexPricesEntityManager(), ImmutableSet.of(STOCK_PRICES, COMPOSITIONS));

    engine = engineBuilder.build();
  }

  @Test
  public void testWithElementView() {

    engine.setSourceState(STOCK_PRICES, "A", 2.0);
    engine.setSourceState(STOCK_PRICES, "B", 4.0);
    engine.setSourceState(
        COMPOSITIONS, "A+B", new IndexComposition(ImmutableMap.of("A", 1.0, "B", 1.0)));

    engine.runOnce();
    OlympusAssert.assertElement(engine.getElement(INDEX_PRICES, "A+B"), ElementStatus.OK, 6.0, 1);

    engine.setSourceState(STOCK_PRICES, "B", 5.0);
    engine.runOnce();
    OlympusAssert.assertElement(engine.getElement(INDEX_PRICES, "A+B"), ElementStatus.OK, 7.0, 2);

    engine.runOnce();
    OlympusAssert.assertElement(engine.getElement(INDEX_PRICES, "A+B"), ElementStatus.OK, 7.0, 2);
  }

  @Test
  public void testUnsubscribe() {

    engine.setSourceState(STOCK_PRICES, "A", 2.0);
    engine.setSourceState(STOCK_PRICES, "B", 4.0);
    engine.setSourceState(
        COMPOSITIONS, "A+B", new IndexComposition(ImmutableMap.of("A", 1.0, "B", 1.0)));

    engine.runOnce();
    OlympusAssert.assertElement(engine.getElement(INDEX_PRICES, "A+B"), ElementStatus.OK, 6.0, 1);

    engine.setSourceState(COMPOSITIONS, "A+B", new IndexComposition(ImmutableMap.of("A", 1.0)));
    engine.runOnce();
    OlympusAssert.assertElement(engine.getElement(INDEX_PRICES, "A+B"), ElementStatus.OK, 2.0, 2);

    engine.runOnce();
    engine.setSourceState(STOCK_PRICES, "B", 3.0);
    OlympusAssert.assertElement(engine.getElement(INDEX_PRICES, "A+B"), ElementStatus.OK, 2.0, 2);
  }

  /** Weights of index constituent */
  public static class IndexComposition {

    public final Map<String, Double> weights;

    public IndexComposition(Map<String, Double> weights) {
      this.weights = ImmutableMap.copyOf(weights);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("weights", weights).toString();
    }
  }

  /**
   * Creates a new {@link IndexPricesElementUpdater} when a new {@link IndexComposition} is created
   */
  public static class IndexPricesEntityManager implements ElementManager<String, Double> {

    @Override
    public ElementUpdater<Double> create(String key, UpdateContext updateContext, ELementToolbox ELementToolbox) {
      return new IndexPricesElementUpdater();
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<String> toNotify) {

      if (entityKey.equals(COMPOSITIONS)) {
        toNotify.accept((String) key);
      }
    }
  }

  /** Updates index prices when an update comes in */
  public static class IndexPricesElementUpdater implements ElementUpdater<Double> {

    final List<ElementHandle<String, Double>> elements;
    ElementHandle<String, IndexComposition> composition;

    public IndexPricesElementUpdater() {
      this.elements = new ArrayList<>();
    }

    @Override
    public UpdateResult<Double> update(
        Double previous, UpdateContext updateContext, ELementToolbox ELementToolbox) {

      if (composition.hasUpdated()) {
        elements.forEach(p -> p.subscribe(SubscriptionType.NONE));
        elements.clear();
        for (String stock : composition.getState().weights.keySet()) {
          elements.add(ELementToolbox.get(STOCK_PRICES, stock).subscribe(SubscriptionType.STRONG));
        }
      }
      // This could be done more efficiently (but less readable) by storing both weight and
      // handles in together in the vector
      double result = 0;
      for (ElementHandle<String, Double> stock : elements) {
        result +=
            composition.getState().weights.get(stock.getKey())
                * stock.getStateOrDefault(Double.NaN);
      }
      return UpdateResult.update(result);
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      // We get notified when the composition gets created
      this.composition = COMPOSITIONS.castHandle(handle).subscribe(SubscriptionType.STRONG);
      return true;
    }
  }
}

package org.aa.olympus.example;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EngineBuilder;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

// TODO: add test for price being removed (set to null)
// TODO: use the lifecycle support (not ready)
// TODO: add test to make sure it unsubscribes when composition change
public class IndexCalculatorExample {

  public static final EntityKey<String, IndexComposition> COMPOSITIONS =
      EntityKey.of("COMPOSITIONS", String.class, IndexComposition.class);
  public static final EntityKey<String, Double> STOCK_PRICES =
      EntityKey.of("STOCK_PRICES", String.class, Double.class);
  public static final EntityKey<String, Double> INDEX_PRICES =
      EntityKey.of("INDEX_PRICES", String.class, Double.class);

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

  public static class IndexPricesEntityManager implements ElementManager<String, Double> {

    @Override
    public ElementUpdater<Double> create(String key, UpdateContext updateContext, Toolbox toolbox) {
      return new IndexPricesElementUpdater(
          toolbox.get(COMPOSITIONS, key).subscribe(SubscriptionType.STRONG));
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<String> toNotify) {

      if (entityKey.equals(COMPOSITIONS)) {
        toNotify.accept((String) key);
      }
    }
  }

  public static class IndexPricesElementUpdater implements ElementUpdater<Double> {

    final ElementHandle<String, IndexComposition> composition;
    final List<ElementHandle<String, Double>> elements;

    public IndexPricesElementUpdater(ElementHandle<String, IndexComposition> composition) {
      this.composition = composition;
      this.elements = new ArrayList<>();
    }

    @Override
    public UpdateResult<Double> update(
        Double previous, UpdateContext updateContext, Toolbox toolbox) {

      if (composition.hasUpdated()) {
        elements.forEach(p -> p.subscribe(SubscriptionType.NONE));
        elements.clear();
        for (String stock : composition.getState().weights.keySet()) {
          elements.add(toolbox.get(STOCK_PRICES, stock).subscribe(SubscriptionType.STRONG));
        }
      }
      // This could be done more efficiently (and more convoluted by storing both weight and
      // handel in together in the vector
      double result = 0;
      for (ElementHandle<String, Double> stock : elements) {
        result +=
            composition.getState().weights.get(stock.getKey())
                * stock.getStateOrDefault(Double.NaN);
      }
      return UpdateResult.update(result);
    }

    @Override
    public <K2, S2> void onNewElement(ElementHandle<K2, S2> handle) {}
  }

  private Engine engine;

  @Before
  public void setUp() {
    EngineBuilder engineBuilder = Olympus.builder();
    engineBuilder.registerSource(COMPOSITIONS);
    engineBuilder.registerSource(STOCK_PRICES);
    engineBuilder.registerEntity(
        INDEX_PRICES, new IndexPricesEntityManager(), ImmutableSet.of(STOCK_PRICES, COMPOSITIONS));

    engine = engineBuilder.build();
  }

  @Test
  public void testCompositionUpdates() {

    engine.setSourceState(STOCK_PRICES, "A", 1.0);
    engine.setSourceState(STOCK_PRICES, "G", 2.0);
    engine.setSourceState(STOCK_PRICES, "IBM", 3.0);

    engine.setSourceState(COMPOSITIONS, "TECH", new IndexComposition(ImmutableMap.of("IBM", 1.0)));
    engine.runOnce(LocalDateTime.now());
    System.out.println(engine.toString());

    Assert.assertEquals(3.0, engine.getState(INDEX_PRICES, "TECH"), 0.0);

    engine.setSourceState(
        COMPOSITIONS, "TECH", new IndexComposition(ImmutableMap.of("IBM", 0.5, "G", 0.5)));
    engine.runOnce(LocalDateTime.now());
    System.out.println(engine.toString());

    Assert.assertEquals(2.0 * 0.5 + 3.0 * 0.5, engine.getState(INDEX_PRICES, "TECH"), 0.0);
  }

  @Test
  public void testPriceUpdate() {

    engine.setSourceState(STOCK_PRICES, "A", 2.0);
    engine.setSourceState(
        COMPOSITIONS, "NOT_READY", new IndexComposition(ImmutableMap.of("A", 1.0, "B", 1.0)));

    engine.runOnce(LocalDateTime.now());
    Assert.assertTrue(Double.isNaN(engine.getState(INDEX_PRICES, "NOT_READY")));

    engine.setSourceState(STOCK_PRICES, "B", 4.0);
    engine.runOnce(LocalDateTime.now());
    Assert.assertEquals(6.0, engine.getState(INDEX_PRICES, "NOT_READY"), 0.0);

    engine.setSourceState(STOCK_PRICES, "B", 3.0);
    engine.runOnce(LocalDateTime.now());
    Assert.assertEquals(5.0, engine.getState(INDEX_PRICES, "NOT_READY"), 0.0);
  }

  @Test
  public void testWithElementView() {

    engine.setSourceState(STOCK_PRICES, "A", 2.0);
    engine.setSourceState(STOCK_PRICES, "B", 4.0);
    engine.setSourceState(
        COMPOSITIONS, "A+B", new IndexComposition(ImmutableMap.of("A", 1.0, "B", 1.0)));

    engine.runOnce(LocalDateTime.now());
    assertElement(engine.getElement(INDEX_PRICES, "A+B"), ElementStatus.UPDATED, 6.0, 1);

    engine.setSourceState(STOCK_PRICES, "B", 5.0);
    engine.runOnce(LocalDateTime.now());
    assertElement(engine.getElement(INDEX_PRICES, "A+B"), ElementStatus.UPDATED, 7.0, 2);

    engine.runOnce(LocalDateTime.now());
    assertElement(engine.getElement(INDEX_PRICES, "A+B"), ElementStatus.UPDATED, 7.0, 2);
  }

  <K, S> void assertElement(
      ElementView<K, S> view, ElementStatus expectedStatus, S expectedState, int expectedUpdateId) {

    Assert.assertEquals(expectedStatus, view.getStatus());
    Assert.assertEquals(expectedState, view.getState());
    Assert.assertEquals(expectedUpdateId, view.getUpdateContext().getUpdateId());
  }
}

package org.aa.olympus.example;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EngineBuilder;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.junit.Test;

public class DynamicSubscription {

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
  }

  @Test
  public void test() {

    EngineBuilder engineBuilder = Olympus.builder();
    engineBuilder.registerSource(COMPOSITIONS);
    engineBuilder.registerSource(STOCK_PRICES);
    engineBuilder.registerEntity(
        INDEX_PRICES, new IndexPricesEntityManager(), ImmutableSet.of(STOCK_PRICES, COMPOSITIONS));

    Engine engine = engineBuilder.build();
    engine.setSourceState(STOCK_PRICES, "A", 1.0);
    engine.setSourceState(STOCK_PRICES, "G", 2.0);
    engine.setSourceState(STOCK_PRICES, "IBM", 3.0);

    
    engine.setSourceState(COMPOSITIONS, "TECH", new IndexComposition(ImmutableMap.of("A", 1.0)));

    engine.runOnce(new Date());

    System.out.println(engine.toString());
  }

  public static class IndexPricesEntityManager implements ElementManager<String, Double> {

    @Override
    public ElementUpdater<Double> create(String key, UpdateContext updateContext, Toolbox toolbox) {
      return new IndexPricesElemetUpdater(
          toolbox.subscribe(COMPOSITIONS, key, SubscriptionType.STRONG));
    }

    @Override
    public void onNewKey(ElementHandle newElement, Consumer<String> toNotify) {
      if (newElement.getEntityKey().equals(COMPOSITIONS)) {
        toNotify.accept(COMPOSITIONS.castHandle(newElement).getKey());
      }
    }
  }

  public static class IndexPricesElemetUpdater implements ElementUpdater<Double> {

    final ElementHandle<String, IndexComposition> composition;
    final List<ElementHandle<String, Double>> elements;

    public IndexPricesElemetUpdater(ElementHandle<String, IndexComposition> composition) {
      this.composition = composition;
      this.elements = new ArrayList<>();
    }

    @Override
    public UpdateResult<Double> update(
        Double previous, UpdateContext updateContext, Toolbox toolbox) {

      if (composition.hasUpdated()) {
        for (ElementHandle<String, Double> elementHandle : elements) {
          toolbox.subscribe(
              elementHandle.getEntityKey(), elementHandle.getKey(), SubscriptionType.NONE);
        }
        elements.clear();
        for (String stock : composition.getState().weights.keySet()) {
          elements.add(toolbox.subscribe(STOCK_PRICES, stock, SubscriptionType.STRONG));
        }
      }
      double result = 0;
      for (ElementHandle<String, Double> stock : elements) {
        result += composition.getState().weights.get(stock.getKey()) * stock.getState();
      }
      return UpdateResult.update(result);
    }

    @Override
    public <K2, S2> void onNewElement(ElementHandle<K2, S2> handle) {}
  }
}

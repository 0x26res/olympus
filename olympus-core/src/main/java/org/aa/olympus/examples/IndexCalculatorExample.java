package org.aa.olympus.examples;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EngineBuilder;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.api.Notifier;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;

public class IndexCalculatorExample {

  public static final EventChannel<KeyValuePair<String, IndexComposition>> COMPOSITION_CHANNEL =
      Olympus.channel("COMPOSITION", new TypeToken<KeyValuePair<String, IndexComposition>>() {});
  public static final EventChannel<KeyValuePair<String, Double>> PRICE_CHANNEL =
      Olympus.channel("PRICE", new TypeToken<KeyValuePair<String, Double>>() {});
  public static final EntityKey<String, IndexComposition> COMPOSITION =
      Olympus.key("COMPOSITION", String.class, IndexComposition.class);
  public static final EntityKey<String, Double> PRICE =
      Olympus.key("PRICE", String.class, Double.class);
  public static final EntityKey<String, Double> INDEX_PRICE =
      Olympus.key("INDEX_PRICE", String.class, Double.class);

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
    public ElementUpdater<Double> create(String key, UpdateContext updateContext, Toolbox toolbox) {
      return new IndexPricesElementUpdater();
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Notifier<String> notifier) {

      if (entityKey.equals(COMPOSITION)) {
        notifier.notifyElement((String) key);
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
        Double previous, UpdateContext updateContext, Toolbox toolbox) {

      if (composition.hasUpdated()) {
        elements.forEach(p -> p.subscribe(SubscriptionType.NONE));
        elements.clear();
        for (String stock : composition.getState().weights.keySet()) {
          elements.add(toolbox.get(PRICE, stock).subscribe(SubscriptionType.STRONG));
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
      this.composition = COMPOSITION.castHandle(handle).subscribe(SubscriptionType.STRONG);
      return true;
    }
  }

  static Engine createEngine() {
    EngineBuilder engineBuilder =
        Olympus.builder()
            .registerEventChannel(COMPOSITION_CHANNEL)
            .registerEventChannel(PRICE_CHANNEL)
            .eventToEntity(
                COMPOSITION_CHANNEL, COMPOSITION, KeyValuePair::getKey, KeyValuePair::getValue)
            .eventToEntity(PRICE_CHANNEL, PRICE, KeyValuePair::getKey, KeyValuePair::getValue)
            .registerInnerEntity(
                INDEX_PRICE, new IndexPricesEntityManager(), ImmutableSet.of(PRICE, COMPOSITION));

    return engineBuilder.build();
  }
}

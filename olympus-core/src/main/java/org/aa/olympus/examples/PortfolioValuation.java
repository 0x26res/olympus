package org.aa.olympus.examples;

import com.google.common.collect.ImmutableSet;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;

public class PortfolioValuation {

  public static final class StringDoublePair {
    public final String key;
    public final Double value;

    public static StringDoublePair of(String key, Double value) {
      return new StringDoublePair(key, value);
    }

    private StringDoublePair(String key, Double value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public Double getValue() {
      return value;
    }
  }

  public static final EventChannel<StringDoublePair> PRICE_CHANNEL =
      Olympus.channel("PRICE", StringDoublePair.class);
  public static final EventChannel<StringDoublePair> QUANTITY_CHANNEL =
      Olympus.channel("QUANTITY", StringDoublePair.class);

  public static final EntityKey<String, Double> PRICE =
      Olympus.key("PRICE", String.class, Double.class);
  public static final EntityKey<String, Double> QUANTITY =
      Olympus.key("QUANTITY", String.class, Double.class);
  public static final EntityKey<String, Double> VALUATION =
      Olympus.key("VALUATION", String.class, Double.class);

  public static final class ValuationUpdater implements ElementUpdater<Double> {

    private final ElementHandle<String, Double> hello;
    private final ElementHandle<String, Double> world;

    public ValuationUpdater(
        ElementHandle<String, Double> hello, ElementHandle<String, Double> world) {
      this.hello = hello;
      this.world = world;
    }

    @Override
    public UpdateResult<Double> update(
        Double previous, UpdateContext updateContext, Toolbox toolbox) {
      return UpdateResult.maybe(hello.getState() + world.getState());
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      return true;
    }
  }

  public static final class ValuationManager implements ElementManager<String, Double> {

    @Override
    public ElementUpdater<Double> create(
        String elementKey, UpdateContext updateContext, Toolbox toolbox) {
      return new ValuationUpdater(
          toolbox.get(PRICE, elementKey).subscribe(SubscriptionType.STRONG),
          toolbox.get(QUANTITY, elementKey).subscribe(SubscriptionType.STRONG));
    }

    @Override
    public void onNewKey(EntityKey entityKey, Object key, Consumer toNotify) {
      toNotify.accept((String) key);
    }
  }

  public static Engine createEngine() {
    return Olympus.builder()
        .registerEventChannel(PRICE_CHANNEL)
        .registerEventChannel(QUANTITY_CHANNEL)
        .eventToEntity(PRICE_CHANNEL, PRICE, StringDoublePair::getKey, StringDoublePair::getValue)
        .eventToEntity(
            QUANTITY_CHANNEL, QUANTITY, StringDoublePair::getKey, StringDoublePair::getValue)
        .registerInnerEntity(VALUATION, new ValuationManager(), ImmutableSet.of(PRICE, QUANTITY))
        .build();
  }
}

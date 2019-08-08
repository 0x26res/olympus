package org.aa.olympus.example;

import com.google.common.collect.ImmutableSet;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;

public class PortfolioValuation {

  public static final class KeyValuePair {
    public final String key;
    public final Double value;

    public static KeyValuePair of(String key, Double value) {
      return new KeyValuePair(key, value);
    }

    private KeyValuePair(String key, Double value) {
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

  public static final EntityKey<String, KeyValuePair> PRICE =
      Olympus.key("PRICE", String.class, KeyValuePair.class);
  public static final EntityKey<String, KeyValuePair> QUANTITY =
      Olympus.key("QUANTITY", String.class, KeyValuePair.class);
  public static final EntityKey<String, KeyValuePair> VALUATION =
      Olympus.key("VALUATION", String.class, KeyValuePair.class);

  public static final class ValuationUpdater implements ElementUpdater<KeyValuePair> {

    private final ElementHandle<String, KeyValuePair> hello;
    private final ElementHandle<String, KeyValuePair> world;

    public ValuationUpdater(
        ElementHandle<String, KeyValuePair> hello, ElementHandle<String, KeyValuePair> world) {
      this.hello = hello;
      this.world = world;
    }

    @Override
    public UpdateResult<KeyValuePair> update(
        KeyValuePair previous, UpdateContext updateContext, Toolbox toolbox) {
      return UpdateResult.maybe(
          new KeyValuePair(hello.getKey(), hello.getState().value + world.getState().value));
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      return true;
    }
  }

  public static final class ValuationManager implements ElementManager<String, KeyValuePair> {

    @Override
    public ElementUpdater<KeyValuePair> create(
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
        .registerSource(PRICE)
        .registerSource(QUANTITY)
        .registerInnerEntity(VALUATION, new ValuationManager(), ImmutableSet.of(PRICE, QUANTITY))
        .build();
  }
}

package org.aa.olympus.examples;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EngineBuilder;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.api.Notifier;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;

public class PositionTracking {

  static final EventChannel<KeyValuePair<PositionKey, Integer>> POSITION_CHANNEL =
      Olympus.channel("POSITION_CHANNEL", new TypeToken<KeyValuePair<PositionKey, Integer>>() {});

  static final EntityKey<PositionKey, Integer> POSITION =
      Olympus.key("POSITION", PositionKey.class, Integer.class);
  static final EntityKey<PositionKey, Integer> PRODUCT_ACCOUNT =
      Olympus.key("PRODUCT_ACCOUNT", PositionKey.class, Integer.class);
  static final EntityKey<PositionKey, Integer> ACCOUNT =
      Olympus.key("ACCOUNT", PositionKey.class, Integer.class);
  static final EntityKey<PositionKey, Integer> COMPANY =
      Olympus.key("COMPANY", PositionKey.class, Integer.class);

  static PositionKey key(String product, String maturity, String account) {
    return new PositionKey(product, maturity, account);
  }

  static final class PositionKey {

    final String product;
    final String maturity;
    final String account;

    PositionKey(String product, String maturity, String account) {
      this.product = product;
      this.maturity = maturity;
      this.account = account;
    }

    @Override
    public int hashCode() {
      return Objects.hash(product, maturity, account);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (o == null || o.getClass() != this.getClass()) {
        return false;
      } else {
        PositionKey other = (PositionKey) o;
        return Objects.equals(this.product, other.product)
            && Objects.equals(this.maturity, other.maturity)
            && Objects.equals(this.account, other.account);
      }
    }
  }

  static Engine createEngine() {
    EngineBuilder engineBuilder = Olympus.builder();

    engineBuilder.registerEventChannel(POSITION_CHANNEL);
    engineBuilder.eventToEntity(
        POSITION_CHANNEL, POSITION, KeyValuePair::getKey, KeyValuePair::getValue);
    engineBuilder.registerInnerEntity(
        PRODUCT_ACCOUNT,
        new PositionManager(POSITION, p -> new PositionKey(p.product, null, p.account)),
        ImmutableSet.of(POSITION));

    engineBuilder.registerInnerEntity(
        ACCOUNT,
        new PositionManager(PRODUCT_ACCOUNT, p -> new PositionKey(null, null, p.account)),
        ImmutableSet.of(PRODUCT_ACCOUNT));

    engineBuilder.registerInnerEntity(
        COMPANY,
        new PositionManager(ACCOUNT, p -> new PositionKey(null, null, null)),
        ImmutableSet.of(ACCOUNT));

    return engineBuilder.build();
  }

  public static final class PositionManager implements ElementManager<PositionKey, Integer> {

    final EntityKey<PositionKey, Integer> subKey;
    final Function<PositionKey, PositionKey> keyTransformer;

    PositionManager(
        EntityKey<PositionKey, Integer> subKey, Function<PositionKey, PositionKey> keyTransformer) {
      this.subKey = subKey;
      this.keyTransformer = keyTransformer;
    }

    @Override
    public ElementUpdater<Integer> create(
        PositionKey key, UpdateContext updateContext, Toolbox toolbox) {
      return new PositionUpdater(subKey);
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Notifier<PositionKey> notifier) {
      notifier.notifyElement(keyTransformer.apply((PositionKey) key));
    }
  }

  public static final class PositionUpdater implements ElementUpdater<Integer> {
    final EntityKey<PositionKey, Integer> subKey;
    final List<ElementHandle<PositionKey, Integer>> children = new ArrayList<>();

    PositionUpdater(EntityKey<PositionKey, Integer> parentKey) {
      this.subKey = parentKey;
    }

    @Override
    public UpdateResult<Integer> update(
        Integer previous, UpdateContext updateContext, Toolbox toolbox) {
      return UpdateResult.update(children.stream().mapToInt(ElementHandle::getState).sum());
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      children.add(subKey.castHandle(handle));
      return true;
    }
  }
}

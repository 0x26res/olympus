package org.aa.olympus.example;

import com.google.common.collect.ImmutableSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EngineBuilder;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.junit.Assert;
import org.junit.Test;

public class PositionTracking {

  private static final EntityKey<PositionKey, Integer> POSITION =
      Olympus.key("POSITION", PositionKey.class, Integer.class);
  private static final EntityKey<PositionKey, Integer> PRODUCT_ACCOUNT =
      Olympus.key("PRODUCT_ACCOUNT", PositionKey.class, Integer.class);
  private static final EntityKey<PositionKey, Integer> ACCOUNT =
      Olympus.key("ACCOUNT", PositionKey.class, Integer.class);
  private static final EntityKey<PositionKey, Integer> COMPANY =
      Olympus.key("COMPANY", PositionKey.class, Integer.class);

  private static PositionKey key(String product, String maturity, String account) {
    return new PositionKey(product, maturity, account);
  }

  @Test
  public void demo() {

    EngineBuilder engineBuilder = Olympus.builder();

    engineBuilder.registerSource(POSITION);
    engineBuilder.registerEntity(
        PRODUCT_ACCOUNT,
        new PositionManager(POSITION, p -> new PositionKey(p.product, null, p.account)),
        ImmutableSet.of(POSITION));

    engineBuilder.registerEntity(
        ACCOUNT,
        new PositionManager(PRODUCT_ACCOUNT, p -> new PositionKey(null, null, p.account)),
        ImmutableSet.of(PRODUCT_ACCOUNT));

    engineBuilder.registerEntity(
        COMPANY,
        new PositionManager(ACCOUNT, p -> new PositionKey(null, null, null)),
        ImmutableSet.of(ACCOUNT));

    Engine engine = engineBuilder.build();

    engine.setSourceState(POSITION, key("S&P500", "DEC18", "FOO"), 10);
    engine.setSourceState(POSITION, key("S&P500", "MAR18", "FOO"), -10);
    engine.setSourceState(POSITION, key("S&P500", "MAR18", "BAR"), 30);
    engine.setSourceState(POSITION, key("DOW30", "MAR18", "BAR"), -20);

    engine.runOnce(LocalDateTime.now());

    Assert.assertEquals(10, engine.getState(COMPANY, key(null, null, null)).intValue());
    Assert.assertEquals(0, engine.getState(ACCOUNT, key(null, null, "FOO")).intValue());
    Assert.assertEquals(10, engine.getState(ACCOUNT, key(null, null, "BAR")).intValue());
    Assert.assertEquals(
        30, engine.getState(PRODUCT_ACCOUNT, key("S&P500", null, "BAR")).intValue());
    Assert.assertEquals(
        -20, engine.getState(PRODUCT_ACCOUNT, key("DOW30", null, "BAR")).intValue());
  }

  private static final class PositionKey {

    final String product;
    final String maturity;
    final String account;

    private PositionKey(String product, String maturity, String account) {
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
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<PositionKey> toNotify) {

      toNotify.accept(keyTransformer.apply((PositionKey) key));
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

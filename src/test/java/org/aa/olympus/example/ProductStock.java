package org.aa.olympus.example;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
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
import org.junit.Test;

/** This example doesn't work ATM because there are circular dependencies. */
// TODO: crete add hoc way to make it work
public class ProductStock {

  private static final EntityKey<StockKey, Integer> STOCK =
      Olympus.createKey("STOCK", StockKey.class, Integer.class);
  private static final EntityKey<StockKey, Integer> AGGREGATE =
      Olympus.createKey("AGGREGATE", StockKey.class, Integer.class);

  private static final StockKey POTATO = StockKey.of("food", "vegetable", "potato");
  private static final StockKey APPLE = StockKey.of("food", "fruit", "apple");
  private static final StockKey PEAR = StockKey.of("food", "fruit", "pear");
  private static final StockKey PEN = StockKey.of("stationary", "writing", "pen");

  @Test
  public void test() {

    EngineBuilder engineBuilder = Olympus.builder();
    engineBuilder.registerSource(STOCK);
    engineBuilder.registerEntity(AGGREGATE, new AggregateManager(), ImmutableSet.of(STOCK));

    Engine engine = engineBuilder.build();
    engine.setSourceState(STOCK, POTATO, 1);
    engine.setSourceState(STOCK, APPLE, 30);
    engine.setSourceState(STOCK, PEAR, 31);
    engine.setSourceState(STOCK, PEN, 1000);

    engine.runOnce(LocalDateTime.now());

    System.out.println(engine.toString());
  }

  static final class StockKey {
    private List<String> hierarchy;

    private StockKey(List<String> hierarchy) {
      this.hierarchy = ImmutableList.copyOf(hierarchy);
    }

    static StockKey of(String... hierarchy) {
      return new StockKey(ImmutableList.copyOf(hierarchy));
    }

    boolean isRoot() {
      return this.hierarchy.isEmpty();
    }

    StockKey parent() {
      if (isRoot()) {
        throw new IllegalStateException("Root has got no parent");
      } else {
        return new StockKey(hierarchy.subList(0, hierarchy.size() - 1));
      }
    }

    @Override
    public String toString() {
      return Joiner.on('/').join(hierarchy);
    }

    @Override
    public int hashCode() {
      return Objects.hash(hierarchy);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o == null || o.getClass() != this.getClass()) {
        return false;
      } else {
        StockKey other = (StockKey) o;
        return other.hierarchy.equals(this.hierarchy);
      }
    }
  }

  public static final class Updater implements ElementUpdater<Integer> {
    List<ElementHandle<StockKey, Integer>> children = new ArrayList<>();

    @Override
    public UpdateResult<Integer> update(
        Integer previous, UpdateContext updateContext, Toolbox toolbox) {
      return UpdateResult.update(children.stream().mapToInt(ElementHandle::getState).sum());
    }

    @Override
    public <K2, S2> void onNewElement(ElementHandle<K2, S2> handle) {
      if (handle.getEntityKey().equals(STOCK)) {
        children.add(STOCK.castHandle(handle));
      } else if (handle.getEntityKey().equals(AGGREGATE)) {
        children.add(AGGREGATE.castHandle(handle));
      }
    }
  }

  public static class AggregateManager implements ElementManager<StockKey, Integer> {

    @Override
    public ElementUpdater<Integer> create(
        StockKey key, UpdateContext updateContext, Toolbox toolbox) {
      return new Updater();
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<StockKey> toNotify) {
      toNotify.accept(((StockKey) key).parent());
    }
  }
}

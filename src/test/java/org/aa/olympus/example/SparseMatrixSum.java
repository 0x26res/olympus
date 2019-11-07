package org.aa.olympus.example;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
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
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.ELementToolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.impl.UnsupportedEntityException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SparseMatrixSum {

  private static final EntityKey<Position, Integer> CELL =
      Olympus.key("CELL", Position.class, Integer.class);
  private static final EntityKey<Position, Integer> AGGREGATE =
      Olympus.key("AGGREGATE", Position.class, Integer.class);
  private static final EntityKey<Position, Integer> TOTAL =
      Olympus.key("TOTAL", Position.class, Integer.class);

  private static final Position ROOT = new Position(-1, -1);

  private Engine engine;

  @Before
  public void setUp() {
    EngineBuilder builder = Olympus.builder();
    builder.registerSource(CELL);

    builder.registerInnerEntity(AGGREGATE, new AggregateManager(), ImmutableSet.of(CELL));
    builder.registerInnerEntity(TOTAL, new TotalManager(), ImmutableSet.of(AGGREGATE));

    engine = builder.build();
  }

  @Test
  public void test() {

    engine.setSourceState(CELL, Position.of(0, 0), 10);
    engine.setSourceState(CELL, Position.of(0, 1), 11);
    engine.setSourceState(CELL, Position.of(0, 2), 12);

    engine.setSourceState(CELL, Position.of(1, 0), 100);
    engine.setSourceState(CELL, Position.of(2, 0), 110);
    engine.setSourceState(CELL, Position.of(3, 0), 120);

    engine.runOnce(LocalDateTime.now());
    Assert.assertEquals(363, engine.getState(TOTAL, Position.of(-1, -1)).intValue());

    engine.setSourceState(CELL, Position.of(3, 0), 130);
    engine.runOnce(LocalDateTime.now());
    Assert.assertEquals(373, engine.getState(TOTAL, Position.of(-1, -1)).intValue());

    engine.setSourceState(CELL, Position.of(3, 0), 0);
    engine.runOnce(LocalDateTime.now());
    Assert.assertEquals(243, engine.getState(TOTAL, Position.of(-1, -1)).intValue());

    engine.setSourceState(CELL, Position.of(200, 23), 120);
    engine.runOnce(LocalDateTime.now());
    Assert.assertEquals(363, engine.getState(TOTAL, Position.of(-1, -1)).intValue());
    Assert.assertEquals(120, engine.getState(AGGREGATE, Position.of(200, -1)).intValue());
    Assert.assertEquals(120, engine.getState(AGGREGATE, Position.of(-1, 23)).intValue());
  }

  public static final class Position {
    final int row;
    final int col;

    Position(int row, int col) {
      this.row = row;
      this.col = col;
    }

    static Position of(int row, int col) {
      return new Position(row, col);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("row", row).add("col", col).toString();
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || o.getClass() != this.getClass()) {
        return false;
      } else {
        Position other = (Position) o;
        return this.row == other.row && this.col == other.col;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(row, col);
    }
  }

  private static class AggregateManager implements ElementManager<Position, Integer> {

    @Override
    public ElementUpdater<Integer> create(
        Position key, UpdateContext updateContext, ELementToolbox ELementToolbox) {
      Preconditions.checkArgument(key.col == -1 || key.row == -1);
      return new Aggregator(CELL);
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<Position> toNotify) {

      if (entityKey.equals(CELL)) {
        Position position = (Position) key;
        toNotify.accept(new Position(-1, position.col));
        toNotify.accept(new Position(position.row, -1));
      } else if (entityKey.equals(AGGREGATE)) {
        toNotify.accept(ROOT);
      } else {
        throw new UnsupportedEntityException(entityKey);
      }
    }
  }

  private static class TotalManager implements ElementManager<Position, Integer> {

    @Override
    public ElementUpdater<Integer> create(
        Position key, UpdateContext updateContext, ELementToolbox ELementToolbox) {
      Preconditions.checkArgument(key.row < 0 && key.col < 0);
      return new Aggregator(AGGREGATE);
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<Position> toNotify) {
      Position position = (Position) key;
      if (position.row == -1) {
        toNotify.accept(ROOT);
      }
    }
  }

  private static final class Aggregator implements ElementUpdater<Integer> {

    private final EntityKey<Position, Integer> elementsEntity;
    private final List<ElementHandle<Position, Integer>> elements = new ArrayList<>();

    private Aggregator(EntityKey<Position, Integer> elementsEntity) {
      this.elementsEntity = elementsEntity;
    }

    @Override
    public UpdateResult<Integer> update(
        Integer previous, UpdateContext updateContext, ELementToolbox ELementToolbox) {
      return UpdateResult.update(elements.stream().mapToInt(ElementHandle::getState).sum());
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      elements.add(elementsEntity.castHandle(handle).subscribe(SubscriptionType.STRONG));
      return true;
    }
  }
}

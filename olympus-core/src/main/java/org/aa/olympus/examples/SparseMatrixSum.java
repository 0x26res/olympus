package org.aa.olympus.examples;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.api.Notifier;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.impl.UnsupportedEntityException;

public class SparseMatrixSum {

  static final EventChannel<KeyValuePair<Position, Integer>> UPDATE_CHANNEL =
      Olympus.channel("UPDATE", new TypeToken<KeyValuePair<Position, Integer>>() {});

  static final EntityKey<Position, Integer> CELL =
      Olympus.key("CELL", Position.class, Integer.class);
  static final EntityKey<Position, Integer> AGGREGATE =
      Olympus.key("AGGREGATE", Position.class, Integer.class);
  static final EntityKey<Position, Integer> TOTAL =
      Olympus.key("TOTAL", Position.class, Integer.class);

  static final Position ROOT = new Position(-1, -1);

  static Engine createEngine() {
    return Olympus.builder()
        .registerEventChannel(UPDATE_CHANNEL)
        .eventToEntity(UPDATE_CHANNEL, CELL, KeyValuePair::getKey, KeyValuePair::getValue)
        .registerInnerEntity(AGGREGATE, new AggregateManager(), ImmutableSet.of(CELL))
        .registerInnerEntity(TOTAL, new TotalManager(), ImmutableSet.of(AGGREGATE))
        .build();
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
        Position key, UpdateContext updateContext, Toolbox toolbox) {
      Preconditions.checkArgument(key.col == -1 || key.row == -1);
      return new Aggregator(CELL);
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Notifier<Position> toNotify) {

      if (entityKey.equals(CELL)) {
        Position position = (Position) key;
        toNotify.notifyElement(new Position(-1, position.col));
        toNotify.notifyElement(new Position(position.row, -1));
      } else if (entityKey.equals(AGGREGATE)) {
        toNotify.notifyElement(ROOT);
      } else {
        throw new UnsupportedEntityException(entityKey);
      }
    }
  }

  private static class TotalManager implements ElementManager<Position, Integer> {

    @Override
    public ElementUpdater<Integer> create(
        Position key, UpdateContext updateContext, Toolbox toolbox) {
      Preconditions.checkArgument(key.row < 0 && key.col < 0);
      return new Aggregator(AGGREGATE);
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Notifier<Position> notifier) {

      Position position = (Position) key;
      if (position.row == -1) {
        notifier.notifyElement(ROOT);
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
        Integer previous, UpdateContext updateContext, Toolbox toolbox) {
      return UpdateResult.update(elements.stream().mapToInt(ElementHandle::getState).sum());
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      elements.add(elementsEntity.castHandle(handle).subscribe(SubscriptionType.STRONG));
      return true;
    }
  }
}

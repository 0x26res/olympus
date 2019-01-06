package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;

final class EngineImpl implements Engine {

  private final List<EntityKey> sorted;
  private final Map<EntityKey, SourceManager> sources;
  private final Map<EntityKey, EntityManager> entities;

  EngineImpl(
      List<EntityKey> sorted,
      final Map<EntityKey, SourceManager> sources,
      final Map<EntityKey, EntityManager> entities) {
    this.sorted = sorted;
    this.sources = ImmutableMap.copyOf(sources);
    this.entities = ImmutableMap.copyOf(entities);
  }

  @Override
  public <K, S> void setSourceState(EntityKey<K, S> entityKey, K key, S state) {
    // This is safe as the EntityKey equals guarantees type equality
    sources.get(entityKey).setState(key, state);
  }

  @Override
  public void runOnce(Date time) {
    propagateCreations();
    propagateUpdates();
  }

  private void propagateUpdates() {

    for (EntityKey entityKey : sorted) {
      EntityManager<?, ?> entityManager = entities.get(entityKey);
      runEntity(entityManager);
    }
  }

  private <K, S> void runEntity(EntityManager<K, S> entityManager) {
    entityManager.run();
  }

  private void propagateCreations() {
    for (EntityKey entityKey : sorted) {
      EntityManager<?, ?> entityManager = entities.get(entityKey);
      propagateCreations(entityManager);
    }
  }

  private <K, S> void propagateCreations(EntityManager<K, S> entityManager) {
    Map<K, Set<ElementUnit>> subscriptions = new HashMap<>();
    for (EntityKey key : entityManager.getDependencies()) {
      EntityManager dependency = entities.get(key);
      // TODO: understand why we need an intermediate variable
      List<ElementUnit<?, ?>> handles = dependency.getCreated();
      for (ElementUnit<?, ?> elementUnit : handles) {
        Consumer<K> consumer =
            k -> subscriptions.computeIfAbsent(k, p -> new HashSet<>()).add(elementUnit);
        entityManager.getElementManager().onNewKey(elementUnit.getHandleAdapter(), consumer);
      }
    }
    for (Map.Entry<K, Set<ElementUnit>> entry : subscriptions.entrySet()) {
      ElementUnit<K, S> unit = entityManager.get(entry.getKey(), true);
      unit.addBroadcasters(entry.getValue());
      unit.stain();
    }
  }

  @Override
  public <K, S> S getState(EntityKey<K, S> entityKey, K key) {
    EntityManager<K, S> entityManager = getEntityManager(entityKey);
    Preconditions.checkArgument(entityManager != null, "Unknown entity %s", entityKey);
    ElementUnit<K, S> unit = entityManager.get(key, false);
    if (unit != null) {
      return unit.getState();
    } else {
      return null;
    }
  }

  <K, S> EntityManager<K, S> getEntityManager(EntityKey<K, S> entityKey) {
    // This is safe as the EntityKet guarantees this through equality
    return (EntityManager<K, S>) entities.get(entityKey);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(Strings.repeat("*", 250)).append('\n');
    for (EntityKey key : sorted) {
      builder.append(key).append('\n');
      builder.append(entities.get(key));
      builder.append(Strings.repeat("+", 250)).append('\n');
    }
    return builder.toString();
  }
}

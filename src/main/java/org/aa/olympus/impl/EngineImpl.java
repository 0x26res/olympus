package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.UpdateContext;

final class EngineImpl implements Engine {

  private final List<EntityKey> sorted;
  private final Map<EntityKey, SourceManager> sources;
  private final Map<EntityKey, EntityManager> entities;

  // TODO: consider adding an extra layer of abstraction to run the engine
  private UpdateContext updateContext;

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
  public void runOnce(LocalDateTime time) {
    updateContext =
        new UpdateContextImpl(time, updateContext == null ? 1 : updateContext.getUpdateId() + 1);
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
    entityManager.run(updateContext);
  }

  private void propagateCreations() {
    for (EntityKey entityKey : sorted) {
      EntityManager<?, ?> entityManager = entities.get(entityKey);
      propagateCreations(entityManager);
    }
  }

  private <K, S> void propagateCreations(EntityManager<K, S> entityManager) {
    for (EntityKey key : entityManager.getDependencies()) {
      EntityManager dependency = entities.get(key);
      propagateCreations(dependency, entityManager);
    }
  }

  private <KB, SB, KS, SS> void propagateCreations(
      EntityManager<KB, SB> broadcasters, EntityManager<KS, SS> subscribers) {

    HashSet<KS> toCreate = new HashSet<>();
    List<ElementUnit<KB, SB>> handles = broadcasters.getCreated();
    for (ElementUnit<KB, SB> elementUnit : handles) {
      Consumer<KS> consumer = toCreate::add;
      subscribers
          .getElementManager()
          .onNewKey(broadcasters.getKey(), elementUnit.getKey(), consumer);
    }

    for (KS key : toCreate) {
      ElementUnit<KS, SS> unit = subscribers.get(key, true);
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

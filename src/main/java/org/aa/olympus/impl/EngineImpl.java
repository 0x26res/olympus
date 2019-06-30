package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;

// TODO: consider adding an extra layer of abstraction to run the engine
final class EngineImpl implements Engine {

  private final EngineContext engineContext;
  private final List<EntityKey> sorted;
  private final Map<EntityKey, SourceManager> sources;
  private final Map<EntityKey, EntityManager> entities;

  EngineImpl(
      EngineContext engineContext,
      List<EntityKey> sorted,
      final Map<EntityKey, SourceManager> sources,
      final Map<EntityKey, EntityManager> entities) {
    this.engineContext = engineContext;
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
    Preconditions.checkArgument(!time.isBefore(this.engineContext.getLatestContext().getTime()));
    this.engineContext.setLatestContext(
        new UpdateContextImpl(time, this.engineContext.getLatestContext().getUpdateId() + 1));
    propagateCreations();
    propagateUpdates();
  }

  @Override
  public void runOnce() {
    runOnce(LocalDateTime.now());
  }

  private void propagateUpdates() {

    for (EntityKey entityKey : sorted) {
      EntityManager<?, ?> entityManager = entities.get(entityKey);
      entityManager.run();
    }
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

    HashMap<KS, Set<ElementUnit<KB, SB>>> toNotify = new HashMap<>();
    List<ElementUnit<KB, SB>> handles = broadcasters.getCreated();
    for (ElementUnit<KB, SB> elementUnit : handles) {
      Consumer<KS> consumer =
          p -> toNotify.computeIfAbsent(p, k -> new HashSet<>()).add(elementUnit);
      subscribers
          .getElementManager()
          .onNewKey(broadcasters.getKey(), elementUnit.getKey(), consumer);
    }

    for (Map.Entry<KS, Set<ElementUnit<KB, SB>>> entry : toNotify.entrySet()) {
      ElementUnit<KS, SS> subscriber = subscribers.get(entry.getKey(), true);
      for (ElementUnit<KB, SB> broadcaster : entry.getValue()) {
        subscriber.onNewElement(broadcaster.createHandleAdapter(subscriber));
        subscriber.stain();
      }
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

  @Override
  public <K, S> ElementView<K, S> getElement(EntityKey<K, S> entityKey, K key) {
    EntityManager<K, S> entityManager = getEntityManager(entityKey);
    Preconditions.checkArgument(entityManager != null, "Unknown entity %s", entityKey);
    ElementUnit<K, S> unit = entityManager.get(key, false);
    if (unit != null) {
      return unit;
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

package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Event;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.api.Notifier;
import org.aa.olympus.api.UpdateContext;

// TODO: consider adding an extra layer of abstraction to run the engine
final class EngineImpl implements Engine {

  private final EngineContext engineContext;
  private final TimerStore timerStore;
  private final List<EntityKey> sorted;
  private final Map<EntityKey, SourceManager> sources;
  private final Map<EntityKey, EntityManager> entities;
  private final Map<EventChannel, List<EntityManager>> channelToEntities;
  private final List<EventImpl> pendingEvents = new ArrayList<>();

  EngineImpl(
      EngineContext engineContext,
      TimerStore timerStore,
      List<EntityKey> sorted,
      Map<EntityKey, SourceManager> sources,
      Map<EntityKey, EntityManager> entities,
      Map<EventChannel, List<EntityManager>> channelToEntities) {
    this.engineContext = engineContext;
    this.timerStore = timerStore;
    this.sorted = sorted;
    this.sources = ImmutableMap.copyOf(sources);
    this.entities = ImmutableMap.copyOf(entities);
    this.channelToEntities = ImmutableMap.copyOf(channelToEntities);
  }

  @Override
  public void runOnce(LocalDateTime time) {
    Preconditions.checkArgument(!time.isBefore(this.engineContext.getLatestContext().getTime()));
    this.engineContext.setLatestContext(
        new UpdateContextImpl(time, this.engineContext.getLatestContext().getUpdateId() + 1));
    flushTimers();
    propagateEvents();
    propagateCreations();
    propagateUpdates();
  }

  @Override
  public void runOnce() {
    runOnce(LocalDateTime.now());
  }

  private void flushTimers() {
    timerStore.notifyNext(engineContext.getLatestContext().getTime());
  }

  private void propagateUpdates() {

    for (EntityKey entityKey : sorted) {
      EntityManager<?, ?> entityManager = entities.get(entityKey);
      entityManager.run();
    }
  }

  private void propagateEvents() {
    for (Event event : pendingEvents) {
      List<EntityManager> entities =
          channelToEntities.getOrDefault(event.getChannel(), Collections.emptyList());
      for (EntityManager entity : entities) {
        entity.processEvent(event);
      }
    }
    pendingEvents.clear();
  }

  private void propagateCreations() {
    for (EntityKey entityKey : sorted) {
      EntityManager<?, ?> entityManager = entities.get(entityKey);
      propagateCreations(entityManager);
    }

    for (EntityKey entityKey : sorted) {
      EntityManager<?, ?> entityManager = entities.get(entityKey);
      entityManager.flushCreations();
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

    List<ElementUnit<KB, SB>> createdUnits = broadcasters.getCreated();
    for (ElementUnit<KB, SB> createdUnit : createdUnits) {
      Notifier<KS> notifier =
          new Notifier<KS>() {
            @Override
            public void notifyElement(KS elementKey) {
              // TODO: this is two heavy for a callback
              subscribers.get(elementKey, true).queueCreation(createdUnit);
            }

            @Override
            public void notifyAllElements() {
              subscribers.queueCreation(createdUnit);
            }
          };
      subscribers
          .getElementManager()
          .onNewKey(broadcasters.getKey(), createdUnit.getKey(), notifier);
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

  @Override
  public UpdateContext getLatestContext() {
    return this.engineContext.getLatestContext();
  }

  @Override
  public <K, S> List<ElementView<K, S>> getUpdated(
      EntityKey<K, S> entityKey, UpdateContext previous) {
    if (previous.getUpdateId() >= getLatestContext().getUpdateId()) {
      return Collections.emptyList();
    } else {
      List<ElementView<K, S>> modified = new ArrayList<>();
      for (ElementUnit<K, S> unit : getEntityManager(entityKey).getUnits().values()) {
        if (unit.getUpdateContext().getUpdateId() > previous.getUpdateId()) {
          modified.add(unit);
        }
      }
      return modified;
    }
  }

  @Override
  public <E> Engine injectEvent(EventChannel<E> channel, E event) {
    Preconditions.checkArgument(
        channelToEntities.containsKey(channel),
        "%s is not subscribed to %s",
        channel,
        channelToEntities.keySet());
    this.pendingEvents.add(new EventImpl<>(channel, event));
    return this;
  }
}

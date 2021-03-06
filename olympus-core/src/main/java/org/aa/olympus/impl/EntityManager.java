package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Event;
import org.aa.olympus.api.EventChannel;

final class EntityManager<K, S> {

  private final EngineContext engineContext;
  private final TimerStore timerStore;
  private final EntityKey<K, S> key;
  private final ElementManager<K, S> elementManager;
  private final ImmutableMap<EntityKey, EntityManager> dependencies;
  private final EventNotifer<K> notifier = new EventNotifer<>();
  private final Set<EntityKey> dependents;
  private final Set<EventChannel> eventChannels;
  private final Set<ElementUnit> pendingCreations = new LinkedHashSet<>();

  private final Map<K, ElementUnit<K, S>> units = new HashMap<>();

  EntityManager(
      EngineContext engineContext,
      TimerStore timerStore,
      EntityKey<K, S> key,
      ElementManager<K, S> elementManager,
      Map<EntityKey, EntityManager> dependencies,
      Set<EntityKey> dependents,
      Set<EventChannel> eventChannels) {
    this.engineContext = engineContext;
    this.timerStore = timerStore;
    this.key = key;
    this.elementManager = elementManager;
    this.dependencies = ImmutableMap.copyOf(dependencies);
    this.dependents = ImmutableSet.copyOf(dependents);
    this.eventChannels = ImmutableSet.copyOf(eventChannels);
  }

  public EntityKey<K, S> getKey() {
    return key;
  }

  Map<K, ElementUnit<K, S>> getUnits() {
    return Collections.unmodifiableMap(this.units); // TODO: cache
  }

  Set<EntityKey> getDependencies() {
    return dependencies.keySet();
  }

  ElementManager<K, S> getElementManager() {
    return elementManager;
  }

  ElementUnit<K, S> get(K key, boolean createUpdater) {
    ElementUnit<K, S> unit = units.get(key);
    if (unit == null) {
      unit = new ElementUnit<>(engineContext, timerStore, this.key, key, dependencies);
      units.put(key, unit);
    }
    if (unit.getStatus() == ElementStatus.SHADOW && createUpdater) {
      unit.createUpdater(elementManager);
    }
    return unit;
  }

  void run() {
    for (ElementUnit<K, S> element : units.values()) {
      if (element.getNotifications() != 0) {
        element.update();
      }
    }
  }

  List<ElementUnit<K, S>> getCreated() {
    return units.values().stream()
        .filter(p -> p.getStatus() == ElementStatus.CREATED)
        .collect(Collectors.toList());
  }

  public Set<EventChannel> getEventChannels() {
    return eventChannels;
  }

  public <E> void processEvent(Event<E> event) {
    Preconditions.checkArgument(eventChannels.contains(event.getChannel()));
    Preconditions.checkArgument(this.notifier.isEmpty());
    elementManager.onEvent(event, this.notifier);
    for (K key : notifier.getToNotify()) {
      get(key, true).queueEvent(event);
    }
    if (notifier.getNotifyAll()) {
      this.units.values().forEach(p -> p.queueEvent(event));
    }
    notifier.reset();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (ElementUnit<K, S> unit : units.values()) {
      builder
          .append(unit.getKey())
          .append("(")
          .append(unit.getStatus())
          .append('@')
          .append(unit.getUpdateId())
          .append("): ")
          .append(unit.getState())
          .append('\n');
    }
    return builder.toString();
  }

  public <SB, KB> void onNewKey(EntityKey<KB, SB> entityKey, KB newKey) {
    elementManager.onNewKey(entityKey, newKey, notifier);
  }

  public void checkNotifierEmpty() {
    Preconditions.checkArgument(notifier.isEmpty());
  }

  public <SB, KB> void queueCreation(ElementUnit<KB, SB> createdUnit) {
    pendingCreations.add(createdUnit);
  }

  public void flushCreations() {
    for (ElementUnit<K, S> unit : this.units.values()) {
      for (ElementUnit created : pendingCreations) {
        unit.queueCreation(created);
      }
      unit.flushCreations();
    }
    pendingCreations.clear();
  }
}

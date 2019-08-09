package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
  private final EntityKey<K, S> key;
  private final ElementManager<K, S> elementManager;
  private final ImmutableMap<EntityKey, EntityManager> dependencies;
  private final Set<EntityKey> dependents;
  private final Set<EventChannel> eventChannels;

  private final Map<K, ElementUnit<K, S>> units = new HashMap<>();

  EntityManager(
      EngineContext engineContext,
      EntityKey<K, S> key,
      ElementManager<K, S> elementManager,
      Map<EntityKey, EntityManager> dependencies,
      Set<EntityKey> dependents,
      Set<EventChannel> eventChannels) {
    this.engineContext = engineContext;
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
      unit = new ElementUnit<>(engineContext, this.key, key, dependencies);
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
    List<K> toNotify = new ArrayList<>();
    elementManager.onEvent(event, toNotify::add);
    for (K key : toNotify) {
      get(key, true).queueEvent(event);
    }
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
}

package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.aa.olympus.api.CreationContext;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;

final class EntityManager<K, S> {

  private final EntityKey<K, S> key;
  private final ElementManager<K, S> elementManager;
  private final Set<EntityKey> dependencies;
  private final Set<EntityKey> dependents;

  private final Map<K, ElementUnit<K, S>> units = new HashMap<>();

  EntityManager(
      EntityKey<K, S> key,
      ElementManager<K, S> elementManager,
      Set<EntityKey> dependencies,
      Set<EntityKey> dependents) {
    this.key = key;
    this.elementManager = elementManager;
    this.dependencies = ImmutableSet.copyOf(dependencies);
    this.dependents = ImmutableSet.copyOf(dependents);
  }

  Set<EntityKey> getDependencies() {
    return dependencies;
  }

  ElementManager<K, S> getElementManager() {
    return elementManager;
  }

  ElementUnit<K, S> get(K key, boolean create) {
    ElementUnit<K, S> unit = units.get(key);
    if (unit == null && create) {
      UpdateContext updateContext = null; // TODO: find
      Toolbox toolbox = null; // TODO: provide
      ElementUpdater<S> updater = elementManager.create(key, updateContext, toolbox);
      Preconditions.checkNotNull(
          updater,
          "%s an not refuse to create a %s",
          ElementManager.class.getSimpleName(),
          ElementUpdater.class.getSimpleName());
      unit = new ElementUnit<>(this.key, key, updater);
      units.put(key, unit);
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

  List<ElementUnit<?, ?>> getCreated() {
    return units
        .values()
        .stream()
        .filter(p -> p.getStatus() == ElementStatus.CREATED)
        .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (ElementUnit<K, S> unit : units.values()) {
      builder
          .append(unit.getKey())
          .append("(")
          .append(unit.getStatus())
          .append("): ")
          .append(unit.getState())
          .append('\n');
    }
    return builder.toString();
  }
}

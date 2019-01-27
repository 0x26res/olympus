package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;

final class EntityManager<K, S> {

  private final EntityKey<K, S> key;
  private final ElementManager<K, S> elementManager;
  private final Map<EntityKey, EntityManager> dependencies;
  private final Set<EntityKey> dependents;

  private final Map<K, ElementUnit<K, S>> units = new HashMap<>();

  EntityManager(
      EntityKey<K, S> key,
      ElementManager<K, S> elementManager,
      Map<EntityKey, EntityManager> dependencies,
      Set<EntityKey> dependents) {
    this.key = key;
    this.elementManager = elementManager;
    this.dependencies = ImmutableMap.copyOf(dependencies);
    this.dependents = ImmutableSet.copyOf(dependents);
  }

  public EntityKey<K, S> getKey() {
    return key;
  }

  Set<EntityKey> getDependencies() {
    return dependencies.keySet();
  }

  ElementManager<K, S> getElementManager() {
    return elementManager;
  }

  ElementUnit<K, S> get(K key, boolean create) {
    ElementUnit<K, S> unit = units.get(key);
    if (unit == null) {
      unit = new ElementUnit<>(this.key, key);
      units.put(key, unit);
    }
    if (unit.getStatus() == ElementStatus.SHADOW && create) {
      UpdateContext updateContext = null; // TODO: find
      Toolbox toolbox = new ToolboxImpl(dependencies, unit);
      ElementUpdater<S> updater = elementManager.create(key, updateContext, toolbox);
      Preconditions.checkNotNull(
          updater,
          "%s an not refuse to create a %s",
          ElementManager.class.getSimpleName(),
          ElementUpdater.class.getSimpleName());
      unit.setUpdater(updater);
    }
    return unit;
  }

  void run(UpdateContext updateContext) {
    for (ElementUnit<K, S> element : units.values()) {
      if (element.getNotifications() != 0) {
        element.update(updateContext, new ToolboxImpl(dependencies, element));
      }
    }
  }

  List<ElementUnit<K, S>> getCreated() {
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
          .append('@')
          .append(unit.getUpdateId())
          .append("): ")
          .append(unit.getState())
          .append('\n');
    }
    return builder.toString();
  }
}

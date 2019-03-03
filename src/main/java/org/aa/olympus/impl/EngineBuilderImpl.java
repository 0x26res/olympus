package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EngineBuilder;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.SimpleElementManager;

public final class EngineBuilderImpl implements EngineBuilder {

  Map<EntityKey, EntityUnit> entities = new HashMap<>();
  Map<EntityKey, SourceUnit> sources = new HashMap<>();

  private static void checkNoDuplicate(Collection<EntityKey> keys, String name) {
    Preconditions.checkArgument(keys.stream().map(EntityKey::getName).noneMatch(name::equals));
  }

  @Override
  public <K, S> EngineBuilderImpl registerEntity(
      EntityKey<K, S> key, ElementManager<K, S> manager, Set<EntityKey> dependencies) {
    Preconditions.checkArgument(!dependencies.isEmpty(), "Entities must have dependencies");
    checkKey(key);
    checkDependencies(key, dependencies);
    entities.put(key, new EntityUnit<>(key, manager, dependencies));
    return this;
  }

  @Override
  public <K, S> EngineBuilder registerSimpleEntity(EntityKey<K, S> key,
      SimpleElementManager<K, S> manager, Set<EntityKey<K, ?>> dependencies) {
    for (EntityKey<K, ?> dependency : dependencies) {
      Preconditions.checkArgument(
          key.getKeyType().equals(dependency.getKeyType()),
          "%s dependencies must have the same key type. %s vs %s",
          SimpleElementManager.class.getSimpleName(),
          key.getKeyType(),
          dependency.getKeyType());
    }
    return registerEntity(
        key,
        new SimpleElementManagerAdapter<>(manager),
        new HashSet<>(dependencies));
  }

  @Override
  public <K, S> EngineBuilderImpl registerSource(EntityKey<K, S> key) {
    checkKey(key);
    sources.put(key, new SourceUnit<>(key));
    return this;
  }

  @Override
  public Engine build() {
    return new EngineAssembler(this).assemble();
  }

  /** Check key doesn't exists already */
  private void checkKey(EntityKey key) {
    checkNoDuplicate(entities.keySet(), key.getName());
    checkNoDuplicate(sources.keySet(), key.getName());
  }

  /** Gets the dependencies of an entity, or null if it doesn't exists */
  private boolean exists(EntityKey entityKey) {
    return entities.containsKey(entityKey) || sources.containsKey(entityKey);
  }

  private void checkDependencies(EntityKey key, Set<EntityKey> dependencies) {
    Set<String> missing = new TreeSet<>();
    for (EntityKey dependency : dependencies) {
      if (!exists(dependency)) {
        missing.add(dependency.getName());
      }
    }
    if (!missing.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Missing dependencies for %s: %s", key.getName(), dependencies));
    }
  }

  static final class SourceUnit<K, S> {
    final EntityKey<K, S> key;

    SourceUnit(EntityKey<K, S> key) {
      this.key = key;
    }
  }

  static final class EntityUnit<K, S> {
    final EntityKey<K, S> entityKey;
    final ElementManager<K, S> elementManager;
    final Set<EntityKey> dependencies;

    EntityUnit(
        EntityKey<K, S> entityKey,
        ElementManager<K, S> elementManager,
        Set<EntityKey> dependencies) {
      this.entityKey = entityKey;
      this.elementManager = elementManager;
      this.dependencies = ImmutableSet.copyOf(dependencies);
    }

    EntityManager<K, S> createManager(
        Map<EntityKey, EntityManager> dependencies, Set<EntityKey> dependents) {
      Preconditions.checkArgument(dependencies.keySet().equals(this.dependencies));
      return new EntityManager<>(entityKey, elementManager, dependencies, dependents);
    }

    public EntityKey<K, S> getEntityKey() {
      return entityKey;
    }

    public ElementManager<K, S> getElementManager() {
      return elementManager;
    }

    Set<EntityKey> getDependencies() {
      return dependencies;
    }
  }
}

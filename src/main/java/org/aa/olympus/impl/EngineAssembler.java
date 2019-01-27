package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.impl.EngineBuilderImpl.EntityUnit;
import org.aa.olympus.impl.SourceManager.ElementManagerAdapter;

/** Turn a {@link EngineBuilderImpl} into an {@link EngineImpl} */
final class EngineAssembler {

  private final EngineBuilderImpl builder;

  private Map<EntityKey, Set<EntityKey>> entityToDependencies;
  private Map<EntityKey, Set<EntityKey>> entityToDependents;
  private List<EntityKey> topologicalSort;
  private Map<EntityKey, EntityManager> entities;
  private Map<EntityKey, SourceManager> sources;

  EngineAssembler(EngineBuilderImpl builder) {
    this.builder = builder;
  }

  private static Map<EntityKey, Set<EntityKey>> deepCopy(Map<EntityKey, Set<EntityKey>> input) {
    HashMap<EntityKey, Set<EntityKey>> copy = new HashMap<>();
    input.forEach((k, v) -> copy.put(k, new HashSet<EntityKey>(v)));
    return copy;
  }

  private static Map<EntityKey, Set<EntityKey>> makeImmutable(
      Map<EntityKey, Set<EntityKey>> input) {
    ImmutableMap.Builder<EntityKey, Set<EntityKey>> builder = ImmutableMap.builder();
    input.forEach((k, v) -> builder.put(k, ImmutableSet.copyOf(v)));
    return builder.build();
  }

  EngineImpl assemble() {
    createDependencies();
    reverseDependencies();
    sort();
    prepareManagers();
    buildSources();
    buildEntities();
    return new EngineImpl(topologicalSort, sources, entities);
  }

  private void createDependencies() {
    entityToDependencies =
        builder
            .entities
            .values()
            .stream()
            .collect(Collectors.toMap(EntityUnit::getEntityKey, EntityUnit::getDependencies));
    builder.sources.keySet().forEach(k -> entityToDependencies.put(k, Collections.emptySet()));
    entityToDependencies = makeImmutable(entityToDependencies);
  }

  private Set<EntityKey> getAllKeys() {
    Set<EntityKey> results = new HashSet<>(builder.entities.size() + builder.sources.size());
    results.addAll(builder.sources.keySet());
    results.addAll(builder.entities.keySet());
    return results;
  }

  private void reverseDependencies() {
    entityToDependents = new HashMap<>();
    getAllKeys().forEach(p -> entityToDependents.put(p, new HashSet<>()));

    for (Map.Entry<EntityKey, Set<EntityKey>> entry : entityToDependencies.entrySet()) {
      for (EntityKey entityKey : entry.getValue()) {
        entityToDependents.get(entityKey).add(entry.getKey());
      }
    }
    entityToDependents = makeImmutable(entityToDependents);
  }

  private void prepareManagers() {
    this.entities = new HashMap<>();
    this.sources = new HashMap<>();
  }

  private void buildSources() {
    Preconditions.checkArgument(!builder.sources.isEmpty());
    for (EngineBuilderImpl.SourceUnit<?, ?> sourceUnit : builder.sources.values()) {
      createSource(sourceUnit);
    }
  }

  private <K, S> void createSource(EngineBuilderImpl.SourceUnit<K, S> sourceUnit) {
    Map<K, SourceUnit<K, S>> units = new HashMap<>();

    EntityManager<K, S> entityManager =
        new EntityManager<>(
            sourceUnit.key,
            new ElementManagerAdapter<>(units),
            ImmutableMap.of(),
            getDependents(sourceUnit.key));

    entities.put(sourceUnit.key, entityManager);
    sources.put(sourceUnit.key, new SourceManager<>(units, entityManager));
  }

  private void buildEntities() {

    for (EngineBuilderImpl.EntityUnit entity : builder.entities.values()) {
      entities.put(
          entity.getEntityKey(),
          entity.createManager(
              getDependenciesManagers((entity.getEntityKey())),
              getDependents(entity.getEntityKey())));
    }
  }

  Set<EntityKey> getDependencies(EntityKey entityKey) {
    return entityToDependencies.getOrDefault(entityKey, Collections.emptySet());
  }

  private Set<EntityKey> getDependents(EntityKey entityKey) {
    return entityToDependents.getOrDefault(entityKey, Collections.emptySet());
  }

  private Map<EntityKey, EntityManager> getDependenciesManagers(EntityKey entityKey) {

    return getDependencies(entityKey)
        .stream()
        .map(entities::get)
        .collect(Collectors.toMap(EntityManager::getKey, p -> p));
  }

  private void sort() {
    Map<EntityKey, Set<EntityKey>> local = deepCopy(entityToDependencies);
    List<EntityKey> results = new ArrayList<>();
    while (!local.isEmpty()) {
      List<EntityKey> toRemove = new ArrayList<>();
      for (Map.Entry<EntityKey, Set<EntityKey>> entry : local.entrySet()) {
        if (entry.getValue().isEmpty()) {
          results.add(entry.getKey());
          toRemove.add(entry.getKey());
        }
      }
      toRemove.forEach(local::remove);
      local.values().forEach(p -> p.removeAll(toRemove));
    }
    topologicalSort = ImmutableList.copyOf(results);
  }
}

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
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.impl.EngineBuilderImpl.EntityUnit;
import org.slf4j.LoggerFactory;

/** Turn a {@link EngineBuilderImpl} into an {@link EngineImpl} */
final class EngineAssembler {

  private final EngineBuilderImpl builder;

  private EngineContext engineContext;
  private TimerStore timerStore;
  private Map<EntityKey, Set<EntityKey>> entityToDependencies;
  private Map<EntityKey, Set<EntityKey>> entityToDependents;
  private List<EntityKey> topologicalSort;
  private Map<EntityKey, EntityManager> entities;
  private Map<EntityKey, SourceManager> sources;
  private Map<EventChannel, List<EntityManager>> channelToEntities;

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
    createContext();
    createTimerStore();
    createDependencies();
    reverseDependencies();
    sort();
    prepareManagers();
    buildEntities();
    mapChannelToEntities();
    return new EngineImpl(
        engineContext, timerStore, topologicalSort, sources, entities, channelToEntities);
  }

  private void createContext() {
    engineContext = new EngineContext(LoggerFactory.getLogger(EngineImpl.class.getName()));
  }

  private void createTimerStore() {
    timerStore = new TimerStore(engineContext);
  }

  private void createDependencies() {
    entityToDependencies =
        builder.entities.values().stream()
            .collect(Collectors.toMap(EntityUnit::getEntityKey, EntityUnit::getDependencies));
    entityToDependencies = makeImmutable(entityToDependencies);
  }

  private Set<EntityKey> getAllKeys() {
    Set<EntityKey> results = new HashSet<>(builder.entities.size());
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

  private void buildEntities() {

    for (EntityKey entityKey : topologicalSort) {
      EngineBuilderImpl.EntityUnit entity = builder.entities.get(entityKey);
      Preconditions.checkArgument(entity != null);
      entities.put(
          entity.getEntityKey(),
          entity.createManager(
              engineContext,
              timerStore,
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

    return getDependencies(entityKey).stream()
        .map(p -> Preconditions.checkNotNull(entities.get(p), "Missing deps %s", p))
        .collect(Collectors.toMap(EntityManager::getKey, p -> p));
  }

  private void mapChannelToEntities() {

    Map<EventChannel, List<EntityManager>> results = new HashMap<>();
    for (EntityManager entityManager : entities.values()) {
      Set<EventChannel> channels = entityManager.getEventChannels();
      for (EventChannel<?> eventChannel : channels) {
        results.computeIfAbsent(eventChannel, p -> new ArrayList<>()).add(entityManager);
      }
    }
    this.channelToEntities =
        results.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, p -> ImmutableList.copyOf(p.getValue())));
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

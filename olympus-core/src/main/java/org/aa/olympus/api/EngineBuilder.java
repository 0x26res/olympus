package org.aa.olympus.api;

import java.util.Set;

/** Utility to set up and build a {@link Engine} */
public interface EngineBuilder {

  /**
   * Register a source entity.
   *
   * <p>A source entity is an entity whose keys & state are managed externally, between update
   * cycles. The user controls the keys and state of these entities manually.
   *
   * @param key Key of the source entity
   * @param <K> The type of the keys for each element
   * @param <S> The type of the state of each element
   * @return {@code this}
   */
  <K, S> EngineBuilder registerSource(EntityKey<K, S> key);

  /**
   * Register an inner entity.
   *
   * <p>An inner entity register to other upstream entities (either source entities, or other inner
   * entities). When new elements are created within its upstream entities, it will be notified and
   * be able to create & notify its elements. Its elements can then subscribe to state updates from
   * the upstream elements.
   *
   * @param key Key of the inner entity
   * @param manager Manager for the entity (manages the creation and notification of elements
   * @param dependencies kyes of the upstream entities the inner entity is interested in
   * @param <K> The type of the key for each element
   * @param <S> The type of the state for each
   * @return {@code this}
   */
  <K, S> EngineBuilder registerInnerEntity(
      EntityKey<K, S> key, ElementManager<K, S> manager, Set<EntityKey> dependencies);

  /** Simpler way of registering an inner entity. See {@link SimpleElementManager} */
  <K, S> EngineBuilder registerSimpleEntity(
      EntityKey<K, S> key, SimpleElementManager<K, S> manager, Set<EntityKey<K, ?>> dependencies);

  Engine build();
}

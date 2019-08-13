package org.aa.olympus.api;

import java.util.Set;
import java.util.function.Function;

/** Utility to set up and build a {@link Engine} */
public interface EngineBuilder {

  /**
   * Register an event channel.
   *
   * <p>An event channel is how outside events come inside the engine
   *
   * @param <E> the type of the event
   */
  <E> EngineBuilder registerEventChannel(EventChannel<E> key);

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
   * @param dependencies keys of the upstream entities the inner entity is interested in
   * @param channels channels of the outside events
   * @param <K> The type of the key for each element
   * @param <S> The type of the state for each
   * @return {@code this}
   */
  <K, S> EngineBuilder registerInnerEntity(
      EntityKey<K, S> key,
      ElementManager<K, S> manager,
      Set<EntityKey> dependencies,
      Set<EventChannel> channels);

  /** @See {@link #registerInnerEntity(EntityKey, ElementManager, Set, Set)} */
  <K, S> EngineBuilder registerInnerEntity(
      EntityKey<K, S> key, ElementManager<K, S> manager, Set<EntityKey> dependencies);

  /** Simpler way of registering an inner entity. See {@link SimpleElementManager} */
  <K, S> EngineBuilder registerSimpleEntity(
      EntityKey<K, S> key, SimpleElementManager<K, S> manager, Set<EntityKey<K, ?>> dependencies);

  <E, K, S> EngineBuilder eventToEntity(
      EventChannel<E> eventChannel,
      EntityKey<K, S> entityKey,
      Function<E, K> keyExtractor,
      Function<E, S> stateExtractor);

  /**
   * Convenience function to apply custom engine transformation while keeping a functional/flowing
   * API
   *
   * @param transformer a transformer to apply to the builder
   * @return {@code this}
   */
  EngineBuilder pipe(Function<EngineBuilder, EngineBuilder> transformer);

  /**
   * Assemble the engine into it's runtime implementation
   *
   * @return a built {@link Engine}
   */
  Engine build();
}

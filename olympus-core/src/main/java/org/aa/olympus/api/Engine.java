package org.aa.olympus.api;

import java.time.LocalDateTime;
import java.util.List;

/** Holds an instance of the engine */
public interface Engine {

  <E> void injectEvent(EventChannel<E> channel, E event);

  /** Run the engine once, processing all updates */
  void runOnce(LocalDateTime time);

  /** Calls {@link #runOnce()} with the current time */
  void runOnce();

  /**
   * Gets the state of one entity
   *
   * @deprecated use {@link #getElement(EntityKey, Object)}
   */
  @Deprecated
  <K, S> S getState(EntityKey<K, S> entityKey, K key);

  /** Gets a view of the a given element */
  <K, S> ElementView<K, S> getElement(EntityKey<K, S> entityKey, K key);

  /** The context of the latest update */
  UpdateContext getLatestContext();

  <K, S> List<ElementView<K, S>> getUpdated(EntityKey<K, S> entityKey, UpdateContext previous);
}

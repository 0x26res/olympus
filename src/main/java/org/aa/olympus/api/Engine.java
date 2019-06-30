package org.aa.olympus.api;

import java.time.LocalDateTime;

/** Holds an instance of the engine */
public interface Engine {

  /** Updates the state of a source entity element */
  <K, S> void setSourceState(EntityKey<K, S> entityKey, K key, S state);

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
}

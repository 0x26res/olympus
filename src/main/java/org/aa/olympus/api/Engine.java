package org.aa.olympus.api;

import java.time.LocalDateTime;

public interface Engine {

  /** Updates the state of into source entity element */
  <K, S> void setSourceState(EntityKey<K, S> entityKey, K key, S state);

  /** Run the engine once, processing all updates */
  void runOnce(LocalDateTime time);

  /** Gets the state of one entity */
  <K, S> S getState(EntityKey<K, S> entityKey, K key);
}

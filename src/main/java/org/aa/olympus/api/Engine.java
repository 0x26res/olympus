package org.aa.olympus.api;

import java.util.Date;

public interface Engine {

  /** Updates the state of into source entity element*/
  <K, S> void setSourceState(EntityKey<K, S> entityKey, K key, S state);

  /** Run the engine once, processing all updates */
  void runOnce(Date time);

  /** Gets the state of one entity */
  <K, S> S getState(EntityKey<K, S> entityKey, K key);
}

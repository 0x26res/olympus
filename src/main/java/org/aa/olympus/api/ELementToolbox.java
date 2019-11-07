package org.aa.olympus.api;

public interface ELementToolbox {

  /** Give access to a handle to a parent element */
  <K, V> ElementHandle<K, V> get(EntityKey<K, V> entityKey, K elementKey);
}

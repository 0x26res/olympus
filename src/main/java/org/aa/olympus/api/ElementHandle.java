package org.aa.olympus.api;

public interface ElementHandle<K, S> {

  EntityKey<K, S> getEntityKey();

  K getKey();

  S getState();

  ElementStatus getStatus();

  /** Did this element update since the last time the subscriber was notified */
  boolean hasUpdated();
}

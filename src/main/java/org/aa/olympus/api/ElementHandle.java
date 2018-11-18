package org.aa.olympus.api;

public interface ElementHandle<K, S> {

  EntityKey<K, S> getEntityKey();

  K getKey();

  S getState();

  ElementStatus getStatus();
}

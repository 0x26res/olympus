package org.aa.olympus.api;

// TODO: add more convenience functions (get or default, get optional...)
public interface ElementView<K, S> {

  EntityKey<K, S> getEntityKey();

  K getKey();

  S getState();

  ElementStatus getStatus();

  UpdateContext getUpdateContext();

  S getStateOrDefault(S defaultState);
}

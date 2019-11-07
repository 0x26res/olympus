package org.aa.olympus.api;

/** An immutable view on an element of the engine */
public interface ElementView<K, S> {

  EntityKey<K, S> getEntityKey();

  K getKey();

  S getState();

  /** Get the state or it's default value */
  S getStateOrDefault(S defaultState);

  ElementStatus getStatus();

  UpdateContext getUpdateContext();

  /** The last error (only available in error state) */
  Exception getError();
}

package org.aa.olympus.api;

public interface ElementView<K, S> {

  EntityKey<K, S> getEntityKey();

  K getKey();

  S getState();

  ElementStatus getStatus();

  UpdateContext getUpdateContext();

}

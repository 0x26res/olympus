package org.aa.olympus.api;

import com.google.common.reflect.TypeToken;

/** Key for an engine entity. This has value semantics */
// TODO: add a qualified element key (entity key + key of the element)
@SuppressWarnings("UnstableApiUsage")
public interface EntityKey<K, S> {

  /** Uniquely identify the entity */
  String getName();

  /** The type of the key of this entity's element */
  TypeToken<K> getKeyType();

  /** The type of the state of this entity's element */
  TypeToken<S> getStateType();

  /**
   * Cast a raw {@link ElementHandle} to be of the same key/state type as this {@link EntityKey}
   *
   * <p>The {@link ElementHandle#getEntityKey()} must be the same as {@code this}
   */
  ElementHandle<K, S> castHandle(ElementHandle elementHandle);

  /** @deprecated use {@link #castHandle(ElementHandle)} */
  @Deprecated
  K castKey(K key);
}

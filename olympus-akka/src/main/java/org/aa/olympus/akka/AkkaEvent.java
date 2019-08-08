package org.aa.olympus.akka;

import org.aa.olympus.api.EntityKey;

final class AkkaEvent<K, S> {
  private final EntityKey<K, S> entityKey;
  private final K key;
  private final S state;

  AkkaEvent(EntityKey<K, S> entityKey, K key, S state) {
    this.entityKey = entityKey;
    this.key = key;
    this.state = state;
  }

  EntityKey<K, S> getEntityKey() {
    return entityKey;
  }

  K getKey() {
    return key;
  }

  S getState() {
    return state;
  }
}

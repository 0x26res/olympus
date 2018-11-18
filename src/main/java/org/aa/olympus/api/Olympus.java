package org.aa.olympus.api;

import org.aa.olympus.impl.EngineBuilderImpl;

public final class Olympus {

  public static EngineBuilder builder() {
    return new EngineBuilderImpl();
  }

  public static <K, S> EntityKey<K, S> createKey(String name, Class<K> key, Class<S> state) {
    return EntityKey.of(name, key, state);
  }
}

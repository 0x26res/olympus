package org.aa.olympus.api;

import com.google.common.reflect.TypeToken;
import org.aa.olympus.impl.EngineBuilderImpl;

public final class Olympus {

  public static EngineBuilder builder() {
    return new EngineBuilderImpl();
  }

  public static <K, S> EntityKey<K, S> key(
      String name, TypeToken<K> keyType, TypeToken<S> stateType) {
    return new EntityKey<>(name, keyType, stateType);
  }

  public static <K, S> EntityKey<K, S> key(String name, Class<K> keyType, Class<S> stateType) {
    return key(name, TypeToken.of(keyType), TypeToken.of(stateType));
  }

  public static <K, S> EntityKey<K, S> key(String name, Class<K> keyType, TypeToken<S> stateType) {
    return new EntityKey<>(name, TypeToken.of(keyType), stateType);
  }

  public static <K, S> EntityKey<K, S> key(String name, TypeToken<K> keyType, Class<S> stateType) {
    return new EntityKey<>(name, keyType, TypeToken.of(stateType));
  }
}

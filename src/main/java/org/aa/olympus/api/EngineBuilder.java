package org.aa.olympus.api;

import java.util.Set;

public interface EngineBuilder {

  <K, S> EngineBuilder registerEntity(
      EntityKey<K, S> key, ElementManager<K, S> manager, Set<EntityKey> dependencies);

  <K, S> EngineBuilder registerSource(EntityKey<K, S> key);

  Engine build();
}

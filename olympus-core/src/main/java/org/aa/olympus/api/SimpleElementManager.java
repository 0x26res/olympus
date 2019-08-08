package org.aa.olympus.api;

/**
 * A simpler version of the {@link ElementManager}, where each dependency has got the same key type.
 */
@FunctionalInterface
public interface SimpleElementManager<K, S> {

  /** Called when a new element is created in a parent entity */
  ElementUpdater<S> create(K key);
}

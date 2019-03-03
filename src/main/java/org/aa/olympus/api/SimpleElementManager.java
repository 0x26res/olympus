package org.aa.olympus.api;

/**
 * A simpler version of the {@link ElementManager}, where each dependency has got the same key
 * type.
 *
 * Whenever a new key is created in a parent entity, this will get notified
 */
@FunctionalInterface
public interface SimpleElementManager<K, S> {

  ElementUpdater<S> create(K key);
}
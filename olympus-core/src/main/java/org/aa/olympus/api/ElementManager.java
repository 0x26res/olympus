package org.aa.olympus.api;

import java.util.function.Consumer;

/**
 * Controls the creation and notification of {@link ElementUpdater} for a given {@link EntityKey}.
 */
public interface ElementManager<K, S> {

  /** Called when a new element needs to be created within this {@link EntityKey} */
  ElementUpdater<S> create(K elementKey, UpdateContext updateContext, Toolbox toolbox);

  /**
   * Called when an element is created within an upstream {@link EntityKey}
   *
   * <p>If new {@link ElementUpdater} need to be notified/created their element key should be
   * appended to {@code toNotify}
   */
  <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<K> toNotify);

  default <E> void onEvent(Event<E> event, Consumer<K> toNotify) {};
}

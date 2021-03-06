package org.aa.olympus.api;

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
  default <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Notifier<K> notifier) {};

  default <E> void onEvent(Event<E> event, Notifier<K> toNotify) {}
}

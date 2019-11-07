package org.aa.olympus.impl;

import java.util.function.Consumer;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.SimpleElementManager;
import org.aa.olympus.api.ELementToolbox;
import org.aa.olympus.api.UpdateContext;

/** Wraps a {@link SimpleElementManager} into a {@link ElementManager} */
// TODO: Pass the not ready state / error state
public final class SimpleElementManagerAdapter<K, S> implements ElementManager<K, S> {

  private final SimpleElementManager<K, S> simpleElementManager;

  public SimpleElementManagerAdapter(SimpleElementManager<K, S> simpleElementManager) {
    this.simpleElementManager = simpleElementManager;
  }

  @Override
  public ElementUpdater<S> create(K key, UpdateContext updateContext, ELementToolbox ELementToolbox) {
    return simpleElementManager.create(key);
  }

  @Override
  public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<K> toNotify) {
    toNotify.accept(((K) key));
  }
}

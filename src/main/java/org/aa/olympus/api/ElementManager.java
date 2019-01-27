package org.aa.olympus.api;

import java.util.function.Consumer;

public interface ElementManager<K, S> {

  ElementUpdater<S> create(K key, UpdateContext updateContext, Toolbox toolbox);

  <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<K> toNotify);
}

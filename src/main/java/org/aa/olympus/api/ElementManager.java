package org.aa.olympus.api;

import java.util.function.Consumer;

public interface ElementManager<K, S> {

  ElementUpdater<S> create(K key, UpdateContext updateContext, Toolbox toolbox);

  void onNewKey(ElementHandle newElement, Consumer<K> toNotify);
}
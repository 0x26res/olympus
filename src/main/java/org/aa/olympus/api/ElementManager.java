package org.aa.olympus.api;

import java.util.function.Consumer;

public interface ElementManager<K, S> {

  ElementUpdater<S> create(K key, CreationContext context);

  void onNewKey(ElementHandle newElement, Consumer<K> toNotify);
}

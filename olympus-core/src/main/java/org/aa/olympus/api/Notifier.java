package org.aa.olympus.api;

public interface Notifier<K> {

  void notifyElement(K elementKey);

  void notifyAllElements();
}

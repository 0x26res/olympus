package org.aa.olympus.impl;

import java.util.HashSet;
import java.util.Set;
import org.aa.olympus.api.Notifier;

public class EventNotifer<K> implements Notifier<K> {

  private final Set<K> toNotify = new HashSet<>();
  private boolean notifyAll = false;

  @Override
  public void notifyElement(K elementKey) {
    toNotify.add(elementKey);
  }

  @Override
  public void notifyAllElements() {
    notifyAll = true;
  }

  public boolean isEmpty() {
    return !notifyAll && toNotify.isEmpty();
  }

  public boolean getNotifyAll() {
    return this.notifyAll;
  }

  Set<K> getToNotify() {
    return toNotify;
  }

  public void reset() {
    this.toNotify.clear();
    this.notifyAll = false;
  }
}

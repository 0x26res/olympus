package org.aa.olympus.akka;

import org.aa.olympus.api.EventChannel;

final class AkkaEvent<E> {
  private final EventChannel<E> channel;
  private final E value;

  public AkkaEvent(EventChannel<E> channel, E value) {
    this.channel = channel;
    this.value = value;
  }

  public EventChannel<E> getChannel() {
    return channel;
  }

  public E getValue() {
    return value;
  }
}

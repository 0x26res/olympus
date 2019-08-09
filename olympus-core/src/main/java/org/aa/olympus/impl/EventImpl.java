package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import org.aa.olympus.api.Event;
import org.aa.olympus.api.EventChannel;

public final class EventImpl<E> implements Event<E> {

  private final EventChannel<E> channel;
  private final E value;

  public EventImpl(EventChannel<E> channel, E value) {
    this.channel = Preconditions.checkNotNull(channel);
    this.value = Preconditions.checkNotNull(value);
  }

  @Override
  public EventChannel<E> getChannel() {
    return channel;
  }

  @Override
  public E getValue() {
    return value;
  }
}

package org.aa.olympus.api;

public interface Event<E> {

  EventChannel<E> getChannel();

  E getValue();
}

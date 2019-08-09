package org.aa.olympus.api;

import com.google.common.reflect.TypeToken;

public interface EventChannel<E> {

  /** Uniquely identify the channel within the engine */
  String getName();

  /** The type of the data of this channel's events */
  TypeToken<E> getEventType();

  E castEvent(Event<?> event);
}

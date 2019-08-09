package org.aa.olympus.impl;

import com.google.common.base.MoreObjects;
import com.google.common.reflect.TypeToken;
import java.util.Objects;
import org.aa.olympus.api.Event;
import org.aa.olympus.api.EventChannel;

public final class EventChannelImpl<E> implements EventChannel<E> {

  private final String name;
  private final TypeToken<E> eventType;

  public EventChannelImpl(String name, TypeToken<E> eventType) {
    this.name = name;
    this.eventType = eventType;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public TypeToken<E> getEventType() {
    return eventType;
  }

  @Override
  public E castEvent(Event<?> event) {
    if (event.getChannel().equals(this)) {
      return (E) event.getValue();
    } else {
      throw new ClassCastException(
          String.format("Can't cast from %s to %s", event.getChannel(), this));
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, eventType);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    } else if (other == null || other.getClass() != EntityKeyImpl.class) {
      return false;
    } else {
      EventChannelImpl that = (EventChannelImpl) other;
      return this.name.equals(that.name) && this.eventType == that.eventType;
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).toString();
  }
}

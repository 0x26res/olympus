package org.aa.olympus.impl;

import com.google.common.base.MoreObjects;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.UpdateContext;

final class ElementHandleAdapter<K, S> implements ElementHandle<K, S> {

  private final ElementUnit<K, S> broadcaster;
  private final ElementUnit subscriber;
  private SubscriptionType subscriptionType;

  ElementHandleAdapter(ElementUnit<K, S> broadcaster, ElementUnit subscriber) {
    this.broadcaster = broadcaster;
    this.subscriber = subscriber;
    this.subscriptionType = SubscriptionType.NONE;
  }

  @Override
  public EntityKey<K, S> getEntityKey() {
    return broadcaster.getEntityKey();
  }

  @Override
  public K getKey() {
    return broadcaster.getKey();
  }

  @Override
  public S getState() {
    return broadcaster.getState();
  }

  @Override
  public ElementStatus getStatus() {
    return broadcaster.getStatus();
  }

  @Override
  public boolean hasUpdated() {
    return subscriber == null || broadcaster.getUpdateId() > subscriber.getUpdateId();
  }

  @Override
  public UpdateContext getUpdateContext() {
    return broadcaster.getUpdateContext();
  }

  @Override
  public SubscriptionType getSubscriptionType() {
    return subscriptionType;
  }

  @Override
  public ElementHandle<K, S> subscribe(SubscriptionType subscriptionType) {

    if (subscriptionType != this.subscriptionType) {
      switch (subscriptionType) {
        case STRONG:
          subscriber.subscribe(broadcaster);
          break;
        case NONE:
          subscriber.unsubscribe(broadcaster);
          break;
        default:
          throw new UnsupportedValueException(SubscriptionType.class, subscriptionType);
      }
      this.subscriptionType = subscriptionType;
    }
    return this;
  }

  @Override
  public S getStateOrDefault(S defaultState) {
    switch (getStatus()) {
      case UPDATED:
        return getState();
      default:
        return defaultState;
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(ElementHandle.class)
        .add("broadcaster", broadcaster)
        .add("subscriber", subscriber)
        .toString();
  }
}

package org.aa.olympus.impl;

import org.aa.olympus.api.SubscriptionType;

public final class Subscription {

  private final ElementUnit broadcaster;
  private final SubscriptionType type;

  public Subscription(ElementUnit broadcaster, SubscriptionType type) {
    this.broadcaster = broadcaster;
    this.type = type;
  }

  public ElementUnit getBroadcaster() {
    return broadcaster;
  }

  public SubscriptionType getType() {
    return type;
  }
}

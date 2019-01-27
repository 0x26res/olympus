package org.aa.olympus.api;

// TODO: add convenience function (get or default, get optional...)
public interface ElementHandle<K, S> {

  EntityKey<K, S> getEntityKey();

  K getKey();

  S getState();

  ElementStatus getStatus();

  /** Did this element update since the last time the subscriber was notified */
  boolean hasUpdated();

  SubscriptionType getSubscriptionType();

  ElementHandle<K, S> subscribe(SubscriptionType subscriptionType);

  S getStateOrDefault(S defaultState);
}

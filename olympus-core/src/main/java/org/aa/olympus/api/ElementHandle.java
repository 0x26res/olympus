package org.aa.olympus.api;

// TODO: add more convenience function (get or default, get optional...)
// TODO: add access to the error message
public interface ElementHandle<K, S> extends ElementView<K, S> {

  /** Did this element update since the last time the subscriber was notified */
  boolean hasUpdated();

  SubscriptionType getSubscriptionType();

  ElementHandle<K, S> subscribe(SubscriptionType subscriptionType);
}

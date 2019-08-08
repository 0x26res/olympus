package org.aa.olympus.api;

/** Represent the type of a subscription between two entity elements */
public enum SubscriptionType {

  /** Notify on all updates, propagate failure state */
  STRONG(true, true),
  /** Notify on all updates, propagate failure state */
  OPTIONAL(true, false),
  /** Does not notify on updates, does not propagate failure */
  WEAK(false, false),
  /** Not subscribed at all (can be used to unsubscribe) */
  NONE(false, false);

  public final boolean propagateUpdates;
  public final boolean propagateError;

  SubscriptionType(boolean propagateUpdates, boolean propagateError) {
    this.propagateUpdates = propagateUpdates;
    this.propagateError = propagateError;
  }
}

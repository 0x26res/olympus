package org.aa.olympus.api;

public enum SubscriptionType {

  /** Notify on all updates, propagate failure state */
  STRONG(true, true),
  /** Does not notify updates, does not propagate failure */
  WEAK(false, false),
  /** Unsubscribe */
  NONE(false, false);

  public final boolean propagateUpdates;
  public final boolean propagateError;

  SubscriptionType(boolean propagateUpdates, boolean propagateError) {
    this.propagateUpdates = propagateUpdates;
    this.propagateError = propagateError;
  }
}

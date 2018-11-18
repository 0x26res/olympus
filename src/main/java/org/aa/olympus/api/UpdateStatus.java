package org.aa.olympus.api;

public enum UpdateStatus {
  /** Nothing happened, this is a non event */
  NOTHING,
  /** The state changed */
  UPDATED,
  /** The element doesn't exist any more */
  DELETED,
  /** More work is needed, keep going */
  MORE_WORK,
  /** Not ready to compute */
  NOT_READY
}

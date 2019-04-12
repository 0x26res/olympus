package org.aa.olympus.api;

public enum UpdateStatus {
  /** Nothing happened, this is a non event */
  NOTHING,
  /** The state changed */
  UPDATED,
  /** The element doesn't exist any more */
  DELETED,
  /**
   * Not ready to compute (some upstream elements are missing
   */
  NOT_READY,
  /**
   * An error as happened
   */
  ERROR,
  /**
   * An error as happened in a broadcaster
   */
  UPSTREAM_ERROR
}

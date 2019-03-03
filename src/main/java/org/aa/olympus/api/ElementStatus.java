package org.aa.olympus.api;

public enum ElementStatus {
  /** Empty shell: down stream requires it but there's no upstream data */
  SHADOW,
  /**
   * The element has been created and validated, but has'nt updated yet
   *
   * <p>This state is transient, after the first call to update, the unit will change. One cannot
   * receive another element that is in this state, excepted when propagating creation
   */
  CREATED,
  /** The element is valid but doesn't have the required upstream data */
  NOT_READY,
  /** The element has successfully updated, returning a value */
  UPDATED,
  /**
   * The element has failed during update
   */
  // TODO: add test
  ERROR,
  /**
   * The element could not update because of failed upstream element
   */
  // TODO: add test
  UPSTREAM_ERROR,
  /** The element has decided it no longer needed to exist */
  // TODO: add support for it
  DELETED
}

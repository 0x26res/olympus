package org.aa.olympus.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/** Result of the update of an element. */
public final class UpdateResult<T> {

  private static final UpdateResult<?> DELETED = new UpdateResult<>(UpdateStatus.DELETED, null);
  private static final UpdateResult<?> NOT_READY = new UpdateResult<>(UpdateStatus.NOT_READY, null);
  private static final UpdateResult<?> NOTHING = new UpdateResult<>(UpdateStatus.NOTHING, null);
  private static final UpdateResult<?> ERROR = new UpdateResult<>(UpdateStatus.ERROR, null);
  private static final UpdateResult<?> UPSTREAM_ERROR =
      new UpdateResult<>(UpdateStatus.UPSTREAM_ERROR, null);

  private final UpdateStatus status;
  private final T state;

  private UpdateResult(UpdateStatus status, T state) {
    this.status = status;
    this.state = state;
  }

  /** The element updated and its state changed */
  public static <T> UpdateResult<T> update(T state) {
    Preconditions.checkNotNull(state);
    return new UpdateResult<>(UpdateStatus.UPDATED, state);
  }

  /**
   * The element state has been recalculated but may be the same.
   *
   * <p>If the state is equals to the previous state, the update won't be propagated
   */
  public static <T> UpdateResult<T> maybe(T newState) {
    Preconditions.checkNotNull(newState);
    return new UpdateResult<>(UpdateStatus.MAYBE, newState);
  }

  /** The element no long exists */
  public static <T> UpdateResult<T> delete() {
    return (UpdateResult<T>) DELETED;
  }

  /** Something is missing upstream */
  public static <T> UpdateResult<T> notReady() {
    return (UpdateResult<T>) NOT_READY;
  }

  /** An error occurred. Not that throwing an exception has got the same effects */
  public static <T> UpdateResult<T> error() {
    return (UpdateResult<T>) ERROR;
  }

  /** An error occured upstream */
  public static <T> UpdateResult<T> upstreamError() {
    return (UpdateResult<T>) UPSTREAM_ERROR;
  }

  /** Nothing changed, use the same state don't propage updates */
  private static <T> UpdateResult<T> unchanged() {
    return (UpdateResult<T>) NOTHING;
  }

  public UpdateStatus getStatus() {
    return status;
  }

  public T getState() {
    return state;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("status", status).add("state", state).toString();
  }

  public enum UpdateStatus {
    /** Nothing happened, this is a non event */
    NOTHING,
    /** The state may have changed, if the value is different from the previous one */
    MAYBE,
    /** The state changed */
    UPDATED,
    /** The element doesn't exist any more */
    DELETED,
    /** Not ready to compute (some upstream elements are missing */
    NOT_READY,
    /** An error as happened */
    ERROR,
    /** An error as happened in a broadcaster */
    UPSTREAM_ERROR
  }
}

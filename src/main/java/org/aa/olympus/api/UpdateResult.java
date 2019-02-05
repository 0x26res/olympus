package org.aa.olympus.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.Objects;

public final class UpdateResult<T> {

  private static final UpdateResult<?> DELETED = new UpdateResult<>(UpdateStatus.DELETED, null);
  private static final UpdateResult<?> NOT_READY = new UpdateResult<>(UpdateStatus.NOT_READY, null);
  private static final UpdateResult<?> NOTHING = new UpdateResult<>(UpdateStatus.NOTHING, null);
  private static final UpdateResult<?> MORE_WORK = new UpdateResult<>(UpdateStatus.MORE_WORK, null);

  private final UpdateStatus status;
  private final T state;

  private UpdateResult(UpdateStatus status, T state) {
    this.status = status;
    this.state = state;
  }

  public static <T> UpdateResult<T> update(T state) {
    Preconditions.checkNotNull(state);
    return new UpdateResult<>(UpdateStatus.UPDATED, state);
  }

  public static <T> UpdateResult<T> maybe(T previousState, T newState) {
    if (Objects.equals(previousState, newState)) {
      return unchanged();
    } else {
      return update(newState);
    }
  }

  public static <T> UpdateResult<T> delete() {
    return (UpdateResult<T>) DELETED;
  }

  public static <T> UpdateResult<T> notReady() {
    return (UpdateResult<T>) NOT_READY;
  }

  private static <T> UpdateResult<T> unchanged() {
    return (UpdateResult<T>) NOTHING;
  }

  public static <T> UpdateResult<T> moreWork() {
    return (UpdateResult<T>) MORE_WORK;
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
}

package org.aa.olympus.impl;

import com.google.common.base.MoreObjects;
import java.time.LocalDateTime;
import org.aa.olympus.api.UpdateContext;

public class UpdateContextImpl implements UpdateContext {

  public static final UpdateContext NONE = new UpdateContextImpl(LocalDateTime.MIN, 0);

  private final LocalDateTime time;
  private final int updateId;

  public UpdateContextImpl(LocalDateTime time, int updateId) {
    this.time = time;
    this.updateId = updateId;
  }

  @Override
  public LocalDateTime getTime() {
    return time;
  }

  @Override
  public int getUpdateId() {
    return updateId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("time", time)
        .add("updateId", updateId)
        .toString();
  }
}

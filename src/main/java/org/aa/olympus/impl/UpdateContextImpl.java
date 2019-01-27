package org.aa.olympus.impl;

import java.time.LocalDateTime;
import org.aa.olympus.api.UpdateContext;

public class UpdateContextImpl implements UpdateContext {

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
}

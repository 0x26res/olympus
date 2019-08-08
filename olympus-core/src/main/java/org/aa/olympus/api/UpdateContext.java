package org.aa.olympus.api;

import java.time.LocalDateTime;
import org.aa.olympus.impl.UpdateContextImpl;

/** Information about a cycle of updates. Has got value semantics */
public interface UpdateContext {

  static UpdateContext none() {
    return UpdateContextImpl.NONE;
  }

  LocalDateTime getTime();

  int getUpdateId();
}

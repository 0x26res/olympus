package org.aa.olympus.api;

import java.time.LocalDateTime;

/** Information about a cycle of updates. Has got value semantics */
public interface UpdateContext {

  LocalDateTime getTime();

  int getUpdateId();
}

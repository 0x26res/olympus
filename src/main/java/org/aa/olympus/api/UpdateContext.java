package org.aa.olympus.api;

import java.time.LocalDateTime;

public interface UpdateContext {

  LocalDateTime getTime();

  int getUpdateId();
}

package org.aa.olympus.api;

import java.time.LocalDateTime;

public interface ElementTimer {

  LocalDateTime getTimerAt();

  TimerState getState();

  void cancel();
}

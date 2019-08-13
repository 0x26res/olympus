package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.List;
import org.aa.olympus.api.ElementTimer;
import org.aa.olympus.api.TimerState;

public class ElementTimerImpl implements ElementTimer {

  private final ElementUnit unit;
  private final LocalDateTime timerAt;
  private final TimerStore timerStore;
  private final List<ElementTimerImpl> timers;
  private TimerState state;

  public ElementTimerImpl(
      ElementUnit unit,
      LocalDateTime timerAt,
      TimerStore timerStore,
      List<ElementTimerImpl> timers) {
    this.unit = unit;
    this.timerAt = timerAt;
    this.timerStore = timerStore;
    this.timers = timers;
    this.state = TimerState.READY;
  }

  @Override
  public void cancel() {
    Preconditions.checkState(
        this.state == TimerState.READY,
        "Cannot cancel {} when in {}={}",
        ElementTimer.class.getSimpleName(),
        TimerState.class.getSimpleName(),
        this.state);

    boolean removed = this.timers.remove(this);
    if (!removed) {
      throw new RuntimeException("Timer already expired/cancel");
    }
    if (this.timers.isEmpty()) {
      timerStore.removeSlot(this.timerAt);
    }
    this.state = TimerState.CANCELLED;
  }

  @Override
  public LocalDateTime getTimerAt() {
    return timerAt;
  }

  @Override
  public TimerState getState() {
    return state;
  }

  void trigger() {
    Preconditions.checkArgument(this.state == TimerState.READY);
    unit.stain();
    state = TimerState.TRIGGERED;
  }
}

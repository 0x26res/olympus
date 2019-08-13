package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class TimerStore {

  private final EngineContext engineContext;
  private final Map<LocalDateTime, List<ElementTimerImpl>> timers = new TreeMap<>();

  public TimerStore(EngineContext engineContext) {
    this.engineContext = engineContext;
  }

  ElementTimerImpl create(ElementUnit unit, LocalDateTime timerAt) {
    Preconditions.checkArgument(
        timerAt.isAfter(engineContext.getLatestContext().getTime()),
        "Cannot set timer in the past: {} vs {}",
        timerAt,
        engineContext.getLatestContext());
    List<ElementTimerImpl> slot = timers.computeIfAbsent(timerAt, k -> new ArrayList<>());
    ElementTimerImpl timer = new ElementTimerImpl(unit, timerAt, this, slot);
    slot.add(timer);
    return timer;
  }

  void removeSlot(LocalDateTime timerAt) {
    List<ElementTimerImpl> timers = this.timers.remove(timerAt);
    Preconditions.checkArgument(
        timers != null && timers.isEmpty(),
        "Only empty slots can be removed: %s %s",
        timerAt,
        timers);
  }

  int notifyNext(LocalDateTime limit) {
    int results = 0;
    for (Map.Entry<LocalDateTime, List<ElementTimerImpl>> entry : timers.entrySet()) {
      if (entry.getKey().isAfter(limit)) {
        break;
      } else {
        entry.getValue().forEach(ElementTimerImpl::trigger);
        entry.getValue().clear();
        results += entry.getValue().size();
      }
    }
    return results;
  }
}

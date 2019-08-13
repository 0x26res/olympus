package org.aa.olympus.api;

import com.google.common.annotations.Beta;
import java.time.LocalDateTime;
import java.util.List;

// TODO: rename to ElementToolbox since it's at element level
// TODO: add possibility to set timers
public interface Toolbox {

  /** Give access to a handle to a parent element */
  <K, V> ElementHandle<K, V> get(EntityKey<K, V> entityKey, K elementKey);

  @Beta
  List<Event> getEvents();

  ElementTimer setTimer(LocalDateTime timerAt);
}

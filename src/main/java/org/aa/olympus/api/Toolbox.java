package org.aa.olympus.api;

import java.time.LocalDateTime;
import java.util.List;

public interface Toolbox {

  <K, V> ElementHandle<K, V> subscribe(
      EntityKey<K, V> entityKey, K elementKey, SubscriptionType subscriptionType);

  // TODO: refine, with possibility to cancel and repeat
  void addTimer(LocalDateTime at);
}

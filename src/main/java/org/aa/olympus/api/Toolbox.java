package org.aa.olympus.api;

import java.time.LocalDateTime;
import java.util.List;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.EntityKey;

public interface Toolbox {

  <K, V> ElementHandle<K, V> subscribe(EntityKey<K, V> entity, K key);

  <K, V> void unsubscribe(ElementHandle<K, V> handle);
  //
  <K, V> List<ElementHandle<K, V>> getCreated(EntityKey<K, V> key);

  // TODO: refine, with possibility to cancel and repeat
  void addTimer(LocalDateTime at);
}

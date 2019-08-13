package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.util.List;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementTimer;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Event;
import org.aa.olympus.api.Toolbox;

// TODO: One toolbox per ElementUnit
// TODO: prefetch deps
// TODO: how to deal with multiple subscriptions
public class ToolboxImpl implements Toolbox {

  private final TimerStore timerStore;
  private final ImmutableMap<EntityKey, EntityManager> dependencies;
  private final ElementUnit unit;
  private final List<Event> events;

  public ToolboxImpl(
      TimerStore timerStore,
      ImmutableMap<EntityKey, EntityManager> dependencies,
      ElementUnit unit,
      List<Event> events) {
    this.timerStore = timerStore;
    this.dependencies = dependencies;
    this.unit = unit;
    this.events = events;
  }

  @Override
  public <K, S> ElementHandle<K, S> get(EntityKey<K, S> entityKey, K elementKey) {

    Preconditions.checkArgument(
        dependencies.containsKey(entityKey),
        "Cannot %s see elements from to %s. You must declare %s as a dependency of %s",
        unit.getEntityKey(),
        entityKey,
        unit.getEntityKey(),
        entityKey);
    @SuppressWarnings("unchecked") // we know this is safe
    EntityManager<K, S> entityManager = (EntityManager<K, S>) dependencies.get(entityKey);
    ElementUnit<K, S> broadcaster = entityManager.get(elementKey, true);
    return broadcaster.createHandleAdapter(unit);
  }

  @Override
  public List<Event> getEvents() {
    return events;
  }

  @Override
  public ElementTimer setTimer(LocalDateTime timerAt) {
    return timerStore.create(this.unit, timerAt);
  }
}

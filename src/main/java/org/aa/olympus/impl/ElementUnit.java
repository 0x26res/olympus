package org.aa.olympus.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Set;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.api.UpdateStatus;

final class ElementUnit<K, S> {
  private final EntityKey<K, S> entityKey;
  private final K key;
  private final ElementUpdater<S> updater;
  private final HandleAdapter handleAdapter;

  private final Set<ElementUnit> broadcasters = new HashSet<>();
  private final Set<ElementUnit> listeners = new HashSet<>();

  private ElementStatus status;
  private S state;
  private int notifications;

  ElementUnit(EntityKey<K, S> entityKey, K key, ElementUpdater<S> updater) {
    this.entityKey = entityKey;
    this.key = key;
    this.updater = updater;
    handleAdapter = new HandleAdapter();

    status = ElementStatus.CREATED;
  }

  public K getKey() {
    return key;
  }

  ElementStatus getStatus() {
    return status;
  }

  S getState() {
    return state;
  }

  int getNotifications() {
    return notifications;
  }

  ElementHandle<K, S> getHandleAdapter() {
    return handleAdapter;
  }

  void stain() {
    ++notifications;
  }

  public void update() {
    UpdateResult<S> result = this.updater.update(state, null, null);
    if (handleUpdateResult(result)) {
      listeners.forEach(ElementUnit::stain);
    }
  }

  private boolean handleUpdateResult(UpdateResult<S> results) {
    switch (results.getStatus()) {
      case UPDATED:
        this.state = results.getState();
        this.status = ElementStatus.UPDATED;
        return true;
      case DELETED:
        this.status = ElementStatus.DELETED;
        this.state = null;
        return true;
      case NOT_READY:
        Preconditions.checkState(
            this.status == ElementStatus.NOT_READY || this.status == ElementStatus.CREATED);
        Preconditions.checkState(state == null);
        this.status = ElementStatus.NOT_READY;
        return false;
      case NOTHING:
        Preconditions.checkState(state != null);
        Preconditions.checkState(this.status == ElementStatus.UPDATED);
        return false;
      default:
        throw new UnsupportedValueException(UpdateStatus.class, results.getStatus());
    }
  }

  void addBroadcasters(Set<ElementUnit> newBroadcasters) {
    for (ElementUnit<?, ?> newBroadcaster : newBroadcasters) {
      if (broadcasters.add(newBroadcaster)) {
        updater.onNewElement(newBroadcaster.getHandleAdapter());
        newBroadcaster.listeners.add(this);
      }
    }
  }

  private final class HandleAdapter implements ElementHandle<K, S> {

    @Override
    public EntityKey<K, S> getEntityKey() {
      return entityKey;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public S getState() {
      return state;
    }

    @Override
    public ElementStatus getStatus() {
      return status;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(ElementHandle.class)
          .add("entityKey", entityKey)
          .add("key", key)
          .add("status", status)
          .add("state", state)
          .toString();
    }
  }
}

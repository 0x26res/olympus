package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Set;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.api.UpdateStatus;

final class ElementUnit<K, S> implements ElementView<K, S> {
  private final EntityKey<K, S> entityKey;
  private final K key;
  private final Set<ElementUnit> broadcasters = new HashSet<>();
  private final Set<ElementUnit> subscribers = new HashSet<>();
  private ElementUpdater<S> updater;
  private ElementStatus status;
  private S state;
  private int notifications;
  private UpdateContext updateContext = UpdateContextImpl.NONE;

  ElementUnit(EntityKey<K, S> entityKey, K key) {
    this.entityKey = entityKey;
    this.key = key;
    this.updater = null;
    status = ElementStatus.SHADOW;
  }

  void setUpdater(ElementUpdater<S> updater) {
    Preconditions.checkNotNull(updater);
    Preconditions.checkState(this.updater == null);
    this.updater = updater;
    status = ElementStatus.CREATED;
  }

  public EntityKey<K, S> getEntityKey() {
    return entityKey;
  }

  public K getKey() {
    return key;
  }

  public ElementStatus getStatus() {
    return status;
  }

  public S getState() {
    return state;
  }

  int getNotifications() {
    return notifications;
  }

  @Deprecated // ?
  public int getUpdateId() {
    return updateContext.getUpdateId();
  }

  public UpdateContext getUpdateContext() {
    return updateContext;
  }

  ElementHandleAdapter<K, S> createHandleAdapter(ElementUnit subscriber) {
    return new ElementHandleAdapter<>(this, subscriber);
  }

  void stain() {
    ++notifications;
  }

  public void update(UpdateContext updateContext, Toolbox toolbox) {
    UpdateResult<S> result = this.updater.update(state, updateContext, toolbox);
    if (handleUpdateResult(result)) {
      subscribers.forEach(ElementUnit::stain);
    }
    this.updateContext = updateContext;
    this.notifications = 0;
  }

  public <KB, SB> void onNewElement(ElementHandle<KB, SB> broadcaster) {
    this.updater.onNewElement(broadcaster);
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

  void subscribe(ElementUnit broadcaster) {
    this.broadcasters.add(broadcaster);
    broadcaster.subscribers.add(this);
  }

  void unsubscribe(ElementUnit broadcaster) {
    this.broadcasters.remove(broadcaster);
    broadcaster.subscribers.remove(this);
  }
}

package org.aa.olympus.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.ELementToolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.api.UpdateResult.UpdateStatus;

final class ElementUnit<K, S> implements ElementView<K, S> {

  private final EngineContext engineContext;
  private final EntityKey<K, S> entityKey;
  private final K key;
  private final Set<ElementHandleAdapter> broadcasters = new HashSet<>();
  private final Set<ElementHandleAdapter<K, S>> subscribers = new HashSet<>();

  private ElementUpdater<S> updater;
  private ElementStatus status;
  private S state;
  private int notifications;
  private UpdateContext updateContext = UpdateContextImpl.NONE;
  private Exception lastError;

  ElementUnit(EngineContext engineContext, EntityKey<K, S> entityKey, K key) {
    this.engineContext = engineContext;
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

  @Override
  public EntityKey<K, S> getEntityKey() {
    return entityKey;
  }

  @Override
  public K getKey() {
    return key;
  }

  @Override
  public ElementStatus getStatus() {
    return status;
  }

  @Override
  public S getState() {
    return state;
  }

  @Override
  public S getStateOrDefault(S defaultState) {
    if (status == ElementStatus.OK) {
      return state;
    } else {
      return defaultState;
    }
  }

  @Override
  public Exception getError() {
    if (status == ElementStatus.ERROR) {
      return Preconditions.checkNotNull(lastError);
    } else {
      throw new IllegalStateException(String.format(
          "Can't request error while status is %s", status));
    }
  }

  int getNotifications() {
    return notifications;
  }


  int getUpdateId() {
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

  public void update(ELementToolbox ELementToolbox) {
    UpdateResult<S> result = getUpdateResult(ELementToolbox);
    if (handleUpdateResult(result)) {
      subscribers.forEach(ElementHandleAdapter::stain);
      this.updateContext = engineContext.getLatestContext();
    }
    this.notifications = 0;
  }

  private UpdateResult<S> getUpdateResult(ELementToolbox ELementToolbox) {
    ElementStatus broadcastersStatus = getBroadcastersStatus();
    switch (broadcastersStatus) {
      case OK:
        try {
          UpdateResult<S> results = this.updater
              .update(state, engineContext.getLatestContext(), ELementToolbox);
          this.lastError = null;
          return results;
        } catch (Exception e) {
          engineContext
              .getErrorLogger()
              .log(Level.SEVERE, String.format("%s failed: %s", this, e.getMessage()));
          this.lastError = e;
          return UpdateResult.error();
        }
      case ERROR:
        this.lastError = null;
        return UpdateResult.upstreamError();
      case NOT_READY:
        this.lastError = null;
        return UpdateResult.notReady();
      default:
        throw new UnsupportedValueException(ElementStatus.class, broadcastersStatus);
    }
  }

  private ElementStatus getBroadcastersStatus() {
    int failed = 0;
    int notReady = 0;

    for (ElementHandleAdapter broadcaster : broadcasters) {
      if (broadcaster.getSubscriptionType() == SubscriptionType.STRONG) {
        switch (broadcaster.getStatus()) {
          case ERROR:
          case UPSTREAM_ERROR:
            ++failed;
            break;
          case OK:
            break;
          case NOT_READY:
          case CREATED:
          case SHADOW:
          case DELETED:
            ++notReady;
            break;
          default:
            throw new UnsupportedValueException(ElementStatus.class, broadcaster.getStatus());
        }
      }
    }

    if (failed != 0) {
      return ElementStatus.ERROR;
    } else if (notReady != 0) {
      return ElementStatus.NOT_READY;
    } else {
      return ElementStatus.OK;
    }
  }

  <KB, SB> void onNewElement(ElementHandle<KB, SB> broadcaster) {
    // TODO: rethrow any error with informative message
    if (this.updater.onNewElement(broadcaster)) {
      this.stain();
    }
  }

  private boolean handleUpdateResult(UpdateResult<S> results) {
    switch (results.getStatus()) {
      case UPDATED:
        this.state = results.getState();
        this.status = ElementStatus.OK;
        return true;
      case MAYBE:
        boolean changed = !Objects.equals(this.state, results.getState());
        this.state = results.getState();
        this.status = ElementStatus.OK;
        return changed;
      case DELETED:
        this.state = null;
        return changeStatus(ElementStatus.DELETED);
      case NOT_READY:
        this.state = null;
        return changeStatus(ElementStatus.NOT_READY);
      case NOTHING:
        Preconditions.checkState(state != null);
        Preconditions.checkState(this.status == ElementStatus.OK);
        return false;
      case ERROR:
        this.state = null;
        return changeStatus(ElementStatus.ERROR);
      case UPSTREAM_ERROR:
        this.state = null;
        return changeStatus(ElementStatus.UPSTREAM_ERROR);
      default:
        throw new UnsupportedValueException(UpdateStatus.class, results.getStatus());
    }
  }

  private boolean changeStatus(ElementStatus elementStatus) {
    boolean changed = this.status != elementStatus;
    this.status = elementStatus;
    return changed;
  }

  public boolean updateSubscriber(ElementHandleAdapter<K, S> handle, boolean add) {
    if (add) {
      return this.subscribers.add(handle);
    } else {
      return this.subscribers.remove(handle);
    }
  }

  public boolean updateBroadcaster(ElementHandleAdapter handle, boolean add) {
    if (add) {
      return this.broadcasters.add(handle);
    } else {
      return this.broadcasters.remove(handle);
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("entityKey", entityKey).add("key", key).toString();
  }
}

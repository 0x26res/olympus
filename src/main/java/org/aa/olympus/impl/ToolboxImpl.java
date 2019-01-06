package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.SubscriptionType;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.impl.EngineBuilderImpl.EntityUnit;

public class ToolboxImpl implements Toolbox {

  EngineImpl engine;
  EntityUnit entity;
  ElementUnit element;

  @Override
  public <K, S> ElementHandle<K, S> subscribe(EntityKey<K, S> entityKey, K elementKey,
      SubscriptionType subscriptionType) {
    Preconditions.checkArgument(
        entity.dependencies.contains(entityKey),
        "Cannot %s subscribe to %s. You must declare %s as de dependency of %s",
        entity.entityKey,
        entityKey,
        entity.entityKey,
        entityKey);
    EntityManager<K, S> entityManager = engine.getEntityManager(entityKey);
    ElementUnit<K,S> broadcaster = entityManager.get(elementKey, true);
    // TODO: add the subscription
    return broadcaster.getHandleAdapter();
  }

  @Override
  public void addTimer(LocalDateTime at) {

  }
}

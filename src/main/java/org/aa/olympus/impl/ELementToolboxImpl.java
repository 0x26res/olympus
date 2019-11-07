package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import java.util.Map;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.ELementToolbox;

// TODO: One toolbox per ElementUnit
// TODO: prefetch deps
// TODO: how to deal with multiple subscriptions
public class ELementToolboxImpl implements ELementToolbox {

  private final Map<EntityKey, EntityManager> dependencies;
  private final ElementUnit unit;

  public ELementToolboxImpl(Map<EntityKey, EntityManager> dependencies, ElementUnit unit) {
    this.dependencies = dependencies;
    this.unit = unit;
  }

  @Override
  public <K, S> ElementHandle<K, S> get(EntityKey<K, S> entityKey, K elementKey) {

    Preconditions.checkArgument(
        dependencies.containsKey(entityKey),
        "Cannot %s see elements from to %s. You must declare %s as de dependency of %s",
        unit.getEntityKey(),
        entityKey,
        unit.getEntityKey(),
        entityKey);
    @SuppressWarnings("unchecked") // we know this is safe
    EntityManager<K, S> entityManager = (EntityManager<K, S>) dependencies.get(entityKey);
    ElementUnit<K, S> broadcaster = entityManager.get(elementKey, true);
    return broadcaster.createHandleAdapter(unit);
  }
}

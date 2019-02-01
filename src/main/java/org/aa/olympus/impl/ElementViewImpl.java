package org.aa.olympus.impl;

import com.google.common.base.MoreObjects;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.UpdateContext;

public class ElementViewImpl<K, S> implements ElementView<K, S> {

  private final ElementUnit<K, S> unit;

  public ElementViewImpl(ElementUnit<K, S> unit) {
    this.unit = unit;
  }

  @Override
  public EntityKey<K, S> getEntityKey() {
    return unit.getEntityKey();
  }

  @Override
  public K getKey() {
    return unit.getKey();
  }

  @Override
  public S getState() {
    return unit.getState();
  }

  @Override
  public ElementStatus getStatus() {
    return unit.getStatus();
  }

  @Override
  public UpdateContext getUpdateContext() {
    return unit.getUpdateContext();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("unit", unit)
        .toString();
  }
}

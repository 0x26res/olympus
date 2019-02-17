package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;

final class SourceUnit<K, S> implements ElementUpdater<S> {
  private ElementUnit<K, S> elementUnit;
  private S lastState;

  public void setElementUnit(ElementUnit<K, S> elementUnit) {
    Preconditions.checkState(this.elementUnit == null);
    Preconditions.checkNotNull(elementUnit);
    this.elementUnit = elementUnit;
  }

  void setState(S lastState) {
    this.lastState = lastState;
    elementUnit.stain();
  }

  boolean ready() {
    return elementUnit != null;
  }

  @Override
  public UpdateResult<S> update(S previous, UpdateContext updateContext, Toolbox toolbox) {
    if (lastState == null) {
      // TODO: there's a potential memory leak here, unit should delete themselves from the map
      return UpdateResult.delete();
    } else {

      return UpdateResult.update(lastState);
    }
  }

  @Override
  public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
    throw new IllegalStateException("Should not be notified of new elements");
  }
}

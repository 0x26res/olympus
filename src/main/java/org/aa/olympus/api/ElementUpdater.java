package org.aa.olympus.api;

public interface ElementUpdater<S> {

  UpdateResult<S> update(S previous, UpdateContext updateContext, Toolbox toolbox);

  <K2, S2> void onNewElement(ElementHandle<K2, S2> handle);
}

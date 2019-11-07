package org.aa.olympus.api;

/** Manages updates for an element */
public interface ElementUpdater<S> {

  /** Called when the state needs to be updated because of upstream changes or timer */
  UpdateResult<S> update(S previous, UpdateContext updateContext, ELementToolbox ELementToolbox);

  /**
   * Called when a new element this handle may be interested is created
   *
   * @return true if this element should be stained
   */
  <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle);
}

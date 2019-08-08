package org.aa.olympus.api;

/** Use as key for entities with only one element */
public final class SingletonKey {

  private static final SingletonKey INSTANCE = new SingletonKey();

  public static SingletonKey getInstance() {
    return INSTANCE;
  }
}

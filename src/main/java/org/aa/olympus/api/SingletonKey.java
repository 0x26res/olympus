package org.aa.olympus.api;

/** For entities with only one element */
public final class SingletonKey {

  private static final SingletonKey INSTANCE = new SingletonKey();

  public static SingletonKey getInstance() {
    return INSTANCE;
  }
}

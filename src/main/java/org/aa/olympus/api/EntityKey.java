package org.aa.olympus.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import java.util.Objects;

// TODO: make an interface, add a creator to Olympus
// TODO: add a qualified key (entity key + element key)
@SuppressWarnings("UnstableApiUsage")
public final class EntityKey<K, S> {

  private final String name;
  private final TypeToken<K> keyType;
  private final TypeToken<S> stateType;

  EntityKey(String name, TypeToken<K> keyType, TypeToken<S> stateType) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
    this.name = name;
    this.keyType = Preconditions.checkNotNull(keyType);
    this.stateType = Preconditions.checkNotNull(stateType);
  }

  public String getName() {
    return name;
  }

  public TypeToken<K> getKeyType() {
    return keyType;
  }

  public TypeToken<S> getStateType() {
    return stateType;
  }

  public ElementHandle<K, S> castHandle(ElementHandle elementHandle) {
    if (elementHandle.getEntityKey().equals(this)) {
      // This is safe as the type are the same
      return (ElementHandle<K, S>) elementHandle;
    } else {
      throw new ClassCastException(
          String.format(
              "%s doesn't match %s for %s",
              elementHandle.getEntityKey(), this, elementHandle.getKey()));
    }
  }

  @Deprecated // use the qualified key to do that safely
  public K castKey(K key) {
    // TODO: add a proper check on the type
    return (K) key;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, keyType, stateType);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    } else if (other == null || other.getClass() != EntityKey.class) {
      return false;
    } else {
      EntityKey that = (EntityKey) other;
      return this.name.equals(that.name)
          && this.stateType == that.stateType
          && this.keyType == that.keyType;
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).toString();
  }
}

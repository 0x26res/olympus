package org.aa.olympus.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import java.util.Objects;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.EntityKey;

public class EntityKeyImpl<K, S> implements EntityKey<K, S> {

  private final String name;
  private final TypeToken<K> keyType;
  private final TypeToken<S> stateType;

  public EntityKeyImpl(String name, TypeToken<K> keyType, TypeToken<S> stateType) {
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
    } else if (other == null || other.getClass() != EntityKeyImpl.class) {
      return false;
    } else {
      EntityKeyImpl that = (EntityKeyImpl) other;
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

package org.aa.olympus.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import java.util.Objects;

public final class EntityKey<K, S> {

  private final String name;
  private final TypeToken<K> keyType;
  private final TypeToken<S> stateType;

  private EntityKey(String name, TypeToken<K> keyType, TypeToken<S> stateType) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
    this.name = name;
    this.keyType = Preconditions.checkNotNull(keyType);
    this.stateType = Preconditions.checkNotNull(stateType);
  }

  public static <K, S> EntityKey<K, S> of(
      String name, TypeToken<K> keyType, TypeToken<S> stateType) {
    return new EntityKey<>(name, keyType, stateType);
  }

  public static <K, S> EntityKey<K, S> of(String name, Class<K> keyType, Class<S> stateType) {
    return of(name, TypeToken.of(keyType), TypeToken.of(stateType));
  }

  public static <K, S> EntityKey<K, S> of(String name, Class<K> keyType, TypeToken<S> stateType) {
    return new EntityKey<>(name, TypeToken.of(keyType), stateType);
  }

  public static <K, S> EntityKey<K, S> of(String name, TypeToken<K> keyType, Class<S> stateType) {
    return new EntityKey<>(name, keyType, TypeToken.of(stateType));
  }

  public String getName() {
    return name;
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

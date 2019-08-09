package org.aa.olympus.examples;

public final class KeyValuePair<K, V> {

  private final K key;
  private final V value;

  public KeyValuePair(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public static <K, V> KeyValuePair<K, V> of(K key, V value) {
    return new KeyValuePair<>(key, value);
  }

  public K getKey() {
    return key;
  }

  public V getValue() {
    return value;
  }
}

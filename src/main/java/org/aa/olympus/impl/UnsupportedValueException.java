package org.aa.olympus.impl;

class UnsupportedValueException extends IllegalArgumentException {

  public <T> UnsupportedValueException(Class<T> type, T value) {
    super(String.format("Unsupported %s: %s", type.getName(), value));
  }
}

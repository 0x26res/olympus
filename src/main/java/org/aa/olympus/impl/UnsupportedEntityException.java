package org.aa.olympus.impl;

import org.aa.olympus.api.EntityKey;

public class UnsupportedEntityException extends IllegalArgumentException {

  public UnsupportedEntityException(EntityKey entityKey) {
    super(String.format("Unsupported %s: %s", EntityKey.class.getName(), entityKey.getName()));
  }
}

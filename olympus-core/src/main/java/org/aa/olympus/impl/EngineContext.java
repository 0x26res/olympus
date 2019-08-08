package org.aa.olympus.impl;

import org.aa.olympus.api.UpdateContext;
import org.slf4j.Logger;

// TODO: fine a better logger interface
public final class EngineContext {

  private final Logger errorLogger;
  private UpdateContext latestContext;

  public EngineContext(Logger errorLogger) {
    this.errorLogger = errorLogger;
    this.latestContext = UpdateContextImpl.NONE;
  }

  public Logger getErrorLogger() {
    return errorLogger;
  }

  public UpdateContext getLatestContext() {
    return latestContext;
  }

  void setLatestContext(UpdateContext latestContext) {
    this.latestContext = latestContext;
  }
}

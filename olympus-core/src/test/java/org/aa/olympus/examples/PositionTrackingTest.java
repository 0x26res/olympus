package org.aa.olympus.examples;

import java.time.LocalDateTime;
import org.aa.olympus.api.Engine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PositionTrackingTest {

  private Engine engine;

  @Before
  public void setUp() {
    engine = PositionTracking.createEngine();
  }

  void setPosition(String product, String maturity, String account, int value) {
    engine.injectEvent(
        PositionTracking.POSITION_CHANNEL,
        KeyValuePair.of(PositionTracking.key(product, maturity, account), value));
  }

  @Test
  public void demo() {

    setPosition("S&P500", "DEC18", "FOO", 10);
    setPosition("S&P500", "MAR19", "FOO", -10);
    setPosition("S&P500", "MAR19", "BAR", 30);
    setPosition("DOW30", "MAR19", "BAR", -20);

    engine.runOnce(LocalDateTime.now());

    Assert.assertEquals(
        10,
        engine
            .getState(PositionTracking.COMPANY, PositionTracking.key(null, null, null))
            .intValue());
    Assert.assertEquals(
        0,
        engine
            .getState(PositionTracking.ACCOUNT, PositionTracking.key(null, null, "FOO"))
            .intValue());
    Assert.assertEquals(
        10,
        engine
            .getState(PositionTracking.ACCOUNT, PositionTracking.key(null, null, "BAR"))
            .intValue());
    Assert.assertEquals(
        30,
        engine
            .getState(PositionTracking.PRODUCT_ACCOUNT, PositionTracking.key("S&P500", null, "BAR"))
            .intValue());
    Assert.assertEquals(
        -20,
        engine
            .getState(PositionTracking.PRODUCT_ACCOUNT, PositionTracking.key("DOW30", null, "BAR"))
            .intValue());
  }
}

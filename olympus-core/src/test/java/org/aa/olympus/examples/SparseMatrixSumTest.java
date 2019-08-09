package org.aa.olympus.examples;

import static org.aa.olympus.examples.SparseMatrixSum.AGGREGATE;
import static org.aa.olympus.examples.SparseMatrixSum.TOTAL;

import java.time.LocalDateTime;
import org.aa.olympus.api.Engine;
import org.aa.olympus.examples.SparseMatrixSum.Position;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SparseMatrixSumTest {

  private Engine engine;

  @Before
  public void setUp() {
    engine = SparseMatrixSum.createEngine();
  }

  void update(int row, int col, int value) {
    engine.injectEvent(
        SparseMatrixSum.UPDATE_CHANNEL, KeyValuePair.of(Position.of(row, col), value));
  }

  @Test
  public void test() {

    update(0, 0, 10);
    update(0, 1, 11);
    update(0, 2, 12);

    update(1, 0, 100);
    update(2, 0, 110);
    update(3, 0, 120);

    engine.runOnce(LocalDateTime.now());
    Assert.assertEquals(363, engine.getState(TOTAL, Position.of(-1, -1)).intValue());

    update(3, 0, 130);
    engine.runOnce(LocalDateTime.now());
    Assert.assertEquals(373, engine.getState(TOTAL, Position.of(-1, -1)).intValue());

    update(3, 0, 0);
    engine.runOnce(LocalDateTime.now());
    Assert.assertEquals(243, engine.getState(TOTAL, Position.of(-1, -1)).intValue());

    update(200, 23, 120);
    engine.runOnce(LocalDateTime.now());
    Assert.assertEquals(363, engine.getState(TOTAL, Position.of(-1, -1)).intValue());
    Assert.assertEquals(120, engine.getState(AGGREGATE, Position.of(200, -1)).intValue());
    Assert.assertEquals(120, engine.getState(AGGREGATE, Position.of(-1, 23)).intValue());
  }
}

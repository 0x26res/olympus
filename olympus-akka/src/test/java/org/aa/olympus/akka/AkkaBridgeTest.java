package org.aa.olympus.akka;

import akka.stream.ClosedShape;
import akka.stream.javadsl.GraphDSL;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestPublisher;
import akka.stream.testkit.TestSubscriber;
import org.aa.olympus.api.Engine;
import org.aa.olympus.examples.PortfolioValuation;
import org.aa.olympus.examples.PortfolioValuation.StringDoublePair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AkkaBridgeTest {
  private AkkaTestTools akkaTestTools;
  private Engine engine;
  private AkkaBridge bridge;
  private TestPublisher.Probe<StringDoublePair> quantityProbe;
  private TestPublisher.Probe<StringDoublePair> priceProbe;
  private TestSubscriber.Probe<StringDoublePair> valuationProbe;

  @Before
  public void setUp() {
    akkaTestTools = AkkaTestTools.create();
    engine = PortfolioValuation.createEngine();
    bridge = new AkkaBridge(engine);

    akkaTestTools.createTestGraph(this::build);
  }

  ClosedShape build(GraphDSL.Builder<?> builder) {

    quantityProbe = TestPublisher.probe(0, akkaTestTools.system);
    priceProbe = TestPublisher.probe(0, akkaTestTools.system);
    valuationProbe = TestSubscriber.probe(akkaTestTools.system);

    builder
        .from(builder.add(Source.fromPublisher(quantityProbe)))
        .to(builder.add(bridge.fromAkka(PortfolioValuation.PRICE_CHANNEL)));
    builder
        .from(builder.add(Source.fromPublisher(priceProbe)))
        .to(builder.add(bridge.fromAkka(PortfolioValuation.QUANTITY_CHANNEL)));
    builder
        .from(builder.add(bridge.toAkka(PortfolioValuation.VALUATION, StringDoublePair::of)))
        .to(builder.add(Sink.fromSubscriber(valuationProbe)));

    return ClosedShape.getInstance();
  }

  @Test
  public void testAkkaBridge() {
    quantityProbe.sendNext(StringDoublePair.of("1", 1.0));
    priceProbe.sendNext(StringDoublePair.of("1", 2.0));

    valuationProbe.ensureSubscription();
    Assert.assertEquals(2.0 + 1.0, valuationProbe.requestNext().value, 0.0);

    quantityProbe.sendNext(StringDoublePair.of("1", 2.0));
    Assert.assertEquals(2.0 + 2.0, valuationProbe.requestNext().value, 0.0);

    quantityProbe.sendNext(StringDoublePair.of("1", 1.1));
    priceProbe.sendNext(StringDoublePair.of("1", 2.2));
    quantityProbe.sendNext(StringDoublePair.of("1", 1.3));
    Assert.assertEquals(2.2 + 1.3, valuationProbe.requestNext().value, 0.0);

    quantityProbe.sendNext(StringDoublePair.of("IBM", 1.1));
    priceProbe.sendNext(StringDoublePair.of("GOOG", 2.2));
    quantityProbe.sendNext(StringDoublePair.of("1", 1.3));
  }

  @After
  public void close() {
    akkaTestTools.close();
  }
}

package org.aa.olympus.akka;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.function.Function;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.ClosedShape;
import akka.stream.Materializer;
import akka.stream.Supervision;
import akka.stream.Supervision.Directive;
import akka.stream.javadsl.GraphDSL;
import akka.stream.javadsl.GraphDSL.Builder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AkkaTestTools implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(AkkaTestTools.class);

  public final ActorSystem system;
  public final Materializer materializer;

  public static AkkaTestTools create() {
    return create(ConfigFactory.load());
  }

  public void createTestGraph(akka.japi.function.Function<Builder<NotUsed>, ClosedShape> creator) {
    materializer.materialize(GraphDSL.create(creator));
  }

  private static AkkaTestTools create(Config config) {
    ActorSystem system = ActorSystem.create("TEST", config);
    ActorMaterializerSettings settings =
        ActorMaterializerSettings.create(system)
            .withInputBuffer(1, 1)
            .withDebugLogging(true)
            .withSupervisionStrategy((Function<Throwable, Directive>) AkkaTestTools::onError);
    return new AkkaTestTools(system, ActorMaterializer.create(settings, system));
  }

  private AkkaTestTools(ActorSystem system, Materializer materializer) {
    this.system = system;
    this.materializer = materializer;
  }

  private static Directive onError(Throwable error) {
    LOG.error("Error when running graph", error);
    return Supervision.stop();
  }

  @Override
  public void close() {
    akka.testkit.javadsl.TestKit.shutdownActorSystem(system);
  }
}

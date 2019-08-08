package org.aa.olympus.akka;

import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.UpdateContext;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AkkaBridge {
  private static final Logger LOG = LoggerFactory.getLogger(AkkaBridge.class);

  private final Engine engine;
  private final BatchQueue<AkkaEvent> queue;
  private final Map<EntityKey, BridgeSubscription> subscriptions;

  public AkkaBridge(Engine engine) {
    this.engine = engine;
    this.queue = new BatchQueue<>();
    this.subscriptions = new HashMap<>();
  }

  // TODO: add back pressure policy (queue size, already running)
  // TODO: add error handling
  public <V, K, S> Sink<V, NotUsed> fromAkka(
      EntityKey<K, S> entityKey, Function<V, K> keyExtractor, Function<V, S> valueExtractor) {
    return Sink.fromSubscriber(
        new BridgeSubscriber<>(this, entityKey, keyExtractor, valueExtractor));
  }

  public <K, S, V> Source<V, NotUsed> toAkka(
      EntityKey<K, S> entityKey, BiFunction<K, S, V> stitcher) {
    BridgeSubscription<K, S, V> subscription = new BridgeSubscription<>(this, entityKey, stitcher);
    this.subscriptions.put(entityKey, subscription);

    return Source.fromPublisher(
        new Publisher<V>() {
          @Override
          public void subscribe(Subscriber<? super V> s) {
            subscription.setSubscriber(s);
          }
        });
  }

  boolean runIfNeeded() {
    if (!this.queue.isEmpty() && allReady()) {
      run();
      return true;
    } else {
      return false;
    }
  }

  public boolean queue(AkkaEvent event) {
    return queue.push(event);
  }

  private void feedDemand() {
    for (BridgeSubscription subscription : subscriptions.values()) {
      subscription.feedDemand();
    }
  }

  private boolean allReady() {
    for (BridgeSubscription subscription : subscriptions.values()) {
      if (!subscription.ready()) {
        return false;
      }
    }
    return true;
  }

  private void run() {
    UpdateContext previousContext = engine.getLatestContext();
    List<AkkaEvent> events = queue.flush();
    for (AkkaEvent<?, ?> event : events) {
      handleEvent(event);
    }
    engine.runOnce();
    int flushed = 0;
    for (BridgeSubscription subscription : subscriptions.values()) {
      flushed += subscription.queueUpdates(engine, previousContext);
    }
    feedDemand();
    LOG.info(
        "Run inputs={} outputs={} context={}", events.size(), flushed, engine.getLatestContext());
  }

  private <K, S> void handleEvent(AkkaEvent<K, S> event) {
    engine.setSourceState(event.getEntityKey(), event.getKey(), event.getState());
  }
}

package org.aa.olympus.akka;

import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.UpdateContext;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AkkaBridge {
  private static final Logger LOG = LoggerFactory.getLogger(AkkaBridge.class);

  private static class Event<K, S> {
    private final EntityKey<K, S> entityKey;
    private final K key;
    private final S state;

    private Event(EntityKey<K, S> entityKey, K key, S state) {
      this.entityKey = entityKey;
      this.key = key;
      this.state = state;
    }
  }

  private final Engine engine;
  private final BatchQueue<Event> queue;
  private final Map<EntityKey, BridgeSubscription> subscriptions;

  public AkkaBridge(Engine engine) {
    this.engine = engine;
    this.queue = new BatchQueue<>();
    this.subscriptions = new HashMap<>();
  }

  // TODO: add back pressure policy (queue size, already running)
  // TODO: add error handling
  public <K, V> Sink<V, NotUsed> fromAkka(EntityKey<K, V> entityKey, Function<V, K> keyExtractor) {
    return Sink.fromSubscriber(new BridgeSubscriber<>(entityKey, keyExtractor));
  }

  public <K, S> Source<S, NotUsed> toAkka(EntityKey<K, S> entityKey) {
    BridgeSubscription<K, S> subscription = new BridgeSubscription<>(this, entityKey);
    this.subscriptions.put(entityKey, subscription);

    return Source.fromPublisher(
        new Publisher<S>() {
          @Override
          public void subscribe(Subscriber<? super S> s) {
            subscription.setSubscriber(s);
          }
        });
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

  boolean runIfNeeded() {
    if (!this.queue.isEmpty() && allReady()) {
      run();
      return true;
    } else {
      return false;
    }
  }

  private void run() {
    UpdateContext previousContext = engine.getLatestContext();
    List<Event> events = queue.flush();
    for (Event<?, ?> event : events) {
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

  private <K, S> void handleEvent(Event<K, S> event) {
    engine.setSourceState(event.entityKey, event.key, event.state);
  }

  public class BridgeSubscriber<K, S> implements Subscriber<S> {

    private final EntityKey<K, S> entityKey;
    private final Function<S, K> keyExtractor;

    public BridgeSubscriber(EntityKey<K, S> entityKey, Function<S, K> keyExtractor) {
      this.entityKey = entityKey;
      this.keyExtractor = keyExtractor;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(S value) {
      Event<K, S> event = new Event<>(entityKey, keyExtractor.apply(value), value);
      queue.push(event);
      runIfNeeded();
    }

    @Override
    public void onError(Throwable throwable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void onComplete() {
      throw new UnsupportedOperationException();
    }
  }
}

package org.aa.olympus.akka;

import java.util.function.Function;
import org.aa.olympus.api.EntityKey;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class BridgeSubscriber<V, K, S> implements Subscriber<V> {

  private AkkaBridge akkaBridge;
  private final EntityKey<K, S> entityKey;
  private final Function<V, K> keyExtractor;
  private final Function<V, S> valueExtractor;

  public BridgeSubscriber(
      AkkaBridge akkaBridge,
      EntityKey<K, S> entityKey,
      Function<V, K> keyExtractor,
      Function<V, S> valueExtractor) {
    this.akkaBridge = akkaBridge;
    this.entityKey = entityKey;
    this.keyExtractor = keyExtractor;
    this.valueExtractor = valueExtractor;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    subscription.request(Long.MAX_VALUE);
  }

  @Override
  public void onNext(V value) {
    AkkaEvent<K, S> event =
        new AkkaEvent<>(entityKey, keyExtractor.apply(value), valueExtractor.apply(value));
    akkaBridge.queue(event);
    akkaBridge.runIfNeeded();
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

package org.aa.olympus.akka;

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import org.aa.olympus.api.ElementView;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.UpdateContext;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public final class BridgeSubscription<K, S, V> implements Subscription {

  private final AkkaBridge akkaBridge;
  private final EntityKey<K, S> entityKey;
  private final BiFunction<K, S, V> assembler;
  private final Queue<V> values;

  private boolean isFlushing = false;
  private Subscriber<? super V> subscriber = null;
  private long demand = 0;

  BridgeSubscription(
      AkkaBridge akkaBridge, EntityKey<K, S> entityKey, BiFunction<K, S, V> assembler) {
    this.akkaBridge = akkaBridge;
    this.entityKey = entityKey;
    this.assembler = assembler;
    this.values = Queues.newConcurrentLinkedQueue();
  }

  @Override
  public void request(long n) {
    this.demand += n;
    if (!isFlushing) {
      feedDemand();
      akkaBridge.runIfNeeded();
    }
  }

  @Override
  public void cancel() {
    throw new UnsupportedOperationException();
  }

  void feedDemand() {
    if (!this.isFlushing) {
      this.isFlushing = true;
      while (!values.isEmpty() && demand > 0) {
        subscriber.onNext(values.poll());
        demand -= 1;
      }
      this.isFlushing = false;
    }
  }

  boolean ready() {
    return this.values.isEmpty() && this.demand > 0;
  }

  void setSubscriber(Subscriber<? super V> subscriber) {
    Preconditions.checkArgument(this.subscriber == null);
    this.subscriber = subscriber;
    this.subscriber.onSubscribe(this);
  }

  int queueUpdates(Engine engine, UpdateContext previous) {
    Preconditions.checkArgument(values.isEmpty());
    Preconditions.checkArgument(demand > 0);
    List<ElementView<K, S>> values = engine.getUpdated(entityKey, previous);
    values.forEach(p -> this.values.add(assembler.apply(p.getKey(), p.getState())));
    return values.size();
  }
}

package org.aa.olympus.akka;

import org.aa.olympus.api.EventChannel;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class BridgeSubscriber<E> implements Subscriber<E> {

  private AkkaBridge akkaBridge;
  private final EventChannel<E> eventChannel;

  public BridgeSubscriber(AkkaBridge akkaBridge, EventChannel<E> eventChannel) {
    this.akkaBridge = akkaBridge;
    this.eventChannel = eventChannel;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    subscription.request(Long.MAX_VALUE);
  }

  @Override
  public void onNext(E value) {

    AkkaEvent<E> event = new AkkaEvent<>(eventChannel, value);
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

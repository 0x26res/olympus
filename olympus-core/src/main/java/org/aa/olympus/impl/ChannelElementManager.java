package org.aa.olympus.impl;

import com.google.common.base.Preconditions;
import java.util.function.Consumer;
import java.util.function.Function;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Event;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;

public final class ChannelElementManager<E, K, S> implements ElementManager<K, S> {
  private final EventChannel<E> eventChannel;
  private final Function<E, K> keyExtractor;
  private final Function<E, S> stateExtractor;

  public ChannelElementManager(
      EventChannel<E> eventChannel, Function<E, K> keyExtractor, Function<E, S> stateExtractor) {
    this.eventChannel = eventChannel;
    this.keyExtractor = keyExtractor;
    this.stateExtractor = stateExtractor;
  }

  @Override
  public ElementUpdater<S> create(K elementKey, UpdateContext updateContext, Toolbox toolbox) {
    return new ChannelElementUpdater<>(eventChannel, stateExtractor);
  }

  @Override
  public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<K> toNotify) {}

  @Override
  public <E2> void onEvent(Event<E2> event, Consumer<K> toNotify) {
    E castedEvent = eventChannel.castEvent(event);
    K key = keyExtractor.apply(castedEvent);
    toNotify.accept(key);
  }

  private static final class ChannelElementUpdater<E, K, S> implements ElementUpdater<S> {

    private final EventChannel<E> eventChannel;
    private final Function<E, S> extractor;

    private ChannelElementUpdater(EventChannel<E> eventChannel, Function<E, S> extractor) {
      this.eventChannel = eventChannel;
      this.extractor = extractor;
    }

    @Override
    public UpdateResult<S> update(S previous, UpdateContext updateContext, Toolbox toolbox) {
      E latest = null;
      for (Event event : toolbox.getEvents()) {
        Preconditions.checkArgument(event.getChannel().equals(eventChannel));
        latest = eventChannel.castEvent(event);
      }
      Preconditions.checkNotNull(latest, "Latest shouldn't be null");
      return UpdateResult.update(extractor.apply(latest));
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      return false;
    }
  }
}

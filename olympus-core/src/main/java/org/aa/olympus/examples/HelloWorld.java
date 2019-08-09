package org.aa.olympus.examples;

import com.google.common.collect.ImmutableSet;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Event;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.impl.UnsupportedEntityException;

public class HelloWorld {

  public static class Message {
    public final String key;
    public final String value;

    public static Message of(String key, String value) {
      return new Message(key, value);
    }

    public Message(String key, String value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public String getValue() {
      return value;
    }
  }

  public static EventChannel<Message> RIGHT_CHANNEL = Olympus.channel("RIGHT", Message.class);
  public static EventChannel<Message> LEFT_CHANNEL = Olympus.channel("LEFT", Message.class);

  public static EntityKey<String, String> RIGHT = Olympus.key("RIGHT", String.class, String.class);
  public static EntityKey<String, String> LEFT = Olympus.key("LEFT", String.class, String.class);
  public static EntityKey<String, String> BOTH = Olympus.key("BOTH", String.class, String.class);

  public static class Concatenator implements ElementUpdater<String> {

    private ElementHandle<String, String> left;
    private ElementHandle<String, String> right;

    @Override
    public UpdateResult<String> update(
        String previous, UpdateContext updateContext, Toolbox toolbox) {
      if (left != null && right != null) {
        return UpdateResult.maybe(left.getState() + ' ' + right.getState());
      } else {
        return UpdateResult.notReady();
      }
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      if (handle.getEntityKey().equals(RIGHT)) {
        right = RIGHT.castHandle(handle);
        return true;
      } else if (handle.getEntityKey().equals(LEFT)) {
        left = LEFT.castHandle(handle);
        return true;
      } else {
        throw new UnsupportedEntityException(handle.getEntityKey());
      }
    }
  }

  public static class ConcatenatorElementManager implements ElementManager<String, String> {

    @Override
    public ElementUpdater<String> create(String key, UpdateContext updateContext, Toolbox toolbox) {
      return new Concatenator();
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<String> toNotify) {
      toNotify.accept((String) key);
    }

    @Override
    public <E> void onEvent(Event<E> event, Consumer<String> toNotify) {
      toNotify.accept(((Message) event.getValue()).key);
    }
  }

  public static Engine createEngine() {
    return Olympus.builder()
        .registerEventChannel(LEFT_CHANNEL)
        .registerEventChannel(RIGHT_CHANNEL)
        .eventToEntity(LEFT_CHANNEL, LEFT, Message::getKey, Message::getValue)
        .eventToEntity(RIGHT_CHANNEL, RIGHT, Message::getKey, Message::getValue)
        .registerInnerEntity(
            HelloWorld.BOTH,
            new ConcatenatorElementManager(),
            ImmutableSet.of(HelloWorld.LEFT, HelloWorld.RIGHT))
        .build();
  }
}

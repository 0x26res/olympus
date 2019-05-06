package org.aa.olympus.example;

import com.google.common.collect.ImmutableSet;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.impl.UnsupportedEntityException;

public class HelloWorld {

  public static EntityKey<String, String> RIGHT =
      Olympus.key("RIGHT", String.class, String.class);
  public static EntityKey<String, String> LEFT =
      Olympus.key("LEFT", String.class, String.class);
  public static EntityKey<String, String> BOTH =
      Olympus.key("BOTH", String.class, String.class);

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
  }

  public static Engine createEngine() {
    return Olympus.builder()
        .registerSource(HelloWorld.LEFT)
        .registerSource(HelloWorld.RIGHT)
        .registerEntity(
            HelloWorld.BOTH,
            new ConcatenatorElementManager(),
            ImmutableSet.of(HelloWorld.LEFT, HelloWorld.RIGHT))
        .build();
  }

  public static Engine createEngineUsingSimpleAPI() {
    return Olympus.builder()
        .registerSource(HelloWorld.LEFT)
        .registerSource(HelloWorld.RIGHT)
        .registerSimpleEntity(
            HelloWorld.BOTH,
            p -> new Concatenator(),
            ImmutableSet.of(HelloWorld.LEFT, HelloWorld.RIGHT))
        .build();
  }
}

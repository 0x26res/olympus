package org.aa.olympus.example;

import java.util.function.Consumer;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.impl.UnsupportedEntityException;

public class HelloWorld {

  public static EntityKey<String, String> HELLO =
      Olympus.createKey("hello", String.class, String.class);
  public static EntityKey<String, String> WORLD =
      Olympus.createKey("world", String.class, String.class);
  public static EntityKey<String, String> HELLO_WORLD =
      Olympus.createKey("hello_world", String.class, String.class);

  public static class Concatenator implements ElementUpdater<String> {

    private ElementHandle<String, String> hello;
    private ElementHandle<String, String> world;

    @Override
    public UpdateResult<String> update(
        String previous, UpdateContext updateContext, Toolbox toolbox) {
      if (hello != null && world != null) {
        return UpdateResult.maybe(previous, hello.getState() + ' ' + world.getState());
      } else {
        return UpdateResult.notReady();
      }
    }

    @Override
    public <K2, S2> void onNewElement(ElementHandle<K2, S2> handle) {
      if (handle.getEntityKey().equals(HELLO)) {
        hello = HELLO.castHandle(handle);
      } else if (handle.getEntityKey().equals(WORLD)) {
        world = WORLD.castHandle(handle);
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
}

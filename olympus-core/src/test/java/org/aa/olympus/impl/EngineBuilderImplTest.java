package org.aa.olympus.impl;

import static org.hamcrest.core.StringStartsWith.startsWith;

import com.google.common.collect.ImmutableSet;
import java.time.LocalDateTime;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.examples.HelloWorld;
import org.aa.olympus.examples.HelloWorld.ConcatenatorElementManager;
import org.aa.olympus.examples.HelloWorld.Message;
import org.aa.olympus.utils.OlympusAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EngineBuilderImplTest {

  @Rule public ExpectedException expectedException = ExpectedException.none();

  private Engine engine;

  private void sendMessage(EventChannel<Message> channel, String key, String value) {
    engine.injectEvent(channel, Message.of(key, value));
  }

  @Test
  public void helloWorldTest() {

    engine = HelloWorld.createEngine();

    sendMessage(HelloWorld.LEFT_CHANNEL, "UC", "LEFT");
    sendMessage(HelloWorld.RIGHT_CHANNEL, "UC", "RIGHT");
    engine.runOnce(LocalDateTime.now());

    sendMessage(HelloWorld.LEFT_CHANNEL, "LC", "hello");
    sendMessage(HelloWorld.RIGHT_CHANNEL, "LC", "world");
    engine.runOnce(LocalDateTime.now());

    sendMessage(HelloWorld.LEFT_CHANNEL, "LC", "hello");
    sendMessage(HelloWorld.RIGHT_CHANNEL, "LC", "world");
    engine.runOnce(LocalDateTime.now());

    sendMessage(HelloWorld.LEFT_CHANNEL, "CC", "Hello");
    // Only partial
    engine.runOnce(LocalDateTime.now());

    sendMessage(HelloWorld.RIGHT_CHANNEL, "CC", "World");
    engine.runOnce(LocalDateTime.now());
  }

  @Test
  public void helloWorldEventTest() {

    engine = HelloWorld.createEngine();

    sendMessage(HelloWorld.LEFT_CHANNEL, "UC", "LEFT");
    engine.runOnce();

    OlympusAssert.assertElement(
        engine.getElement(HelloWorld.BOTH, "UC"), ElementStatus.NOT_READY, null, 1);

    sendMessage(HelloWorld.RIGHT_CHANNEL, "UC", "RIGHT");
    engine.runOnce();

    OlympusAssert.assertElement(
        engine.getElement(HelloWorld.BOTH, "UC"), ElementStatus.OK, "LEFT RIGHT", 2);
  }

  @Test
  public void missingDependenciesTest() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(startsWith("Missing dependencies for BOTH"));

    Olympus.builder()
        .registerInnerEntity(
            HelloWorld.BOTH, new ConcatenatorElementManager(), ImmutableSet.of(HelloWorld.LEFT));
  }

  @Test
  public void noDependenciesTest() {
    expectedException.expect(IllegalArgumentException.class);
    Olympus.builder()
        .registerInnerEntity(HelloWorld.BOTH, new ConcatenatorElementManager(), ImmutableSet.of());
  }
}

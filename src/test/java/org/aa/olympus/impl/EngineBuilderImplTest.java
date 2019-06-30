package org.aa.olympus.impl;

import static org.hamcrest.core.StringStartsWith.startsWith;

import com.google.common.collect.ImmutableSet;
import java.time.LocalDateTime;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.example.HelloWorld;
import org.aa.olympus.example.HelloWorld.ConcatenatorElementManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EngineBuilderImplTest {

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void helloWorldTest() {

    Engine engine = HelloWorld.createEngineUsingSimpleAPI();

    engine.setSourceState(HelloWorld.LEFT, "UC", "LEFT");
    engine.setSourceState(HelloWorld.RIGHT, "UC", "RIGHT");
    engine.runOnce(LocalDateTime.now());
    System.out.println(engine.toString());

    engine.setSourceState(HelloWorld.LEFT, "LC", "hello");
    engine.setSourceState(HelloWorld.RIGHT, "LC", "world");
    engine.runOnce(LocalDateTime.now());
    System.out.println(engine.toString());

    engine.setSourceState(HelloWorld.LEFT, "LC", "hello");
    engine.setSourceState(HelloWorld.RIGHT, "LC", "world");
    engine.runOnce(LocalDateTime.now());
    System.out.println(engine.toString());

    engine.setSourceState(HelloWorld.LEFT, "CC", "Hello");
    // Only partial
    engine.runOnce(LocalDateTime.now());
    System.out.println(engine.toString());

    engine.setSourceState(HelloWorld.RIGHT, "CC", "World");
    engine.runOnce(LocalDateTime.now());
    System.out.println(engine.toString());
  }

  @Test
  public void missingDependenciesTest() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(startsWith("Missing dependencies for BOTH"));

    Olympus.builder()
        .registerSource(HelloWorld.LEFT)
        .registerInnerEntity(
            HelloWorld.BOTH,
            new ConcatenatorElementManager(),
            ImmutableSet.of(HelloWorld.LEFT, HelloWorld.RIGHT));
  }

  @Test
  public void noDependenciesTest() {
    expectedException.expect(IllegalArgumentException.class);
    Olympus.builder()
        .registerInnerEntity(HelloWorld.BOTH, new ConcatenatorElementManager(), ImmutableSet.of());
  }
}

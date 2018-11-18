package org.aa.olympus.impl;

import static org.hamcrest.core.StringStartsWith.startsWith;

import com.google.common.collect.ImmutableSet;
import java.util.Date;
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
    Engine engine =
        Olympus.builder()
            .registerSource(HelloWorld.HELLO)
            .registerSource(HelloWorld.WORLD)
            .registerEntity(
                HelloWorld.HELLO_WORLD,
                new ConcatenatorElementManager(),
                ImmutableSet.of(HelloWorld.HELLO, HelloWorld.WORLD))
            .build();

    engine.setSourceState(HelloWorld.HELLO, "UC", "HELLO");
    engine.setSourceState(HelloWorld.WORLD, "UC", "WORLD");
    engine.runOnce(new Date());
    System.out.println(engine.toString());

    engine.setSourceState(HelloWorld.HELLO, "LC", "hello");
    engine.setSourceState(HelloWorld.WORLD, "LC", "world");
    engine.runOnce(new Date());
    System.out.println(engine.toString());

    engine.setSourceState(HelloWorld.HELLO, "LC", "hello");
    engine.setSourceState(HelloWorld.WORLD, "LC", "world");
    engine.runOnce(new Date());
    System.out.println(engine.toString());

    engine.setSourceState(HelloWorld.HELLO, "CC", "Hello");
    // Only partial
    engine.runOnce(new Date());
    System.out.println(engine.toString());

    engine.setSourceState(HelloWorld.WORLD, "CC", "World");
    engine.runOnce(new Date());
    System.out.println(engine.toString());
  }

  @Test
  public void missingDependenciesTest() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(startsWith("Missing dependencies for hello_world"));

    Olympus.builder()
        .registerSource(HelloWorld.HELLO)
        .registerEntity(
            HelloWorld.HELLO_WORLD,
            new ConcatenatorElementManager(),
            ImmutableSet.of(HelloWorld.HELLO, HelloWorld.WORLD));
  }

  @Test
  public void noDependenciesTest() {
    expectedException.expect(IllegalArgumentException.class);
    Olympus.builder()
        .registerEntity(
            HelloWorld.HELLO_WORLD, new ConcatenatorElementManager(), ImmutableSet.of());
  }
}

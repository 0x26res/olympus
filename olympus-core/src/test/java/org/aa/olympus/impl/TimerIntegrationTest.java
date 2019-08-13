package org.aa.olympus.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.aa.olympus.api.ElementHandle;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementStatus;
import org.aa.olympus.api.ElementTimer;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.Engine;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.Event;
import org.aa.olympus.api.EventChannel;
import org.aa.olympus.api.Olympus;
import org.aa.olympus.api.TimerState;
import org.aa.olympus.api.Toolbox;
import org.aa.olympus.api.UpdateContext;
import org.aa.olympus.api.UpdateResult;
import org.aa.olympus.utils.OlympusAssert;
import org.junit.Before;
import org.junit.Test;

public class TimerIntegrationTest {

  private static final EventChannel<CreateTimerEvent> CREATE_CHANNEL =
      Olympus.channel("CREATE_CHANNEL", CreateTimerEvent.class);
  private static final EventChannel<CancelTimerEvent> CANCEL_CHANNEL =
      Olympus.channel("CANCEL_CHANNEL", CancelTimerEvent.class);
  private static final EntityKey<String, TimersState> TIMERS_STATE =
      Olympus.key("TIMERS_STATE", String.class, TimersState.class);

  public static final class CreateTimerEvent {
    public final String key;
    public final String timerId;
    public final LocalDateTime time;

    public CreateTimerEvent(String key, String timerId, LocalDateTime time) {
      this.key = key;
      this.timerId = timerId;
      this.time = time;
    }
  }

  public static final class CancelTimerEvent {
    public final String key;
    public final String timerId;

    public CancelTimerEvent(String key, String timerId) {
      this.key = key;
      this.timerId = timerId;
    }
  }

  public static final class TimersState {

    public final Map<String, TimerState> states;
    public final UpdateContext updateContext;

    public TimersState(Map<String, TimerState> states, UpdateContext updateContext) {
      this.states = ImmutableMap.copyOf(states);
      this.updateContext = updateContext;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (o == null || getClass() != o.getClass()) {
        return false;
      } else {
        TimersState that = (TimersState) o;
        return Objects.equal(states, that.states)
            && Objects.equal(updateContext, that.updateContext);
      }
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(states, updateContext);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("states", states)
          .add("updateContext", updateContext)
          .toString();
    }
  }

  public static final class TimerElementManager implements ElementManager<String, TimersState> {

    @Override
    public ElementUpdater<TimersState> create(
        String elementKey, UpdateContext updateContext, Toolbox toolbox) {
      return new TimerElementUpdater();
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<String> toNotify) {}

    @Override
    public <E> void onEvent(Event<E> event, Consumer<String> toNotify) {
      if (event.getChannel().equals(CREATE_CHANNEL)) {
        toNotify.accept(((CreateTimerEvent) event.getValue()).key);
      } else if (event.getChannel().equals(CANCEL_CHANNEL)) {
        toNotify.accept(((CancelTimerEvent) event.getValue()).key);
      } else {
        throw new IllegalArgumentException(event.getChannel().toString());
      }
    }
  }

  public static final class TimerElementUpdater implements ElementUpdater<TimersState> {

    public Map<String, ElementTimer> timers = new LinkedHashMap<>();

    @Override
    public UpdateResult<TimersState> update(
        TimersState previous, UpdateContext updateContext, Toolbox toolbox) {

      filter(toolbox.getEvents(), CREATE_CHANNEL)
          .forEach(
              e -> {
                timers.put(e.timerId, toolbox.setTimer(e.time));
              });
      filter(toolbox.getEvents(), CANCEL_CHANNEL)
          .forEach(
              e -> {
                timers.get(e.timerId).cancel();
              });

      return UpdateResult.update(
          new TimersState(
              timers.entrySet().stream()
                  .collect(Collectors.toMap(Entry::getKey, p -> p.getValue().getState())),
              updateContext));
    }

    @Override
    public <K2, S2> boolean onNewElement(ElementHandle<K2, S2> handle) {
      return false;
    }
  }

  static <E> Stream<E> filter(List<Event> events, EventChannel<E> channel) {
    return events.stream().filter(e -> channel.equals(e.getChannel())).map(channel::castEvent);
  }

  private Engine engine;
  private LocalDateTime now = LocalDateTime.of(2019, 8, 13, 12, 0);

  @Before
  public void setUp() {
    engine =
        Olympus.builder()
            .registerEventChannel(CREATE_CHANNEL)
            .registerEventChannel(CANCEL_CHANNEL)
            .registerInnerEntity(
                TIMERS_STATE,
                new TimerElementManager(),
                ImmutableSet.of(),
                ImmutableSet.of(CREATE_CHANNEL, CANCEL_CHANNEL))
            .build();
  }

  @Test
  public void testOneTimer() {

    engine.runOnce(now);
    engine.injectEvent(
        CREATE_CHANNEL, new CreateTimerEvent("UNIT1", "TIMER1", now.plusSeconds(10)));

    engine.runOnce(now.plusSeconds(1));

    OlympusAssert.assertElement(
        engine.getElement(TIMERS_STATE, "UNIT1"),
        ElementStatus.OK,
        new TimersState(ImmutableMap.of("TIMER1", TimerState.READY), engine.getLatestContext()),
        2);

    engine.runOnce(now.plusSeconds(10));

    OlympusAssert.assertElement(
        engine.getElement(TIMERS_STATE, "UNIT1"),
        ElementStatus.OK,
        new TimersState(ImmutableMap.of("TIMER1", TimerState.TRIGGERED), engine.getLatestContext()),
        3);
  }

  @Test
  public void testCancelTimer() {

    engine.runOnce(now);
    engine.injectEvent(
        CREATE_CHANNEL, new CreateTimerEvent("UNIT1", "TIMER1", now.plusSeconds(10)));

    engine.runOnce(now.plusSeconds(1));

    OlympusAssert.assertElement(
        engine.getElement(TIMERS_STATE, "UNIT1"),
        ElementStatus.OK,
        new TimersState(ImmutableMap.of("TIMER1", TimerState.READY), engine.getLatestContext()),
        2);

    engine.injectEvent(CANCEL_CHANNEL, new CancelTimerEvent("UNIT1", "TIMER1"));
    engine.runOnce(now.plusSeconds(2));
    OlympusAssert.assertElement(
        engine.getElement(TIMERS_STATE, "UNIT1"),
        ElementStatus.OK,
        new TimersState(ImmutableMap.of("TIMER1", TimerState.CANCELLED), engine.getLatestContext()),
        3);

    UpdateContext previous = engine.getLatestContext();
    engine.runOnce(now.plusSeconds(10));

    OlympusAssert.assertElement(
        engine.getElement(TIMERS_STATE, "UNIT1"),
        ElementStatus.OK,
        new TimersState(ImmutableMap.of("TIMER1", TimerState.CANCELLED), previous),
        3);
  }

  @Test
  public void testTwoTimers() {

    engine.runOnce(now);
    engine.injectEvent(
        CREATE_CHANNEL, new CreateTimerEvent("UNIT1", "TIMER1", now.plusSeconds(10)));
    engine.injectEvent(
        CREATE_CHANNEL, new CreateTimerEvent("UNIT1", "TIMER2", now.plusSeconds(11)));

    engine.runOnce(now.plusSeconds(1));

    OlympusAssert.assertElement(
        engine.getElement(TIMERS_STATE, "UNIT1"),
        ElementStatus.OK,
        new TimersState(
            ImmutableMap.of("TIMER1", TimerState.READY, "TIMER2", TimerState.READY),
            engine.getLatestContext()),
        2);

    engine.runOnce(now.plusSeconds(10));

    OlympusAssert.assertElement(
        engine.getElement(TIMERS_STATE, "UNIT1"),
        ElementStatus.OK,
        new TimersState(
            ImmutableMap.of("TIMER1", TimerState.TRIGGERED, "TIMER2", TimerState.READY),
            engine.getLatestContext()),
        3);

    engine.runOnce(now.plusSeconds(15));

    OlympusAssert.assertElement(
        engine.getElement(TIMERS_STATE, "UNIT1"),
        ElementStatus.OK,
        new TimersState(
            ImmutableMap.of("TIMER1", TimerState.TRIGGERED, "TIMER2", TimerState.TRIGGERED),
            engine.getLatestContext()),
        4);
  }
}

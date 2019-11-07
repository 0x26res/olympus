package org.aa.olympus.impl;

import java.util.Map;
import java.util.function.Consumer;
import org.aa.olympus.api.ElementManager;
import org.aa.olympus.api.ElementUpdater;
import org.aa.olympus.api.EntityKey;
import org.aa.olympus.api.ELementToolbox;
import org.aa.olympus.api.UpdateContext;

final class SourceManager<K, S> {

  private final EntityManager<K, S> entityManager;

  private final Map<K, SourceUnit<K, S>> units;

  SourceManager(Map<K, SourceUnit<K, S>> units, EntityManager<K, S> entityManager) {
    this.units = units;
    this.entityManager = entityManager;
  }

  void setState(K key, S state) {
    SourceUnit<K, S> unit = units.get(key);
    if (unit == null) {
      ElementUnit<K, S> elementUnit = entityManager.get(key, true);
      unit = units.get(key);
      unit.setElementUnit(elementUnit);
      elementUnit.stain();
    } else if (!unit.ready()) {
      ElementUnit<K, S> elementUnit = entityManager.get(key, false);
      unit.setElementUnit(elementUnit);
      elementUnit.stain();
    }
    unit.setState(state);
  }

  static final class ElementManagerAdapter<K, S> implements ElementManager<K, S> {
    private final Map<K, SourceUnit<K, S>> units;

    ElementManagerAdapter(Map<K, SourceUnit<K, S>> units) {
      this.units = units;
    }

    @Override
    public ElementUpdater<S> create(K key, UpdateContext updateContext, ELementToolbox ELementToolbox) {
      SourceUnit<K, S> unit = new SourceUnit<>();
      units.put(key, unit);
      return unit;
    }

    @Override
    public <K2> void onNewKey(EntityKey<K2, ?> entityKey, K2 key, Consumer<K> toNotify) {
      throw new IllegalArgumentException("Should not be notified of new elements");
    }
  }
}

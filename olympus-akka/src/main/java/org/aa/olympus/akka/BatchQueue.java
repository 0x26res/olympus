package org.aa.olympus.akka;

import java.util.ArrayList;
import java.util.List;

public class BatchQueue<E> {

  private final ArrayList<E> values = new ArrayList<>();

  synchronized boolean isEmpty() {
    return values.isEmpty();
  }

  synchronized boolean push(E element) {
    boolean wasEmpty = values.isEmpty();
    values.add(element);
    return wasEmpty;
  }

  synchronized void flush(List<E> destination) {
    destination.addAll(values);
    values.clear();
  }

  synchronized List<E> flush() {
    ArrayList<E> results = new ArrayList<>(values.size());
    flush(results);
    return results;
  }
}

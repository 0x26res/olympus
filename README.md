### Olympus

<p align="center">
    <a href="https://github.com/arthurandres/olympusgraphs/contributors" alt="Contributors">
        <img src="https://img.shields.io/github/contributors/arthurandres/olympus.svg" /></a>
    <a href="https://circleci.com/gh/arthurandres/olympus/tree/master">
        <img src="https://img.shields.io/github/languages/top/arthurandres/olympus.svg" alt="build status"></a>
</p>
    

# TL;DR

Olympus is a real time event processing engine, with a twist:
* Emphasis on stateful elements
* Fine granularity of dependencies
* Efficient event compression algorithm
* One callback to rule them all
* More declarative code, less imperative bug

It is inspired by some design pattern seen in video game, such as the [game loop](http://gameprogrammingpatterns.com/game-loop.html) and the [update method](http://gameprogrammingpatterns.com/update-method.html).

# Overview

The olympus engine is based on a [DAG](https://en.wikipedia.org/wiki/Directed_acyclic_graph) of Elements. 
Each `element` can subscribe to others in order to receive updates notifications.
When an `element` updates, its subscribers will be updated accordingly. 
Unlike stream processing engines, elements don't publish event, they update their states. 
Instead of receiving events from other elements, they can read the state of other elements.

Updates are processed in micro-batches instead of one by one. 
Updates are propagated through the engine based on the topological order of elements.
Only the elements that need to be updated are notified, and they are notified only once. 

It works well for systems that don't have a lot of elements but receive a lot of updates on each individual elements. 
It's also recommended when you have complicated dependencies between your different elements.


# Lexicon

### Element

The base component of your engine. Think of it like a gear within an engine. 
It has a key, a state, and a mean to update its state.
Each `element` depend on (or subscribe to) other elements.
They get notified when these other elements update.
Upon update they can read the state of the elements they are subscribed to and publish their updated state accordingly

### Entity

In Object Oriented terms, an `element` would be the Object and an `entity` the Class. 
Each `element` belongs to an `entity`. 
Each `entity` defines how elements are created and which element gets notified or created when new elements are added to the engine.
It also has to declare which other entities they depend on. 

### Event

An external update coming inside the engine. 

### Event Channel

A channel on which events are published. Each Entity decides which channels they listen to. 
Upon receiving an event, entities dispatch it to relevant Elements. 
Elements can then iterate through events to update their states or (un)subscribe to other elements 

# Example

![Stok Index Diagram](/images/stock_index.png)

In this example we receive stock prices and index composition in real time. 
As stock prices and index compositions change, we update the indices values.

The S&P500 valuation calculator subscribes to:
* prices of the stocks in its composition: Ford, Google etc 
* its composition  

When a price updates, the valuation calculator for S&P500 gets notified and recalculate the index value accordingly.
When its composition updates, it can subscribe/unsubscribe to the relevant prices. Subscriptions can be updated dynamically at run time.

Some key advantages of the olympus engine is that:
* Emphasis on stateful elements: index calculator rely on the state of each price, rather than receiving price change events when a price change. This mean they don't have to maintain a map with with the current value of each stock.
* Fine granularity of dependencies: elements only subscribe to what they are interested in. If a price that's not in the index updates, the index calculator won't be notified.
* Efficient event compression algorithm: if several prices update at the same time, relevant indices only get notified once
* One callback to rule them all: if a price updates or a composition updates, the same callback get called.
* More declarative code, less imperative bug: Entities keys and values are declared once, and their state is managed by the engine. There's no need to store the state of every prices in each index calculator.  
* Propagate events, creation and updates entity by entity and pass the value upon creation?
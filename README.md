### Olympus

# TL;DR

Olympus is a real time event processing engine, with a twist:
* Emphasis on stateful elements
* Efficient event compression algorithm
* Fine granularity of dependencies
* One callback to rule them all
* More declarative code, less imperative bug

It is inspired by some design patten seen in video game, such as the [game loop](http://gameprogrammingpatterns.com/game-loop.html) and the [update method](http://gameprogrammingpatterns.com/update-method.html).

# Overview

The olympus engine is based on a DAG of Elements. 
Each element can subscribe to others in order to receive updates notifications.
When an element updates, its subscribers will be updated accordingly. 
Unlike stream processing engines, elements don't publish event, they update their states. 
Instead of receiving events from other elements, they can read the state of other elements.

Updates are processed in micro-batches instead of one by one. 
Updates are propagated through the engine based on the topological order of elements.
Only the elements that need to be updated are notified, and they are notified once and once only. 

It works well for systems that don't have a lot of elements but receive a lot of updates on each individual elements. 
It's also recommended when you have complicated dependencies between your different elements.


# Lexicon

### Element

The base element of your engine. Think of it like a gear within an engine. 
It has a key, a state, and a mean to update its state.
Elements depend on (or subscribe to) other elements.
They get notified when these other elements update.
Upon update they can read the state of the elements they are subscribed to and publish their updated state accordingly

### Entity

In Object Oriented terms, elements would be the Object and entities the Class. 
Each element belongs to an entity. 
Each entity defines how elements are created and how they subscribe to other elements.
It also has to declare which other entities they depend on. 

### Source

A special entity whose elements can receive external event to modify their state.

# TODO:
* Dynamic subscription example
* add a less restrictive cast to the key
* Add end of life events
* Entity manager could be entities themselves
* Implement the UpdateContext, ToolBox, CreationContext...
* Add tracking information (created at, update at...)
* State machine for elements, add failure, how to propagate
* Fault tolerance
* Add examples and FAQ to doc
* Add Meta entities: store the new & deleted elements
* One cycle for creation, one cycle for updates?
* Add a special case for Result.<Double>update because of nan
* Add a tutorial to this page
* Add rules
  * Everything must go downstream
  * subscriber / broadcaster (not listener) 
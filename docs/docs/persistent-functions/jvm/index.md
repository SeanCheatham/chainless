---
description: Information about Chainless Persistent Functions using the JVM.
slug: /persistent-functions/jvm
---

# JVM Functions
The Java Virtual Machine (JVM) runs applications compiled from languages like Java, Scala, or Kotlin.  These languages can produce runnable `.jar` files.

The overall approach is the same for all JVM-based targets:
- Create a new project (i.e. with Maven or SBT)
- Add your library dependencies
- Write a `main` method which calls the event API and executes the function logic in a loop
- Assemble the project into a self-contained JAR, which is a JAR that includes all of the required dependencies
- Upload the JAR file to Chainless

## What you'll need
- [JDK](https://adoptium.net/installation/) version 17 or above

## Languages
We provide documentation for the following languages, but as long as you can produce a self-contained JAR, it should work in other languages too.
- [Scala](jvm/scala)

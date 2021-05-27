# Access Warden


## Introduction

**Access Warden** is a simple to use yet powerful toolkit designed to allow Java developers to *enforce* access restrictions in their applications. It works by generating special runtime stacktrace and environment inspection code and injecting it in your classes after build.


## The problem

### Simple Example

Let's imagine we're developing a game. Let's say we have a class `Player` with this method:

```java
private void jump() {
    this.posY += 1.0; // (some hyper-realistic physics simulation here)
}
```

Assuming this method is only called from inside of our `Player` class, we can define it `private`. This means that we are protected from all other classes/sources accessing this method and causing the player to jump infinitely... or, *are we*?


### Standard Java modifiers are not so reliable

Actually the `jump` method can still be accessed by *any* other third-party. For example, an *attacker* can use *reflection* for that:

```java
void hack() {
    Player player = ...
            
    Class<?> clazz = Class.forName("our.cool.game.Player");
    Method method = clazz.getDeclaredMethod("jump");
    method.setAccessible(true);
            
    method.invoke(player); // here, we bypass the "private" access restriction
}
```

Additionally, an *attacker* could potentially use some native (C/C++) tricks and possibly even something more to ignore the `private` keyword you've just put so much effort in writing. This is definitely not something most applications should worry about. But some certain kinds of applications actually *do* want to avoid the possibility of such *"attacks"*.

### Setting up a global SecurityManager

One way of getting around this is to use a `SecurityManager`, either by defining one before launching your application (which also has to be enforced somehow) with some pre-defined security policies files, or programmatically, by directly inheriting from `java.lang.SecurityManager` and writing code that will filter all the forbidden calls on your own (which may be incompatible with user's current environment, say, due to another `SecurityManager` being already set). Both options involve comparably much work and have severe drawbacks.


## The solution

**Access Warden** provides a toolkit that helps you deal with this kind of issue *easily, quickly, and comparably reliably*. Here's some brief overview of all *Access Warden** modules:
* **[API](https://github.com/MeGysssTaa/access-warden/wiki/The-API-module)** — Provides low-level access to stacktrace/environment inspection and allows you write some *really specific* checks suitable for your particular application case. However, in most cases you'll be only including this module for *annotations*, on which the other modules are based.
* **[Core](https://github.com/MeGysssTaa/access-warden/wiki/The-Core-module)** — Provides very high-level, control-less access to *JVM bytecode generation* in existing JAR files. Mostly used either as a standalone application or by other modules (such as the Gradle module) for transformation of existing (built) application JAR files. However, you can also use it to run JAR transformations programmatically, at a high level (with barely writing any code).
* **[Gradle](https://github.com/MeGysssTaa/access-warden/wiki/The-Gradle-module)** — Removes the need in running the Core module as a standalone application after each application build. Instead, all the necessary transformations will be automatically applied after you build your application with Gradle.
* **[Demo](https://github.com/MeGysssTaa/access-warden/wiki/The-Demo-module)** — A basic runnable example that demonstrates the way **Access Warden** can be used, uses Gradle. 



# License

**[Apache License 2.0](https://github.com/MeGysssTaa/access-warden/blob/main/LICENSE)**

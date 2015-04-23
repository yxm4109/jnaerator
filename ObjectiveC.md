

# Introduction #

JNAerator can generate ready-to-use JARs that make any Objective-C framework available from Java.

# Objective-C Classes #

JNAerator gathers the methods found in class definitions and protocols. It will add all the methods defined in other frameworks, provided they are parsed.

  * ObjectiveC classes end up in abstract java classes.
  * ObjectiveC protocols end up in java interfaces

# Examples #

Create a JAR with wrappings for the AppKit and Foundation frameworks :
```
java -Xmx1000m -jar jnaerator.jar -framework Foundation -framework AppKit -jar appkit-foundation.jar
```

Create a JAR with Foundation, CoreFoundation, CoreGraphics and AppKit:
```
java -Xmx1000m -jar ../../bin/jnaerator.jar -framework Foundation -framework AppKit -framework CoreFoundation -framework CoreGraphics
```


Create a JAR with all of the system frameworks :
```
java -Xmx1500m -jar jnaerator.jar `for F in /System/Library/Frameworks/*.framework ; do sed -E 's/^.*\/([^/]+)\.framework$/-framework \1/' ; done` -jar all-frameworks.jar
```


# What is JNAerator ? #

**JNAerator** (pronounce _‘generator’_) is a tool that **parses C / C++ & Objective-C** sources (headers) and **generates the corresponding [BridJ](http://bridj.googlecode.com), [JNA](http://jna.dev.java.net/) and [Rococoa](https://rococoa.dev.java.net/)** Java interfaces.

This makes it easy to call large native libraries from Java.

By default, JNAerator creates ready-to-use compiled JARs with self-extractible native libraries. These JARs also contain the generated sources, which contain full JavaDoc that retains the comments from the original headers.

# How to use `MyLib.dll` with `MyLibHeader.h` header from Java ? #

```
java -jar jnaerator.jar MyLib.dll MyLibHeader.h -mode StandaloneJar
```
will produce ` MyLib.jar `

# How to use `MyLib.dll` without headers from Java ? #

This is experimental and has big limitations, but simple cases should work :
```
java -jar jnaerator.jar MyLib.dll MyLibHeader.h -scanSymbols -mode StandaloneJar
```
will produce ` MyLib.jar `

# How to use the output of `MySolution.sln` from Java ? #

First make sure you compiled your solution in Release for both Win32 and Win64 targets, if needed.
```
java -jar jnaerator.jar MyLibHeader.sln -mode StandaloneJar
```
will produce ` MySolution.jar `

# How to include self-extracting libraries binaries in JAR outputs ? #

This is already done by default, provided that you add the paths to all needed libraries binaries to JNAerator's command line.

# Where are libraries looked for at run-time ? #

If you've opted for library bundling within the JNAerated JAR (see previous question), your libraries are shipped with your JAR and need not be pre-installed on the system. JNAerator will extract them in the directory `.jnaerator/extractedLibraries` under the user's home directory and will use them directly from there.

For a library with name 'foo' (which shared library files are named 'foo.dll', 'libfoo.so', 'libfoo.dylib'...), JNAerator's runtime will try the following PATH values, in order :
  * Java preference 'library.foo' (modifiable with `System.setPreference("library.foo", somePath)`)
  * Environment variable 'FOO\_LIBRARY' (`set FOO_LIBRARY=c:\somewhere`on windows, `export FOO_LIBRARY=...` on Unix)
  * Java preference 'jna.library.path'
  * Java preference 'java.library.path'
  * Default system's shared library path environment variable : `PATH` on Windows, `LD_LIBRARY_PATH` on Linux, and `DYLD_LIBRARY_PATH` on OSX.

For  more information please see [JNA's documentation](https://jna.dev.java.net/#getting_started) and [CommandLineOptionsAndEnvironmentVariables](CommandLineOptionsAndEnvironmentVariables.md).

# How to use MacOS X frameworks from Java ? #

For most commonly used public frameworks, you should simply download Rococoa and use it alone.

If you need frameworks not covered by Rococoa, wish to use private headers and / or frameworks or ObjectiveC libraries for which no headers are available, then you should use JNAerator :

```
java -jar jnaerator.jar -mode StandaloneJar -framework Framework1 -framework Framework2 lib1.dylib lib2.dylib... 
```

# What command-line options and environment variables are available ? #
sta
There are quite a few of them : [CommandLineOptionsAndEnvironmentVariables](CommandLineOptionsAndEnvironmentVariables.md)

# Is there a mailing list where I can ask for some help ? #

For now, [JNA's users mailing list](https://jna.dev.java.net/servlets/SummarizeList?listName=users) is THE place to be.

# JNAerator stalls / hangs forever when fed with large headers #

This appears to be a bug coming from its ANTLR parser, which does not react well to low-memory conditions. The parsing takes ages and seems to never end.

You can solve this by increasing the maximum memory allocated to the JVM :
```
java -Xmx1g -jar jnaerator.jar your-arguments-here
```

If this does not help, please [file a bug](Issues.md) with an execution trace you'll capture from `jconsole`.

# There's no .java output #

Use the `-noComp -noJar` switches (see [CommandLineOptionsAndEnvironmentVariables](CommandLineOptionsAndEnvironmentVariables.md) for more options)

# There's no output at all, or with no useful symbols and functions #

Please read [TroubleShootingJNAeration](TroubleShootingJNAeration.md)

# I have many command line arguments, can I write them in a file ? #

You can create a file with the extension `.jnaerator` that will contain all your command line arguments.

Please read [JNAeratorFiles](JNAeratorFiles.md) for more information.

# Can I JNAerate multiple libraries at the same time ? #

Sure thing, you can chain multiple `-library` switches :
```
-library Mine1 mine1.h libmine1.so -library Mine2 mine2.h libmine2.so -library Mine3 mine3.h libmine3.so
```

Here's a real-world example : [OpenCV4Java's config.jnaerator file](http://code.google.com/p/nativelibs4java/source/browse/trunk/libraries/OpenCV/config.jnaerator).

# My header includes other headers, what should I do ? #

You just need to make sure that [JNAerator's integrated preprocessor](http://www.anarres.org/projects/jcpp/) can resolve these includes.

JNAerator has a list of include paths that can be extended using the standard -I switch (which most if not all compilers accept) : adding "-Ipath" to the command line will add path to the include directories.

For instance, if one of your headers includes `core/structs.h` and you know that this header is located in `/usr/myTestLib/include/core/structs.h`, you need to add the following argument to the command line (or to your `.jnaerator` file) :
```
  -I/usr/myTestLib/include
```

Note that JNAerator **should** have default include paths for the most common system headers.

For more details on JNAerator's command line switches, see [CommandLineOptionsAndEnvironmentVariables](CommandLineOptionsAndEnvironmentVariables.md).

# I found a bug in JNAerator. Where shall I file it ? #

The [Issues](http://code.google.com/p/jnaerator/issues) section is here for that.
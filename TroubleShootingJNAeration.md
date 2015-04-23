

First, make sure you've read all of the [FAQ](JNAeratorFAQ.md) and [SimpleMeaningfulExample](SimpleMeaningfulExample.md).

Parsing complex headers (including system headers) rarely yields successful results on the first attempt, so you might need some tweaking.

# Verbosity #

If (when) things go wrong, you can :
  * Use the `-v` (verbose) switch (see [CommandLineOptionsAndEnvironmentVariables](CommandLineOptionsAndEnvironmentVariables.md) for more options)
  * Look at your `TestLibrary.java` file (if it's generated at all) : search for "skipped" (case-insensitive) or "error" for symbols that failed to be converted
  * Look at `_jnaerator.preprocessed.c` to see which macros failed to expand (and the include files as needed or define symbols in command line with `-DMY_SYMBOL=` or `-DMY_SYMBOL=SomeValue`). This file should not contain `#define`s anymore.
  * Look at `out.errors.txt`, that gives (too) highly verbose parsing errors with line number that corresponds to `_jnaerator.preprocessed.c` : these parsing errors typically occur when a macro wasn't expanded (see above) or when a C typedef is missing.

# Parsing chunks #

In C, the macro preprocessor is responsible for resolving includes and assembling a large input for the C parser.
The problem with that is usually drags in some core system headers with exotic syntaxes, which JNAerator sometimes chokes upon. And JNAerator is lazy: it just gives up at the first parsing error...
A workaround is to use `-parseChunks`, which chunks the preprocessor output into smaller chunks (using the preprocessor `#line` directives), allowing it to resume parsing after errors.

# When the parser gets loose #

If you consider the following code :
```
void MYLIB_EXPORT MYLIB_STDCALL f(void*);
```

JNAerator won't probably be able to parse this, unless you provide it the definition of the MYLIB\_EXPORT and MYLIB\_STDCALL macros.

**Worse, JNAerator might be able to parse the program with silent errors** (e.g. swallowing the function's pointer argument...).

It is always very important to look at the exact compiler command used to compile the native projects and reuse the macro definitions in JNAerator's command line so that the parsing has a chance to complete successfully.

In the above example, you could add the following two arguments to JNAerator's command-line (or [config.jnaerator file](JNAeratorFiles.md)) :
```
-DMYLIB_EXPORT -DMYLIB_STDCALL=__stdcall
```

Likewise, you might want to skip compiler-specific attributes, which sometimes break the grammar :
```
-D__attribute(x)= -D__declspec(x)=
```

# Order (does not) matters #

The order of input header files can be significant, but the list of files is even more.

The main issue is that JNAerator only generates code that relates to the files it's explicitely given as input. The reason for that is that you typically don't want to generate constants, functions and structs for just about every header that happen to be included directly or indirectly by headers explicitely listed as input (think of everything windows.h brings with itself !).

Then, some subtle issues can happen in the preprocessor and / or parser if files aren't included the right order... that's something you currently have to play a bit with to get it right.

# Compilation Errors #

If _JNAerator_ fails to compile the code it generates (when you haven't specified the `-noJar -noComp` [command line switches](CommandLineOptionsAndEnvironmentVariables.md)), this is a bug in JNAerator. [Please report it here](http://code.google.com/p/jnaerator/issues/list) with as much detail as you can.

If _you_ fail to compile JNAerated code, that's another matter :
  * make sure you've added JNAeratorRuntime.jar to your classpath

# A work in progress #

## Regressions ##

JNAerator strives to support native code initially written for GCC, Visual C++ and other compilers in C, C++ and Objective-C, which implies its parser is a hybrid piece of... err... delicate engineering.

While attention is paid to avoiding regressions, it is very likely that you might have more luck with an older or a more recent version of JNAerator : please [experiment a bit](https://oss.sonatype.org/content/groups/public/com/nativelibs4java/jnaerator/), and please [report any regression you find](https://github.com/ochafik/nativelibs4java/issues/new).

## Templates ##

JNAerator handles more C++ constructs every month, but it's still unable to parse all of the STL and Boost libraries correctly (for they use templates to the extreme).

Getting reproductible, isolated bug reports is paramount to fixing JNAerator's parser, so [your help](https://github.com/ochafik/nativelibs4java/issues/new) will make a difference.
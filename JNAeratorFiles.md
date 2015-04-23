

You can create a file with the extension `.jnaerator` that will contain all your command line arguments (see [available command line switches](CommandLineOptionsAndEnvironmentVariables.md)).

Additionally, `.jnaerator` files are associated to [JNAerator Studio](http://jnaerator.sourceforge.net/webstart/JNAerator/JNAeratorStudio.jnlp) and when launched in command line, JNAerator will automatically pick the file `config.jnaerator`, if present.

JNAerator files support :
  * nested file inclusions
  * environment variables substitutions (with a special variable `DIR` set to the `.jnaerator` file's parent directory) :
> `$(SOME_VAR)`
  * line and block comments with the C syntax :
```
/// line comment
/*
  Block comment
*/ 
```

Here's a fancy example of such a file :
```
-library Test
-I$(MY_PROJECT_DIR)/include

-DTEST=1
extraMacros.jnaerator // include some other .jnaerator file

$(MY_PROJECT_DIR)/include/MyProject.h
$(MY_PROJECT_DIR)/include/Test.h
```

And here are real-world examples from various projects:
  * [OpenCL4Java](http://code.google.com/p/nativelibs4java/source/browse/trunk/libraries/OpenCL/OpenCL4Java/src/main/jnaerator/config.jnaerator)
  * [Mono4Java](http://code.google.com/p/nativelibs4java/source/browse/trunk/libraries/Mono/Mono4Java/src/main/jnaerator/config.jnaerator)
  * [OpenCV4Java](http://code.google.com/p/nativelibs4java/source/browse/trunk/libraries/OpenCV/config.jnaerator)
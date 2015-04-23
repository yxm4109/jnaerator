

To build JNAerator, run the following commands:
```
git clone git://github.com/ochafik/nativelibs4java.git
cd nativelibs4java
cd libraries/jnaerator
mvn clean install
```

You'll find `jnaerator-x-shaded.jar` in `nativelibs4java/libraries/jnaerator/jnaerator/target`.

If you also wish to build BridJ, run the following from the `nativelibs4java` folder:
```
cd libraries/BridJ
mvn clean install
../scripts/listRuntime
cd ../jnaerator
mvn clean install
```

JNAerator is a moving target and some of its tests might be broken on some platforms. If that's the case, you can skip tests with `-DskipTests` in your Maven command line.
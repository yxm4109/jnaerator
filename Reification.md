

# A common use case #

Let's assume the following header from imaginary library LibGX (or OpenGX) :
```
typedef struct GXEntity_ GXEntity;

GXEntity* gxCreateEntity(int someParam);
void gxEntitySetValue(GXEntity* entity, int key, int value);
int gxEntityGetValue(GXEntity* entity, int key);
```

With JNAerator up to version 0.9.4, it is generated (for BridJ runtime) as :

```
public native static TestLibrary.GXEntity gxCreateEntity(int someParam);
public native static void gxEntitySetValue(TestLibrary.GXEntity entity, int key, int value);
public native static int gxEntityGetValue(TestLibrary.GXEntity entity, int key);
/// Pointer to unknown (opaque) type
public static class GXEntity extends TypedPointer {
	public GXEntity(long address) {
		super(address);
	}
	public GXEntity(Pointer address) {
		super(address);
	}
};
```

Which is not very object-oriented (1-to-1 mapping to C API).

What we'd like is :

```
public static class GXEntity extends TypedPointer {
	public GXEntity(long address) {
		super(address);
	}
	public GXEntity(Pointer address) {
		super(address);
	}
	public static GXEntity create(int someParam) { ... }
	public void setValue(int key, int value) { ... }
	public int getValue(int key) { ... }
}
```

(idea of transformation [taken from manual example in this blog post](http://www.lunatech-research.fr/node/163))

# Reification in upcoming 0.9.6 #

Well, that's just what does a feature currently under development named "reification", scheduled for the 0.9.6 release !

The command line switch that activates reification is `-reification` (accessible from JNAerator Studio's GUI as well).

# Maximum Performance (w/ BridJ) #

In BridJ, performance is not taken lighly. All functions and methods are natively bound to avoid Java reflection, and in some cases (see http://code.google.com/p/bridj/wiki/Design) assembler optimizations make it possible to forward the native call roughly as quickly as with hand-written JNI.

The issue is that these optimizations are only available for functions and methods with primive arguments and return values, hence excluding pointers and typed pointers.

The following methods won't benefit from assembler optimizations :
```
public native static TestLibrary.GXEntity gxCreateEntity(int someParam);
public native static void gxEntitySetValue(TestLibrary.GXEntity entity, int key, int value);
```

But the following "direct"-style methods will be optimized :
```
public native static @Ptr long gxCreateEntity(int someParam);
public native static void gxEntitySetValue(@Ptr long entity, int key, int value);
```

So here's the pitch : when creating the shortcut methods described above, JNAerator creates a second function signature that's optimizable and calls it with the appropriate wrappers :
```
native static @Ptr long gxCreateEntity(int someParam);
native static void gxEntitySetValue(@Ptr long entity, int key, int value);
public static class GXEntity extends TypedPointer {
	public GXEntity(long address) {
		super(address);
	}
	public GXEntity(Pointer address) {
		super(address);
	}
	public static GXEntity create(int someParam) { 
		long ret = gxCreateEntity(someParam);
		return ret == null ? null : new GXEntity(ret);
	}
	public void setValue(int key, int value) { 
		gxEntitySetValue(this.peer, key, value);
	}
	public int getValue(int key) { 
		return gxEntityGetValue(this.peer, key);
	}
}
```

What gives ?
  * Maximum speed (if used with BridJ)
  * Maximum usability !

Yay !
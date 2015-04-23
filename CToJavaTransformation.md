



# Functions #

There may be more than one way to translate some arguments.
For instance, a ` const int* pInts ` argument could be mapped to an ` IntByReference `, an ` IntBuffer ` or an ` int[] `.

It wouldn't be reasonable to provide all of the possible combinations for all arguments (there could be too many of them), so JNAerator does the following : for each function, it tries at most three translations :
  * one which favours [Pointer](https://jna.dev.java.net/javadoc/com/sun/jna/Pointer.html), [IntByReference](https://jna.dev.java.net/javadoc/com/sun/jna/ptr/IntByReference.html) and other [com.sun.jna.ptr.\*](https://jna.dev.java.net/javadoc/com/sun/jna/ptr/package-tree.html) classes
  * one that favours String, [WString](https://jna.dev.java.net/javadoc/com/sun/jna/WString.html), [ByteBuffer](http://java.sun.com/j2se/1.4.2/docs/api/java/nio/ByteBuffer.html) and other [java.nio.\*](http://java.sun.com/j2se/1.4.2/docs/api/java/nio/package-summary.html) buffers and defaults to aforementioned Pointer subclasses when the use of Buffer classes is not appropriate
  * one that favours primitive arrays where possible, then backs to Strings and Buffers, then to Pointers

As a result, the following function :
```
int* process(int* pInts, const int* cpInts, const wchar_t* wstring, double d);
```
is translated to :
```
/// @deprecated use the safer methods {@link #process(java.nio.IntBuffer, int[], com.sun.jna.WString, double)} and {@link #process(java.nio.IntBuffer, java.nio.IntBuffer, com.sun.jna.WString, double)} instead
@Deprecated
com.sun.jna.ptr.IntByReference process(com.sun.jna.ptr.IntByReference pInts, com.sun.jna.ptr.IntByReference cpInts, com.sun.jna.ptr.ShortByReference wstring, double d);

com.sun.jna.ptr.IntByReference process(java.nio.IntBuffer pInts, int cpInts[], com.sun.jna.WString wstring, double d);

com.sun.jna.ptr.IntByReference process(java.nio.IntBuffer pInts, java.nio.IntBuffer cpInts, com.sun.jna.WString wstring, double d);
```

But of course in simple cases, there is only one or two translations :
```
int process(void* ptr);
```
becomes :
```
int process(com.sun.jna.Pointer ptr);
```

# Structs #

JNAerated structs all get ByReference and ByValue subclasses (that have a struct-pointer-copy constructor) + a constructor with a Pointer and an offset, to cast any memory block to a struct.

```
struct A {
    int i;
};
struct B {
   A a;
   A *pa;
};
```
becomes (comments were stripped out) :
```
public static class A extends com.sun.jna.Structure {
	public A() {}
	public  A(com.sun.jna.Pointer pointer, int offset) {
		super();
		useMemory(pointer, offset);
		read();
	}
	public  A(A struct) { this(struct.getPointer(), 0); }
	public static class ByReference extends A implements com.sun.jna.Structure.ByReference {
		ByReference() {}
		ByReference(A struct) { super(struct.getPointer(), 0); }
	}
	public static class ByValue extends A implements com.sun.jna.Structure.ByValue {
		ByValue() {}
		ByValue(A struct) { super(struct.getPointer(), 0); }
	}
	public int i;
}

public static class B extends com.sun.jna.Structure {
	public  B() {}
	public  B(com.sun.jna.Pointer pointer, int offset) {
		super();
		useMemory(pointer, offset);
		read();
	}
	public  B(B struct) { this(struct.getPointer(), 0); }
	public static class ByReference extends B implements com.sun.jna.Structure.ByReference {
		ByReference() {}
		ByReference(B struct) { super(struct.getPointer(), 0); }
	}
	public static class ByValue extends B implements com.sun.jna.Structure.ByValue {
		ByValue() {}
		ByValue(B struct) { super(struct.getPointer(), 0); }
	}
	public A.ByValue a;
	public A.ByReference pa;
}
```

# Struct fields #

## Arrays fields ##

### Arrays fields with initializers ###

Arrays struct fields with initializers are converted to Java arrays :
```
int iarr[4]; 
char* csarr[3][4]; 
char s[8];
long* (*foo2[12][8])();
```
becomes
```
public int[] iarr = new int[(4)];
public com.sun.jna.ptr.ByteByReference[] csarr = new com.sun.jna.ptr.ByteByReference[3 * 4];
public byte[] s = new byte[(8)];
public foo2_callback[] foo2 = new foo2_callback[12 * 8];
public interface foo2_callback extends com.sun.jna.Callback {
	com.sun.jna.ptr.NativeLongByReference callback();
}
```

### Arrays fields with partial initializers ###

When an array has partial size initializers, JNAerator initializes the known part and switches to Pointer for the rest of the type :
```
char *(*(**foo[][8])())[];
```
becomes
```
public com.sun.jna.Pointer[] foo = new com.sun.jna.Pointer[8];
```

## Callbacks fields ##

```
long (__cdecl *const fptr)();
void (*functions[8])(void*);
long (*foo[10])(union { long l; int i; });
```
becomes
```
public fptr_callback fptr;
public interface fptr_callback extends com.sun.jna.Callback {
	com.sun.jna.NativeLong callback();
}

public functions_callback[] functions = new functions_callback[(8)];
public interface functions_callback extends com.sun.jna.Callback {
	void callback(com.sun.jna.Pointer arg1);
}

public foo_callback[] foo = new foo_callback[(10)];
public interface foo_callback extends com.sun.jna.Callback {
	com.sun.jna.NativeLong callback(foo_callback_arg1_union.ByValue arg1);
}
public static class foo_callback_arg1_union extends com.sun.jna.Union {
	public static class ByReference extends foo_callback_arg1_union implements com.sun.jna.Structure.ByReference {}
	public static class ByValue extends foo_callback_arg1_union implements com.sun.jna.Structure.ByValue {}
	public com.sun.jna.NativeLong l;
	public int i;
}
```

## Bit Fields ##

```
// Stored on 1 byte !
struct S {
	unsigned likes_jnaerator : 1;
	unsigned int times_tried_it : 7; // stored on 7 bits
};
```
becomes :
```
/** Stored on 1 byte ! */
public static class S extends com.sun.jna.Structure {
	@com.sun.jna.Bits(1)
	public int likes_jnaerator;
	@com.sun.jna.Bits(7)
	public int times_tried_it;
}
```

# Anonymous types #

In C/C++/Objective-C, it is possible to make use of anonymous types, such as in the following code :
```
void ComplexArgFunction(
	struct { 
		union { 
			enum { A, B } e; 
			float f; 
		} u; 
		long v; 
		void (*fptr)(); 
	}* pArg
);
```
Which is _roughly_ equivalent to the "deanonymized" version :
```
/// S, U and F gain some higher level visibility compared to the anonymous version
typedef struct { 
	typedef union { 
		typedef enum { 
			A, 
			B
		} E; 
		E e;
		float f; 
	} U;
	U u; 
	long v; 
	typedef void (*F)();
	F *fptr; 
} S;
void ComplexArgFunction(S* pArg);
```

## Deanonymization ##

As Java does not support such inline definition of anonymous types, JNA requires us to provide it with something that looks like the "deanonymized" version of the code, that is : choose arbitrary names for anonymous types.

Another option would be to simply skip everything anonymous, but this would exclude way too much code.

In the deanonymizing process, JNAerator tries its best to choose names that make sense (hence no S, F, U, E, no Struct1, Union3... !).

Here's what JNAerator produces out of the anonymous version :
```
	...
	public static class ComplexArgFunction_pArg_struct extends com.sun.jna.Structure {
		public static class ByReference extends ComplexArgFunction_pArg_struct implements com.sun.jna.Structure.ByReference {}
		public static class ByValue extends ComplexArgFunction_pArg_struct implements com.sun.jna.Structure.ByValue {}
		/// Allocate a new ComplexArgFunction_pArg_struct struct on the heap
		public  ComplexArgFunction_pArg_struct() {}
		/// Cast data at given memory location (pointer + offset) as an existing ComplexArgFunction_pArg_struct struct
		public  ComplexArgFunction_pArg_struct(com.sun.jna.Pointer pointer, int offset) {
			super();
			useMemory(pointer, offset);
			read();
		}
		public u_union.ByValue u;
		public com.sun.jna.NativeLong v;
		public fptr_callback fptr;
		public static class u_union extends com.sun.jna.Union {
			public static class ByReference extends u_union implements com.sun.jna.Structure.ByReference {}
			public static class ByValue extends u_union implements com.sun.jna.Structure.ByValue {}
			/// Allocate a new u_union struct on the heap
			u_union() {}
			/// Cast data at given memory location (pointer + offset) as an existing u_union struct
			u_union(com.sun.jna.Pointer pointer, int offset) {
				super();
				useMemory(pointer, offset);
				read();
			}
			public int e;
			public float f;
			public static interface e_enum {
				public static final int A = 0;
				public static final int B = 1;
			}
		}
		public interface fptr_callback extends com.sun.jna.Callback {
			void callback();
		}
	}

	void ComplexArgFunction(ComplexArgFunction_pArg_struct.ByReference pArg);
	...
```

## Naming convention ##

**This is subject to change.**

Deanonymized types get a suffix that indicate their type :
  * _struct for structures
  *_callback for function signatures
  * _enum for enumerations
  *_union for unions

The choice was made not to use Java naming conventions by default, because it would produce strange names when structures and fields have very short names.

# Global Variables #

Global variables are JNAerated with a lazy implementation : we don't want to try and bind all of the globals when the interface is instantiated or some of its static initializers is called. This could lead to many linkage errors, and it's safer to wait until a global is actually used to provoke such a failure.

JNA provides the [NativeLibrary. getGlobalVariableAddress(symbolName)](https://jna.dev.java.net/javadoc/com/sun/jna/NativeLibrary.html#getGlobalVariableAddress(java.lang.String)) method to get the address of a global variable.

In JNAerated code, a global ` int errno; ` in library "c" is accessed through ` CLibrary.errno.get() ` (this returns directly an [IntByReference](https://jna.dev.java.net/javadoc/com/sun/jna/ptr/IntByReference.html)).

## Primitive values ##

```
int errno;
```
becomes:
```
public static final class errno {
	private static com.sun.jna.ptr.IntByReference errno;
	public static synchronized com.sun.jna.ptr.IntByReference get() {
		if (errno == null) {
			errno = new com.sun.jna.ptr.IntByReference();
			errno.setPointer(TestLibrary.INSTANCE.getGlobalVariableAddress("errno"));
		}
		return errno;
	}
}
```

## Primitive pointers ##

```
int* pInt;
void* pVoid;
```
become :
```
public static final class pInt {
	public static final class pInt_holder extends com.sun.jna.Structure {
		public com.sun.jna.ptr.IntByReference value;
		public  pInt_holder(com.sun.jna.Pointer pointer) {
			super();
			useMemory(pointer, 0);
			read();
		}
	}
	private static pInt_holder pInt;
	public static synchronized pInt_holder get() {
		if (pInt == null) 
			pInt = new pInt_holder(TestLibrary.INSTANCE.getGlobalVariableAddress("pInt"));
		return pInt;
	}
}

public static final class pVoid {
	public static final class pVoid_holder extends com.sun.jna.Structure {
		public com.sun.jna.Pointer value;
		public  pVoid_holder(com.sun.jna.Pointer pointer) {
			super();
			useMemory(pointer, 0);
			read();
		}
	}
	private static pVoid_holder pVoid;
	public static synchronized pVoid_holder get() {
		if (pVoid == null) 
			pVoid = new pVoid_holder(TestLibrary.INSTANCE.getGlobalVariableAddress("pVoid"));
		return pVoid;
	}
}
```
## Struct & struct pointers ##

## Constant expressions & Defines ##

Constant expressions are typically not exported in dynamic libraries.
Hence they are simply translated and put as static fields in the resulting interfaces

```
#define X 10
const int i = X + (1 + 2) * 4;
```
becomes :
```
public static final int i = 10 + (1 + 2) * 4;
public static final int X = 10;
```

# Objective-C Classes #

See [ObjectiveC](ObjectiveC.md)
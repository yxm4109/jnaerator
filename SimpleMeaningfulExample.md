

# Input Header #

```
struct Pair {
        /// Comment on both first and second
        int first, second;
};

#define PAIRS_COUNT 10
#define EXPRESSION (1 << 10) | (1 << 5)

enum Values {
	First, // first comments
	Second,  // comments on second
	Last // not a real value
};

struct BiggerStruct {
	Values enumValue;
	bool (*hook)(int val);
	long longValue;
	int intArray[sizeof(Pair) * PAIRS_COUNT];
};

// Wonderful comments on Test function
void Test(BiggerStruct& s);

// Be careful with that one : intArray and source are const, dest is not
void CopyBytes(char* dest, const char* source, size_t n, const int* intArray);
```

# Command-line #

```
java -jar jnaerator.jar -library Test Test.h -o . -v -noJar -noComp
```

(see [CommandLineOptionsAndEnvironmentVariables](CommandLineOptionsAndEnvironmentVariables.md) for more options)

# JNAerator output #

File ` test/TestLibrary.java `

```
package test;

/// JNA Wrapper for library <b>test</b>

public interface TestLibrary extends com.sun.jna.Library
{
	public TestLibrary INSTANCE = (TestLibrary)com.sun.jna.Native.loadLibrary("test", TestLibrary.class);


	/**
	 * <i>native declaration : /Users/ochafik/Prog/Java/null:9</i><br>
	 * enum values
	 */
	public static interface Values {
		/// first comments
		public static final int First = 0;
		/// comments on second
		public static final int Second = 1;
		/// not a real value
		public static final int Last = 2;
	}

	public static final int PAIRS_COUNT = 10;

	public static final int EXPRESSION = (1 << 10) | (1 << 5);

	/// <i>native declaration : /Users/ochafik/Prog/Java/null:1</i>
	public static class Pair extends com.sun.jna.Structure {
		/// Allocate a new Pair struct on the heap
		public Pair() {}
		/// Cast data at given memory location (pointer + offset) as an existing Pair struct
		public Pair(com.sun.jna.Pointer pointer, int offset) {
			super();
			useMemory(pointer, offset);
			read();
		}
		/// Create an instance that shares its memory with another Pair instance
		public Pair(Pair struct) { this(struct.getPointer(), 0); }
		public static class ByReference extends Pair implements com.sun.jna.Structure.ByReference {
			/// Allocate a new Pair.ByRef struct on the heap
			public ByReference() {}
			/// Create an instance that shares its memory with another Pair instance
			public ByReference(Pair struct) { super(struct.getPointer(), 0); }
		}
		public static class ByValue extends Pair implements com.sun.jna.Structure.ByValue {
			/// Allocate a new Pair.ByVal struct on the heap
			public ByValue() {}
			/// Create an instance that shares its memory with another Pair instance
			public ByValue(Pair struct) { super(struct.getPointer(), 0); }
		}
		/// Comment on both first and second
		public int first;
		/// Comment on both first and second
		public int second;
	}

	/// <i>native declaration : /Users/ochafik/Prog/Java/null:15</i>
	public static class BiggerStruct extends com.sun.jna.Structure {
		/// Allocate a new BiggerStruct struct on the heap
		public BiggerStruct() {}
		/// Cast data at given memory location (pointer + offset) as an existing BiggerStruct struct
		public BiggerStruct(com.sun.jna.Pointer pointer, int offset) {
			super();
			useMemory(pointer, offset);
			read();
		}
		/// Create an instance that shares its memory with another BiggerStruct instance
		public BiggerStruct(BiggerStruct struct) { this(struct.getPointer(), 0); }
		public static class ByReference extends BiggerStruct implements com.sun.jna.Structure.ByReference {
			/// Allocate a new BiggerStruct.ByRef struct on the heap
			public ByReference() {}
			/// Create an instance that shares its memory with another BiggerStruct instance
			public ByReference(BiggerStruct struct) { super(struct.getPointer(), 0); }
		}
		public static class ByValue extends BiggerStruct implements com.sun.jna.Structure.ByValue {
			/// Allocate a new BiggerStruct.ByVal struct on the heap
			public ByValue() {}
			/// Create an instance that shares its memory with another BiggerStruct instance
			public ByValue(BiggerStruct struct) { super(struct.getPointer(), 0); }
		}
		/// @see Values
		public int enumValue;
		public hook_callback hook;
		public com.sun.jna.NativeLong longValue;
		public int[] intArray = new int[(new Pair().size() * 10)];
		/// <i>native declaration : /Users/ochafik/Prog/Java/null</i>
		public interface hook_callback extends com.sun.jna.Callback {
			/// Original signature : <code>bool hook_callback(int)</code>
			boolean invoke(int val1);
		}
	}

	/**
	 * Wonderful comments on Test function<br>
	 * Original signature : <code>void Test(BiggerStruct&)</code><br>
	 * <i>native declaration : /Users/ochafik/Prog/Java/null</i>
	 */
	void Test(BiggerStruct s1);

	/**
	 * Be careful with that one : intArray and source are const, dest is not<br>
	 * Original signature : <code>void CopyBytes(char*, const char*, size_t, const int*)</code><br>
	 * <i>native declaration : /Users/ochafik/Prog/Java/null</i><br>
	 * @deprecated use the safer methods {@link #CopyBytes(java.nio.ByteBuffer, java.lang.String, com.sun.jna.NativeLong, int[])} and {@link #CopyBytes(java.nio.ByteBuffer, java.lang.String, com.sun.jna.NativeLong, java.nio.IntBuffer)} instead
	 */
	@Deprecated
	void CopyBytes(com.sun.jna.ptr.ByteByReference dest1, com.sun.jna.ptr.ByteByReference source2, com.sun.jna.NativeLong n3, com.sun.jna.ptr.IntByReference intArray4);

	/**
	 * Be careful with that one : intArray and source are const, dest is not<br>
	 * Original signature : <code>void CopyBytes(char*, const char*, size_t, const int*)</code><br>
	 * <i>native declaration : /Users/ochafik/Prog/Java/null</i
	 */
	void CopyBytes(java.nio.ByteBuffer dest1, java.lang.String source2, com.sun.jna.NativeLong n3, int intArray4[]);

	/**
	 * Be careful with that one : intArray and source are const, dest is not<br>
	 * Original signature : <code>void CopyBytes(char*, const char*, size_t, const int*)</code><br>
	 * <i>native declaration : /Users/ochafik/Prog/Java/null</i>
	 */
	void CopyBytes(java.nio.ByteBuffer dest1, java.lang.String source2, com.sun.jna.NativeLong n3, java.nio.IntBuffer intArray4);
}
```

# More examples #

Please read the page [Documentation](Documentation.md).

# Try it for yourself ! #

Simply download [jna.jar](http://jna.dev.java.net) and [jnaerator.jar](http://ochafik.free.fr/Java/jnaerator.jar) and you'll be generating your JNA interfaces in a minute !

# Nothing works as expected ? #

Please read [JNAeratorFAQ](JNAeratorFAQ.md) and [TroubleShootingJNAeration](TroubleShootingJNAeration.md), or post your question to the [JNA users mailing list](https://jna.dev.java.net/servlets/SummarizeList?listName=users).
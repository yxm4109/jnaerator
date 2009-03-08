/*
	Copyright (c) 2009 Olivier Chafik, All Rights Reserved
	
	This file is part of JNAerator (http://jnaerator.googlecode.com/).
	
	JNAerator is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	JNAerator is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with JNAerator.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.ochafik.lang.jnaerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.anarres.cpp.LexerException;
import org.antlr.runtime.RecognitionException;
import org.junit.runner.JUnitCore;

import com.ochafik.lang.jnaerator.parser.Declaration;
import com.ochafik.lang.jnaerator.parser.Define;
import com.ochafik.lang.jnaerator.parser.Element;
import com.ochafik.lang.jnaerator.parser.ObjCppElementsTests;
import com.ochafik.lang.jnaerator.parser.ObjCppParsingTests;
import com.ochafik.lang.jnaerator.parser.ObjCppTests;
import com.ochafik.lang.jnaerator.studio.JNAeratorStudio;
import com.ochafik.util.string.StringUtils;
import com.sun.jna.Library;
import com.sun.jna.Native;

/*
//include com/ochafik/lang/jnaerator/parser/*.mm
//include com/ochafik/lang/jnaerator/parser/ObjCpp.g
 */

public class JNAerator {

	final JNAeratorConfig config;
	
	public JNAerator(JNAeratorConfig config) {
		this.config = config;
	}

	static final Pattern definePattern = Pattern.compile("#\\s*define\\s+(\\w+)\\s+(.*)");
	static final boolean fullFilePathInComments = true;
	
	static final Class<?>[] includeClassesHack = new Class<?>[] {
		ObjCppParsingTests.class,
		ObjCppElementsTests.class,
		JNAeratorStudio.class
	};

	private static void displayHelp() {
		System.out.println("Credits:   JNAerator is Copyright (c) 2008-2009 Olivier Chafik");
		System.out.println("           Includes Anarres JCPP (Apache 2.0 license), Copyright (c) 2007-2008, Shevek");
		System.out.println("           Includes Java Native Access (JNA) (LGPL license), Copyright (c) 2006-2009 Todd Fast, Timothy Wall, Wayne Meissner & others");
		//System.out.println("           Includes the library GNU Trove (LGPL 2.1 license)");
		System.out.println("           Includes ANTLR's runtime (BSD license), Copyright (c) 2003-2008, Terence Parr");
		System.out.println("           Licensing & Copyright details : http://code.google.com/p/jnaerator/wiki/CreditsAndLicense");
		System.out.println("   Syntax: " + JNAerator.class.getSimpleName() + " options (-framework framework)* files-or-directories*");
		System.out.println("  Options:");
		System.out.println("\t-Iinclude-path");
		System.out.println("\t\tAdd include path.");
		System.out.println("\t-v");
		System.out.println("\t\tVerbose mode.");
		System.out.println("\t-Dsymbol[=value]");
		System.out.println("\t\tDefine a preprocessor symbol.");
		System.out.println("\t-library name");
		System.out.println("\t\tGives name of the library in which definitions will end up. \"c\" library will be generated as CLibrary class.");
		System.out.println("\t-o outputDir");
		System.out.println("\t\tRoot directory where all generated sources go");
		System.out.println("\t-project SolutionFile configString");
		System.out.println("\t\tRead Visual Studio 2008 project or solution file and use the configuration specified (e.g. \"Release|Win32\").");
		System.out.println("\t-package forcedPackageName");
		System.out.println("\t\tPackage name that is to be used for all generated classes.");
		System.out.println("\t\tIt is better to choose a root package name and let the program infer intermediate names (it will add the framework name, if recognized)");
		System.out.println("\t-root rootPackageName");
		System.out.println("\t\tPackage name to be prepended package names for any generated classes");
		System.out.println("\t-frameworksPath");
		System.out.println("\t\tBy default: " + JNAeratorConfigUtils.DEFAULT_FRAMEWORKS_PATH);
	}
	public static void main(String[] args) {
		if (args.length == 0) {
			if (new File("/Users/ochafik").exists()) {
				args = new String[] {
						//"/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator2.0.sdk/System/Library/Frameworks/Foundation.framework/Versions/C/Headers/NSURL.h",
						//"/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator2.0.sdk/System/Library/Frameworks/Foundation.framework/Versions/C/Headers",
						
	//					"-library", "gc", "/Users/ochafik/src/gc6.8/include/",
//						"-I/Developer/SDKs/MacOSX10.5.sdk/usr/include",
//						"/Developer/SDKs/MacOSX10.4u.sdk/usr/include/sys/event.h",
//						"/Developer/SDKs/MacOSX10.4u.sdk/usr/include/machine/types.h",
	//					"/Developer/SDKs/MacOSX10.4u.sdk/usr/include/sys/cdefs.h",
//						"/Developer/SDKs/MacOSX10.4u.sdk/usr/include/sys/_types.h",
//						"/Developer/SDKs/MacOSX10.4u.sdk/usr/include/stdint.h",
	//					
//						"-autoConf",
						//"-library", "c",
//						"-root", "org.rococoa",
						
//						"/Developer/SDKs/MacOSX10.4u.sdk/usr/include/sys/types.h", 
//						"/Developer/SDKs/MacOSX10.4u.sdk/usr/include/architecture/i386/math.h",
//						"/System/Library/Frameworks/Foundation.framework/Headers/NSObjCRuntime.h",
	//					"/System/Library/Frameworks/ApplicationServices.framework/Versions/Current/Frameworks/CoreGraphics.framework/Headers/CGBase.h",
	//					"/System/Library/Frameworks/ApplicationServices.framework/Versions/Current/Frameworks/CoreGraphics.framework/Headers/CGShading.h",
//						"/Developer/SDKs/MacOSX10.4u.sdk/usr/include/AvailabilityMacros.h",
//						"/Users/ochafik/Prog/Java/testxp/test.h",
//						"/Users/ochafik/Prog/Java/test/Test2.h",
//						"-framework", "Foundation", 
//						"/System/Library/Frameworks/Foundation.framework/Headers/NSArray.h",
//						"-framework", "CoreFoundation",
//						"-framework", "CoreGraphics", 
//						"-framework", "CarbonCore", 
						//"-f", "QTKit", 
//						"-o", "/Users/ochafik/Prog/Java/test/objc",
//						"-o", "/Users/ochafik/Prog/Java/testxp",
						"/Users/ochafik/Prog/Java/test/Test.h",
//						"/Users/ochafik/Prog/Java/test/JNATest.h",
						"-o", "/Users/ochafik/Prog/Java",
						"-v"
				};
			} else {
				displayHelp();
				return;
			}
		}
		try {
			final JNAeratorConfig config = new JNAeratorConfig();
			
			List<String> frameworks = new ArrayList<String>();
			config.preprocessorConfig.frameworksPath.addAll(JNAeratorConfigUtils.DEFAULT_FRAMEWORKS_PATH);
//			boolean auto = false;
			for (int iArg = 0, len = args.length; iArg < len; iArg++) {
				String arg = args[iArg];
				if (arg.startsWith("-I")) {
					config.preprocessorConfig.includes.add(arg.substring("-I".length()));
				} else if (arg.startsWith("-D")) {
					int k = arg.indexOf('=');
					String key = arg.substring("-D".length(), k > 0 ? k : arg.length()),
						value = k > 0 ? arg.substring(k) : "";
					config.preprocessorConfig.macros.put(key, value);
//				} else if (arg.equals("-auto")) {
//					auto = true;
					//JNAeratorConfigUtils.autoConfigure(config);
				} else if (arg.equals("-root"))
					config.rootPackageName = args[++iArg];
				else if (arg.equals("-frameworksPath")) {
					config.preprocessorConfig.frameworksPath.clear();
					config.preprocessorConfig.frameworksPath.addAll(Arrays.asList(args[++iArg].split(":")));
				} else if (arg.equals("-v"))
					config.verbose = true;
				else if (arg.equals("-package"))
					config.packageName = args[++iArg];
				else if (arg.equals("-test")) {
					try {
						JUnitCore.main(ObjCppTests.class.getName());
						System.exit(0);
					} catch (Exception ex) {
						ex.printStackTrace();
						System.exit(1);
					}
				}
				else if (arg.equals("-studio")) {
					try {
						JNAeratorStudio.main(new String[0]);
						return;
					} catch (Exception ex) {
						ex.printStackTrace();
						System.exit(1);
					}
				}
				else if (arg.equals("-project")) {
					File projectFile = new File(args[++iArg]);
					String configName = null;
					if (iArg < len)
						configName = args[++iArg];
					
					JNAeratorConfigUtils.readProjectConfig(projectFile, configName, config);
				}
				else if (arg.equals("-library"))
					config.defaultLibrary = args[++iArg];
				else if (arg.equals("-framework"))
					frameworks.add(args[++iArg]);
				else if (arg.equals("-o"))
					config.outputDir = new File(args[++iArg]);
				else if (arg.equals("-h") || arg.equals("-help") || arg.equals("--h")) {
					displayHelp();
					System.exit(0);
				} else if (arg.endsWith(".framework"))
					frameworks.add(arg);
				else
					config.addFile(new File(arg), config.defaultLibrary);
			}
			
			if (config.defaultLibrary == null) {
				for (File f : config.getFiles()) {
					String name = f.getName();
					int i = name.indexOf('.');
					if (i >= 0)
						name = name.substring(0, i).trim();
					if (name.length() > 0)
						config.defaultLibrary = name;
					else
						config.defaultLibrary = "default";
					
					System.out.println("Warning: no -library option, using \"" + config.defaultLibrary + "\".");
					break;
				}
			}
			for (String framework : frameworks)
				JNAeratorConfigUtils.addFramework(config, framework);
			
			config.addRootDir(new File("."));
			for (String i : config.preprocessorConfig.includes) {
				try {
					config.addRootDir(new File(i));
				} catch (Exception ex) {}
			}
			
			//if (auto)
			JNAeratorConfigUtils.autoConfigure(config);
			new JNAerator(config).jnaerate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void jnaerate() throws IOException, LexerException, RecognitionException {
		SourceFiles sourceFiles = parse();
		jnaerate(sourceFiles, new ClassOutputter() {
			public PrintWriter getClassSourceWriter(String className) throws FileNotFoundException {
				File file = new File(JNAerator.this.config.outputDir, className.replace('.', File.separatorChar) + ".java");
				File parent = file.getParentFile();
				if (!parent.exists())
					parent.mkdirs();

				//if (config.verbose)
					System.err.println("Generating: " + file.getAbsolutePath());

				return new PrintWriter(file) {
					@Override
					public void print(String s) {
						super.print(s.replace("\r", "").replace("\n", StringUtils.LINE_SEPARATOR));
					}
				};
			}
		});
	}
			
	public SourceFiles parse() throws IOException, LexerException {
		return JNAeratorParser.parse(config);
	}
	public void addFile(File file, List<File> out) throws IOException {
		if (file.isFile()) {
			out.add(file);
		} else {
			File[] fs = file.listFiles();
			if (fs != null) {
				for (File f : fs) {
					addFile(f, out);
				}
			}
		}
	}

	private static void generateLibraryFiles(SourceFiles sourceFiles, Result result, ClassOutputter outputter) throws IOException {
		
		for (String library : result.libraries) {
			if (library == null)
				continue; // to handle code defined in macro-expanded expressions
//				library = "";
			
			String javaPackage = result.javaPackageByLibrary.get(library);
			String libraryClassName = result.getLibraryClassSimpleName(library);
			
			final PrintWriter out = outputter.getClassSourceWriter((javaPackage == null ? "" : javaPackage + ".") + libraryClassName);
			
			if (javaPackage != null)
				out.println("package " + javaPackage + ";");
			
			for (String pn : result.javaPackages) {
				if (pn.equals(""))
					continue;
				
				if (pn.equals(javaPackage))
					continue;
				out.println("import " + pn + ".*;");
			}
			if (!result.objCClasses.isEmpty())
				out.println("import org.rococoa.ID;");
			
			out.println();
			out.println(Element.formatComments("", null, null, false, true, true, "JNA Wrapper for library <b>" + library + "</b>", 
					result.declarationsConverter.getFileCommentContent(result.config.libraryProjectSources.get(library), null)));
			out.println("public interface " + libraryClassName + " extends " + Library.class.getName() + "\n{");
			
			String libraryNameExpression = result.getLibraryFileExpression(library);
			out.println("\tpublic " + libraryClassName + " INSTANCE = (" + libraryClassName + ")" + Native.class.getName() + ".loadLibrary(" + libraryNameExpression  + ", " + libraryClassName + ".class);");
			out.println();
			
			Set<String> signatures = result.getSignaturesForOutputClass(libraryClassName);
			
			List<Declaration> children = new ArrayList<Declaration>();
			
			result.declarationsConverter.convertEnums(result.enumsByLibrary.get(library), signatures, children, libraryClassName);
			result.declarationsConverter.convertConstants(result.definesByLibrary.get(library), sourceFiles, signatures, children, libraryClassName);
			result.declarationsConverter.convertStructs(result.structsByLibrary.get(library), signatures, children, libraryClassName);
			result.declarationsConverter.convertCallbacks(result.callbacksByLibrary.get(library), signatures, children, libraryClassName);
			result.declarationsConverter.convertFunctions(result.functionsByLibrary.get(library), signatures, children, libraryClassName);

			for (Declaration d : children) {
				out.println();
				out.println("\t" + d.toString("\t"));
			}
			
			out.println("}");
			out.close();
		}
	}

	/// To be overridden
	public Result createResult(ClassOutputter outputter) {
		return new Result(config, outputter);
	}
	
	public void jnaerate(SourceFiles sourceFiles, ClassOutputter outputter) throws IOException, LexerException, RecognitionException {
		
		Result result = createResult(outputter);
		
		/// Perform Objective-C-specific pre-transformation (javadoc conversion for enums + find name of enums based on next sibling integer typedefs)
		sourceFiles.accept(new ObjectiveCToJavaPreScanner());

		/// Explode declarations to have only one direct declarator each
		sourceFiles.accept(new CToJavaPreScanner());
		
		/// Give sensible names to anonymous function signatures, structs, enums, unions, and move them up one level as typedefs
		sourceFiles.accept(new MissingNamesChooser());
		
		//##################################################################
		//##### BEGINNING HERE, sourceFiles NO LONGER GETS MODIFIED ! ######
		//##################################################################
		
		/// Gather Objective-C classes
		sourceFiles.accept(result);
		result.chooseLibraryClasses(config.packageName, config.rootPackageName);
		
		//TODO resolve variables in visual studio projects
		//TODO Propagate unconvertible expressions, mark corresponding elements / trees as "to be commented out"
		
		/// Resolution's first pass : define relevant chained environment for each element
//		final DefinitionsVisitor definitions = new DefinitionsVisitor();
//		sourceFiles.accept(definitions);
		
		/// Resolve references of variables and types (map id -> type)
//		ResolutionScanner resolutions = new ResolutionScanner(definitions, originalOut);
//		sourceFiles.accept(resolutions);
		

		/// Filter unused symbols from implicitely included files
//		if (config.symbolsAccepter != null) {
//			originalOut.println("Filtering unused symbols");
//			UnusedScanner unused = new UnusedScanner(resolutions, config.symbolsAccepter, null);//originalOut);
//			sourceFiles.accept(unused);
//			unused.removeUnused(null);	
//		}
		
		
		/// Spit Objective-C classes out
		if (!result.objCClasses.isEmpty()) {
			if (config.verbose)
				System.out.println("Generating Objective-C classes");
			for (ObjCClass type : result.objCClasses.values())
				type.generateWrapperFile();
		}
		
		if (config.verbose)
			System.out.println("Generating libraries");
		
		if (result.libraries.size() == 1) {
			List<Define> list = result.definesByLibrary.get(null);
			if (list != null) {
				String lib = result.libraries.iterator().next();
				Result.getList(result.definesByLibrary, lib).addAll(list);
			}
		}
		
		generateLibraryFiles(sourceFiles, result, outputter);

		//if (config.verbose)
			for (String unknownType : result.typeConverter.unknownTypes) 
				System.out.println("Unknown Type: " + unknownType);
	}

}

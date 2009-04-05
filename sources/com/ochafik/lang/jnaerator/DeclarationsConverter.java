/*
	Copyright (c) 2009 Olivier Chafik, All Rights Reserved
	
	This file is part of JNAerator (http://jnaerator.googlecode.com/).
	
	JNAerator is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	JNAerator is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.
	
	You should have received a copy of the GNU Lesser General Public License
	along with JNAerator.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.ochafik.lang.jnaerator;

import static com.ochafik.lang.SyntaxUtils.as;

import java.io.File;
import java.util.*;

import com.ochafik.lang.jnaerator.JNAeratorConfig.GenFeatures;
import com.ochafik.lang.jnaerator.TypeConversion.JavaPrim;
import com.ochafik.lang.jnaerator.TypeConversion.TypeConversionMode;
import com.ochafik.lang.jnaerator.cplusplus.CPlusPlusMangler;
import com.ochafik.lang.jnaerator.parser.*;
import com.ochafik.lang.jnaerator.parser.Enum;
import com.ochafik.lang.jnaerator.parser.Function;
import com.ochafik.lang.jnaerator.parser.Scanner;
import com.ochafik.lang.jnaerator.parser.ObjCppParser.Language;
import com.ochafik.lang.jnaerator.parser.StoredDeclarations.*;
import com.ochafik.lang.jnaerator.parser.TypeRef.*;
import com.ochafik.lang.jnaerator.parser.Expression.*;
import com.ochafik.lang.jnaerator.parser.Function.Type;
import com.ochafik.lang.jnaerator.parser.Declarator.*;
import com.ochafik.util.listenable.Pair;
import com.ochafik.util.string.StringUtils;
import com.sun.jna.*;
import com.sun.jna.Pointer;

public class DeclarationsConverter {
	public DeclarationsConverter(Result result) {
		this.result = result;
	}

	final Result result;
	
	
	void convertCallback(FunctionSignature functionSignature, Set<String> signatures, DeclarationsHolder out, String callerLibraryName) {
		String name = result.typeConverter.inferCallBackName(functionSignature, false);
		if (name == null)
			return;
		
		name = result.typeConverter.getValidJavaArgumentName(name);
		
		Function function = functionSignature.getFunction();
		
		int i = 1;
		String chosenName = name;
		while (!(signatures.add(chosenName))) {
			chosenName = name + (++i);
		}
		
		Element parent = functionSignature.getParentElement();
		Element comel = parent != null && parent instanceof TypeDef ? parent : functionSignature;
		
		Struct callbackStruct = new Struct();
		callbackStruct.setType(Struct.Type.JavaInterface);
		callbackStruct.addModifiers(Modifier.Public);
		callbackStruct.setParents(Arrays.asList(Callback.class.getName()));
		callbackStruct.setTag(chosenName);
		callbackStruct.addToCommentBefore(comel.getCommentBefore(), comel.getCommentAfter(), getFileCommentContent(comel));
		convertFunction(function, new TreeSet<String>(), true, callbackStruct, callerLibraryName);
		for (Declaration d : callbackStruct.getDeclarations()) {
			if (d instanceof Function) {
				callbackStruct.addAnnotations(callbackStruct.getAnnotations());
				callbackStruct.setAnnotations(null);
				break;
			}
		}
		out.addDeclaration(new TaggedTypeRefDeclaration(callbackStruct));
	}

	void convertCallbacks(List<FunctionSignature> functionSignatures, Set<String> signatures, DeclarationsHolder out, String callerLibraryClass) {
		if (functionSignatures != null) {
			for (FunctionSignature functionSignature : functionSignatures) {
				if (functionSignature.findParentOfType(Struct.class) != null)
					continue;
					
				convertCallback(functionSignature, signatures, out, callerLibraryClass);
			}
		}
		
	}

	public static class DeclarationsOutput {
		Map<String, DeclarationsHolder> holders = new HashMap<String, DeclarationsHolder>();
		public void add(Declaration d, String libraryName) {
			
		}
		public void set(String libraryName, DeclarationsHolder holder) {
			
		}
	}
	void convertConstants(List<Define> defines, Element sourcesRoot, final Set<String> signatures, final DeclarationsHolder out, final String callerLibraryClass) {
		//final List<Define> defines = new ArrayList<Define>();
		sourcesRoot.accept(new Scanner() {
//			@Override
//			public void visitDefine(Define define) {
//				super.visitDefine(define);
//				if (elementsFilter.accept(define))
//					defines.add(define);
//			}
			@Override
			public void visitVariablesDeclaration(VariablesDeclaration v) {
				super.visitVariablesDeclaration(v);
				//if (!elementsFilter.accept(v))
				//	return;
				
				if (v.findParentOfType(Struct.class) != null)
					return;
				
				if (v.getValueType() instanceof FunctionSignature)
					return;
					
				for (Declarator vs : v.getDeclarators()) {
					if (!(vs instanceof DirectDeclarator))
						continue; // TODO provide a mapping of exported values
					
					TypeRef mutatedType = (TypeRef) vs.mutateType(v.getValueType());
					
					if (!mutatedType.getModifiers().contains(Modifier.Const))
						return;
					
					//TypeRef type = v.getValueType();
					JavaPrim prim = result.typeConverter.getPrimitive(mutatedType, callerLibraryClass);
					if (prim == null)
						return;
					
					try {
						
						DirectDeclarator dd = (DirectDeclarator)vs;
						Expression val = result.typeConverter.convertExpressionToJava(vs.getDefaultValue(), callerLibraryClass);
						
						if (!signatures.add(vs.resolveName()))
							continue;
						
						TypeRef tr = result.typeConverter.convertTypeToJNA(mutatedType, TypeConversion.TypeConversionMode.FieldType, callerLibraryClass);
						VariablesDeclaration vd = new VariablesDeclaration(tr, new DirectDeclarator(dd.getName(), val));
						vd.setCommentBefore(v.getCommentBefore());
						vd.addToCommentBefore(vs.getCommentBefore());
						vd.addToCommentBefore(vs.getCommentAfter());
						vd.addToCommentBefore(v.getCommentAfter());
						
						out.addDeclaration(vd);
					} catch (UnsupportedConversionException e) {
						out.addDeclaration(skipDeclaration(v, e.toString()));
					}
					
				}
			}
		});
		
		if (defines != null) {
			for (Define define : reorderDefines(defines)) {
				if (define.getValue() == null)
					continue;
				
				try {
					out.addDeclaration(outputConstant(define.getName(), define.getValue(), signatures, define.getValue(), "define", callerLibraryClass, true));
				} catch (UnsupportedConversionException ex) {
					out.addDeclaration(skipDeclaration(define, ex.toString()));
				}
			}
		}
	}

	EmptyDeclaration skipDeclaration(Element e, String... preMessages) {
		List<String> mess = new ArrayList<String>();
		if (preMessages != null)
			mess.addAll(Arrays.asList(preMessages));
		mess.addAll(Arrays.asList("SKIPPED:", e.formatComments("", true, true, false), getFileCommentContent(e), e.toString().replace("*/", "* /")));
		return new EmptyDeclaration(mess.toArray(new String[0]));
	}
	
	private void convertEnum(Enum e, Set<String> signatures, DeclarationsHolder out, String callerLibraryClass) {
		if (e.isForwardDeclaration())
			return;
		
		DeclarationsHolder localOut = out;
		Set<String> localSignatures = signatures;
		
		Struct enumInterf = null;
		String enumName = getActualTaggedTypeName(e);
		if (enumName != null) {
			
			enumInterf = publicStaticClass(enumName, null, Struct.Type.JavaInterface, e);
			if (result.config.features.contains(JNAeratorConfig.GenFeatures.EnumTypeLocationComments))
				enumInterf.addToCommentBefore("enum values");
			out.addDeclaration(new TaggedTypeRefDeclaration(enumInterf));
			
			localSignatures = new HashSet<String>();
			localOut = enumInterf;
		}
		Integer lastAdditiveValue = null;
		Expression lastRefValue = null;
		boolean failedOnceForThisEnum = false;
		for (com.ochafik.lang.jnaerator.parser.Enum.EnumItem item : e.getItems()) {
			Expression resultingExpression;
			try {
				if (item.getValue() == null) {
					// no explicit value
					if (lastRefValue == null) {
						if (lastAdditiveValue != null) {
							lastAdditiveValue++;
							resultingExpression = new Expression.Constant(Constant.Type.Int, lastAdditiveValue);
						} else {
							if (item == e.getItems().get(0)) {
								lastAdditiveValue = 0;
								resultingExpression = new Expression.Constant(Constant.Type.Int, lastAdditiveValue);
							} else
								resultingExpression = null;
						}
					} else {
						// has a last reference value
						if (lastAdditiveValue != null)
							lastAdditiveValue++;
						else
							lastAdditiveValue = 1;
						
						resultingExpression = //result.typeConverter.convertExpressionToJava(
							new Expression.BinaryOp(
								Expression.BinaryOperator.Plus, 
								lastRefValue.clone(), 
								new Expression.Constant(Constant.Type.Int, lastAdditiveValue)
							//)
						);
					}
				} else {
					// has an explicit value
					failedOnceForThisEnum = false;// reset skipping
					lastAdditiveValue = null;
					lastRefValue = item.getValue();
					resultingExpression = lastRefValue;
					if (lastRefValue instanceof Expression.Constant) {
						try {
							lastAdditiveValue = ((Expression.Constant)lastRefValue).asInteger();
							lastRefValue = null;
						} catch (Exception ex) {}
					}	
				}
			} catch (Exception ex) {
				//ex.printStackTrace();
				resultingExpression = null;
			}
			if (failedOnceForThisEnum || (failedOnceForThisEnum = resultingExpression == null))
				out.addDeclaration(skipDeclaration(item));
			else {
				try {
					localOut.addDeclaration(outputConstant(item.getName(), result.typeConverter.convertExpressionToJava(resultingExpression, callerLibraryClass), localSignatures, item, "enum item", 
							callerLibraryClass, 
							enumInterf == null
					));
				} catch (Exception ex) {
					out.addDeclaration(skipDeclaration(item, ex.toString()));
				}
			}
		}
		//if (enumInterf != null)
		//	enumInterf.addDeclarations(localOut);
	}

	@SuppressWarnings("static-access")
	private Declaration outputConstant(String name, Expression x, Set<String> signatures, Element element, String elementTypeDescription, String callerLibraryClass, boolean addFileComment) throws UnsupportedConversionException {
		try {
			if (!result.typeConverter.isValidJavaIdentifier(name))
				throw new UnsupportedConversionException(element, "The name '" + name + "' is invalid for a Java field.");
			
			Expression converted = result.typeConverter.convertExpressionToJava(x, callerLibraryClass);
			TypeRef tr = result.typeConverter.inferJavaType(converted);
			JavaPrim prim = result.typeConverter.getPrimitive(tr, callerLibraryClass);
			if (prim == null) {
				return new EmptyDeclaration("Failed to infer type of " + converted);
			} else if (prim != JavaPrim.Void) {
				if (signatures.add(name)) {
					String t = converted.toString();
					if (t.contains("sizeof")) {
						converted = result.typeConverter.convertExpressionToJava(x, callerLibraryClass);
					}

					//TypeRef tr = new TypeRef.SimpleTypeRef(result.typeConverter.typeToJNA(type, vs, TypeConversion.TypeConversionMode.FieldType, callerLibraryClass));
					Declaration declaration = new VariablesDeclaration(tr, new DirectDeclarator(name, converted));
					declaration.addModifiers(Modifier.Public, Modifier.Static, Modifier.Final);
					declaration.importDetails(element, false);
					declaration.moveAllCommentsBefore();
					if (addFileComment)
						declaration.addToCommentBefore(getFileCommentContent(element));
					return declaration;
				}
			}
			return skipDeclaration(element, elementTypeDescription);
		} catch (UnsupportedConversionException e) {
			return skipDeclaration(element, elementTypeDescription, e.toString());
		}	
		
	} 

	void convertEnums(List<Enum> enums, Set<String> signatures, DeclarationsHolder out, String callerLibraryClass) {
		if (enums != null) {
			//out.println("public static class ENUMS {");
			for (com.ochafik.lang.jnaerator.parser.Enum e : enums) {
				if (e.findParentOfType(Struct.class) != null)
					continue;
				
				convertEnum(e, signatures, out, callerLibraryClass);
			}
			//out.println("}");
		}
	}

	void convertFunction(Function function, Set<String> signatures, boolean isCallback, DeclarationsHolder out, String callerLibraryClass) {
		if (result.config.functionsAccepter != null && !result.config.functionsAccepter.adapt(function))
			return;
		String functionName = function.getName();
		if (functionName == null)
			functionName = "invoke";
		
//		if (functionName.equals("clEnqueueReadBuffer"))
//			functionName = functionName.toString();
//		
		if (functionName.contains("<")) {
			return;
		}
		
		if (!TypeConversion.isValidJavaIdentifier(functionName))
			return;
		
		Function natFunc = new Function();
		
		Element parent = function.getParentElement();
		List<String> ns = new ArrayList<String>(function.getNameSpace());
		boolean isMethod = parent instanceof Struct;
		if (isMethod) {
			ns.clear();
			ns.addAll(parent.getNameSpace());
			ns.add(((Struct)parent).getTag());
		}
		//String namespaceArrayStr = "{\"" + StringUtils.implode(ns, "\", \"") + "\"}";
		//if (!ns.isEmpty())
		//	natFunc.addAnnotation(new Annotation(Namespace.class, "(value=" + namespaceArrayStr + (isMethod ? ", isClass=true" : "") + ")"));
		
		natFunc.setType(Function.Type.JavaMethod);
		try {
			//StringBuilder outPrefix = new StringBuilder();
			TypeRef returnType = null;
			
			boolean isObjectiveC = function.getType() == Type.ObjCMethod;
			if (!isObjectiveC) {
				returnType = function.getValueType();
				if (returnType == null)
					returnType = new TypeRef.Primitive("int");
			} else {
				returnType = RococoaUtils.fixReturnType(function);
				functionName = RococoaUtils.getMethodName(function);
			}
			
			String modifiedMethodName;
			if (isCallback) {
				modifiedMethodName = "invoke";
			} else {
				modifiedMethodName = result.typeConverter.getValidJavaMethodName(StringUtils.implode(ns, result.config.cPlusPlusNameSpaceSeparator) + (ns.isEmpty() ? "" : result.config.cPlusPlusNameSpaceSeparator) + functionName);
			}
			Set<String> names = new HashSet<String>();
			//if (ns.isEmpty())
			
			if (function.getType() == Type.CppMethod && !function.getModifiers().contains(Modifier.Static))
				return;
			
			if (result.config.features.contains(JNAeratorConfig.GenFeatures.CPlusPlusMangling))
				addCPlusPlusMangledNames(function, names);
			
			if (!modifiedMethodName.equals(functionName) && ns.isEmpty())
				names.add(functionName);
			
			if (!names.isEmpty())
				natFunc.addAnnotation(new Annotation(Name.class, "({\"" + StringUtils.implode(names, "\", \"") + "\"})"));

			//if (isCallback || !modifiedMethodName.equals(functionName))
			//	natFunc.addAnnotation(new Annotation(Name.class, "(value=\"" + functionName + "\"" + (ns.isEmpty() ? "" : ", namespace=" + namespaceArrayStr)  + (isMethod ? ", classMember=true" : "") + ")"));
			
			natFunc.setName(modifiedMethodName);
			natFunc.setValueType(result.typeConverter.convertTypeToJNA(returnType, TypeConversionMode.ReturnType, callerLibraryClass));
			natFunc.importDetails(function, false);
			natFunc.moveAllCommentsBefore();
			if (!isCallback)
				natFunc.addToCommentBefore(getFileCommentContent(function));
			
			boolean alternativeOutputs = !isCallback;
			
			Function primFunc = alternativeOutputs ? natFunc.clone() : null;
			Function bufFunc = alternativeOutputs ? natFunc.clone() : null;
			
			Set<String> argNames = new TreeSet<String>();
			for (Arg arg : function.getArgs())
				if (arg.getName() != null) 
					argNames.add(arg.getName());
				
			int iArg = 1;
			for (Arg arg : function.getArgs()) {
				if (arg.isVarArg() && arg.getValueType() == null) {
					//TODO choose vaname dynamically !
					String vaType = isObjectiveC ? "NSObject" : "Object";
					String argName = chooseJavaArgName("varargs", iArg, argNames);
					natFunc.addArg(new Arg(argName, new TypeRef.SimpleTypeRef(vaType))).setVarArg(true);
					if (alternativeOutputs) {
						primFunc.addArg(new Arg(argName, new TypeRef.SimpleTypeRef(vaType))).setVarArg(true);
						bufFunc.addArg(new Arg(argName, new TypeRef.SimpleTypeRef(vaType))).setVarArg(true);
					}
				} else {
					String argName = chooseJavaArgName(arg.getName(), iArg, argNames);
					
					TypeRef mutType = arg.createMutatedType();
					
					if (mutType.toString().contains("NSOpenGLContextParameter")) {
						argName = argName.toString();
					}
					natFunc.addArg(new Arg(argName, result.typeConverter.convertTypeToJNA(mutType, TypeConversionMode.NativeParameter, callerLibraryClass)));
					if (alternativeOutputs) {
						primFunc.addArg(new Arg(argName, result.typeConverter.convertTypeToJNA(mutType, TypeConversionMode.PrimitiveParameter, callerLibraryClass)));
						bufFunc.addArg(new Arg(argName, result.typeConverter.convertTypeToJNA(mutType, TypeConversionMode.BufferParameter, callerLibraryClass)));
					}
				}
				iArg++;
			}
			
			String natSign = natFunc.computeSignature(false),
				primSign = alternativeOutputs ? primFunc.computeSignature(false) : null,
				bufSign = alternativeOutputs ? bufFunc.computeSignature(false) : null;
				
			if (signatures.add(natSign)) {
				if (alternativeOutputs && !primSign.equals(natSign)) {
					if (primSign.equals(bufSign))
						natFunc.addToCommentBefore(Arrays.asList("@deprecated use the safer method {@link #" + primSign + "} instead"));
					else
						natFunc.addToCommentBefore(Arrays.asList("@deprecated use the safer methods {@link #" + primSign + "} and {@link #" + bufSign + "} instead"));
					natFunc.addAnnotation(new Annotation(Deprecated.class));
				}
				collectParamComments(natFunc);
				out.addDeclaration(natFunc);
			}
			
			if (alternativeOutputs) {
				if (signatures.add(primSign)) {
					collectParamComments(primFunc);
					out.addDeclaration(primFunc);
				}
				if (signatures.add(bufSign)) {
					collectParamComments(bufFunc);
					out.addDeclaration(bufFunc);
				}
			}
		} catch (UnsupportedConversionException ex) {
			out.addDeclaration(new EmptyDeclaration(getFileCommentContent(function), ex.toString()));
		}
	}

	protected boolean isCPlusPlusFileName(String file) {
		if (file == null)
			return true;
			
		file = file.toLowerCase();
		return !file.endsWith(".c") && !file.endsWith(".m");
	}
	private void addCPlusPlusMangledNames(Function function, Set<String> names) {
		if (function.getType() == Type.ObjCMethod)
			return;
		
		ExternDeclarations externDeclarations = function.findParentOfType(ExternDeclarations.class);
		if (externDeclarations != null && !"C++".equals(externDeclarations.getLanguage()))
			return;
		
		if (!isCPlusPlusFileName(Element.getFileOfAscendency(function)))
			return;
		
		for (CPlusPlusMangler mangler : result.config.cPlusPlusManglers) {
			try {
				names.add(mangler.mangle(function, result));
			} catch (Exception ex) {
				System.err.println("Error in mangling of '" + function.computeSignature(true) + "' : " + ex);
			}
		}
	}

	private void collectParamComments(Function f) {
		for (Arg arg : f.getArgs()) {
			arg.moveAllCommentsBefore();
			TypeRef argType = arg.getValueType();
			if (argType != null) {
				argType.moveAllCommentsBefore();
				arg.addToCommentBefore(argType.getCommentBefore());
				argType.stripDetails();
			}
			if (arg.getCommentBefore() != null) {
				f.addToCommentBefore("@param " + arg.getName() + " " + Element.cleanComment(arg.getCommentBefore()));
				arg.stripDetails();
			}
		}
	}

	void convertFunctions(List<Function> functions, Set<String> signatures, DeclarationsHolder out, String callerLibraryClass) {
		if (functions != null) {
			//System.err.println("FUNCTIONS " + functions);
			for (Function function : functions) {
				convertFunction(function, signatures, false, out, callerLibraryClass);
			}
		}
	}

	public String getActualTaggedTypeName(TaggedTypeRef struct) {
		String structName = null;
		String tag = struct.getTag();
		if (tag == null || tag.startsWith("_")) {
			TypeDef parentDef = as(struct.getParentElement(), TypeDef.class);
			if (parentDef != null) {
				structName = JNAeratorUtils.findBestPlainStorageName(parentDef);
			}
		}
		if (structName == null)
			structName = tag;
		return structName;
	}
	void convertStruct(Struct struct, Set<String> signatures, DeclarationsHolder out, String callerLibraryClass) {
		String structName = getActualTaggedTypeName(struct);
		if (structName == null)
			return;
		
		if (struct.isForwardDeclaration() && !result.structsByName.get(structName).isForwardDeclaration())
			return;
		
		String signature = "struct " + structName;
		if (!signatures.add(signature))
			return;
		
		String baseClass = (struct.getType() == Struct.Type.CUnion ? Union.class : Structure.class).getName();
		
		final Struct structJavaClass = publicStaticClass(structName, baseClass, Struct.Type.JavaClass, struct);
		Struct byRef = publicStaticClass("ByReference", structName, Struct.Type.JavaClass, null, Structure.class.getName() + ".ByReference");
		Struct byVal = publicStaticClass("ByValue", structName, Struct.Type.JavaClass, null, Structure.class.getName() + ".ByValue");
		
		if (result.config.features.contains(GenFeatures.StructConstructors))
			addStructConstructors(structName, structJavaClass, byRef, byVal);
		
		structJavaClass.addDeclaration(decl(byRef));
		structJavaClass.addDeclaration(decl(byVal));
		
		final int iChild[] = new int[] {0};
		
		//cl.addDeclaration(new EmptyDeclaration())
		Set<String> childSignatures = new TreeSet<String>();
		//List<Declaration> children = new ArrayList<Declaration>();
		for (Declaration d : struct.getDeclarations()) {
			if (d instanceof VariablesDeclaration) {
				convertVariablesDeclaration((VariablesDeclaration)d, structJavaClass, iChild, callerLibraryClass);
			} else if (d instanceof TaggedTypeRefDeclaration) {
				TaggedTypeRef tr = ((TaggedTypeRefDeclaration) d).getTaggedTypeRef();
				if (tr instanceof Struct) {
					convertStruct((Struct)tr, childSignatures, structJavaClass, callerLibraryClass);
				} else if (tr instanceof Enum) {
					convertEnum((Enum)tr, childSignatures, structJavaClass, callerLibraryClass);
				}
			} else if (d instanceof TypeDef) {
				TypeDef td = (TypeDef)d;
				TypeRef tr = td.getValueType();
				if (tr instanceof Struct) {
					convertStruct((Struct)tr, childSignatures, structJavaClass, callerLibraryClass);
				} else if (tr instanceof FunctionSignature) {
					convertCallback((FunctionSignature)tr, childSignatures, structJavaClass, callerLibraryClass);
				}
			}
		}
		//structJavaClass.addDeclarations(children);
		out.addDeclaration(decl(structJavaClass));
	}

	public void convertStructs(List<Struct> structs, Set<String> signatures, DeclarationsHolder out, String libraryClassName) {
		if (structs != null) {
			for (Struct struct : structs) {
				if (struct.findParentOfType(Struct.class) != null)
					continue;
					
				convertStruct(struct, signatures, out, libraryClassName);
			}
		}
	}

	public VariablesDeclaration convertVariablesDeclaration(String name, TypeRef mutatedType, int[] iChild, String callerLibraryName, Element... toImportDetailsFrom) throws UnsupportedConversionException {
		name = result.typeConverter.getValidJavaArgumentName(name);
		//convertVariablesDeclaration(name, mutatedType, out, iChild, callerLibraryName);

		Expression initVal = null;
		TypeRef  javaType = result.typeConverter.convertTypeToJNA(
			mutatedType, 
			TypeConversion.TypeConversionMode.FieldType,
			callerLibraryName
		);
		mutatedType = result.typeConverter.resolveTypeDef(mutatedType, callerLibraryName, true);
		
		VariablesDeclaration convDecl = new VariablesDeclaration();
		convDecl.addModifiers(Modifier.Public);
		
		if (javaType instanceof ArrayRef && mutatedType instanceof ArrayRef) {
			ArrayRef mr = (ArrayRef)mutatedType;
			ArrayRef jr = (ArrayRef)javaType;
			Expression mul = null;
			List<Expression> dims = mr.flattenDimensions();
			for (int i = dims.size(); i-- != 0;) {
				Expression x = dims.get(i);
			
				if (x == null || x instanceof EmptyArraySize) {
					javaType = jr = new ArrayRef(TypeConversion.typeRef(Pointer.class));
					break;
				} else {
					Expression c = result.typeConverter.convertExpressionToJava(x, callerLibraryName);
					c.setParenthesis(dims.size() == 1);
					if (mul == null)
						mul = c;
					else
						mul = new Expression.BinaryOp(BinaryOperator.Multiply, c, mul);
				}
			}
			initVal = new Expression.NewArray(jr.getTarget(), mul);
		}
		if (javaType == null) {
			throw new UnsupportedConversionException(mutatedType, "failed to convert type to Java");
		} else if (javaType.toString().equals("void")) {
			throw new UnsupportedConversionException(mutatedType, "void type !");
			//out.add(new EmptyDeclaration("SKIPPED:", v.formatComments("", true, true, false), v.toString()));
		} else {
			for (Element e : toImportDetailsFrom)
				convDecl.importDetails(e, false);
			convDecl.importDetails(mutatedType, true);
			convDecl.importDetails(javaType, true);
			
//			convDecl.importDetails(v, false);
//			convDecl.importDetails(vs, false);
//			convDecl.importDetails(valueType, false);
//			valueType.stripDetails();
			convDecl.moveAllCommentsBefore();
			convDecl.setValueType(javaType);
			convDecl.addDeclarator(new DirectDeclarator(name, initVal));
			
			return convDecl;//out.addDeclaration(convDecl);
		}
	}
	public void convertVariablesDeclaration(VariablesDeclaration v, DeclarationsHolder out, int[] iChild, String callerLibraryName) {
		//List<Declaration> out = new ArrayList<Declaration>();
		try {
			TypeRef valueType = v.getValueType();
			for (Declarator vs : v.getDeclarators()) {
				String name = vs.resolveName();
				if (name == null || name.length() == 0)
					continue;
	
				TypeRef mutatedType = valueType;
				if (!(vs instanceof DirectDeclarator))
				{
					mutatedType = (TypeRef)vs.mutateType(valueType);
					vs = new DirectDeclarator(vs.resolveName());
				}
				VariablesDeclaration vd = convertVariablesDeclaration(name, mutatedType, iChild, callerLibraryName, v, vs);
				if (vd != null)
					out.addDeclaration(vd);
				iChild[0]++;
			}
		} catch (UnsupportedConversionException e) {
			out.addDeclaration(new EmptyDeclaration(e.toString()));
		}
	}
	Struct publicStaticClass(String name, String parentName, Struct.Type type, Element toCloneCommentsFrom, String... interfaces) {
		Struct cl = new Struct();
		cl.setType(type);
		cl.setTag(name);
		if (parentName != null)
			cl.setParents(parentName);
		if (type == Struct.Type.JavaInterface)
			for (String inter : interfaces)
				cl.addParent(inter);
		else
		cl.setProtocols(Arrays.asList(interfaces));
		if (toCloneCommentsFrom != null ) {
			cl.importDetails(toCloneCommentsFrom, false);
			cl.moveAllCommentsBefore();
			cl.addToCommentBefore(getFileCommentContent(toCloneCommentsFrom));
		}
		cl.addModifiers(Modifier.Public, Modifier.Static);
		return cl;
	}
	private void addStructConstructors(String structName, Struct structJavaClass, Struct byRef,
			Struct byVal) {
		Function emptyConstructor = new Function(Function.Type.JavaMethod, structName, null).addModifiers(Modifier.Public);
		emptyConstructor.setBody(new Statement.Block());
		
		emptyConstructor.setCommentBefore("Allocate a new " + structName + ".ByRef struct on the heap");
		addConstructor(byRef, emptyConstructor);
		
		emptyConstructor = emptyConstructor.clone();
		emptyConstructor.setCommentBefore("Allocate a new " + structName + ".ByVal struct on the heap");
		addConstructor(byVal, emptyConstructor);
		
		Function pointerConstructor = new Function(Function.Type.JavaMethod, structName, null, 
			new Arg("pointer", new TypeRef.SimpleTypeRef(Pointer.class.getName())),
			new Arg("offset", new TypeRef.Primitive("int"))
		).addModifiers(Modifier.Public);
		
		//pointerConstructor.setCommentBefore("Cast data at given memory location (pointer + offset) as an existing " + structName + ".ByRef struct");
		pointerConstructor.setBody(new Statement.Block(
				new Statement.ExpressionStatement(new Expression.FunctionCall("super", new Expression.VariableRef("pointer"), new Expression.VariableRef("offset")))
		).setCompact(true));
		//byRef.addDeclaration(pointerConstructor);
		//pointerConstructor = pointerConstructor.clone();
		//pointerConstructor.setCommentBefore("Cast data at given memory location (pointer + offset) as an existing " + structName + ".ByValue struct");
		//byVal.addDeclaration(pointerConstructor);
		
		Function shareMemConstructor = new Function(Function.Type.JavaMethod, structName, null, 
			new Arg("struct", new TypeRef.SimpleTypeRef(structName))
		).addModifiers(Modifier.Public);
		
		shareMemConstructor.setBody(new Statement.Block(
			new Statement.ExpressionStatement(new Expression.FunctionCall("super", new Expression.FunctionCall(new Expression.VariableRef("struct"), "getPointer", MemberRefStyle.Dot), new Expression.Constant(Constant.Type.Int, 0)))
		).setCompact(true));
		shareMemConstructor.setCommentBefore("Create an instance that shares its memory with another " + structName + " instance");
		addConstructor(byRef, shareMemConstructor);
		shareMemConstructor = shareMemConstructor.clone();
		addConstructor(byVal, shareMemConstructor);
	
		emptyConstructor = emptyConstructor.clone();
		emptyConstructor.setCommentBefore("Allocate a new " + structName + " struct on the heap");
		addConstructor(structJavaClass, emptyConstructor);
		
		pointerConstructor = pointerConstructor.clone();
		pointerConstructor.setCommentBefore("Cast data at given memory location (pointer + offset) as an existing " + structName + " struct");
		pointerConstructor.setBody(new Statement.Block(
				new Statement.ExpressionStatement(new Expression.FunctionCall("super")),
				new Statement.ExpressionStatement(new Expression.FunctionCall("useMemory", new Expression.VariableRef("pointer"), new Expression.VariableRef("offset"))),
				new Statement.ExpressionStatement(new Expression.FunctionCall("read"))
		));
		addConstructor(structJavaClass, pointerConstructor);
		shareMemConstructor = shareMemConstructor.clone();
		shareMemConstructor.setBody(new Statement.Block(
			new Statement.ExpressionStatement(new Expression.FunctionCall("this", new Expression.FunctionCall(new Expression.VariableRef("struct"), "getPointer", MemberRefStyle.Dot), new Expression.Constant(Constant.Type.Int, 0)))
		).setCompact(true));
		addConstructor(structJavaClass, shareMemConstructor);
	}
	
	void addConstructor(Struct s, Function f) {
		String structName = getActualTaggedTypeName(s);
		
		f.setName(structName);
		s.addDeclaration(f);
	}
	
	String getFileCommentContent(File file, Element e) {
		if (file != null) {
			String path = result.config.relativizeFileForSourceComments(file.getAbsolutePath());
			String inCategoryStr = "";
			if (e instanceof Function) {
				Function fc = (Function)e;
				Struct parent;
				if (fc.getType() == Type.ObjCMethod && ((parent = as(fc.getParentElement(), Struct.class)) != null) && (parent.getCategoryName() != null)) {
					inCategoryStr = "from " + parent.getCategoryName() + " ";
				}
			}
			return "<i>" + inCategoryStr + "native declaration : " + path + (e == null || e.getElementLine() < 0 ? "" : ":" + e.getElementLine()) + "</i>";
		}
		return null;
	}
	String getFileCommentContent(Element e) {
		String f = Element.getFileOfAscendency(e);
		return f == null ? null : getFileCommentContent(new File(f), e);
	}
	

	public List<Define> reorderDefines(Collection<Define> defines) {
		List<Define> reordered = new ArrayList<Define>(defines.size());
		HashSet<String> added = new HashSet<String>(), all = new HashSet<String>();
		
		
		Map<String, Pair<Define, Set<String>>> pending = new HashMap<String, Pair<Define, Set<String>>>();
		for (Define define : defines) {
			Set<String> dependencies = new TreeSet<String>();
			computeVariablesDependencies(define.getValue(), dependencies);
			all.add(define.getName());
			if (dependencies.isEmpty()) {
				reordered.add(define);
				added.add(define.getName());
			} else {
				pending.put(define.getName(), new Pair<Define, Set<String>>(define, dependencies));
			}	
		}
		
		for (int i = 3; i-- != 0 && !pending.isEmpty();) {
			for (Iterator<Map.Entry<String, Pair<Define, Set<String>>>> it = pending.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Pair<Define, Set<String>>> e = it.next(); 
				Set<String> dependencies = e.getValue().getSecond();
				String name = e.getKey();
				boolean missesDep = false;
				for (String dependency : dependencies) {
					if (!added.contains(dependency)) {
						missesDep = true;
						if (!all.contains(dependency)) {
							it.remove();
							all.remove(name);
						}
						
						break;
					}
				}
				if (missesDep)
					continue;
				
				it.remove();
				reordered.add(e.getValue().getFirst());
			}
		}
		
		return reordered;
	}
	public void computeVariablesDependencies(Element e, final Set<String> names) {
		e.accept(new Scanner() {
			@Override
			public void visitSimpleTypeRef(SimpleTypeRef simpleTypeRef) {
				names.add(simpleTypeRef.getName());
			}
		});
	}
	
	private String chooseJavaArgName(String name, int iArg, Set<String> names) {
		String baseArgName = result.typeConverter.getValidJavaArgumentName(name);
		int i = 1;
		if (baseArgName == null)
			baseArgName = "arg";
		
		String argName;
		do {
			argName = baseArgName + iArg + (i == 1 ? "" : i + "");
			i++;
		} while (names.contains(argName) || !TypeConversion.isValidJavaIdentifier(argName));
		return argName;
	}

	TaggedTypeRefDeclaration publicStaticClassDecl(String name, String parentName, Struct.Type type, Element toCloneCommentsFrom, String... interfaces) {
		return decl(publicStaticClass(name, parentName, type, toCloneCommentsFrom, interfaces));
	}
	TaggedTypeRefDeclaration decl(TaggedTypeRef tr) {
		return new TaggedTypeRefDeclaration(tr);
	}
	
}

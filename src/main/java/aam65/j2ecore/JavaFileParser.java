package aam65.j2ecore;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.eclipse.emf.ecore.*;

import java.io.IOException;
import java.nio.file.Path;

public class JavaFileParser {
    private final EcoreModelManager modelManager;

    public JavaFileParser(EcoreModelManager modelManager) {
        this.modelManager = modelManager;
    }

    public void parseFile(Path filePath) throws IOException {
        CharStream codeCharStream = CharStreams.fromPath(filePath);
        JavaLexer lexer = new JavaLexer(codeCharStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);

        ParseTree tree = parser.compilationUnit();
        processTree(tree);
        System.out.println(tree.toStringTree(parser));
    }

    private void processTree(ParseTree tree) {
        if (tree instanceof JavaParser.ClassDeclarationContext) {
            processClass((JavaParser.ClassDeclarationContext) tree);
        } else if (tree instanceof JavaParser.InterfaceDeclarationContext) {
            processInterface((JavaParser.InterfaceDeclarationContext) tree);
        } else if (tree instanceof JavaParser.EnumDeclarationContext) {
            processEnum((JavaParser.EnumDeclarationContext) tree);
        }

        for (int i = 0; i < tree.getChildCount(); i++) {
            processTree(tree.getChild(i));
        }
    }

    private void processClass(JavaParser.ClassDeclarationContext classDecl) {
        String className = classDecl.identifier().getText();
        EClass eClass = modelManager.addClass(className);

        // Handle superclass
        if (classDecl.EXTENDS() != null && classDecl.typeType() != null) {
            String superclassName = modelManager.getTypeName(classDecl.typeType());
            EClass superclass = modelManager.getEClassByName(superclassName);
            if (superclass != null) {
                eClass.getESuperTypes().add(superclass);
            }
        }

        // Handle interfaces
        if (classDecl.IMPLEMENTS() != null) {
            classDecl.typeList().forEach(typeListContext -> typeListContext.typeType().forEach(typeTypeContext -> {
                String interfaceName = modelManager.getTypeName(typeTypeContext);
                EClass interfaceClass = modelManager.getEClassByName(interfaceName);
                if (interfaceClass != null) {
                    eClass.getESuperTypes().add(interfaceClass);
                }
            }));
        }

        classDecl.classBody().classBodyDeclaration().forEach(declaration -> {
            if (declaration.memberDeclaration() != null) {
                JavaParser.MemberDeclarationContext memberCtx = declaration.memberDeclaration();
                if (memberCtx.fieldDeclaration() != null) {
                    extractFields(memberCtx.fieldDeclaration(), eClass);
                } else if (memberCtx.methodDeclaration() != null) {
                    extractMethods(memberCtx.methodDeclaration(), eClass);
                }
            }
        });
    }

    private void processInterface(JavaParser.InterfaceDeclarationContext interfaceDecl) {
        String interfaceName = interfaceDecl.identifier().getText();
        EClass eInterface = modelManager.addInterface(interfaceName);

        // Handle extended interfaces
        if (interfaceDecl.EXTENDS() != null) {
            interfaceDecl.typeList().forEach(typeListContext -> typeListContext.typeType().forEach(typeTypeContext -> {
                String extendedInterfaceName = modelManager.getTypeName(typeTypeContext);
                EClass extendedInterface = modelManager.getEClassByName(extendedInterfaceName);
                if (extendedInterface != null) {
                    eInterface.getESuperTypes().add(extendedInterface);
                }
            }));
        }
    }

    private void processEnum(JavaParser.EnumDeclarationContext enumDecl) {
        String enumName = enumDecl.identifier().getText();
        EEnum eEnum = modelManager.addEnum(enumName);

        int ordinal = 0;
        for (JavaParser.EnumConstantContext enumConstant : enumDecl.enumConstants().enumConstant()) {
            String enumConstantName = enumConstant.identifier().getText();
            modelManager.addEnumLiteral(eEnum, enumConstantName, ordinal++);
        }
    }

    private void extractFields(JavaParser.FieldDeclarationContext fieldCtx, EClass eClass) {
        String fieldName = fieldCtx.variableDeclarators().variableDeclarator(0).variableDeclaratorId().getText();
        String fieldType = fieldCtx.typeType().getText();

        // Determine if the field is a collection type and extract the generic type if it is
        boolean isCollection = fieldType.matches("List<.*>|Set<.*>|Map<.*,.*>");
        String genericType = isCollection ? fieldType.replaceAll(".*<(.*)>.*", "$1") : fieldType;

        EClassifier fieldTypeClassifier = modelManager.getEClassifierByName(genericType);

        if (fieldTypeClassifier instanceof EClass) {
            // Associations or Compositions
            // biderctionality, unique, ordered, opposite
            int lowerBound = 0; // Assume optional by default
            int upperBound = isCollection ? -1 : 1; // Multiple for collections, single otherwise
            boolean isContainment = !isCollection; // Assume containment for non-collection types

            // Add the reference with correct containment
            modelManager.addReference(eClass, fieldName, (EClass)fieldTypeClassifier, lowerBound, upperBound, isContainment);
        } else {
            // Attributes (primitive types or data types)
            EDataType dataType = fieldTypeClassifier instanceof EDataType ? (EDataType) fieldTypeClassifier : EcorePackage.Literals.ESTRING;
            modelManager.addAttribute(eClass, fieldName, dataType);
        }
    }

    private void extractMethods(JavaParser.MethodDeclarationContext methodCtx, EClass eClass) {
        String methodName = methodCtx.identifier().getText();
        String adjustedMethodName = modelManager.adjustOperationName(methodName, eClass);
        EOperation eOperation = modelManager.addOperation(eClass, adjustedMethodName);


        JavaParser.FormalParametersContext formalParametersCtx = methodCtx.formalParameters();
        if (formalParametersCtx != null && formalParametersCtx.formalParameterList() != null) {
            for (JavaParser.FormalParameterContext paramCtx : formalParametersCtx.formalParameterList().formalParameter()) {
                String paramName = paramCtx.variableDeclaratorId().getText();
                String paramType = modelManager.getTypeName(paramCtx.typeType());
                EClassifier eParamType = modelManager.getEClassifierByName(paramType);
                modelManager.addParameterToOperation(eOperation, paramName, eParamType);
            }
        }

        // Handle the return type
        JavaParser.TypeTypeOrVoidContext returnTypeCtx = methodCtx.typeTypeOrVoid();
        if (returnTypeCtx != null) {
            if (returnTypeCtx.VOID() != null) {
                eOperation.setEType(null);
            } else if (returnTypeCtx.typeType() != null) {
                String returnType = modelManager.getTypeName(returnTypeCtx.typeType());
                Object resolvedReturnType = modelManager.resolveReturnType(returnType);

                // Check the instance of the resolved return type and set it appropriately
                if (resolvedReturnType instanceof EGenericType) {
                    eOperation.setEGenericType((EGenericType) resolvedReturnType);
                } else if (resolvedReturnType instanceof EClassifier) {
                    eOperation.setEType((EClassifier) resolvedReturnType);
                }
            }
        }
    }
}

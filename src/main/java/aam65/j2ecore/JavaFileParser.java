package aam65.j2ecore;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.eclipse.emf.ecore.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Handle Annotations
        for (JavaParser.ClassBodyDeclarationContext bodyDecl : classDecl.classBody().classBodyDeclaration()) {
            if (bodyDecl.memberDeclaration() != null) {
                for (JavaParser.ModifierContext modCtx : bodyDecl.modifier()) {
                    if (modCtx.classOrInterfaceModifier() != null &&
                            modCtx.classOrInterfaceModifier().annotation() != null) {
                        handleAnnotation(modCtx.classOrInterfaceModifier().annotation(), eClass);
                    }
                }
            }
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
        List<String> extendedInterfaceNames = new ArrayList<>();

        // Handle extended interfaces
        if (interfaceDecl.EXTENDS() != null) {
            interfaceDecl.typeList().forEach(typeListContext -> typeListContext.typeType().forEach(typeTypeContext -> {
                String extendedInterfaceName = modelManager.getTypeName(typeTypeContext);
                extendedInterfaceNames.add(extendedInterfaceName);
            }));
        }

        EClass eInterface = modelManager.addInterface(interfaceName, extendedInterfaceNames);

        // Process interface methods
        interfaceDecl.interfaceBody().interfaceBodyDeclaration().forEach(declaration -> {
            if (declaration.interfaceMemberDeclaration() != null && declaration.interfaceMemberDeclaration().interfaceMethodDeclaration() != null) {
                extractInterfaceMethods(declaration.interfaceMemberDeclaration().interfaceMethodDeclaration(), eInterface);
            }
        });
    }

    private void extractInterfaceMethods(JavaParser.InterfaceMethodDeclarationContext methodCtx, EClass eInterface) {
        String methodName = methodCtx.interfaceCommonBodyDeclaration().identifier().getText();
        String adjustedMethodName = modelManager.adjustOperationName(methodName, eInterface);
        EOperation eOperation = modelManager.addOperation(eInterface, adjustedMethodName);

        // Extracting formal parameters
        JavaParser.FormalParametersContext formalParametersCtx = methodCtx.interfaceCommonBodyDeclaration().formalParameters();
        extractFormalParameters(eOperation, formalParametersCtx);

        // Handling the return type
        JavaParser.TypeTypeOrVoidContext returnTypeCtx = methodCtx.interfaceCommonBodyDeclaration().typeTypeOrVoid();
        if (returnTypeCtx != null) {
            if (returnTypeCtx.VOID() != null) {
                eOperation.setEType(null); // void return type
            } else if (returnTypeCtx.typeType() != null) {
                String returnType = modelManager.getTypeName(returnTypeCtx.typeType());
                Object resolvedReturnType = modelManager.resolveReturnType(returnType);
                if (resolvedReturnType instanceof EGenericType) {
                    eOperation.setEGenericType((EGenericType) resolvedReturnType);
                } else if (resolvedReturnType != null) {
                    eOperation.setEType((EClassifier) resolvedReturnType);
                }
            }
        }
    }

    private void extractFormalParameters(EOperation eOperation, JavaParser.FormalParametersContext formalParametersCtx) {
        if (formalParametersCtx != null && formalParametersCtx.formalParameterList() != null) {
            for (JavaParser.FormalParameterContext paramCtx : formalParametersCtx.formalParameterList().formalParameter()) {
                String paramName = paramCtx.variableDeclaratorId().getText();
                String paramType = modelManager.getTypeName(paramCtx.typeType());
                EClassifier eParamType = modelManager.getEClassifierByName(paramType);
                if (eParamType != null) {
                    modelManager.addParameterToOperation(eOperation, paramName, eParamType);
                } else {
                    // Handle the case where eParamType is null, indicating a missing type
                    System.err.println("Error: Type " + paramType + " not found for parameter " + paramName);
                }
            }
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

        EClassifier fieldTypeClassifier = modelManager.getEClassifierByName(fieldType);
        if (fieldTypeClassifier instanceof EDataType) {
            modelManager.addAttribute(eClass, fieldName, (EDataType) fieldTypeClassifier);
        } else if (fieldTypeClassifier instanceof EClass) {
            // Assuming all references are containment for simplification
            modelManager.addReferenceInfo(eClass, fieldType, fieldName, true);
        } else {
            // Fallback for unknown types, treat as EObject
            EClassifier fallbackType = EcorePackage.Literals.EOBJECT;
            modelManager.addAttribute(eClass, fieldName, (EDataType) fallbackType);
        }
    }

    private void extractMethods(JavaParser.MethodDeclarationContext methodCtx, EClass eClass) {
        String methodName = methodCtx.identifier().getText();
        String adjustedMethodName = modelManager.adjustOperationName(methodName, eClass);
        EOperation eOperation = modelManager.addOperation(eClass, adjustedMethodName);

        JavaParser.FormalParametersContext formalParametersCtx = methodCtx.formalParameters();
        extractFormalParameters(eOperation, formalParametersCtx);

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

    private void handleAnnotation(JavaParser.AnnotationContext annotationCtx, EModelElement eModelElement) {
        String annotationName = annotationCtx.qualifiedName().getText();
        Map<String, String> elements = new HashMap<>();

        JavaParser.ElementValuePairsContext pairsCtx = annotationCtx.elementValuePairs();
        if (pairsCtx != null) {
            System.out.println("Adding EAnnotation");
            for (JavaParser.ElementValuePairContext pair : pairsCtx.elementValuePair()) {
                // Use the identifier() method from ElementValuePairContext
                String key = pair.identifier().getText();
                // Use the elementValue() method from ElementValuePairContext
                String value = pair.elementValue().getText();
                elements.put(key, value);
            }
        }

        EAnnotation eAnnotation = modelManager.createEAnnotation(annotationName, elements);
        modelManager.addEAnnotationToElement(eModelElement, eAnnotation);
    }
}

package aam65.j2ecore;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.resource.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EcoreModelManager {
    private final EPackage ePackage;
    private final EcoreFactory ecoreFactory;
    private final EcoreUtils ecoreUtils = new EcoreUtils();

    public EcoreModelManager() {
        ecoreFactory = EcoreFactory.eINSTANCE;
        ePackage = ecoreFactory.createEPackage();

        // Initialize the ResourceSet and Resource
        ResourceSetImpl resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
        Resource resource = resourceSet.createResource(URI.createURI("ModelURI.ecore"));
        resource.getContents().add(ePackage);
    }

    public void setPackageName(String packageName) {
        ePackage.setName(packageName);
        ePackage.setNsPrefix(packageName.toLowerCase());
        ePackage.setNsURI("https://www.example.org/" + packageName);
    }

    public EClass addClass(String className) {
        EClass eClass = ecoreFactory.createEClass();
        eClass.setName(className);
        ePackage.getEClassifiers().add(eClass);
        return eClass;
    }

    public EClass addInterface(String interfaceName, List<String> superInterfaceNames) {
        EClass eInterface = ecoreFactory.createEClass();
        eInterface.setName(interfaceName);
        eInterface.setInterface(true);
        eInterface.setAbstract(true);

        for (String superInterfaceName : superInterfaceNames) {
            EClass superInterface = getEClassByName(superInterfaceName);
            if (superInterface != null && superInterface.isInterface()) {
                eInterface.getESuperTypes().add(superInterface);
            }
        }

        ePackage.getEClassifiers().add(eInterface);
        return eInterface;
    }

    public void addAttribute(EClass eClass, String attributeName, EDataType dataType) {
        EAttribute eAttribute = ecoreFactory.createEAttribute();
        eAttribute.setName(attributeName);
        eAttribute.setEType(dataType);
        eClass.getEStructuralFeatures().add(eAttribute);
    }

    public EOperation addOperation(EClass eClass, String operationName) {
        EOperation eOperation = ecoreFactory.createEOperation();
        eOperation.setName(operationName);
        eClass.getEOperations().add(eOperation);
        return eOperation;
    }

    public EPackage getEPackage() {
        return ePackage;
    }

    public EClass getEClassByName(String className) {
        for (EClassifier classifier : ePackage.getEClassifiers()) {
            if (classifier instanceof EClass && classifier.getName().equals(className)) {
                return (EClass) classifier;
            }
        }
        return null;
    }

    public EEnum addEnum(String enumName) {
        EEnum eEnum = ecoreFactory.createEEnum();
        eEnum.setName(enumName);
        ePackage.getEClassifiers().add(eEnum);
        return eEnum;
    }

    public void addEnumLiteral(EEnum eEnum, String literalName, int value) {
        EEnumLiteral eEnumLiteral = ecoreFactory.createEEnumLiteral();
        eEnumLiteral.setName(literalName);
        eEnumLiteral.setValue(value);
        eEnum.getELiterals().add(eEnumLiteral);
    }

    public String getTypeName(JavaParser.TypeTypeContext typeCtx) {
        if (typeCtx == null) {
            return "EObject"; // Default type when no specific type is provided
        }

        if (typeCtx.classOrInterfaceType() != null) {
            JavaParser.ClassOrInterfaceTypeContext classOrInterfaceType = typeCtx.classOrInterfaceType();
            String baseType = getClassOrInterfaceTypeName(classOrInterfaceType);
            List<String> typeArgs = new ArrayList<>();

            for (JavaParser.TypeArgumentsContext typeArgsCtx : classOrInterfaceType.typeArguments()) {
                for (JavaParser.TypeArgumentContext typeArgCtx : typeArgsCtx.typeArgument()) {
                    typeArgs.add(getTypeNameForTypeArgument(typeArgCtx));
                }
            }
            return typeArgs.isEmpty() ? baseType : baseType + "<" + String.join(", ", typeArgs) + ">";
        } else if (typeCtx.primitiveType() != null) {
            return mapPrimitiveTypeToEcore(typeCtx.primitiveType().getText());
        } else {
            return "EObject"; // Default for types that are neither class/interface nor primitives
        }
    }

    private String mapPrimitiveTypeToEcore(String primitiveType) {
        return switch (primitiveType) {
            case "int" -> EcorePackage.Literals.EINT.getName();
            case "boolean" -> EcorePackage.Literals.EBOOLEAN.getName();
            case "byte" -> EcorePackage.Literals.EBYTE.getName();
            case "short" -> EcorePackage.Literals.ESHORT.getName();
            case "long" -> EcorePackage.Literals.ELONG.getName();
            case "float" -> EcorePackage.Literals.EFLOAT.getName();
            case "double" -> EcorePackage.Literals.EDOUBLE.getName();
            case "char" -> EcorePackage.Literals.ECHAR.getName();
            case "String" -> EcorePackage.Literals.ESTRING.getName();
            default -> primitiveType; // Should not happen if all cases are covered
        };
    }

    private String getTypeNameForTypeArgument(JavaParser.TypeArgumentContext typeArg) {
        // Check if the type argument is a wildcard type
        if (typeArg.QUESTION() != null) {
            StringBuilder wildcardType = new StringBuilder("?");
            if (typeArg.EXTENDS() != null) {
                wildcardType.append(" extends ").append(getTypeName(typeArg.typeType()));
            } else if (typeArg.SUPER() != null) {
                wildcardType.append(" super ").append(getTypeName(typeArg.typeType()));
            }
            return wildcardType.toString();
        } else if (typeArg.typeType() != null) {
            // Handle regular types
            return getTypeName(typeArg.typeType());
        }
        return ""; // Fallback
    }

    public String getClassOrInterfaceTypeName(JavaParser.ClassOrInterfaceTypeContext ctx) {
        List<String> parts = new ArrayList<>();
        for (JavaParser.IdentifierContext idCtx : ctx.identifier()) {
            parts.add(idCtx.getText());
        }
        if (ctx.typeIdentifier() != null) {
            parts.add(ctx.typeIdentifier().getText());
        }
        return String.join(".", parts);
    }

    public void addParameterToOperation(EOperation eOperation, String paramName, EClassifier paramType) {
        EParameter eParameter = ecoreFactory.createEParameter();
        eParameter.setName(paramName);
        eParameter.setEType(paramType);
        eOperation.getEParameters().add(eParameter);
    }

    public EClassifier getEClassifierByName(String typeName) {
        return switch (typeName) {
            case "int" -> EcorePackage.Literals.EINT;
            case "boolean" -> EcorePackage.Literals.EBOOLEAN;
            case "float" -> EcorePackage.Literals.EFLOAT;
            case "double" -> EcorePackage.Literals.EDOUBLE;
            case "byte" -> EcorePackage.Literals.EBYTE;
            case "short" -> EcorePackage.Literals.ESHORT;
            case "long" -> EcorePackage.Literals.ELONG;
            case "char" -> EcorePackage.Literals.ECHAR;
            case "String" -> EcorePackage.Literals.ESTRING;
            default -> EcorePackage.Literals.EOBJECT;
        };
    }

    public boolean isGenericType(String typeName) {
        return typeName.contains("<") && typeName.contains(">");
    }

    public EClassifier createArrayType(String arrayTypeName) {
        String componentTypeName = arrayTypeName.substring(0, arrayTypeName.length() - 2);
        EClassifier componentType = getEClassifierByName(componentTypeName);

        EClass listWrapper = ecoreFactory.createEClass();
        listWrapper.setName(componentTypeName + "Array");

        EReference eReference = ecoreFactory.createEReference();
        eReference.setName("values");
        eReference.setEType(componentType);
        eReference.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY);
        eReference.setContainment(true);

        listWrapper.getEStructuralFeatures().add(eReference);

        return listWrapper;
    }

    public Object resolveReturnType(String returnType) {
        if ("EObject".equals(returnType) || !isGenericType(returnType)) {
            return EcorePackage.Literals.EOBJECT;
        } else if (returnType.endsWith("[]")) {
            return createArrayType(returnType);
        } else {
            return getEClassifierByName(returnType);
        }
    }

    public EAnnotation createEAnnotation(String source, Map<String, String> details) {
        EAnnotation eAnnotation = ecoreFactory.createEAnnotation();
        eAnnotation.setSource(source);
        details.forEach(eAnnotation.getDetails()::put);
        return eAnnotation;
    }

    public void addEAnnotationToElement(EModelElement element, EAnnotation annotation) {
        element.getEAnnotations().add(annotation);
    }

    public void addReferenceInfo(EClass source, String targetClassName, String referenceName, boolean containment) {
        EClass target = getEClassByName(targetClassName);
        if (target != null) {
            ecoreUtils.addReferenceInfo(source, target, referenceName, containment);
        }
    }

    public void processReferences() {
        Map<EClass, List<EcoreUtils.ReferenceInfo>> refs = ecoreUtils.getClassReferences();
        for (Map.Entry<EClass, List<EcoreUtils.ReferenceInfo>> entry : refs.entrySet()) {
            EClass sourceClass = entry.getKey();
            for (EcoreUtils.ReferenceInfo info : entry.getValue()) {
                try {
                    addReference(sourceClass, info.target, info.referenceName, info.containment);
                } catch (IllegalArgumentException e) {
                    // Log the error or handle it as appropriate
                    System.err.println("Error adding reference from " + sourceClass.getName() + " to " + info.target.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private void addReference(EClass source, EClass target, String referenceName, boolean containment) {
        EReference eReference = ecoreFactory.createEReference();
        eReference.setName(referenceName);
        eReference.setEType(target);
        eReference.setContainment(containment);

        // Determine the correct multiplicity based on containment and type
        if (containment) {
            eReference.setLowerBound(0);
            eReference.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY);
        } else {
            eReference.setLowerBound(0);
            eReference.setUpperBound(1);
        }

        // Check for existing opposite reference (bi-directional)
        EReference opposite = findOppositeReference(target, source);
        if (opposite != null) {
            eReference.setEOpposite(opposite);
            opposite.setEOpposite(eReference);
        }

        source.getEStructuralFeatures().add(eReference);
    }

    private EReference findOppositeReference(EClass target, EClass source) {
        // Iterate through all EReferences of the target EClass
        for (EReference reference : target.getEReferences()) {
            // Check if the reference's type is the source EClass
            if (reference.getEType().equals(source)) {
                return reference;
            }
        }
        // If no opposite reference is found, return null
        return null;
    }
}

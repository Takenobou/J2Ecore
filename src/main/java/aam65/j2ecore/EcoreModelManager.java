package aam65.j2ecore;

import org.eclipse.emf.ecore.*;

import java.util.ArrayList;
import java.util.List;

public class EcoreModelManager {
    private final EPackage ePackage;
    private final EcoreFactory ecoreFactory;

    public EcoreModelManager() {
        ecoreFactory = EcoreFactory.eINSTANCE;
        ePackage = ecoreFactory.createEPackage();
        ePackage.setName("javaPackage");
        ePackage.setNsPrefix("java");
        ePackage.setNsURI("https://www.example.org/java");
    }

    public EClass addClass(String className) {
        EClass eClass = ecoreFactory.createEClass();
        eClass.setName(className);
        ePackage.getEClassifiers().add(eClass);
        return eClass;
    }

    public EClass addInterface(String interfaceName) {
        EClass eInterface = ecoreFactory.createEClass();
        eInterface.setName(interfaceName);
        eInterface.setInterface(true);
        eInterface.setAbstract(true);
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
        // Adjust the operation name in an attempt to prevent conflicts with getters and setters with the same sig as
        // accessors
        String adjustedOperationName = adjustOperationName(operationName, eClass);
        EOperation eOperation = ecoreFactory.createEOperation();
        eOperation.setName(adjustedOperationName);
        eClass.getEOperations().add(eOperation);
        return eOperation;
    }

    public void addReference(EClass eClass, String referenceName, EClass referenceType, int lowerBound, int upperBound, boolean isContainment) {
        EReference eReference = ecoreFactory.createEReference();
        eReference.setName(referenceName);
        eReference.setEType(referenceType);
        eReference.setLowerBound(lowerBound);
        eReference.setUpperBound(upperBound);
        eReference.setContainment(isContainment);
        eClass.getEStructuralFeatures().add(eReference);
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

    public EEnumLiteral addEnumLiteral(EEnum eEnum, String literalName, int value) {
        EEnumLiteral eEnumLiteral = ecoreFactory.createEEnumLiteral();
        eEnumLiteral.setName(literalName);
        eEnumLiteral.setValue(value);
        eEnum.getELiterals().add(eEnumLiteral);
        return eEnumLiteral;
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
            default -> getComplexTypeClassifier(typeName);
        };
    }

    private EClassifier getComplexTypeClassifier(String typeName) {
        // Check for user-defined types first
        EClass userDefinedClass = getEClassByName(typeName);
        if (userDefinedClass != null) {
            return userDefinedClass;
        }

        // Handle generic types or types with type arguments
        if (typeName.contains("<") && typeName.contains(">")) {
            String baseTypeName = typeName.substring(0, typeName.indexOf('<'));
            String typeArguments = typeName.substring(typeName.indexOf('<') + 1, typeName.lastIndexOf('>'));

            EClass baseType = getEClassByName(baseTypeName);
            if (baseType == null) {
                return EcorePackage.Literals.EOBJECT; // Fallback if base type is not found
            }

            EGenericType eGenericType = ecoreFactory.createEGenericType();
            eGenericType.setEClassifier(baseType);

            // Split the type arguments and recursively resolve each
            for (String arg : typeArguments.split(",")) {
                EGenericType typeArgument = ecoreFactory.createEGenericType();
                EClassifier resolvedType = getEClassifierByName(arg.trim());
                if (resolvedType instanceof EClass) {
                    typeArgument.setEClassifier(resolvedType);
                } else if (resolvedType instanceof EDataType) {
                    typeArgument.setEClassifier(resolvedType);
                }
                eGenericType.getETypeArguments().add(typeArgument);
            }

            // Since EGenericType is not directly an EClassifier, we wrap it in a dummy EClass
            EClass genericTypeWrapper = ecoreFactory.createEClass();
            genericTypeWrapper.getEGenericSuperTypes().add(eGenericType);
            return genericTypeWrapper;
        }

        // Handle arrays by creating an EReference with multiplicity to represent a collection.
        if (typeName.endsWith("[]")) {
            String componentTypeName = typeName.substring(0, typeName.length() - 2);
            EClassifier componentType = getEClassifierByName(componentTypeName);

            // Create a reference with a multiplicity to represent a collection of the component type.
            EReference eReference = ecoreFactory.createEReference();
            eReference.setName(componentTypeName + "List"); // Naming convention can be changed as needed
            eReference.setEType(componentType);
            eReference.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY); // This represents an array (or list)
            eReference.setContainment(true); // Assuming the array elements are contained, not just referenced

            // Create a dummy EClass to contain the EReference
            EClass listWrapper = ecoreFactory.createEClass();
            listWrapper.setName(componentTypeName + "ListWrapper"); // Naming convention can be changed as needed
            listWrapper.getEStructuralFeatures().add(eReference);

            return listWrapper;
        }

        // Handle maps by creating an EClass that represents a map entry with key and value fields.
        if (typeName.startsWith("Map<")) {
            // Extract the key and value types from the map type arguments
            String typeArguments = typeName.substring(typeName.indexOf('<') + 1, typeName.lastIndexOf('>'));
            String[] keyValueTypes = typeArguments.split(",");
            String keyTypeName = keyValueTypes[0].trim();
            String valueTypeName = keyValueTypes[1].trim();

            EClassifier keyType = getEClassifierByName(keyTypeName);
            EClassifier valueType = getEClassifierByName(valueTypeName);

            // Create the map entry EClass
            EClass mapEntry = ecoreFactory.createEClass();
            mapEntry.setName("MapEntry");

            EAttribute keyAttribute = ecoreFactory.createEAttribute();
            keyAttribute.setName("key");
            keyAttribute.setEType(keyType);
            mapEntry.getEStructuralFeatures().add(keyAttribute);

            EAttribute valueAttribute = ecoreFactory.createEAttribute();
            valueAttribute.setName("value");
            valueAttribute.setEType(valueType instanceof EDataType ? valueType : EcorePackage.Literals.EOBJECT);
            mapEntry.getEStructuralFeatures().add(valueAttribute);

            // Create a reference for a list of map entries to represent the map
            EReference mapReference = ecoreFactory.createEReference();
            mapReference.setName("map");
            mapReference.setEType(mapEntry);
            mapReference.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY);
            mapReference.setContainment(true);

            // Create a dummy EClass to contain the map reference
            EClass mapWrapper = ecoreFactory.createEClass();
            mapWrapper.setName("MapWrapper");
            mapWrapper.getEStructuralFeatures().add(mapReference);

            return mapWrapper;
        }

        // Fallback for unknown types
        return EcorePackage.Literals.EOBJECT;
    }


    public String adjustOperationName(String operationName, EClass eClass) {
        // Prefix to indicate that the operation name has been adjusted for Ecore
        final String prefix = "ecoreAdjusted_";

        // Iterate over all features to check for conflicts and annotate
        for (EStructuralFeature feature : eClass.getEStructuralFeatures()) {
            String featureName = feature.getName();
            String capitalizedFeatureName = capitalize(featureName);

            // Construct the annotation detail
            String annotationDetail = "Originally '" + operationName + "' in Java source.";

            // Conflict detection for getter
            if (operationName.equalsIgnoreCase("get" + capitalizedFeatureName)) {
                System.out.println("Conflict for getter detected: " + operationName);
                annotateFeature(feature, annotationDetail);
                return prefix + operationName;
            }

            // Conflict detection for setter
            if (operationName.equalsIgnoreCase("set" + capitalizedFeatureName)) {
                System.out.println("Conflict for setter detected: " + operationName);
                annotateFeature(feature, annotationDetail);
                return prefix + operationName;
            }

            // Special case for boolean attributes to prevent conflict with 'is' prefix getters
            if (feature instanceof EAttribute &&
                    feature.getEType() == EcorePackage.Literals.EBOOLEAN &&
                    operationName.equalsIgnoreCase("is" + capitalizedFeatureName)) {
                System.out.println("Conflict for boolean detected: " + operationName);
                annotateFeature(feature, annotationDetail);
                return prefix + operationName;
            }
        }

        // No conflict detected
        System.out.println("No conflict: " + operationName);
        return operationName;
    }

    private void annotateFeature(EStructuralFeature feature, String annotationDetail) {
        EAnnotation annotation = EcoreFactory.eINSTANCE.createEAnnotation();
        annotation.setSource("https://www.eclipse.org/emf/");
        annotation.getDetails().put("documentation", annotationDetail);
        feature.getEAnnotations().add(annotation);
    }

    private String capitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}

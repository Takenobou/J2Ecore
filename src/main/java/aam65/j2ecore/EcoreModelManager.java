package aam65.j2ecore;

import org.eclipse.emf.ecore.*;

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
        // Adjust the operation name in an attempt to prevent conflicts with getters and setters with the same sig as
        // accessors
        String adjustedOperationName = adjustOperationName(operationName, eClass);
        EOperation eOperation = ecoreFactory.createEOperation();
        eOperation.setName(adjustedOperationName);
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

    public boolean isGenericType(String typeName) {
        return typeName.contains("<") && typeName.contains(">");
    }

    public EGenericType createEGenericType(String typeName) {
        /*if (!isGenericType(typeName)) {
            EClassifier baseType = getEClassifierByName(typeName);
            if (baseType == null) {
                throw new IllegalArgumentException("Unknown type: " + typeName);
            }
            EGenericType eGenericType = ecoreFactory.createEGenericType();
            eGenericType.setEClassifier(baseType);
            return eGenericType;
        }

        // Handle nested generics and EObject more gracefully
        int beginIndex = typeName.indexOf('<');
        int endIndex = typeName.lastIndexOf('>');
        if (beginIndex == -1 || endIndex == -1 || "EObject".equals(typeName.substring(0, beginIndex))) {
            // For EObject or malformed generics, return a non-generic EGenericType
            EClassifier baseType = EcorePackage.Literals.EOBJECT;
            EGenericType eGenericType = ecoreFactory.createEGenericType();
            eGenericType.setEClassifier(baseType);
            return eGenericType;
        }

        String baseTypeName = typeName.substring(0, beginIndex);
        EClassifier baseType = getEClassifierByName(baseTypeName);
        if (baseType == null) {
            throw new IllegalArgumentException("Base type not found for generic type name: " + baseTypeName);
        }

        EGenericType eGenericType = ecoreFactory.createEGenericType();
        eGenericType.setEClassifier(baseType);

        // Extract and process type arguments
        String typeArgumentString = typeName.substring(beginIndex + 1, endIndex).trim();
        if (!typeArgumentString.isEmpty()) {
            String[] typeArguments = typeArgumentString.split("\\s*,\\s*");
            for (String argument : typeArguments) {
                EGenericType typeArgument = createEGenericType(argument.trim());
                eGenericType.getETypeArguments().add(typeArgument);
            }
        }

        return eGenericType;*/
        // Just return null or a default non-generic EGenericType to signify that the type is ignored
        EGenericType eGenericType = ecoreFactory.createEGenericType();
        eGenericType.setEClassifier(EcorePackage.Literals.EOBJECT); // Default to EObject for simplicity
        return eGenericType;
    }

    private EClassifier createCollectionOrMapClassifier(String typeName) {
        String baseTypeName = typeName.substring(0, typeName.indexOf('<'));
        String typeArguments = typeName.substring(typeName.indexOf('<') + 1, typeName.lastIndexOf('>'));

        // For collections, create an EReference with multiplicity instead of an EClass with EGenericType
        if (baseTypeName.equals("List") || baseTypeName.equals("Set") || baseTypeName.equals("ArrayList")) {
            String containedTypeName = typeArguments.trim();
            EClassifier containedType = getEClassifierByName(containedTypeName);

            // Create a dummy EClass to act as the container
            EClass listContainer = ecoreFactory.createEClass();
            listContainer.setName(containedTypeName + "ListContainer");

            // Create an EReference to represent the collection
            EReference listReference = ecoreFactory.createEReference();
            listReference.setName(containedTypeName.toLowerCase() + "s"); // Naming convention
            listReference.setEType(containedType);
            listReference.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY); // Represents multiple values
            listReference.setContainment(true);

            listContainer.getEStructuralFeatures().add(listReference);
            return listContainer;
        } else {
            // For non-collection generic types, return an EGenericType or handle other cases.
            return handleGenericTypes(baseTypeName, typeArguments);
        }
    }
    private EClassifier handleGenericTypes(String baseTypeName, String typeArguments) {
        /*EClass baseType = getEClassByName(baseTypeName);
        if (baseType == null) {
            // If the base type is not found within the known EClasses, create a new EClass for it
            baseType = ecoreFactory.createEClass();
            baseType.setName(baseTypeName);
            ePackage.getEClassifiers().add(baseType);
        }

        EGenericType eGenericType = ecoreFactory.createEGenericType();
        eGenericType.setEClassifier(baseType);

        // Split the type arguments and recursively resolve each
        String[] arguments = typeArguments.split(",");
        for (String argument : arguments) {
            argument = argument.trim();
            if (isGenericType(argument)) {
                EGenericType nestedGenericType = createEGenericType(argument);
                eGenericType.getETypeArguments().add(nestedGenericType);
            } else {
                EClassifier argumentClassifier = getEClassifierByName(argument);
                if (argumentClassifier != null) {
                    EGenericType typeArgument = ecoreFactory.createEGenericType();
                    typeArgument.setEClassifier(argumentClassifier);
                    eGenericType.getETypeArguments().add(typeArgument);
                } else {
                    // If the argument type is not known, create a placeholder EClass for it
                    EClass argumentClass = ecoreFactory.createEClass();
                    argumentClass.setName(argument);
                    ePackage.getEClassifiers().add(argumentClass);
                    EGenericType typeArgument = ecoreFactory.createEGenericType();
                    typeArgument.setEClassifier(argumentClass);
                    eGenericType.getETypeArguments().add(typeArgument);
                }
            }
        }

        EClass genericTypeContainer = ecoreFactory.createEClass();
        genericTypeContainer.setName(baseTypeName + "Container");
        genericTypeContainer.getEGenericSuperTypes().add(eGenericType);

        return genericTypeContainer;*/
        System.out.println("Skipping generic type");
        return null;
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

    public EClass addMapEntryClass(String entryClassName, EClassifier keyType, EClassifier valueType) {
        EClass mapEntry = ecoreFactory.createEClass();
        mapEntry.setName(entryClassName);
        mapEntry.setAbstract(false);
        mapEntry.setInterface(false);

        EAttribute keyAttribute = ecoreFactory.createEAttribute();
        keyAttribute.setName("key");
        keyAttribute.setEType(keyType);
        mapEntry.getEStructuralFeatures().add(keyAttribute);

        EReference valueReference = ecoreFactory.createEReference();
        valueReference.setName("value");
        valueReference.setEType(valueType);
        valueReference.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY);
        valueReference.setContainment(true);
        mapEntry.getEStructuralFeatures().add(valueReference);

        ePackage.getEClassifiers().add(mapEntry);
        return mapEntry;
    }



    private EClassifier getComplexTypeClassifier(String typeName) {
        // Check for user-defined types first
        EClass userDefinedClass = getEClassByName(typeName);
        if (userDefinedClass != null) {
            return userDefinedClass;
        }

        // Handle generic types or types with type arguments
        /*if (typeName.contains("<") && typeName.contains(">")) {
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
            return createCollectionOrMapClassifier(typeName);
        }*/

        // Handle arrays by creating an EReference with multiplicity to represent a collection.
        else if (typeName.endsWith("[]")) {
            String componentTypeName = typeName.substring(0, typeName.length() - 2);
            EClassifier componentType = getEClassifierByName(componentTypeName);

            // Create a reference with a multiplicity to represent a collection of the component type.
            EReference eReference = ecoreFactory.createEReference();
            eReference.setName(componentTypeName + "List");
            eReference.setEType(componentType);
            eReference.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY); // This represents an array (or list)
            eReference.setContainment(true);

            // Create a dummy EClass to contain the EReference
            EClass listWrapper = ecoreFactory.createEClass();
            listWrapper.setName(componentTypeName + "ListWrapper"); // Naming convention can be changed as needed
            listWrapper.getEStructuralFeatures().add(eReference);

            return listWrapper;
        }

        // Handle maps by creating an EClass that represents a map entry with key and value fields.
        else if (typeName.startsWith("Map<")) {
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
         else {return EcorePackage.Literals.EOBJECT;}
    }

    public void addMapFeature(EClass eClass, String featureName, EClassifier keyType, EClassifier valueType, boolean isContainment) {
        String entryClassName = featureName + "Entry";
        EClass mapEntryEClass = addMapEntryClass(entryClassName, keyType, valueType);

        EReference mapReference = ecoreFactory.createEReference();
        mapReference.setName(featureName);
        mapReference.setEType(mapEntryEClass);
        mapReference.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY);
        mapReference.setContainment(isContainment);
        eClass.getEStructuralFeatures().add(mapReference);
    }

    public void addCollectionFeature(EClass eClass, String featureName, EClassifier itemType, boolean isContainment) {
        EReference collectionReference = ecoreFactory.createEReference();
        collectionReference.setName(featureName);
        collectionReference.setEType(itemType);
        collectionReference.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY); // -1 for multiple instances
        collectionReference.setLowerBound(0); // 0 for optional
        collectionReference.setContainment(isContainment);
        eClass.getEStructuralFeatures().add(collectionReference);
    }

    public String adjustOperationName(String operationName, EClass eClass) {
        // Prefix to indicate that the operation name has been adjusted for Ecore
        final String prefix = "ecoreAdjusted_";

        // Iterate over all features to check for conflicts and annotate
        for (EStructuralFeature feature : eClass.getEStructuralFeatures()) {
            String featureName = feature.getName();
            String capitalizedFeatureName = capitalise(featureName);

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

        return operationName;
    }

    private void annotateFeature(EStructuralFeature feature, String annotationDetail) {
        EAnnotation annotation = EcoreFactory.eINSTANCE.createEAnnotation();
        annotation.setSource("https://www.eclipse.org/emf/");
        annotation.getDetails().put("documentation", annotationDetail);
        feature.getEAnnotations().add(annotation);
    }

    private String capitalise(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public Object resolveReturnType(String returnType) {
        if ("EObject".equals(returnType) || !isGenericType(returnType)) {
            return EcorePackage.Literals.EOBJECT;
        }
        if (isGenericType(returnType)) {
            return createEGenericType(returnType);
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
                // If the reference is also containment, it's likely the opposite of a non-containment reference
                // This is a simple heuristic and may not always be correct depending on your model's specifics
                // Further logic required to distinguish between different references based on your domain
                return reference;
            }
        }
        // If no opposite reference is found, return null
        return null;
    }
}

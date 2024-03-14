package aam65.j2ecore;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.io.IOException;
import java.util.Collections;

public class EcoreExporter {
    public void exportModel(EPackage ePackage, String filePath) throws IOException {
        // First validate the model
        Diagnostic diagnostic = validateModel(ePackage);

        if (diagnostic.getSeverity() != Diagnostic.OK) {
            // Handle validation errors or warnings
            printDiagnostic(diagnostic);
            throw new RuntimeException("Model validation failed.");
        } else {
            System.out.println("Model validation passed");
        }

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        reg.getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());

        ResourceSet resourceSet = new ResourceSetImpl();
        URI fileURI = URI.createFileURI(filePath);
        Resource resource = resourceSet.createResource(fileURI);

        if (resource == null) {
            throw new IOException("Failed to create a resource for the file path: " + filePath);
        }

        for (EClassifier classifier : ePackage.getEClassifiers()) {
            if (classifier.eResource() == null) {
                System.err.println("Classifier " + classifier.getName() + " is not associated with a resource.");
                resource.getContents().add(classifier);
            }
        }

        resource.getContents().add(ePackage);
        resource.save(Collections.EMPTY_MAP);
    }

    private Diagnostic validateModel(EPackage ePackage) {
        Diagnostician diagnostician = new Diagnostician();
        return diagnostician.validate(ePackage);
    }

    private void printDiagnostic(Diagnostic diagnostic) {
        String location = diagnostic.getData().isEmpty() ? "N/A" : diagnostic.getData().get(0).toString();
        System.out.println("Source: " + diagnostic.getSource() + " at " + location);
        System.out.println("Issue: " + diagnostic.getMessage());
        for (Diagnostic childDiagnostic : diagnostic.getChildren()) {
            printDiagnostic(childDiagnostic);
        }
    }

}

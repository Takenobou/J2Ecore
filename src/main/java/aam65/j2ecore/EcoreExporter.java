package aam65.j2ecore;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.io.IOException;
import java.util.Collections;

public class EcoreExporter {
    public void exportModel(EPackage ePackage, String filePath) throws IOException {
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
}

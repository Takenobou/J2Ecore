package aam65.j2ecore;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.io.IOException;

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

        resource.getContents().add(ePackage);
        resource.save(null);
    }
}

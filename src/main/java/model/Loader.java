package model;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Loader {

    public static void main(String[] args) throws IOException {
        XMIResource ecoreResource = new XMIResourceImpl();
        try (var ecoreInputStream = Loader.class.getResourceAsStream("/dart.ecore")) {
            ecoreResource.load(ecoreInputStream, null);
        }

        //logAllContent(ecoreResource);
        EPackage ecorePackage = getPackage(ecoreResource);

        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getPackageRegistry().put(ecorePackage.getNsURI(), ecorePackage);
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

        Resource resource = resourceSet.createResource(URI.createURI("/dartlang.dartspec"));

        try (var instanceInputStream = Loader.class.getResourceAsStream("/dartlang.dartspec")) {
            resource.load(instanceInputStream, null);
        }

        System.out.println();

        var descriptions = descriptions(resource);
        System.out.println("Count of all elements in the model: %d".formatted(descriptions.size()));

        System.out.println("List model content:");
        descriptions.forEach(System.out::println);

        // Add content
        EFactory factoryInstance = ecorePackage.getEFactoryInstance();
        EClass folderClass = (EClass) ecorePackage.getEClassifier("Folder");
        EObject folderObject = factoryInstance.create(folderClass);
        EAttribute folderName = folderClass.getEAllAttributes().get(0);
        folderObject.eSet(folderName, "dart:async");

        resource.getContents().add(folderObject);

        String userDir = System.getProperty("user.dir");
        File outputFile = Paths.get(userDir).resolve("new_dart_model.spec").toFile();

        try (var outputStream = new FileOutputStream(outputFile)) {
            Map options = new HashMap();
            options.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.FALSE); // How to include the schema location ???
            try {
                resource.save(outputStream, options);
            } catch (IOException e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
    }

    static List<String> descriptions(Resource resource) {
         return descriptions("", resource.getContents());
    }

    static <T extends EObject> List<String> descriptions(String indent, EList<T> eObjects) {
        return eObjects.stream()
                .flatMap(eObject -> descriptions(indent, eObject).stream())
                .toList();
    }

    static <T extends EObject> List<String> descriptions(String indent, T eObject) {
        List<String> eltDescriptions = new ArrayList<>();
        String content = indent + eObject.eClass().getName();
        if (eObject instanceof ENamedElement eNamedElement) {
            content = content + " " + eNamedElement.getName();
        } else if (eObject instanceof DynamicEObjectImpl dynamicEObject) {
            var name = dynamicEObject.dynamicGet(EcorePackage.EATTRIBUTE__NAME);
            if (name != null && name instanceof String) {
                content = content + " " + name;
            }
        }

        eltDescriptions.add(content);
        var contents = eObject.eContents();
        if (contents != null) {
            eltDescriptions.addAll(descriptions(indent + "  ", contents));
        }
        return eltDescriptions;
    }

    static EPackage getPackage(Resource res) {
        return (EPackage)res.getContents().get(0);
    }
}

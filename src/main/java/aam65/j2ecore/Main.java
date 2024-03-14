package aam65.j2ecore;

import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;
import java.util.stream.Stream;
import java.util.logging.Logger;
import java.util.logging.Level;


public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the directory path to search for .java files:");
        String directoryPath = scanner.nextLine();

        EcoreModelManager modelManager = new EcoreModelManager();
        JavaFileParser parser = new JavaFileParser(modelManager);

        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            parser.parseFile(path);
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Error parsing file: " + path, e);
                        }
                    });

            // Process the references after all files have been parsed.
            modelManager.processReferences();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error walking through directory: " + directoryPath, e);
        }

        System.out.println("Enter the file path to save the Ecore model:");
        String ecoreFilePath = scanner.nextLine();

        EcoreExporter exporter = new EcoreExporter();
        try {
            exporter.exportModel(modelManager.getEPackage(), ecoreFilePath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error exporting Ecore model", e);
        }
    }

}

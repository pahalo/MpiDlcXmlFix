package io.goobi.dlc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * This class searches tif duplicates in the MPI import
 * 
 * @param filesWithDuplicates Number of files with duplicates.
 * @param totalDuplicates Total count of duplicates.
 */
public class FixForXmlFiles {
    private static final Logger logger = LogManager.getLogger(FixForXmlFiles.class);
    private int filesWithDuplicates = 0;
    private int totalDuplicates = 0;

    /**
     * The main entry point of the application for processing XML files
     *
     * @param args Directory to check
     */
    public static void main(String[] args) {

        // List to store parent directory names of files with duplicates
        List<String> folderList = new ArrayList<>();

        if (args.length != 1) {
            logger.error("Please specify only one directory.");
        } else {
            //Instantiating non static functions
            FixForXmlFiles fixer = new FixForXmlFiles();

            File directory = new File(args[0]).getAbsoluteFile();
            if (directory.exists() && directory.isDirectory()) {
                // Process files in the directory that have the specified XML pattern
                List<File> filesWithMetaXml = fixer.processFiles(directory);

                // Process each XML file and identify duplicates
                for (File file : filesWithMetaXml) {
                    Element rootElement = fixer.processXmlFile(file);
                    if (rootElement != null) {
                        List<String> tifElementsList = fixer.collectXmlElements(rootElement);

                        // Find duplicates and obtain the parent directory
                        boolean hasDuplicates = fixer.findDuplicates(tifElementsList, file);
                        if (hasDuplicates) {
                            folderList.add(file.getParent());

                            // Generate BackupFiles of the Files with
                            try {
                                fixer.generateBackupFile(file);
                            } catch (IOException e) {
                                logger.error("Error creating backup file for: " + file.getAbsolutePath(), e);
                            }
                        }
                    }
                }

                // Log the number of files with duplicates and the total count of duplicates
                logger.info("Number of files with duplicates: " + fixer.filesWithDuplicates);
                logger.info("Total count of duplicates: " + fixer.totalDuplicates);

                // log filter query for goobi processes
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < folderList.size(); i++) {
                    String element = folderList.get(i);
                    stringBuilder.append(element);
                    if (i != folderList.size() - 1) {
                        stringBuilder.append(" ");
                    }
                }
                logger.info("\"id:" + stringBuilder + "\"");

            } else {
                logger.error("Das Verzeichnis existiert nicht.");
            }
        }
    }

    /**
     * Recursively traverses all files and directories in the specified directory and processes XML files.
     *
     * @param directory The directory to be searched.
     * @return List of files with meta.xml.
     */
    List<File> processFiles(File directory) {
        List<File> filesWithMetaXml = new ArrayList<>();
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                // Finds all the files that are named meta.xml
                for (File file : files) {
                    if (file.isFile() && "meta.xml".equals(file.getName().toLowerCase())) {
                        //logger.trace("meta.xml found");
                        filesWithMetaXml.add(file);
                    } else if (file.isDirectory()) {
                        filesWithMetaXml.addAll(processFiles(file));
                    }
                }
            }
        }
        return filesWithMetaXml;
    }

    /**
     * Processes a single XML file and collects all XML elements.
     *
     * @param file The XML file to be processed.
     * @return Root XML element of the file.
     */
    Element processXmlFile(File file) {

        try {
            SAXBuilder sax = new SAXBuilder();
            Document doc = sax.build(file);
            // Collects the rootelement
            Element rootElement = doc.getRootElement();
            logger.trace("root element found: " + rootElement);
            return rootElement;
        } catch (JDOMException | IOException e) {
            // TODO: bessere Meldung (en)
            // TODO: Exceptions immer mit Komma
            logger.error("processXmlFile exception", e);
            return null;
        }
    }

    /**
     * Collects XML elements with attributes ending in ".tif".
     *
     * @param element The XML element to start collecting from.
     * @return List of attribute values ending in ".tif".
     */
    List<String> collectXmlElements(Element element) {
        // List to store .tif elements
        List<String> tifElementsList = new ArrayList<>();

        // Check if the element is not null
        if (element != null) {
            // Check attributes of the element
            List<Attribute> attributes = element.getAttributes();
            for (Attribute attribute : attributes) {

                // TODO: mal prüfen ob nötig
                // Check if the attribute value ends with ".tif"
                if (attribute.getValue().endsWith(".tif")) {
                    tifElementsList.add(attribute.getValue());
                }
            }

            // Check children of the element
            List<Element> children = element.getChildren();
            for (Element child : children) {
                // Add all .tif elements from recursive calls
                tifElementsList.addAll(collectXmlElements(child));
            }
        }

        // Return the list of .tif elements
        return tifElementsList;
    }

    /**
     * Finds duplicates of tif attributes within XML elements.
     *
     * @param tifElementsList List of tif elements.
     * @param xmlFile The XML file currently being processed.
     * @return Name of the parent directory if duplicates are found, otherwise null.
     */
    boolean findDuplicates(List<String> tifElementsList, File xmlFile) {
        boolean duplicatesFound = false;

        // TODO: überlegen ob list oder map
        Set<String> tifValues = new HashSet<>();
        List<String> tifDuplicatesList = new ArrayList<>();
        List<String> parentDirectory = new ArrayList<>();

        // Searches for .tif values and adds them to the duplicates list
        for (String tifElement : tifElementsList) {

            // TODO: prüfen ob das nötig ist
            if (tifElement.endsWith(".tif")) {
                if (!tifValues.add(tifElement)) {
                    duplicatesFound = true;
                    if (!tifDuplicatesList.contains(tifElement)) {
                        logger.trace("Duplicate found that is not in list: " + tifElement);
                        tifDuplicatesList.add(tifElement);
                    }
                }
            }
        }

        // TODO: mal prüfen ob zusammengezogen werden kann
        if (duplicatesFound) {
            filesWithDuplicates++;
            totalDuplicates += tifDuplicatesList.size();
            logger.trace(xmlFile.getAbsolutePath());
            File directoryAbove = xmlFile.getParentFile();
            parentDirectory.add(directoryAbove.getName());
            for (String tifValue : tifDuplicatesList) {
                logger.info("   " + tifValue + ".tif");
            }
            tifDuplicatesList.clear();
        }
        return duplicatesFound;
    }

    /**
     * Generates a backup file for the given XML file by creating a copy with a timestamp in the filename.
     *
     * @param xmlFile The File object representing the XML file to be backed up.
     * @throws IOException If an I/O error occurs during the file reading or writing process.
     */
    Path generateBackupFile(File xmlFile) throws IOException {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");

        // Name of the backupfile
        // TODO: besser meta.xml.TIMESTAMP -> workflow channel beitreten und fragen, wie dort die backups heissen (Stelle im Code)
        String backupFileName = "meta_" + currentTime.format(formatter) + ".xml";

        // Creating a backupfile in the same directory
        File backupFile = new File(xmlFile.getParentFile(), backupFileName);

        Path sourcePath = xmlFile.toPath();
        Path destinationPath = backupFile.toPath();

        // TODO: hier noch loggen
        return Files.copy(sourcePath, destinationPath, StandardCopyOption.COPY_ATTRIBUTES);

    }
}

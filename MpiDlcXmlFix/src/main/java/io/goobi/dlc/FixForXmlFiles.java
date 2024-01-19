package io.goobi.dlc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
                        	folderList.add(new File(file.getParent()).getName());
                        	
                            // Generate BackupFiles of the Files with duplicates tif values
                            try {
                                fixer.generateBackupFile(file);
                            } catch (IOException e) {
                                logger.error("Error creating backup file for: " + file.getAbsolutePath(), e);
                            }
                        }
                    }
                }

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
                logger.info("\"id: " + stringBuilder + "\"");

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
                    if (file.isFile() && "meta.xml".equals(file.getName())) {
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
            logger.trace("Root element found: " + rootElement);
            return rootElement;
        } catch (JDOMException | IOException e) {
            logger.trace("No root element found", e);
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
            	// add .tif attributes to list
            	String attributeValue = attribute.getValue();
                if (attributeValue.endsWith(".tif")) {
                    tifElementsList.add(attributeValue);
                }
            }

            // Check children of the element
            List<Element> children = element.getChildren();
            for (Element child : children) {
                // Recursively collect .tif elements
                List<String> childTifElements = collectXmlElements(child);

                // Filter and add only the elements that end with ".tif"
                for (String tifElement : childTifElements) {
                    if (tifElement.endsWith(".tif")) {
                        tifElementsList.add(tifElement);
                    }
                }
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

        List<String> tifValues = new ArrayList<>();
        List<String> tifDuplicatesList = new ArrayList<>();
        List<String> parentDirectory = new ArrayList<>();

        // Adds the .tif values to the list if they are duplicates and not already in the list
        for (String tifElement : tifElementsList) {
            if (!tifValues.contains(tifElement)) {
                tifValues.add(tifElement);
            } else {
                duplicatesFound = true;
                if (!tifDuplicatesList.contains(tifElement)) {
                    logger.trace("Duplicate found that is not in list: " + tifElement);
                    tifDuplicatesList.add(tifElement);
                    logger.info("   " + tifElement);
                }
            }
        }
        
        filesWithDuplicates++;
        logger.trace(xmlFile.getAbsolutePath());
        totalDuplicates += tifDuplicatesList.size();
        File directoryAbove = xmlFile.getParentFile();
        parentDirectory.add(directoryAbove.getName());
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmssSSS");

        // Name of the backupfile
        String backupFileName = "meta.xml." + currentTime.format(formatter);

        // Creating a backupfile in the same directory
        File backupFile = new File(xmlFile.getParentFile(), backupFileName);

        Path sourcePath = xmlFile.toPath();
        Path destinationPath = backupFile.toPath();

        logger.trace(sourcePath);
        logger.trace(destinationPath);
        return Files.copy(sourcePath, destinationPath, StandardCopyOption.COPY_ATTRIBUTES);

    }
}

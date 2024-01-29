package io.goobi.dlc;

import java.io.File;
import java.io.FileWriter;
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
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
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
        List<String> physIDValues = new ArrayList<>();
        logger.info(xmlFile.getAbsolutePath());
        try {
            SAXBuilder sax = new SAXBuilder();
            Document doc = sax.build(xmlFile);
            Element rootElement = doc.getRootElement();
            List<String> allFileIDValues = new ArrayList<>();
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

                        // Find the ID values of the parent elements
                        List<String> fileIDValues = findIDValueOfDuplicateLines(rootElement, tifElement);
                        allFileIDValues.addAll(fileIDValues);
                        
                        for (int i = 0; i < fileIDValues.size(); i++) {
                            String FILEID = fileIDValues.get(i);
                            physIDValues.addAll(findIDValueOfDuplicateLines(rootElement, FILEID));
                        }
                        findAndRewritePHYSValuesOfDuplicateLines(doc.getRootElement(), physIDValues, xmlFile);
                        
                        // Remove the first Object in the List so we dont delete it from the xml file
                        if (!fileIDValues.isEmpty()) {
        	                fileIDValues.remove(0);
        	            }
        	            if (!physIDValues.isEmpty()) {
        	                physIDValues.remove(0);
        	            }
                        rewriteMetsDivAndFile(doc.getRootElement(), fileIDValues, physIDValues, xmlFile);
                        physIDValues.clear();
                        fileIDValues.clear();
                    }
                    
                }
            }
            if (duplicatesFound) {
                filesWithDuplicates++;
                totalDuplicates += tifDuplicatesList.size();
                File directoryAbove = xmlFile.getParentFile();
                parentDirectory.add(directoryAbove.getName());
            }
        } catch (JDOMException | IOException e) {
            logger.error("Error processing XML file: " + xmlFile.getAbsolutePath(), e);
        }
        return duplicatesFound;
    }

    /**
     * Recursive method to find and log the duplicate element in the XML structure.
     *
     * @param element The current XML element being checked.
     * @param duplicateValue The duplicate value to be checked in attributes.
     * @return List of ID values associated with the duplicate element.
     */
    List<String> findIDValueOfDuplicateLines(Element element, String duplicateValue) {
        
        List<String> idValues = new ArrayList<>();
        // Check if the element is not null
        if (element != null) {
            // Check attributes of the element
            List<Attribute> attributes = element.getAttributes();
            for (Attribute attribute : attributes) {
                // Check if the attribute value matches the duplicate value
                if (attribute.getValue().equals(duplicateValue)) {
                    // Log information about the duplicate element
                    Element parentElement = element.getParentElement();
                    if (parentElement != null) {
                        Attribute idAttribute = parentElement.getAttribute("ID");
                        if (idAttribute != null) {
                            String idValue = idAttribute.getValue();
                            logger.trace("ID=\"" + idValue + "\"");
                            idValues.add(idValue);
                            return idValues;
                        }
                    }
                }
            }

            // Check children of the element
            List<Element> children = element.getChildren();
            for (Element child : children) {
                // Recursively check children
                idValues.addAll(findIDValueOfDuplicateLines(child, duplicateValue));
            }
        }
        return idValues;
    }
    
    /**
     * Finds and prints values containing "PHYS_" based on the given XML element and a list of ID values.
     * @param element The XML element to explore.
     * @param allIDValues The list of ID values to check against "PHYS_" attributes.
     */
     Boolean findAndRewritePHYSValuesOfDuplicateLines(Element element, List<String> physIDValues, File xmlFile) {
        // Check attributes of the current element
        List<Attribute> attributes = element.getAttributes();
        Element parentElement = element.getParentElement();
        boolean changesMade = false;
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

        // Output the element with its attributes
        String elementString = xmlOutputter.outputString(element);

        for (Attribute attribute : attributes) {
            String attributeValue = attribute.getValue();
            for (String physIDValue : physIDValues) {
                // Check if the attribute value is equal the current ID value
                if (attributeValue.contains(physIDValue)) {
                	if ("structLink".equals(parentElement.getName())) {
                		//logger.info("<mets:structLink> PHYS_" + idValue);
                		logger.trace("Element: " + elementString);
                		// Getting the Value that should be there
                		String newValue = physIDValues.get(0);
                		// Setting the Value
                		attribute.setValue(newValue);
                		changesMade = true;
                	} else {
                		logger.trace(physIDValue);
                	}
                }
            }
        }
        
        // If changes were made, save the document
        if (changesMade) {
            changesMade = saveDocument(element.getDocument(), xmlFile);
            if(changesMade) {
                logger.trace("XML file successfully edited and saved.");
            }
        }
        // Recursively explore child elements
        List<Element> children = element.getChildren();
        for (Element child : children) {
        	findAndRewritePHYSValuesOfDuplicateLines(child, physIDValues, xmlFile);
        }
        return changesMade;
    }
     
     
    boolean rewriteMetsDivAndFile(Element element, List<String> fileIDValues, List<String> physIDValues, File xmlFile) {
        // Traverse the element to find elements to remove
		List<Element> elementsToRemove = new ArrayList<>();
		traverseElement(element, fileIDValues, physIDValues, elementsToRemove);

		// Remove elements from the document after traversal
		for (Element e : elementsToRemove) {
		    e.removeContent(); // Remove all children of the element
		    e.getParentElement().removeContent(e); // Remove the element itself from its parent
		}

		// Write the modified document back to the XML file
		saveDocument(element.getDocument(), xmlFile);
        return true;
    }
    	

        void traverseElement(Element element, List<String> fileIDValues, List<String> physIDValues, List<Element> elementsToRemove) { 
        	    List<org.jdom2.Attribute> attributes = element.getAttributes();
        	    for (org.jdom2.Attribute attribute : attributes) {
        	        // Check if the value of the attribute is contained in either of the lists
        	        if((attribute.getName().equals("ID"))) {
        	            // Removing the first Element of the list because this one should remain untouched
        	            String value = attribute.getValue();
        	            if ((fileIDValues.contains(value))) {
        	            	System.out.println(value );
        	                elementsToRemove.add(element);
        	            } else if((physIDValues.contains(value))){
        	            	System.out.println(value);
        	                elementsToRemove.add(element);
        	            } 
        	        }
        	    }
        	    
        	    List<Element> children = new ArrayList<>(element.getChildren());
        	    for (Element child : children) {
        	        traverseElement(child, fileIDValues, physIDValues, elementsToRemove);
        	    }
        	}
        
    // Method to save the updated document to the file
     Boolean saveDocument(Document document, File xmlFile) {
        try {

            // Create a FileWriter to overwrite the XML file
        	FileWriter writer = new FileWriter(xmlFile);

            // Output the updated XML document to the file
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            xmlOutputter.output(document, writer);

            // Close the FileWriter
            writer.close();
        } catch (IOException e) {
            logger.error("Error saving the XML file: " + e.getMessage());
            return false;
        }
        return true;
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

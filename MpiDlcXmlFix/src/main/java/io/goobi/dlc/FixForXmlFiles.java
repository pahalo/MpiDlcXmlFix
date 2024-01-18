package io.goobi.dlc;

import java.io.*;
import java.util.*;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * This class searches tif duplicates in the MPI import
 * @param filesWithDuplicates Number of files with duplicates.
 * @param totalDuplicates Total count of duplicates.
 */
public class FixForXmlFiles {
	private static final Logger logger = LogManager.getLogger(FixForXmlFiles.class);
	private static int filesWithDuplicates = 0;
	private static int totalDuplicates = 0;
	
	/**
	 * The main entry point of the application for processing XML files
	 * @param args Directory to check
	 */
    public static void main(String[] args) {
    	
    	StringBuilder stringBuilder = new StringBuilder();
    	// List to store parent directory names of files with duplicates
    	List<String> parentDirectory = new ArrayList<>();
    	
        if (args.length != 1) {
        	logger.error("Please specify only one directory."); 
        } else {
        	//Instantiating non static functions
        	 FixForXmlFiles fileProcessor = new FixForXmlFiles();
        	 FixForXmlFiles xmlElementCollector = new FixForXmlFiles();
        	 FixForXmlFiles duplicateFinder = new FixForXmlFiles();
             FixForXmlFiles backupGenerator = new FixForXmlFiles();
       	     FixForXmlFiles xmlFilesProcessor = new FixForXmlFiles();
       	   
        	 File directory = new File(args[0]).getAbsoluteFile();
             if (directory.exists() && directory.isDirectory()) {
            	// Process files in the directory that have the specified XML pattern
            	List<File> filesWithMetaXml = fileProcessor.processFiles(directory);
             	 // Process each XML file and identify duplicates
             	for (File file : filesWithMetaXml) {
             	   Element rootElement = xmlFilesProcessor.processXmlFile(file);
             	   if (rootElement != null) {
             		   List<String> tifElementsList = xmlElementCollector.collectXmlElements(rootElement);
             		   // Find duplicates and obtain the parent directory
             		   String parentDir = duplicateFinder.findDuplicates(tifElementsList, file);
                       if (parentDir != null) {
                           parentDirectory.add(parentDir);
                           // Generate BackupFiles of the Files with 
                           try {
                        	    backupGenerator.generateBackupFile(file);
                        	} catch (IOException e) {
                        		logger.error("Error creating backup file for: " + file.getAbsolutePath(), e);
                        	}
                       }
             		}
             	}
             	// Log the number of files with duplicates and the total count of duplicates
                logger.info("Number of files with duplicates: " + filesWithDuplicates); 
                logger.info("Total count of duplicates: " + totalDuplicates); 

                for (int i = 0; i < parentDirectory.size(); i++) {
                    String element = parentDirectory.get(i);
                    stringBuilder.append(element);
                    if (i != parentDirectory.size() - 1) {
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
                	if (file.isFile() && file.getName().toLowerCase().equals("meta.xml")) {
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
        } catch (Exception e) {
            logger.error("processXmlFile exception" + e);
            return null;
        }
    }
    
     /**
      * Collects XML elements with attributes ending in ".tif".
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
     * @param tifElementsList List of tif elements.
     * @param xmlFile The XML file currently being processed.
     * @return Name of the parent directory if duplicates are found, otherwise null.
     */
     String findDuplicates(List<String> tifElementsList, File xmlFile) {
    	    boolean duplicatesFound = false;
    	    Set<String> tifValues = new HashSet<>();
    	    List<String> tifDuplicatesList = new ArrayList<>();
    	    List<String> parentDirectory = new ArrayList<>();

    	    // Searches for .tif values and adds them to the duplicates list
    	    for (String tifElement : tifElementsList) {
    	        if (tifElement.endsWith(".tif")) {	        	
    	        	// Extract the numeric part of the file name by removing the last 4 characters (".tif")
    	            String tifValue = tifElement.substring(0, tifElement.length() - 4);
    	            if (!tifValues.add(tifValue)) {
    	                duplicatesFound = true;
    	                if (!tifDuplicatesList.contains(tifValue)) {
    	                    logger.trace("Duplicate Found that is not in List: " + tifValue);
    	                    tifDuplicatesList.add(tifValue);
    	                }
    	            }
    	        }
    	    }

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
    	        return directoryAbove.getName();
    	    }
    	    return null;
    	}

    
    /**
     * Generates a backup file for the given XML file by creating a copy with a timestamp in the filename.
     * @param xmlFile The File object representing the XML file to be backed up.
     * @throws IOException If an I/O error occurs during the file reading or writing process.
     */
     Path generateBackupFile(File xmlFile) throws IOException {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");
        
        // Name of the backupfile
        String backupFileName = "meta_" + currentTime.format(formatter) + ".xml"; 

        // Creating a backupfile in the same directory
        File backupFile = new File(xmlFile.getParentFile(), backupFileName); 
        
        Path sourcePath = xmlFile.toPath();
        Path destinationPath = backupFile.toPath();
        return Files.copy(sourcePath, destinationPath, StandardCopyOption.COPY_ATTRIBUTES);
        
    }
}

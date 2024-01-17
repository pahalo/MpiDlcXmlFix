package io.goobi.dlc;

import java.io.*;
import java.util.*;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.Namespace;

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
        	
        	 File directory = new File(args[0]).getAbsoluteFile();
             if (directory.exists() && directory.isDirectory()) {
            	// Process files in the directory that have the specified XML pattern
             	List<File> filesWithMetaXml = processFiles(directory);
             	 // Process each XML file and identify duplicates
             	for (File file : filesWithMetaXml) {
             	    Element rootElement = processXmlFile(file);
             	    
             	   if (rootElement != null) {
             		   List<String> xmlElementsList = collectXmlElements(rootElement);
             		   // Find duplicates and obtain the parent directory
             		   String parentDir = findDuplicates(xmlElementsList, file);
                       if (parentDir != null) {
                           parentDirectory.add(parentDir);
                           // Generate BackupFiles of the Files with 
                           //generateBackupFile(file);
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
    static List<File> processFiles(File directory) {
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
     static Element processXmlFile(File file) {

        try {
            SAXBuilder sax = new SAXBuilder();
            Document doc = sax.build(file);
            // Collects the rootelement
            Element rootElement = doc.getRootElement();
            //logger.trace("root element found: " + rootElement);
            return rootElement;
        } catch (Exception e) {
            logger.error("processXmlFile exception" + e);
            return null;
        }
    }
    
    /**
     * Collects XML elements recursively.
     * @param element The XML element being processed.
     * @param xmlFile The XML file currently being processed.
     * @return List of collected XML elements as strings.
     */
    static List<String> collectXmlElements(Element element) {
        List<String> xmlElementsList = new ArrayList<>();
    	if (element != null) {
            StringBuilder elementString = new StringBuilder();
            
            Namespace ns = element.getNamespace(); 
            String prefix = ns.getPrefix(); 
            
            // Checks for prefix
            if (!prefix.isEmpty()) {
                elementString.append(prefix).append(":").append(element.getName());
            	//logger.trace("Prefix found");
            } else {
                elementString.append(element.getName());
                
            }
            
            // Checks for attributes
            List<Attribute> attributes = element.getAttributes();
            for (Attribute attribute : attributes) {
            	//logger.trace("Adding attribute");
            	System.out.println(attribute.getValue());
                elementString.append(" ").append(attribute.getQualifiedName()).append("=\"").append(attribute.getValue()).append("\"");
                //logger.debug(elementString);
                
                    // Überprüfe, ob der Attributwert mit ".tif" endet
                    if (attribute.getValue().endsWith(".tif")) {
                        System.out.println("tif"+attribute.getValue());
                        elementString.append(" ").append(attribute.getQualifiedName()).append("=\"").append(attribute.getValue()).append("\"");
                    }
                
            }
            
            List<Element> children = element.getChildren();	
            
            if (!children.isEmpty()) {
                xmlElementsList.add(elementString.toString());

                for (Element child : children) {
                	xmlElementsList.addAll(collectXmlElements(child));
                }

                if (!prefix.isEmpty()) {
                    xmlElementsList.add( prefix + ":" + element.getName());
                } else {
                    xmlElementsList.add(element.getName());
                }
            } else {  
                 	xmlElementsList.add(elementString.toString());             
            }
        }
    	//System.out.println(xmlElementsList);
    	return xmlElementsList;
    }

    /**
     * Finds duplicates of xlink:href attributes within XML elements.
     * @param xmlElementsList List of XML elements.
     * @param xmlFile The XML file currently being processed.
     * @return Name of the parent directory if duplicates are found, otherwise null.
     */
    static String findDuplicates(List<String> xmlElementsList, File xmlFile) {
        boolean duplicatesFound = false;
        Set<String> hrefs = new HashSet<>();
        List<String> hrefDuplicatesList = new ArrayList<>();
        List<String> parentDirectory = new ArrayList<>();

        // Searches for all lines where xlink:href is used and might add them to the duplicates list
        for (String element : xmlElementsList) {
            int hrefIndex = element.indexOf("xlink:href=\"");
            if (hrefIndex != -1) {
                int start = hrefIndex + "xlink:href=\"".length();
                int end = element.indexOf("\"", start);
                if (end != -1) {
                    String href = element.substring(start, end);
                    if (!hrefs.add(href)) {
                        duplicatesFound = true;
                        if (!hrefDuplicatesList.contains(href)) {
                        	//logger.trace("Duplicate Found that is not in List: " + href);
                            hrefDuplicatesList.add(href);
                        }
                    }
                }
            }
        }

        if (duplicatesFound) {
        	filesWithDuplicates++;
            totalDuplicates += hrefDuplicatesList.size();
            //logger.trace(xmlFile.getAbsolutePath()); 
            File directoryAbove = xmlFile.getParentFile();
            parentDirectory.add(directoryAbove.getName());
            //for (String element : hrefDuplicatesList) {
            	//logger.info("   " + element);
            //}
            hrefDuplicatesList.clear();
            return directoryAbove.getName();
        } 
        return null;
    }
    
    /**
     * Generates a backup file for the given XML file by creating a copy with a timestamp in the filename.
     * @param xmlFile The File object representing the XML file to be backed up.
     * @throws IOException If an I/O error occurs during the file reading or writing process.
     */
     static Path generateBackupFile(File xmlFile) throws IOException {
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

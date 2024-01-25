package io.goobi.dlc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class FixForXmlFiles_Test {
    private static final Logger logger = LogManager.getLogger(FixForXmlFiles.class);

    @Test
    void testProcessFiles() {
        File testDirectory = new File("testDirectory");
        testDirectory.mkdir();

        try {
        	  // Creating new File in directory
            File file1 = new File(testDirectory, "meta.xml");
            file1.createNewFile();

            File subDirectory = new File(testDirectory, "subDirectory");
            subDirectory.mkdir();
            File file2 = new File(subDirectory, "meta.xml");
            File file3 = new File(subDirectory, "meta.txt");
            file2.createNewFile();
            file3.createNewFile();

            File subDirectory02 = new File(testDirectory, "subDirectory02");
            subDirectory02.mkdir();
            File file4 = new File(subDirectory02, "meta.xml");
            File file5 = new File(subDirectory02, "meta.txt");
            file4.createNewFile();
            file5.createNewFile();

            // Process files and check results
            FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
            List<File> result = fixForXmlFiles.processFiles(testDirectory);

            assertEquals(3, result.size()); 
            assertTrue(result.contains(file1));
            assertTrue(result.contains(file2));
            assertFalse(result.contains(file3));
            assertTrue(result.contains(file4));
            assertFalse(result.contains(file5));
        } catch (IOException e) {
            // Handle IOException
            logger.error("IOException occurred", e);
        } catch (SecurityException e) {
            // Handle SecurityException
            logger.error("SecurityException occurred", e);
        } catch (NullPointerException e) {
            // Handle NullPointerException
            logger.error("NullPointerException occurred", e);
        } finally {
            // Clean up the test directory
            deleteDirectory(testDirectory.toPath());
        }
    }


    // Deleting directory
    public void deleteDirectory(Path directory) {
        try {
            File dirFile = directory.toFile();
            if (dirFile.isDirectory()) {
                File[] files = dirFile.listFiles();
                if (files != null) {
                	// Iterate through all files in the directory
                    for (File file : files) {
                        if (file.isDirectory()) {
                            deleteDirectory(file.toPath());
                        } else {
                            file.delete();
                        }
                    }
                }
            }
            dirFile.delete();
        
         // Handle SecurityException or NullPointerException
        } catch (SecurityException | NullPointerException e) {
            logger.error("Directory could not be deleted", e);
        }
    }

    @Test
    public void testProcessXmlFile() {
        try {
            // Load a valid XML file for processing
            File validXmlFile = new File("src/test/resources/183112/meta.xml");

            FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
            Element result = fixForXmlFiles.processXmlFile(validXmlFile);

            //If NotNull: elements were found and the file is valid
            assertNotNull(result);
        } catch (Exception e) {
            logger.error("Processed xml File is unvalid", e);
        }
        try {
            // Load a unexisting xml File for processing 
            File validXmlFile = new File("src/test/resources/183112/met.xml");

            FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
            Element result = fixForXmlFiles.processXmlFile(validXmlFile);

            assertNull(result);
        } catch (Exception e) {
        	logger.error("The verification of a non-existent XML file was successful.", e);
        }
    }

    @Test
    public void testCollectXmlElements() throws Exception {
        try {
            String relativeXmlFilePath = "src/test/resources/186004/meta.xml";
            File xmlFile = new File(relativeXmlFilePath);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(xmlFile);
            Element rootElement = document.getRootElement();

            FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
            List<String> xmlElementsList = fixForXmlFiles.collectXmlElements(rootElement);

            assertTrue(xmlElementsList.size() > 0);
        } catch (IOException e) {
            logger.error("No tif values found", e);
        }
    }

    @Test
    public void testFindDuplicates() {
        try {
            // Test values
            List<String> xmlElementsList = Arrays.asList("00001.tif",
                    "00001.tif", "00002.tif",
                    "00003.tif", "00003.tif",
                    "00004.tif");
            File xmlFile = new File("src/test/resources/183112/meta.xml");

            FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
            boolean result = fixForXmlFiles.findDuplicates(xmlElementsList, xmlFile);
            // Checking if Duplicates are found
            assertTrue(result);
        } catch (Exception e) {
            logger.error("No duplicates found", e);
        }

        try {
            // Test values
            List<String> xmlElementsList = Arrays.asList("00001.tif",
                    "00001.tif", "00001.tif",
                    "00001.tif", "00001.tif",
                    "00001.tif");
            File xmlFile = new File("src/test/resources/183112/meta.xml");


            FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
            boolean result = fixForXmlFiles.findDuplicates(xmlElementsList, xmlFile);
            // Checking if Duplicates are found
            assertTrue(result);
        } catch (Exception e) {
        	logger.error("No duplicates found", e);
        }

        try {
            // Test values
            List<String> xmlElementsList = Arrays.asList("00001.tif",
                    "00002.tif", "00003.tif",
                    "00004.tif", "00005.tif",
                    "00001.tif");
            File xmlFile = new File("src/test/resources/183112/meta.xml");

            FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
            boolean result = fixForXmlFiles.findDuplicates(xmlElementsList, xmlFile);
            // Checking if Duplicates are found
            assertTrue(result);
        } catch (Exception e) {
        	logger.error("No duplicates found", e);
        }

    }
    
    @Test
    void testFindIDValueOfDuplicateLines() {
        FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();

//		 This is what it what it looks like: 
//        <mets:file ID="FILE_0271" MIMETYPE="">
//        	<mets:FLocat LOCTYPE="URL" xlink:href="00000265.tif"/>
//        </mets:file>
        
        // Create the sample XML document directly as an element
        Namespace metsNamespace = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
        Element fileElement = new Element("file", metsNamespace);
        fileElement.setAttribute("ID", "FILE_0271");
        fileElement.setAttribute("MIMETYPE", "");
        Element fLocatElement = new Element("FLocat", metsNamespace);
        fLocatElement.setAttribute("LOCTYPE", "URL", Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink"));
        fLocatElement.setAttribute("href", "00000265.tif", Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink"));
        fileElement.addContent(fLocatElement);

        // Test if the method extracts the correct ID value associated with the duplicate TIF value
        List<String> result = fixForXmlFiles.findIDValueOfDuplicateLines(fileElement, "00000265.tif");
        assertEquals("FILE_0271", result.get(0)); 
    }


    @Test
    void testGenerateBackupFile() {
        try {
            // Path to the original XML file
            String filePath = "src/test/resources/183112/meta.xml";
            File xmlFilePath = new File(filePath);

            FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
            // Create a backup copy
            Path expectedBackupFilePath = fixForXmlFiles.generateBackupFile(xmlFilePath);

            // Check if the backup copy exists
            assertTrue(expectedBackupFilePath.toFile().exists());

            // Verify content
            List<String> originalLines = Files.readAllLines(xmlFilePath.toPath());
            List<String> backupLines = Files.readAllLines(expectedBackupFilePath);
            // Check if the backup is identical 
            assertEquals(originalLines, backupLines);

        } catch (IOException e) {
            logger.error("Generated Backupfile does not match the original Lines", e);
        }
    }
    @AfterAll
    private static void deletingBackups() {
    	String directoryPath = "src/test/resources";
    	CleanupBackups cleanupBackups = new CleanupBackups();
    	// Calls the cleanupBackups class in order to delete backup files
        int numberOfBackupsDeleted = cleanupBackups.processFiles(new File(directoryPath));
        logger.info("Number of BackupsDeleted = " + numberOfBackupsDeleted);
    }
    
}

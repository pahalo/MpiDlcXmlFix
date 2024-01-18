package io.goobi.dlc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import java.util.Arrays;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	        file2.createNewFile();

	        FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
	        List<File> result = fixForXmlFiles.processFiles(testDirectory);

	        // Process files and check results
	        assertEquals(2, result.size());
	        assertTrue(result.contains(file1));
	        assertTrue(result.contains(file2));
	    } catch (Exception e) {
	        logger.error("testProcessFiles exception", e);
	    } finally {
	        // Clean up the test directory
	        deleteDirectory(testDirectory.toPath());
	    }
	}


	// Deleting directory
	public void deleteDirectory(Path directory) {
		File dirFile = directory.toFile();
		if (dirFile.isDirectory()) {
			File[] files = dirFile.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isDirectory()) {
						deleteDirectory(file.toPath());
					} else {
						file.delete();
					}
				}
			}
			dirFile.delete();
		}
	}

	@Test
	public void testProcessXmlFileValidFile() {
		try {
			// Load a valid XML file for processing
			File validXmlFile = new File("src/test/resources/183112/meta.xml");
			
			FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
	        Element result = fixForXmlFiles.processXmlFile(validXmlFile);

			assertNotNull(result);
		} catch (Exception e) {
			logger.error("testCollectXmlElements exception" + e);
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
	        logger.error("testCollectXmlElements exception" + e);
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
		    File xmlFile = new File("/Users/paul/git/xmlMitPaul/src/test/resources/183112/meta.xml");
		    // The example directory
		    String expectedDirectoryName = "183112";
		    
		    FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
	        // Checking if Duplicates are found
	        assertEquals(expectedDirectoryName, fixForXmlFiles.findDuplicates(xmlElementsList, xmlFile));
		} catch (Exception e) {
		    logger.error("Exception thrown during test", e);
		}

		try {
		    // Test values
		    List<String> xmlElementsList = Arrays.asList("00001.tif",
		            "00001.tif", "00001.tif",
		            "00001.tif", "00001.tif",
		            "00001.tif");
		    File xmlFile = new File("/src/test/resources/183112/meta.xml");
		    // The example directory
		    String expectedDirectoryName = "183112"; 
		    
		    FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
	        // Checking if Duplicates are found
	        assertEquals(expectedDirectoryName, fixForXmlFiles.findDuplicates(xmlElementsList, xmlFile));
		} catch (Exception e) {
		    logger.error("Exception thrown during test", e);
		}

		try {
		    // Test values
		    List<String> xmlElementsList = Arrays.asList("00001.tif",
		            "00002.tif", "00003.tif",
		            "00004.tif", "00005.tif",
		            "00001.tif");
		    File xmlFile = new File("/src/test/resources/183112/meta.xml");
		    // The example directory
		    String expectedDirectoryName = "183112";
		    
		    FixForXmlFiles fixForXmlFiles = new FixForXmlFiles();
	        // Checking if Duplicates are found
	        assertEquals(expectedDirectoryName, fixForXmlFiles.findDuplicates(xmlElementsList, xmlFile));
		} catch (Exception e) {
		    logger.error("testFindDuplicatesWithDuplicates exception" + e);
		}

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
	        assertEquals(originalLines, backupLines);

	    } catch (IOException e) {
	    	logger.error("testGenerateBackupFile exception", e); 
	    }
	}
}

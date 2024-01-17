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

			List<File> result = FixForXmlFiles.processFiles(testDirectory);
			// Process files and check results
			assertEquals(2, result.size());
			assertTrue(result.contains(file1));
			assertTrue(result.contains(file2));
		} catch (Exception e) {
			logger.error("testProcessXmlFileValidFile exception" + e);
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
			File validXmlFile = new File("src/test/resources/meta.xml");
			
			Element result = FixForXmlFiles.processXmlFile(validXmlFile);

			assertNotNull(result);
		} catch (Exception e) {
			logger.error("testCollectXmlElements exception" + e);
		}
	}

	@Test
	public void testCollectXmlElements() throws Exception {
	    try {
	        String relativeXmlFilePath = "src/test/resources/meta.xml";
	        File xmlFile = new File(relativeXmlFilePath);
	        SAXBuilder saxBuilder = new SAXBuilder();
	        Document document = saxBuilder.build(xmlFile);
	        Element rootElement = document.getRootElement();	
	        
	        List<String> xmlElementsList = FixForXmlFiles.collectXmlElements(rootElement);
	        assertTrue(xmlElementsList.size() > 0);
	    } catch (IOException e) {
	    	logger.error("testCollectXmlElements exception" + e);
	    }
	}

	@Test
	public void testFindDuplicatesWithDuplicates() {
		try {
			// Test values
			List<String> xmlElementsList = Arrays.asList("<root xlink:href=\"00001.tif\"/>",
					"<root xlink:href=\"00001.tif\"/>", "<root xlink:href=\"00002.tif\"/>",
					"<root xlink:href=\"00003.tif\"/>", "<root xlink:href=\"00003.tif\"/>",
					"<root xlink:href=\"00004.tif\"/>");
			File xmlFile = new File("/Users/paul/git/xmlMitPaul/src/test/resources/meta.xml");
			// Checking if Duplicates are found
			assertEquals("resources", FixForXmlFiles.findDuplicates(xmlElementsList, xmlFile));
		} catch (Exception e) {
			fail("Exception thrown during test", e);
		}

		try {
			// Test values
			List<String> xmlElementsList = Arrays.asList("<root xlink:href=\"00001.tif\"/>",
					"<root xlink:href=\"00001.tif\"/>", "<root xlink:href=\"00001.tif\"/>",
					"<root xlink:href=\"00001.tif\"/>", "<root xlink:href=\"00001.tif\"/>",
					"<root xlink:href=\"00001.tif\"/>");
			File xmlFile = new File("/src/test/resources/meta.xml");
			// Checking if Duplicates are found
			assertEquals("resources", FixForXmlFiles.findDuplicates(xmlElementsList, xmlFile));
		} catch (Exception e) {
			fail("Exception thrown during test", e);
		}

		try {
			// Test values
			List<String> xmlElementsList = Arrays.asList("<root xlink:href=\"00001.tif\"/>",
					"<root xlink:href=\"00002.tif\"/>", "<root xlink:href=\"00003.tif\"/>",
					"<root xlink:href=\"00004.tif\"/>", "<root xlink:href=\"00005.tif\"/>",
					"<root xlink:href=\"00001.tif\"/>");
			File xmlFile = new File("/src/test/resources/meta.xml");
			// Checking if Duplicates are found
			String expectedDirectoryName = "resources"; // The example directory
			assertEquals(expectedDirectoryName, FixForXmlFiles.findDuplicates(xmlElementsList, xmlFile));
		} catch (Exception e) {
			logger.error("testFindDuplicatesWithDuplicates exception" + e);
		}
	}

	@Test
	void testGenerateBackupFile() {
	    try {
	        // Path to the original XML file
	        String filePath = "src/test/resources/meta.xml";
	        File xmlFilePath = new File(filePath);
	        
	        // Create a backup copy
	        Path expectedBackupFilePath = FixForXmlFiles.generateBackupFile(xmlFilePath);
	        
	        // Check if the backup copy exists
	        assertTrue(expectedBackupFilePath.toFile().exists());

	        // Verify content
	        List<String> originalLines = Files.readAllLines(xmlFilePath.toPath());
	        List<String> backupLines = Files.readAllLines(expectedBackupFilePath);
	        assertEquals(originalLines, backupLines);
	        
	    } catch (IOException e) {
	        // Fail the test if an exception is thrown
	    	logger.error("testGenerateBackupFile exception" + e);
	    }
	}

}

package io.goobi.dlc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
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

import org.apache.logging.log4j.core.config.Configurator;

class FixForXmlFiles_Test {
	private static boolean logCreated = false;
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

			assertEquals(2, result.size());
			assertTrue(result.contains(file1));
			assertTrue(result.contains(file2));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
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
			File validXmlFile = new File("src/test/resources/meta.xml");

			Element result = FixForXmlFiles.processXmlFile(validXmlFile);

			assertNotNull(result);
		} catch (Exception e) {
			fail("Exception thrown during test", e);
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

	        List<String> xmlElementsList = FixForXmlFiles.collectXmlElements(rootElement, xmlFile);
	        assertTrue(xmlElementsList.size() > 0);
	    } catch (IOException e) {
	        fail("Exception thrown during test", e);
	    }
	}

	@Test
	public void testFindDuplicatesWithDuplicates() {
		try {
			// Checking if Duplicates are found
			List<String> xmlElementsList = Arrays.asList("<root xlink:href=\"00001.tif\"/>",
					"<root xlink:href=\"00001.tif\"/>", "<root xlink:href=\"00002.tif\"/>",
					"<root xlink:href=\"00003.tif\"/>", "<root xlink:href=\"00003.tif\"/>",
					"<root xlink:href=\"00004.tif\"/>");
			File xmlFile = new File("/Users/paul/git/xmlMitPaul/src/test/resources/meta.xml");

			assertEquals("resources", FixForXmlFiles.findDuplicates(xmlElementsList, xmlFile));
		} catch (Exception e) {
			fail("Exception thrown during test", e);
		}

		try {
			List<String> xmlElementsList = Arrays.asList("<root xlink:href=\"00001.tif\"/>",
					"<root xlink:href=\"00001.tif\"/>", "<root xlink:href=\"00001.tif\"/>",
					"<root xlink:href=\"00001.tif\"/>", "<root xlink:href=\"00001.tif\"/>",
					"<root xlink:href=\"00001.tif\"/>");
			File xmlFile = new File("/src/test/resources/meta.xml");

			assertEquals("resources", FixForXmlFiles.findDuplicates(xmlElementsList, xmlFile));
		} catch (Exception e) {
			fail("Exception thrown during test", e);
		}

		try {
			List<String> xmlElementsList = Arrays.asList("<root xlink:href=\"00001.tif\"/>",
					"<root xlink:href=\"00002.tif\"/>", "<root xlink:href=\"00003.tif\"/>",
					"<root xlink:href=\"00004.tif\"/>", "<root xlink:href=\"00005.tif\"/>",
					"<root xlink:href=\"00001.tif\"/>");
			File xmlFile = new File("/src/test/resources/meta.xml");

			String expectedDirectoryName = "resources"; // The example directory
			assertEquals(expectedDirectoryName, FixForXmlFiles.findDuplicates(xmlElementsList, xmlFile));
		} catch (Exception e) {
			fail("Exception thrown during test", e);
		}
	}

	@Test
	void testGenerateBackupFile() {
	    try {
	        // Pfad zur ursprünglichen XML-Datei
	    	String filePath = "src/test/resources/meta.xml";
	    	File xmlFilePath = new File(filePath);
	    	
	    	// Sicherungskopie erstellen
	    	String expectedBackupFilePath = FixForXmlFiles.generateBackupFile(xmlFilePath);
	        File expectedBackupFile = new File(expectedBackupFilePath);
	        
	    	// Überprüfen, ob die Sicherungskopie existiert
	    	assertTrue(expectedBackupFile.exists());

	    	// Inhalt überprüfen
	    	List<String> originalLines = Files.readAllLines(xmlFilePath.toPath());
	    	List<String> backupLines = Files.readAllLines(expectedBackupFile.toPath());
	    	assertEquals(originalLines, backupLines);
	    	
	    } catch (IOException e) {
	        fail("Exception thrown during test", e);
	    }
	}

	@BeforeEach
	private void loggingTests() {
	    if (logCreated) {
	        return;
	    }
	    try {
	        Configurator.initialize(null, "log4j2.xml");
	        logCreated = true;
	    } catch (Exception e) {
	        fail("Exception thrown during test", e);
	    }
	}
}
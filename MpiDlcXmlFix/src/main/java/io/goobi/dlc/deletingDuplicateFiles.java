package io.goobi.dlc;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class deletingDuplicateFiles {
	private static final Logger logger = LogManager.getLogger(deletingDuplicateFiles.class);
	
	public static void main(String[] args) {
		Configurator.setRootLevel(org.apache.logging.log4j.Level.DEBUG);
		int duplicateFiles = 0;
		if (args.length != 1) {
        	logger.error("Please specify only one directory."); 
        } else {
        	File directory = new File(args[0]).getAbsoluteFile();
            if (directory.exists() && directory.isDirectory()) {
            	duplicateFiles = processFiles(directory);
            }
        }
		System.out.println(duplicateFiles);
	}
	static int processFiles(File directory) {
        int duplicateFiles = 0;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
            	// Finds all the files that are named like duplicates
                for (File file : files) {
                	if (file.getName().contains("meta_") && file.getName().endsWith(".xml")) {
                        logger.trace("Deleting file: " + file.getAbsolutePath());
                        duplicateFiles += 1;
                        file.delete();
                    }
                }
            }
        }
        return duplicateFiles;
    }
}

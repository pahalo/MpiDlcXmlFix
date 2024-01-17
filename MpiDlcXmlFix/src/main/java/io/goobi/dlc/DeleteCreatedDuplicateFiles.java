package io.goobi.dlc;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * This class deletes duplicate files created during a specific process.
 * It looks for files in a given directory that match a certain pattern and deletes them.
 */
public class DeleteCreatedDuplicateFiles {

    private static final Logger logger = LogManager.getLogger(DeleteCreatedDuplicateFiles.class);

    /**
     * The main entry point of the application.
     * @param args Command line arguments - specify only one directory.
     */
    public static void main(String[] args) {
        // Set the root log level to DEBUG
        Configurator.setRootLevel(org.apache.logging.log4j.Level.DEBUG);
        int duplicateFiles = 0;

        if (args.length != 1) {
            // Log an error if the number of specified directories is not equal to one
            logger.error("Please specify only one directory.");
        } else {
            // Process the specified directory if it exists and is a directory
            File directory = new File(args[0]).getAbsoluteFile();
            if (directory.exists() && directory.isDirectory()) {
                duplicateFiles = processFiles(directory);
            }
        }

        // Log the total number of duplicate files found and deleted
        logger.info("Total duplicate files deleted: " + duplicateFiles);
    }

    /**
     * Process files in the given directory, deleting files that match a specific pattern.
     * @param directory The directory to process.
     * @return The number of duplicate files deleted.
     */
    private static int processFiles(File directory) {
        int duplicateFiles = 0;

        if (directory.isDirectory()) {
            // List all files in the directory
            File[] files = directory.listFiles();
            if (files != null) {
                // Find and delete files that match the specified pattern
                for (File file : files) {
                    if (file.getName().contains("meta_") && file.getName().endsWith(".xml")) {
                        // Log the deletion of each duplicate file
                        logger.trace("Deleting file: " + file.getAbsolutePath());
                        duplicateFiles += 1;
                        // Delete the file
                        file.delete();
                    }
                }
            }
        }

        return duplicateFiles;
    }
}

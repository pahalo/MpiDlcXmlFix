package io.goobi.dlc;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class deletes backuped files created during a specific process. It looks for files in a given directory and its subdirectories that match a
 * certain pattern and deletes them.
 */
public class CleanupBackups {

    private static final Logger logger = LogManager.getLogger(CleanupBackups.class);

    /**
     * The main entry point of the application.
     *
     * @param args Command line arguments - specify only one directory.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            // Log an error if the number of specified directories is not equal to one
            logger.error("Please specify only one directory.");
        } else {
            // Process the specified directory and its subdirectories if it exists
            File directory = new File(args[0]).getAbsoluteFile();
            if (directory.exists()) {
                CleanupBackups fileProcessor = new CleanupBackups();
                int numberOfBackups = fileProcessor.processFiles(directory);
                logger.info("Total duplicate files deleted: " + numberOfBackups);
            } else {
                // Log an error if the specified path is not a valid directory
                logger.error("Please specify a valid directory.");
            }
        }
    }

    /**
     * Process files in the given directory and its subdirectories, deleting files that match a specific pattern.
     *
     * @param directory The directory to process.
     * @return The number of duplicate files deleted.
     */
    int processFiles(File directory) {
        int numberOfBackups = 0;

        if (directory.isDirectory()) {
            // List all files in the directory, including subdirectories
            File[] files = directory.listFiles();
            if (files != null) {
                // Find and delete files that match the specified pattern
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Recursively process files in subdirectories
                        numberOfBackups += processFiles(file);
                        // If the filename is in this format: meta.xml.yyyy-MM-dd-HHmmssSSS then it will be deleted
                    } else if (file.getName().matches("meta\\.xml\\.\\d+.*")) {
                        logger.trace("Deleting file: " + file.getAbsolutePath());
                        numberOfBackups++;
                        // Delete the file
                        file.delete();
                    }
                }
            }
        }
        return numberOfBackups;
    }
}
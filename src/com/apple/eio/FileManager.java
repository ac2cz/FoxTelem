package com.apple.eio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileManager {
	
	public static final short kOnAppropriateDisk = 0;
	public static final short kSystemDomain = 0;
	public static final short kLocalDomain = 0;
	public static final short kNetworkDomain = 0;
	public static final short kUserDomain = 0;
	
	public FileManager() {}

    public static String findFolder(int folderType) throws FileNotFoundException {
        return null;
    }

    public static String findFolder(short domain, int folderType) throws FileNotFoundException {
        return null;
    }

    public static String findFolder(short domain, int folderType, boolean createIfNeeded) throws FileNotFoundException {
        return null;
    }

    public static int getFileCreator(String filename) throws IOException {
        return 0;
    }

    public static int getFileType(String filename) throws IOException {
        return 0;
    }
    
    public static String getPathToApplicationBundle() {
    	return null;
    }

    public static String getResource(String resourceName) throws FileNotFoundException {
        return null;
    }

    public static String getResource(String resourceName, String subDirName) throws FileNotFoundException {
        return null;
    }
    
    public static String getResource(String resourceName, String subDirName, String type) throws FileNotFoundException {
        return null;
    }

    @Deprecated
    public static void openURL(String url) throws IOException {}
    
    public static int OSTypeToInt(String type) {
    	return 0;
    }

    public static void setFileCreator(String fileName, int creator) throws IOException {}

    public static void setFileType(String fileName, int creator) throws IOException {}

    public static void setFileTypeAndCreator(String fileName, int type, int creator) throws IOException {}

    /** 
     * @since Added 10.6 Update 1 and 10.5 Update 5 
     */
    public static boolean revealInFinder(File file) throws FileNotFoundException {
        return false;
    }


    /** 
     * @since Added 10.6 Update 1 and 10.5 Update 5 
     */
    public static boolean moveToTrash(File file) throws FileNotFoundException {
        return false;
    }
}

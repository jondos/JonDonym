package jap;

import java.util.Observable;

/**
 * A generic interface that allows the JAPModel to maintain a consistent file storage layer 
 * when the path for external help files changes. It also allows to obtain information about storage 
 * processes via the observer pattern.
 * @author Simon Pecher
 *
 */
public interface HelpFileStorageManager 
{
	public static final String HELP_INVALID_NULL = "invalidHelpPathNull";
	public static final String HELP_INVALID_PATH_NOT_EXISTS = "invalidHelpPathNotExists";
	public static final String HELP_INVALID_NOWRITE = "invalidHelpPathNoWrite";
	public static final String HELP_DIR_EXISTS = "helpDirExists";
	public static final String HELP_JONDO_EXISTS = "helpJonDoExists";
	public static final String HELP_VALID = "HELP_IS_VALID";
	
	/**
	 * Performs the specific file storage operation to maintain a consistent file storage state
	 * when the help path changes
	 * @param oldHelpPath the path of the help files before the change.
	 * @param newHelpPath the path where the help files shall be installed
	 * @return true if the storage layer could be maintained consistent, false otherwise.
	 * 			In the latter case the file storage layer must be in the same state as if 
	 * 			there has not been a change at all.
	 */
	public boolean handleHelpPathChanged(String oldHelpPath, String newHelpPath);
	
	public String helpPathValidityCheck(String absolutePath);
	
	/**
	 * returns an observable object which allows ViewObjects to display storage processes, i.e.
	 * to display the file installation progress in a JProgressBar
	 * @return an observable object from which to obtain storage process informations or null
	 * 			if the manager has no such object associated with it.
	 */
	public Observable getStorageObservable();
	
	public void ensureMostRecentVersion(String helpPath);
	
	public boolean helpInstallationExists(String helpPath);
}

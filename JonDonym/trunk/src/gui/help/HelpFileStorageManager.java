/*
	Copyright (c) The JAP-Team, JonDos GmbH
	All rights reserved.
	Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
	Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
	Redistributions in binary form must reproduce the above copyright notice,
	 this list of conditions and the following disclaimer in the documentation and/or
	 other materials provided with the distribution.
	Neither the name of the University of Technology Dresden, Germany, nor the name of
	 the JonDos GmbH, nor the names of their contributors may be used to endorse or 
	 promote products derived from this software without specific prior written permission.
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
	"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
	LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
	A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
	EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
	PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
	LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package gui.help;

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
	public static final String HELP_NO_DIR = "helpNoDir";
	public static final String HELP_DIR_EXISTS = "helpDirExists";
	public static final String HELP_JONDO_EXISTS = "helpJonDoExists";
	public static final String HELP_NESTED = "helpNested";
	public static final String HELP_VALID = "HELP_IS_VALID";
	
	/**
	 * Performs the specific file storage operation to maintain a consistent file storage state
	 * when the help path changes
	 * @param oldHelpPath the path of the help files before the change.
	 * @param newHelpPath the path where the help files shall be installed
	 * @param a_bIgnoreExistingHelpDir true if any existing directory named help
	 * will be removed in the installation folder
	 * @return true if the storage layer could be maintained consistent, false otherwise.
	 * 			In the latter case the file storage layer must be in the same state as if 
	 * 			there has not been a change at all.
	 */
	public boolean handleHelpPathChanged(String oldHelpPath, 
			String newHelpPath, boolean a_bIgnoreExistingHelpDir);
	
	public String helpPathValidityCheck(String absolutePath, boolean a_bIgnoreExistingHelpDir);
	
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

/*
 Copyright (c) 2000 - 2006, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package jpi.db;

import logging.LogType;
import logging.LogHolder;
import logging.LogLevel;
import anon.pay.xml.XMLPaymentSettings;
import jpi.Configuration;

/**
  * Thread that periodically (every 5 minutes) updates the configuration
  * by reading the current paymentsetting from the database table
  * and calling Configuration.updateSettings()
  *
  * Thereby makes sure that the option settings that Configuration returns
  * are the ones from the database, not the config file,
  * and that updates to the database are written to Configuration
  *
  * started on Jpi startup
  *
 * @author Elmar Schraml
 */
public class ConfigurationUpdater extends Thread
{
	private static final long UPDATE_INTERVAL = 1000 * 60* 5; //5 minutes

	public void run()
	{
		try
		{
			//get current settings from db
			DBInterface dbConn = DBSupplier.getDataBase();
			XMLPaymentSettings allSettings = dbConn.getPaymentSettings();
			//Configuration will handle the update internally
			Configuration.updateSettings(allSettings);
			//wait before doint it again
			sleep(UPDATE_INTERVAL);
		}
		catch (InterruptedException ex)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "ConfigurationUpdater thread has been interrupted");
			//probably okay, just continue
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "error (probably caused by the database) in ConfigurationUpdater thread");
			//nothing else we can do about it
		}
	}
}

/*
 Copyright (c) 2000, The JAP-Team
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
package jap;

import java.io.FileNotFoundException;
import java.util.Locale;

import gui.JAPMessages;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

final public class JAPiPAQ
{

	private JAPNewView view;
	private JAPController m_controller;

	public JAPiPAQ()
	{
	}

	public void startJAP(String strJapConfFile)
	{
		// Init Messages....
		JAPMessages.init(JAPConstants.MESSAGESFN);
		JAPModel.getInstance().setSmallDisplay(true);
		// Test (part 2) for right JVM....
		// Create the controller object
		m_controller = JAPController.getInstance();
		// Create debugger object
		LogHolder.setLogInstance(JAPDebug.getInstance());
		JAPDebug.getInstance().setLogType(LogType.NET + LogType.GUI + LogType.THREAD + LogType.MISC);
		JAPDebug.getInstance().setLogLevel(LogLevel.WARNING);
		// load settings from config file
		m_controller.loadConfigFile(strJapConfFile, null);

		// Output some information about the system
		// Create the view object
		view = new JAPNewView(JAPConstants.TITLE, m_controller);
		// Create the main frame
		view.create(false);
		// Switch Debug Console Parent to MainView
		JAPDebug.setConsoleParent(view);
		// Add observer
		m_controller.addJAPObserver(view);
		// Register the views where they are needed
		m_controller.setView(view, new ConsoleSplash());
		// initially start services
		m_controller.initialRun(null, 0);

		view.setSize(240, 300);
		view.setLocation(0, 0);
		view.setResizable(false);
		view.setVisible(true);
	}

	public void setLocale(Locale l)
	{
		JAPMessages.setLocale(l);
	}

	public static void main(String[] argv)
	{
		JAPiPAQ japOniPAQ = new JAPiPAQ();
		japOniPAQ.startJAP(null);
		//Test
		//JFrame frame=new JFrame("JAP");
		//frame.setIconImage(JAPUtil.);
		//frame.setContentPane(japOniPAQ.getMainPanel());
	}

}

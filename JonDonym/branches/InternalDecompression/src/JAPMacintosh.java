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
import jap.JAPController;
import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

public class JAPMacintosh extends JAP 
{

	JAPMacintosh(String[] argv) 
	{
		super(argv);

	}

	protected void registerMRJHandlers() {
		try
		{
			Application app=Application.getApplication();
			app.addApplicationListener(new AppListener());
			app.addPreferencesMenuItem();
			app.setEnabledAboutMenu(true);
			app.setEnabledPreferencesMenu(true);
		}
		catch(Exception e)
		{
		//Register MRJ handlers for about and quit.
		MRJI IMRJI = new MRJI();
		com.apple.mrj.MRJApplicationUtils.registerQuitHandler(IMRJI);
		com.apple.mrj.MRJApplicationUtils.registerAboutHandler(IMRJI);
		}
	}

	class AppListener extends ApplicationAdapter
	{
		public void handleAbout(ApplicationEvent event)
		{
			JAPController.aboutJAP();
			event.setHandled(true);
		}

		public void handleQuit(ApplicationEvent event)
		{
			event.setHandled(true);
			JAPController.goodBye(true);
		}
		public void handlePreferences(ApplicationEvent event)
		{
			event.setHandled(true);
			JAPController.getInstance().showConfigDialog();
		}		
}
	// Inner class defining the MRJ Interface
	class MRJI implements com.apple.mrj.MRJQuitHandler, com.apple.mrj.MRJAboutHandler
	{
		public void handleQuit() {
			JAPController.goodBye(true);
		}
		public void handleAbout() {
			JAPController.aboutJAP();
		}
	}
	
	public static void main(String[] argv)
	{
		JAPMacintosh japOnMac = new JAPMacintosh(argv);
		japOnMac.registerMRJHandlers();
		japOnMac.startJAP();
	}
}

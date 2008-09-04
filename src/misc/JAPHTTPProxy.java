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
package misc;
import jap.JAPDebug;

import java.net.ServerSocket;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import proxy.DirectProxy;
final public class JAPHTTPProxy {
	boolean runFlag;
	int port = 4008;
	ServerSocket server;

	public JAPHTTPProxy (int port) {
		this.port = port;
		// Create debugger object
		LogHolder.setLogInstance(JAPDebug.getInstance());
		JAPDebug.getInstance().setLogType(LogType.NET+LogType.GUI+LogType.THREAD+LogType.MISC);
		JAPDebug.getInstance().setLogLevel(LogLevel.DEBUG);
	}

	public void startService() {
		server = null;
		runFlag = true;
	//	while (runFlag) {
			System.out.println("Service on port " + port + " started.");
			try {
				server = new ServerSocket (port);
        DirectProxy c = new DirectProxy(server);
        c.startService();
//          Thread ct = new Thread(c);
	//				ct.start();
				//}
			} catch (Exception e) {
				try {
					server.close();
				} catch (Exception e2) {
					;
				}
				LogHolder.log(LogLevel.EXCEPTION,LogType.NET,"Exception: " +e);
			}
		//}
		//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Service on port " + port + " stopped.");
	}

	public void stopService() {
		runFlag = false;
		try {
			server.close();
		} catch(Exception e) {
			;
		}
	}

	public static void help() {
		System.out.println("HTTPProxy");
		System.out.println(" Options: -debug -port <port>");
	}

	public static void main(String[] args) {
		int cmdPort = 4001;
		boolean showHelp=false;
		if (args.length==0)
					showHelp=true;
		try {
		    for (int i = 0; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("-port")) {
		    		cmdPort = Integer.parseInt(args[i+1]);
		    		i++;
				} else {
					showHelp=true;
	    		}
		    }
		} catch (Exception e) {
					showHelp=true;
		}
		if (showHelp)
			help();
		JAPHTTPProxy p = new JAPHTTPProxy(cmdPort);
		p.startService();

	}

}

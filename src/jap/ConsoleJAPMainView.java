package jap;

/*
 Copyright (c) 2006, The JAP-Team
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
 */import javax.swing.JPanel;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.MixCascade;
import anon.AnonServerDescription;
import java.awt.event.ActionListener;

/**
 *
 * @author Rolf Wendolsky
 */
public class ConsoleJAPMainView implements IJAPMainView
{

	public int addStatusMsg(String msg, int type, boolean bAutoRemove)
	{
		LogHolder.log(LogLevel.ALERT, LogType.MISC, msg);
		return 0;
	}

	public void doClickOnCascadeChooser()
	{
	}
	
	public String[] getBrowserCommand()
	{
		return null;
	}
	
	public void updateValues(boolean bSync)
	{
	}

	public void showConfigDialog()
	{
	}
	
	public void showConfigDialog(String card, Object a_value)
	{
	}


	/**
	 * Shows the console view.
	 */
	public void setVisible(boolean a_bVisible)
	{
		try
		{
			String entered = null;
			while (true)
			{
				System.out.println("Type 'exit' to quit or 'save' to save the configuration.");
				entered = new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
				if (entered == null)
				{
					continue;
				}
				if (entered.equals("exit"))
				{
					break;
				}
				if (entered.equals("save"))
				{
					System.out.println("Saving configuration...");
					if (!JAPController.getInstance().saveConfigFile())
					{
						System.out.println("Configuration saved!");
					}
					else
					{
						System.out.println("Error while saving configuration!");
					}
				}

			}
			JAPController.goodBye(true);
		}
		catch (java.io.IOException a_e)
		{
		}
	}

	public void channelsChanged(int channels)
	{
	}

	public void packetMixed(long a_totalBytes)
	{
	}

	public void dataChainErrorSignaled()
	{
		LogHolder.log(LogLevel.ALERT, LogType.NET, "Disconnected because the service proxy is not working!");
	}

	public void disconnected()
	{
		LogHolder.log(LogLevel.ALERT, LogType.NET, "Disconnected!");
	}

	public void connectionError()
	{
		LogHolder.log(LogLevel.ALERT, LogType.NET, "Disconnected because of connection error!");
	}

	public void connecting(AnonServerDescription a_serverDescription)
	{
		if (a_serverDescription instanceof MixCascade)
		{
			MixCascade cascade = (MixCascade)a_serverDescription;
			LogHolder.log(LogLevel.ALERT, LogType.NET, "Connecting to " +
						  cascade.getId() + "(" + cascade.getName() + ")" + "...");
		}
		else
		{
			LogHolder.log(LogLevel.ALERT, LogType.NET, "Connecting...");
		}
	}


	public void connectionEstablished(AnonServerDescription a_serverDescription)
	{
		if (a_serverDescription instanceof MixCascade)
		{
			MixCascade cascade = (MixCascade)a_serverDescription;
			LogHolder.log(LogLevel.ALERT, LogType.NET, "Connected to " +
						  cascade.getId() + "(" + cascade.getName() + ")" + "!");
		}
		else
		{
			LogHolder.log(LogLevel.ALERT, LogType.NET, "Connected!");
		}
	}

	public void create(boolean bWithPay)
	{
	}

	public void disableSetAnonMode()
	{
	}

	public void onUpdateValues()
	{
	}

	public JPanel getMainPanel()
	{
		return null;
	}

	public void registerViewIconified(JAPViewIconified viewIconified)
	{
	}

	public JAPViewIconified getViewIconified()
	{
		return null;
	}

	public void removeStatusMsg(int id)
	{
	}

	public void transferedBytes(long bytes, int protocolType)
	{
	}
}

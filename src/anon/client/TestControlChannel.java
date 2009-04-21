/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.client;

import java.util.Observable;
import java.util.Observer;

import org.w3c.dom.Document;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.IServiceContainer;
import anon.util.XMLUtil;

/**
 * This is the implementation for control channel test which uses the test control channel for sending a dummy XML message and receiving the echo.
 */
public class TestControlChannel extends XmlControlChannel implements Runnable
{
	public static final int DT_MIN_INTERVAL_MS = 500;
	public static final int DT_MAX_INTERVAL_MS = 30000;
	public static final int DT_DISABLE = Integer.MAX_VALUE;
	
  /**
   * Stores whether the internal thread shall work (true) or come to the end
   * (false).
   */
  private volatile boolean m_bRun;

  /**
   * Stores the instance of the internal dummy traffic thread.
   */
  private Thread m_threadRunLoop;

  /**
   * Stores the dummy traffic interval in milliseconds. After that interval of
   * inactivity (no traffic) on the connection, a dummy packet is sent.
   */
  private long m_interval = DT_MAX_INTERVAL_MS;

  private Object m_internalSynchronization;

  /**
   * Creates a new DummyTrafficControlChannel instance. The dummy traffic
   * interval is set to -1 (dummy traffic disabled) and the internal thread
   * isn't started.
   *
   * a_multiplexer The multiplexer the new DummyTrafficControlChannel instance
   *               belongs to.
   */
  public TestControlChannel(Multiplexer a_multiplexer, IServiceContainer a_serviceContainer) {
    super(ChannelTable.CONTROL_CHANNEL_ID_TEST, a_multiplexer, a_serviceContainer,true);
    m_internalSynchronization = new Object();
    m_bRun = false;
    m_threadRunLoop = null;
    m_interval = -1;
  }


  /**
   * This is the implementation for the dummy traffic thread.
   */
  public void run() 
  {
	  LogHolder.log(LogLevel.NOTICE, LogType.NET, "Test control channel sent interval: " + m_interval + "ms");
	  while (m_bRun) 
	  {
		  try 
		  {
			  Thread.sleep(m_interval);
			  if (m_bRun)
			  {
				  /* if we reach the timeout without interruption, we have to send a dummy */
				  LogHolder.log(LogLevel.INFO, LogType.NET, "Sending control channel test message!");
				  Document doc=XMLUtil.createDocument();
				  doc.appendChild(doc.createElement("TestControlChannel"));
				  sendXmlMessage(doc);
			  }
	      }	     
		  catch (InterruptedException e) 
		  {
			  //LogHolder.log(LogLevel.WARNING, LogType.NET, "Dummy thread interrupted!");
			  /* if we got an interruption within the timeout, everything is ok */
		  }
      }
  }

	/**
	 * Holds the internal dummy traffic thread. This method blocks until the
	 * internal thread has come to the end.
	 * @todo: stopping dummy traffic sometimes causes deadlocks 
	 * 		(when Infoservice performance test is running)
	 */
  	public void stop() 
  	{
	    synchronized (m_internalSynchronization) 
	    {
	    	m_bRun = false;
	     	
	    	if (m_threadRunLoop != null) 
	    	{
	    	  	while (m_threadRunLoop.isAlive())
				{
	    	  		LogHolder.log(LogLevel.NOTICE, LogType.NET, "Shutting down test control channel...");
	    	  		m_threadRunLoop.interrupt(); 
	    	  		
	    	  		try
	    	  		{
	    	  			m_threadRunLoop.join();
	    	  		}					
					catch (InterruptedException e)
					{
					}
				}
	    	  	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Test control channel closed!");
	    	  	m_threadRunLoop = null;
	    	}
	    }
  	}


  /**
   * Changes the dummy traffic interval.
   *
   * @param a_interval The new dummy traffic interval in milliseconds or -1,
   *                   if dummy traffic shall be disabled.
   */
  public void setMessageInterval(int a_interval) {
    synchronized (m_internalSynchronization) {
      stop();
      if(a_interval == DT_DISABLE)
      {
    	  LogHolder.log(LogLevel.WARNING, LogType.NET, "Test control channel sent disabled!");
    	  return;
      }
	  // force the use of dummy traffic < DT_MAX_INTERVAL_MS, so that the connection to the first Mix is held
	  if (a_interval < DT_MIN_INTERVAL_MS)
	  {
		  a_interval = DT_MIN_INTERVAL_MS;
	  }
	  else if  (a_interval > DT_MAX_INTERVAL_MS)
	  {
		  a_interval = DT_MAX_INTERVAL_MS;
	  }

      m_interval = (long)a_interval;
      if (a_interval > 0) {
        start();
      }
    }
  }


  /**
   * This method is called by the multiplexer, if a packet is received on the
   * test control channel. All received packets are simply ignored.
   *
   * @param a_packet The data within the received packet (should be random
   *                 bytes).
   */
  protected void processXmlMessage(Document doc) {
    /* simply log the packet */
    LogHolder.log(LogLevel.DEBUG, LogType.NET, "ControlChannelTest: Received control message:"+XMLUtil.toString(doc));
  }


  /**
   * Starts the internal dummy traffic thread, if it is not already running.
   */
  private void start() {
    synchronized (m_internalSynchronization) {
      if (m_bRun == false) {
        m_bRun = true;
        m_threadRunLoop = new Thread(this, "JAP - Control Channel Test");
        m_threadRunLoop.setDaemon(true);
        m_threadRunLoop.start();
      }
    }
  }

}

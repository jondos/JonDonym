/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package forward.server;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class manages the whole forwarding server and supplies some important methods to other
 * classes. This class is a singleton.
 */
public class ForwardServerManager {

  /**
   * This is the timeout for all socket methods on the client connection. We need a timeout there
   * because we have no keepalive option. Without a timeout, a physical interruption on the
   * connection would result in a open connection until the server shutdown. The default value
   * is 200 seconds. So if the client creates dummy traffic every 180 seconds, there should be
   * no problem.
   */
  public static final int CLIENT_CONNECTION_TIMEOUT = 200 * 1000;

  /**
   * This is the interval, we need dummy traffic from the client (in milliseconds). This value
   * should be a little bit smaller than the CLIENT_CONNECTION_TIMEOUT, so if the client
   * produces the dummy traffic with this rate, the connection keeps open and there should be
   * no problem with timeouts. The default value is dummy traffic every 180 seconds.
   */
  public static final int CLIENT_DUMMYTRAFFIC_INTERVAL = 180 * 1000;

  /**
   * Stores the instance of ForwardServerManager (Singleton).
   */
  private static ForwardServerManager ms_fsmInstance = null;


  /**
   * Stores the dummy traffic interval (in ms), if we need dummy traffic for holding a connection
   * while phases of inactivity. If this value is -1, no dummy traffic is needed.
   */
  private int m_dummyTrafficInterval;

  /**
   * Stores the database with with the MixCascades, where connections can be forwarded to.
   */
  private ForwardCascadeDatabase m_allowedCascadesDatabase;

  /**
   * Stores the associated ForwardScheduler. If this value is null, forwarding is disabled.
   */
  private ForwardScheduler m_forwardScheduler;


  /**
   * Returns the instance of ForwardServerManager (Singleton). If there is no instance,
   * there is a new one created.
   *
   * @return The ForwardServerManager instance.
   */
  public static ForwardServerManager getInstance() {
    if (ms_fsmInstance == null) {
      ms_fsmInstance = new ForwardServerManager();
    }
    return ms_fsmInstance;
  }


  /**
   * This creates a new instance of ForwardManager. Forwarding is disabled (m_forwardScheduler is
   * set to null).
   */
  private ForwardServerManager() {
    m_dummyTrafficInterval = -1;
    m_allowedCascadesDatabase = new ForwardCascadeDatabase();
    m_forwardScheduler = null;
  }


  /**
   * This sets the dummy traffic interval. If a connection has to take a proxy server, that
   * proxy maybe closes the connection after some time of inactivity. So there is the need of
   * doing some dummy traffic. This method doesn't have any effect on the local system, but any
   * new connecting client will get this value in the connection offer, so he has to create the
   * dummy traffic for us. Already established connections are not concerned by the change of
   * this value. The maximum dummy traffic interval is defined in CLIENT_DUMMYTRAFFIC_INTERVAL.
   * We need dummy traffic with at least that rate because of the timeout rule for any client
   * connection. So bigger intervals or disabling dummy traffic result in dummy traffic with
   * that default interval.
   *
   * @param a_interval The dummy traffic interval (in milliseconds) or -1, if dummy traffic is
   *                   disabled.
   */
  public void setDummyTrafficInterval(int a_interval) {
    if (a_interval >= 0) {
      /* dummy traffic enabled, take the smaller value */
      m_dummyTrafficInterval = Math.min(a_interval, CLIENT_DUMMYTRAFFIC_INTERVAL);
    }
    else {
      /* we need dummy traffic because of the connection timeout -> take the default interval */
      m_dummyTrafficInterval = CLIENT_DUMMYTRAFFIC_INTERVAL;
    }
  }

  /**
   * Returns the dummy traffic interval. Sometimes dummy traffic is needed for holding a
   * connection while phases of inactivity.
   *
   * @return The dummy traffic interval in milliseconds or -1, if no dummy traffic is needed.
   */
  public int getDummyTrafficInterval() {
    return m_dummyTrafficInterval;
  }

  /**
   * Returns the database with the MixCascades, where connections are allowed to be forwarded to.
   *
   * @return The database of allowed MixCascades.
   */
  public ForwardCascadeDatabase getAllowedCascadesDatabase() {
    return m_allowedCascadesDatabase;
  }

  /**
   * Changes the number of simultaneously forwarded client connections. If the new number is less
   * than the old one and there are more forwarded connections at the moment, we closes some
   * randomly choosed connections. If forwarding is not enabled while the call of this function,
   * nothing happens.
   *
   * @a_maximumNumberOfConnections The new maximum number of simultaneously forwarded client
   *                               connections.
   */
  public void setMaximumNumberOfConnections(int a_maxNumberOfConnections) {
    synchronized (this) {
      /* only one operation on the scheduler at one time */
      if (m_forwardScheduler != null) {
        m_forwardScheduler.setMaximumNumberOfConnections(a_maxNumberOfConnections);
      }
    }
  }

  /**
   * Changes the maximum bandwidth (net bandwidth, without TCP/IP headers...) which can be used
   * by all client connections together. If forwarding is not enabled while the call of this
   * function, nothing happens.
   *
   * @param a_netBandwidth The maximum bandwidth for all client connections together (= average
   *                       upstream = average downstream) in bytes/sec.
   */
  public void setNetBandwidth(int a_netBandwidth) {
    synchronized (this) {
      /* only one operation on the scheduler at one time */
      if (m_forwardScheduler != null) {
        m_forwardScheduler.setNetBandwidth(a_netBandwidth);
      }
    }
  }

  /**
   * This opens a listen socket at the specified portnumber. Clients can connect to this socket
   * and the associated ForwardScheduler will manage that forwarded connections. It is possible
   * that the ForwardScheduler manages more than one listening socket. If forwarding is not
   * enabled while the call of this function, nothing happens and the result is false.
   *
   * @param a_portNumber The portnumber where the new server socket will listen on.
   *
   * @return An Object with the ID of the created ServerManager, which can be used to close that
   *         ServerManager. If there occured an error while opening the socket or there is no
   *         ForwardingScheduler running (normally because the forwarding server is not running),
   *         null is returned.
   */
  public Object addListenSocket(int a_portNumber) {
    Object serverManagerId = null;
    synchronized (this) {
      /* only one operation on the scheduler at one time */
      if (m_forwardScheduler != null) {
        ServerSocketManager serverSocketManager = new ServerSocketManager(a_portNumber);
        try {
          m_forwardScheduler.addServerManager(serverSocketManager);
          /* ServerSocketManager established successful */
          serverManagerId = serverSocketManager.getId();
          LogHolder.log(LogLevel.DEBUG, LogType.NET, "Establishing ServerManager with ID '" + serverManagerId.toString() + "' was successful.");
        }
        catch (Exception e) {
          LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "Error establishing socket at port " + Integer.toString(a_portNumber) + ". Reason: " + e.toString());
        }
      }
    }
    return serverManagerId;
  }
  
  /**
   * Adds an ServerManager. Clients can connect to it as it starts Listning on his own Endpoint
   * and the associated ForwardScheduler will manage that forwarded connections. If forwarding is not
   * enabled while the call of this function, nothing happens and the result is false. 
   */ 
  public Object addServerManager(IServerManager a_manager) {
	  Object serverManagerId = null;
	    synchronized (this) {
	      if (m_forwardScheduler != null) {
	        try {
	          m_forwardScheduler.addServerManager(a_manager);
	          /* ServerSocketManager established successful */
	          serverManagerId = a_manager.getId();
	          LogHolder.log(LogLevel.DEBUG, LogType.NET, "Establishing ServerManager with ID '" + serverManagerId.toString() + "' was successful.");
	        }
	        catch (Exception e) {
	          LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "Error adding Servermanager of Type "+ a_manager.getClass().getName() +
	        		  ". Reason: " + e.toString());
	        }
	      }
	    }
	    return serverManagerId;
  }

  /**
   * Removes one ServerManager from the list of associated ServerManagers of this
   * ForwardScheduler. The shutdown() method is called on that ServerManager and it is removed
   * from the internal list. Active forwarded connections are not affected by this call. If the
   * ForwardScheduler isn't running or it doesn't know any ServerManager with the specified ID,
   * nothing is done.
   *
   * @param a_serverManagerId The ID of the ServerManager to close, see IServerManager.getId().
   */
  public void removeServerManager(Object a_serverManagerId) {
    if (a_serverManagerId != null) {
      synchronized (this) {
        if (m_forwardScheduler != null) {
          m_forwardScheduler.removeServerManager(a_serverManagerId);
          LogHolder.log(LogLevel.DEBUG, LogType.NET, "ForwardServerManager: removeServerManager: ServerManager with ID '" + a_serverManagerId.toString() + "' was removed (if it was running).");
        }
      }
    }
    else {
      LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "ForwardServerManager: removeServerManager: ServerManager ID null is invalid.");
    }
  }

  /**
   * Removes all ServerManagers from the list of associated ServerManagers of the internal
   * ForwardScheduler. The shutdown() method is called on every ServerManager. There can't be
   * opened new connections until at least one server manager is added to the list. Active
   * forwarded connections are not affected by this call.
   */
  public void removeAllServerManagers() {
    synchronized (this) {
      if (m_forwardScheduler != null) {
        m_forwardScheduler.removeAllServerManagers();
        LogHolder.log(LogLevel.DEBUG, LogType.NET, "ForwardServerManager: removeAllServerManagers: All server managers removed.");
      }
    }
  }

  /**
   * This method must be called, when forwarding shall come to an end. This method blocks until
   * all connections, sockets and threads are closed. If forwarding is not enabled while the call
   * of this function, nothing happens.
   */
  public void shutdownForwarding() {
    synchronized (this) {
      /* only one operation on the scheduler at one time */
      if (m_forwardScheduler != null) {
        m_forwardScheduler.shutdown();
        m_forwardScheduler = null;
      }
    }
  }

  /**
   * This method starts the forwarding code. The initial bandwidth is set to 0 bytes/sec, the
   * maximum number of forwarded connections is set to 0 and there are no ServerManagers bound to
   * the ForwardScheduler. So the corresponding methods must be called to change those values.
   * If the forwarding code is already running, nothing is done.
   */
  public void startForwarding() {
    synchronized (this) {
      /* only one operation on the scheduler at one time */
      if (m_forwardScheduler == null) {
        m_forwardScheduler = new ForwardScheduler();
      }
    }
  }

  /**
   * Returns the statistics instance of the scheduler. If no scheduler is running, null is
   * returned.
   *
   * @return The statistics instance of the scheduler or null, if no scheduler is running.
   */
  public ForwardSchedulerStatistics getSchedulerStatistics() {
    ForwardSchedulerStatistics schedulerStatistics = null;
    synchronized (this) {
      /* only one operation on the scheduler at one time */
      if (m_forwardScheduler != null) {
        schedulerStatistics = m_forwardScheduler.getStatistics();
      }
    }
    return schedulerStatistics;
  }

  /**
   * Returns the number of currently forwarded connections. If no scheduler is running, 0 is
   * returned.
   *
   * @return The number of currently forwarded connections.
   */
  public int getCurrentlyForwardedConnections() {
    int forwardedConnections = 0;
    synchronized (this) {
      /* only one operation on the scheduler at one time */
      if (m_forwardScheduler != null) {
        forwardedConnections = m_forwardScheduler.getCurrentlyForwardedConnections();
      }
    }
    return forwardedConnections;
  }

}

/*
 Copyright (c) 2000 - 2005 The JAP-Team
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
package infoservice.mailsystem.central;

import java.net.InetAddress;

import infoservice.mailsystem.central.server.GenericServer;

/**
 * This class is the main class for the JAP mail system. It parses the command-line options
 * and initializes the system.
 */
public class MailSystem {
  
  /**
   * This is the default name and location of the mailsystem configuration file. We look
   * in the current path for the "mailsystem.conf" file.
   */
  private static final String DEFAULT_CONFIG_FILE = "mailsystem.conf";

  
  /**
   * The main method for the mailsystem. The following command-line options are supported
   * at the moment: -config FILENAME, which uses the specified filename as configuration
   * file.
   *
   * @param a_args The command-line options.
   */
  public static void main(String[] a_args) throws Exception {
    String configFileName = DEFAULT_CONFIG_FILE;
    /* check, whether there is the -config parameter, which means that we use userdefined config
     * file
     */
    if (a_args != null) {
      for (int i = 0; i < a_args.length; i++) {
        if (a_args[i].equalsIgnoreCase("-config")) {
          if (i + 1 < a_args.length) {
            configFileName = a_args[i + 1];
          }
          break;
        }
      }
    }
    /* create the mail context */
    MailContext.createInstance(configFileName);
    /* initialize the server process */
    GenericServer centralMailServer = new GenericServer(MailContext.getInstance().getCentralProcessPort(), InetAddress.getByName(null));
    centralMailServer.setConnectionHandlerFactory(new MailSystemServerImplementationFactory());
    centralMailServer.startServer();
    /* now create a deadlock to keep everything running */
    Object deadlock = new Object();
    synchronized (deadlock) {
      deadlock.wait();
    }
  }

}
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
package infoservice.mailsystem.forwarder;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Properties;

/**
 * This class reads a mail from the input pipe and forwards it to the mailsystem main process.
 * The port where the main process is listening is read from the configuration file specified in
 * the command line.
 */
public class MailForwarder {
  
  /**
   * This is the default name and location of the mailsystem configuration file. As default we
   * look in the current path for the "mailsystem.conf" file.
   */
  private static final String DEFAULT_CONFIG_FILE = "mailsystem.conf";
  
  /**
   * This is the size of the buffer in bytes used when reading the mail from the input pipe and
   * forwarding it to the main process.
   */
  private static final int BUFFER_SIZE = 1000;

  
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
    Properties mailConfig = new Properties();
    FileInputStream configurationFile = new FileInputStream(configFileName);
    try {
      mailConfig.load(configurationFile);
    }
    finally {
      /* try to close the configuration file regardless whether loading the configuration was
       * successful or not
       */      
      if (configurationFile != null) {
        try {
          configurationFile.close();
        }
        catch (Exception e) {
        }
      }
    }
    /* there seems to be no error while loading the configuration */
    int mainProcessPort = Integer.parseInt(mailConfig.getProperty("MailSystemMainProcessPort").trim());
    /* try to connect to the main process port on localhost */
    Socket mainProcessSocket = null;
    try {
      mainProcessSocket = new Socket((String)null, mainProcessPort);   
      OutputStream streamToMainProcess = mainProcessSocket.getOutputStream();
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead = System.in.read(buffer);
      while (bytesRead != -1) {
        streamToMainProcess.write(buffer, 0, bytesRead);
        bytesRead = System.in.read(buffer);
      }
      /* that's it, we have forwarded the whole mail, now the main process will do the job */
    }
    finally {
      /* try to cleanup everything regardless whether forwarding the mail was successful or not */
      if (mainProcessSocket != null) {
        try {
          mainProcessSocket.getOutputStream().close();
        }
        catch (Exception e) {
        }
        try {
          mainProcessSocket.getInputStream().close();
        }
        catch (Exception e) {
        }
        try {
          mainProcessSocket.close();
        }
        catch (Exception e) {
        }
      }
    } 
  }

}
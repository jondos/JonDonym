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

import java.util.Locale;
import java.util.StringTokenizer;

import infoservice.mailsystem.central.commands.BanWarningCommand;
import infoservice.mailsystem.central.commands.GetForwarderCommand;
import infoservice.mailsystem.central.commands.HelpCommand;

/**
 * This class finds the correct implementation of the reply-generating class depending on the
 * subject of the received mail.
 */
public class CommandFactory {
    
  /**
   * The default language to use, if no language code to use could be found in the subject
   * line of the received mail. The default language is English.
   */
  private static final Locale DEFAULT_LANGUAGE = Locale.ENGLISH;
  
  /**
   * The command code of the GetHelp command.
   */
  private static final int COMMAND_GETHELP = 0;
  
  /**
   * The command code of the GetForwarder command.
   */
  private static final int COMMAND_GETFORWARDER = 1;
  
  /**
   * The command code for the BanWarning command.
   */
  private static final int COMMAND_BANMESSAGE = 2;
    
  /**
   * The default command to use, if we could not find a matching command in the subject
   * line of the received mail. The default is the GetHelp command.
   */
  private static final int DEFAULT_COMMAND = COMMAND_GETHELP;
  
  
  /**
   * This method finds the matching command implementation and reply language depending on
   * the subject line of the received mail.
   *
   * @param a_mailSubject The subject line of a received mail.
   * @param a_banMessage If this value is true, the command in the subject line is ignored and the
   *                     ban message implementation is returned. Only the preferred language is
   *                     tried to obtain from the subject line. If this value is false, the
   *                     command and the preferred language is read from the subject line and
   *                     the corresponding command implementation is returned.
   *
   * @return The command implementation to use for creating a response for the received message.
   */
  public static AbstractMailSystemCommand getCommandImplementation(String a_mailSubject, boolean a_banMessage) {
    /* define a default action if no or an invalid subject is specified */
    int commandCode = DEFAULT_COMMAND;
    Locale languageToUse = DEFAULT_LANGUAGE;
    if (a_banMessage == true) {
      commandCode = COMMAND_BANMESSAGE;
    }
    if (a_mailSubject != null) {
      StringTokenizer stMailCommand = new StringTokenizer(a_mailSubject, " ");
      try {
        String command = stMailCommand.nextToken().trim();
        if (a_banMessage == false) {
          if (command.equalsIgnoreCase("GetHelp")) {
            commandCode = COMMAND_GETHELP;
          }
          if (command.equalsIgnoreCase("GetForwarder")) {
            commandCode = COMMAND_GETFORWARDER;
          }
        }
        String language = stMailCommand.nextToken().trim();
        if (language.equalsIgnoreCase("en")) {
          languageToUse = Locale.ENGLISH;
        }
        if (language.equalsIgnoreCase("de")) {
          languageToUse = Locale.GERMAN;
        }
      }
      catch (Exception e) {
        /* maybe no command or no language was specified -> use the default values */
      }
    }
    AbstractMailSystemCommand commandImplementation = null;
    if (commandCode == COMMAND_GETFORWARDER) {
      commandImplementation = new GetForwarderCommand(new MailMessages(languageToUse));
    }
    else if (commandCode == COMMAND_BANMESSAGE) {
      commandImplementation = new BanWarningCommand(new MailMessages(languageToUse));
    }
    else {
      /* the default is the help command */
      commandImplementation = new HelpCommand(new MailMessages(languageToUse));
    }
    return commandImplementation;
  }
  
}
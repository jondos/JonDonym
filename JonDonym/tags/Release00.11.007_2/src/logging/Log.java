/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package logging;

public interface Log
{
	/** Output a debug message.
	 *  @param level The level of the debugging message (EMERG,ALERT,CRIT,ERR,WARNING,NOTICE,INFO,DEBUG)
	 *  @param type The type of the debugging message (GUI, NET, THREAD, MISC)
	 *  @param txt   The message itself
	 */

	public void log(int level, int type, String mesg);

	/** Set the debugging type you like to output. To activate more than one type you simly add
	 *  the types like this <code>setDebugType(JAPDebug.GUI+JAPDebug.NET)</code>.
	 *  @param type The debug type (NUL, GUI, NET, THREAD, MISC)
	 */
	public void setLogType(int type);

	/** Get the current debug type.
	 */
	public int getLogType();

	/** Set the debugging level you would like to output.
	 *  The possible parameters are (EMERG, ALERT, EXCEPTION, ERR, WARNING, NOTICE, INFO, DEBUG).
	 *  DEBUG means output all messages, EMERG means only emergency messages.
	 *  @param level The debug level (EMERG, ALERT, EXCEPTION, ERR, WARNING, NOTICE, INFO, DEBUG)
	 */
	public void setLogLevel(int level);

	/** Get the current debug level.
	 */
	public int getLogLevel();
}

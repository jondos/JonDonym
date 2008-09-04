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
/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */
package anon.infoservice;

/**
 * This is used to store the IDs of recently added database entries, so that they are not added
 * several times after they have expired.
 *
 * @author Rolf Wendolsky
 */
public abstract class AbstractIDEntry extends AbstractDatabaseEntry
{
	private long m_lastUpdate;
	private long m_versionNumber;
	private String m_id;

	public AbstractIDEntry(AbstractDatabaseEntry a_entry, long a_expireTime)
	{
		super(a_expireTime);
		if (a_entry == null)
		{
			throw new IllegalArgumentException("No database entry given.");
		}
		m_lastUpdate = a_entry.getLastUpdate();
		m_versionNumber = a_entry.getVersionNumber();
		m_id = a_entry.getId();
	}

	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	public long getVersionNumber()
	{
		return m_versionNumber;
	}

	public String getId()
	{
		return m_id;
	}
}

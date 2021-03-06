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
package gui;

import java.awt.Component;
import java.awt.Container;
import java.net.URL;

/**
 * This class represents the help context of JAP.
 */
public final class JAPHelpContext
{
	public static final String INDEX = "index";
	public static final IHelpContext INDEX_CONTEXT =
						createHelpContext(INDEX);

	public static IHelpContext createHelpContext(String a_context)
	{
		return createHelpContext(a_context, null);
	}
	
	public static IHelpContext createHelpContext(String a_context, Component a_container)
	{
		final Component container = a_container;
		final String context = a_context;
		
		return new IHelpContext()
		{
			public Component getHelpExtractionDisplayContext() 
			{
				return container;
			}

			public String getHelpContext() 
			{
				return context;
			}
		};
	}

	/**
	 * An interface that is used to get the help context of an object.
	 */	
	public interface IHelpContext
	{
		String getHelpContext();
		Component getHelpExtractionDisplayContext();
	}
	
	public interface IURLHelpContext extends IHelpContext
	{
		String getURLMessage();
		URL getHelpURL();
		String getHelpContext();
		Component getHelpExtractionDisplayContext();
	}
}

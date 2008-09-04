/*
 Copyright (c) 2000 - 2006, The JAP-Team
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

import anon.util.ClassUtil;
import org.w3c.dom.Element;
import anon.util.XMLParseException;

/**
 * This class is for registering cascades that have been previously known to JAP
 * (may be a lot of cascades...).
 *
 * @author Rolf Wendolsky
 */
public class PreviouslyKnownCascadeIDEntry extends AbstractCascadeIDEntry
{
	public static final String XML_ELEMENT_NAME =
		ClassUtil.getShortClassName(PreviouslyKnownCascadeIDEntry.class);
	public static final String XML_ELEMENT_CONTAINER_NAME = "PreviouslyKnownCascades";

	public PreviouslyKnownCascadeIDEntry(MixCascade a_cascade)
	{
		super(a_cascade, Long.MAX_VALUE);
	}
	public PreviouslyKnownCascadeIDEntry(Element a_XmlElement) throws XMLParseException
	{
		super(a_XmlElement);
	}
}

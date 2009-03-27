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
package junitx.framework.extension;

import junitx.framework.*;
import java.lang.reflect.*;

/**
 * This class should be inherited by other classes names "TestProxy" and
 * be put in each package, where private members of classes should be tested.
 * The method createInstance must be copied and implemented, further implementation is not
 * required, nor any constructor is needed.
 *
 * @author Rolf Wendolsky
 */
public abstract class TestProxy extends junitx.framework.TestProxy
{
	protected Object createInstance(Constructor a_Constructor, Object[] a_args)
		throws Exception
	{
		return a_Constructor.newInstance(a_args);
	}

	/**
	 * Gets a class instance by calling the default constructor.
	 *
	 * @param args Object[] the constructor arguments
	 * @throws TestAccessException if the class could not be instanciated
	 * @return Object an instance of the TestedClass
	 * @see newInstanceWithKey
	 */
	public Object newInstance(Object[] a_args) throws TestAccessException
	{
	  return newInstanceWithKey(null, a_args);
	}

	/**
	 * Gets a class instance by calling a constructor.
	 *
	 * @param strConstructorKey String call the class constructor with the
	 * given name; <code> null </code> calls the default constructor
	 * @param args Object[] the constructor arguments
	 * @throws TestAccessException if the class could not be instanciated
	 * @return Object an instance of the TestedClass
	 */
	public Object newInstanceWithKey(String a_strConstructorKey, Object[] a_args)
		throws TestAccessException
	{
		try
		{
			if (a_strConstructorKey == null)
			{
				return createInstance(getProxiedClass().getConstructor(a_args), a_args);
			} else
			{
				return createInstance(
							getProxiedClass().getConstructor(a_strConstructorKey), a_args);
			}
		}  catch (Exception e)
		{
			throw new TestAccessException ("Could not instanciate " + getTestedClassName(), e);
		}
	}
}

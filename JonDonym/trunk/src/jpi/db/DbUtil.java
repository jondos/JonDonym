/*
 Copyright (c) 2000 - 2007, The JAP-Team
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

package jpi.db;

import java.text.DecimalFormatSymbols;
import java.text.DecimalFormat;
import java.util.Locale;
import java.math.BigDecimal;

/**
 * Contains a bunch of static utility methods for dealing with the database
 * (format and type conversions etc)
 *
 * @author Elmar Schraml
 */
public class DbUtil
{
	final static int DECIMAL_SCALE = 8; //nr of decimal digits

	public static String formatDoubleForSql(double value)
	{
		value = (Math.rint(value * 100)) / 100; //round to two decimal digits (otherwise postgres won't find the cert)
		DecimalFormat df = new DecimalFormat("######.##"); //uses decimal separator according to locale, but we need a dot for sql
		DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
		df.setDecimalFormatSymbols(dfs);
		return df.format(value);
	}

	/**
	 *
	 * @param input BigDecimal as received from the database
	 * @return int: the same value as integer in eurocent (NOT euros)
	 */
	public static int intFromDecimal(BigDecimal input)
	{
		//TODO: clean this up
		Double tempDouble = new Double(input.doubleValue());
		int tempInt = new Double(tempDouble.doubleValue()*100).intValue();
		return tempInt;
	}

	public static BigDecimal decimalFromInt(long input)
	{
		return BigDecimal.valueOf(input,DECIMAL_SCALE);
	}


}

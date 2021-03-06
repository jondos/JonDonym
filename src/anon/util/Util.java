/*
 Copyright (c) 2000-2006, The JAP-Team
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
package anon.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.ListenerInterface;


public final class Util
{
	/** Defines the format of version numbers in the AN.ON project. */
	public static final String VERSION_FORMAT = "00.00.000";
	
	private final static String WHITESPACE_ENCODED = "%20";
	private final static String WHITESPACE = " ";
	
	public static final int MAX_FORMAT_BYTES = 0;
	public static final int MAX_FORMAT_KBYTES = 1;
	public static final int MAX_FORMAT_MBYTES = 2;
	public static final int MAX_FORMAT_GBYTES = 3;
	
	public static final int MAX_FORMAT_KBIT_PER_SEC = 0;
	public static final int MAX_FORMAT_MBIT_PER_SEC = 1;
	public static final int MAX_FORMAT_GBIT_PER_SEC = 2;
	
	public static final int MAX_FORMAT_ALL = 4;
	
	/**
	 * This class works without being initialised and is completely static.
	 * Therefore, the constructor is not needed and private.
	 */
	private Util()
	{
	}

	public static String cutString(String a_string, int a_maxLength)
	{
		if (a_string != null && a_string.length() > a_maxLength)
		{
			a_string = a_string.substring(0, a_maxLength).trim();
		}
		return a_string;
	}
	
	public static String stripString(String a_string, String a_charactersToStrip)
	{
		if (a_string == null || a_charactersToStrip == null)
		{
			return null;
		}
		String stripped = "";
		StringTokenizer tokenizer = new StringTokenizer(a_string, a_charactersToStrip);
		while (tokenizer.hasMoreTokens())
		{
			stripped += tokenizer.nextToken().trim();
		}
		return stripped;
	}
	
	/**
	 * Normalises a String to the given length by filling it up with spaces, if it
	 * does not already have this length or is even longer.
	 * @param a_string a String
	 * @param a_normLength a length to normalise the String
	 * @return the normalised String
	 */
	public static String normaliseString(String a_string, int a_normLength)
	{
		if (a_string.length() < a_normLength)
		{
			char[] space = new char[a_normLength - a_string.length()];
			for (int i = 0; i < space.length; i++)
			{
				space[i] = ' ';
			}
			a_string = a_string + new String(space);
		}

		return a_string;
	}

	/**
	 * Gets the stack trace of a Throwable as String.
	 * @param a_t a Throwable
	 * @return the stack trace of a throwable as String
	 */
	public static String getStackTrace(Throwable a_t)
	{
		StringWriter strWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(strWriter);

		a_t.printStackTrace(writer);

		return strWriter.toString();
	}
	
	public static String decodeString(String a_compressed)
	{
		String decodedString = a_compressed;
		byte[] decodedBytes;
		
		try
		{
			decodedBytes = Base64.decode(a_compressed);
			if (decodedBytes != null)
			{
				decodedString = new String(ZLibTools.decompress(decodedBytes));
			}
			
		}
		catch (Exception a_e)
		{
			// this String has not been compressed
			LogHolder.log(LogLevel.ALERT, LogType.MISC, a_e); 			
		}
		
		return decodedString;
	}

	/**
	 * Tests if two byte arrays are equal.
	 * @param arrayOne a byte array
	 * @param arrayTwo another byte array
	 * @return true if the two byte arrays are equal or both arrays are null; false otherwise
	 */
	public static boolean arraysEqual(byte[] arrayOne, byte[] arrayTwo)
	{
		if (arrayOne == null && arrayTwo == null)
		{
			return true;
		}

		if (arrayOne == null || arrayTwo == null)
		{
			return false;
		}

		if (arrayOne.length != arrayTwo.length)
		{
				return false;
		}

		for (int i = 0; i < arrayOne.length; i++)
		{
			if (arrayOne[i] != arrayTwo[i])
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Tests if two charactet arrays are equal.
	 * @param arrayOne a charactet array
	 * @param arrayTwo another charactet array
	 * @return true if the two charactet arrays are equal or both arrays are null; false otherwise
	 */
	public static boolean arraysEqual(char[] arrayOne, char[] arrayTwo)
	{
		if (arrayOne == null && arrayTwo == null)
		{
			return true;
		}

		if (arrayOne == null || arrayTwo == null)
		{
			return false;
		}

		if (arrayOne.length != arrayTwo.length)
		{
			return false;
		}

		for (int i = 0; i < arrayOne.length; i++)
		{
			if (arrayOne[i] != arrayTwo[i])
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Tests if a_length positions of two arrays are equal.
	 * @param a_arrayA byte[]
	 * @param a_Aoff int
	 * @param a_arrayB byte[]
	 * @param a_Boff int
	 * @param a_length int
	 * @return boolean
	 */
	public static final boolean arraysEqual(byte[] a_arrayA, int a_Aoff,
											byte[] a_arrayB, int a_Boff,
											int a_length)
	{
		if (a_length <= 0)
		{
			return true;
		}
		if (a_arrayA == null || a_arrayB == null || a_Aoff < 0 || a_Boff < 0)
		{
			return false;
		}
		if (a_Aoff + a_length > a_arrayA.length ||
			a_Boff + a_length > a_arrayB.length)
			{
				return false;
			}

		for (int i = 0; i < a_length; i++)
		{
			if (a_arrayA[a_Aoff + i] != a_arrayB[a_Boff + i])
			{
				return false;
		}
		}

		return true;
	}

	/**
	 * Creates a Vector from a single Object.
	 * @param a_object an Object
	 * @return a Vector containing the given Object or an empty Vector if the Object was null
	 */
	public static Vector toVector(Object a_object)
	{
		Vector value = new Vector();

		if (a_object != null)
		{
			value.addElement(a_object);
		}
		return value;
	}

	/**
	 * Creates an Object array from a single Object.
	 * @param a_object an Object
	 * @return an Object array containing the given Object or an empty array if the Object was null
	 */
	public static Object[] toArray(Object a_object)
	{
		Object[] value;

		if (a_object != null)
		{
			value = new Object[1];
			value[0] = a_object;
		}
		else
		{
			value = new Object[0];
		}

		return value;
	}

	private static void swap(String a_IDs[], String a_values[], int a, int b)
	{
		String temp = a_IDs[a];
		a_IDs[a] = a_IDs[b];
		a_IDs[b] = temp;

		temp = a_values[a];
		a_values[a] = a_values[b];
		a_values[b] = temp;
	}

	/**
	 * Sorts a Vector alphabetically using the toString() method of each object.
	 * @param a_vector a Vector
	 * @return an alphabetically sorted Vector
	 */
	public static Vector sortStrings(Vector a_vector)
	{
		Vector sortedVector = new Vector();
		String buffer[] = new String[a_vector.size()];
		int bufferIndices[] = new int[a_vector.size()];
		String umlauts[] = new String[2];
		String temp;
		boolean bUmlauts;

		for (int i = 0; i < buffer.length; i++)
		{
			buffer[i] = a_vector.elementAt(i).toString().toLowerCase();
			bufferIndices[i] = i;
			// if one of the first letters is an umlaut, convert it
			bUmlauts = false;
			for (int j = 0; j < umlauts.length && j < buffer[i].length(); j++)
			{
				if (isUmlaut(buffer[i].charAt(j), umlauts, j))
				{
					bUmlauts = true;
				}
			}
			if (bUmlauts)
			{
				temp = "";
				int j = 0;
				for (; j < umlauts.length && j < buffer[i].length(); j++)
				{
					if (umlauts[j] == null)
					{
						temp += buffer[i].charAt(j);
					}
					else
					{
						temp += umlauts[j];
					}
				}
				if (j < buffer[i].length())
				{
					temp += buffer[i].substring(j, buffer[i].length());
				}
				buffer[i] = temp;
			}
		}

		// do the sorting operation
		bubbleSortStrings(a_vector, buffer, bufferIndices);

		for (int i = 0; i < buffer.length; i++)
		{
			sortedVector.addElement(a_vector.elementAt(bufferIndices[i]));
		}
		return sortedVector;
	}

	/**
	 * Implementation of parseFloat not implemented in JDK 1.1.8
	 * @param a_string String
	 * @return float
	 * @throws NumberFormatException
	 */
	public static double parseDouble(String a_string) throws NumberFormatException
	{
		char c;
		int integerPart = 0;
		int mantissaPart = 0;
		int afterCommaDigits = 1;
		boolean preComma = true;
		int sign = 1;

		if (a_string == null)
		{
			throw new NumberFormatException("NULL cannot be parsed as float!");
		}

		for (int i = 0; i < a_string.length(); i++)
		{
			c = a_string.charAt(i);

			if (Character.isDigit(c))
			{
				if (preComma)
				{
					integerPart = integerPart * 10 + (c - '0');
				}
				else
				{
					afterCommaDigits = afterCommaDigits * 10;
					mantissaPart = mantissaPart * 10 + (c - '0');
				}
			}
			else if (preComma && (c == '.' || c == ',') && a_string.length() > 1)
			{
				preComma = false;
			}
			else if (c == '+')
			{}
			else if (c == '-' && i == 0)
			{
				sign = -1;
			}
			else
			{
				throw new NumberFormatException(
								"No valid float value '" + a_string + "'!");
			}
		}
		
		double d = (double) (integerPart + ( (double) mantissaPart / (double) afterCommaDigits)) * sign;
		return d;
	}


	public static void sort(String[] a_ids, String[] a_values)
	{
		quicksort(a_ids, a_values, 0, a_ids.length - 1);
	}

	private static int divide(String a_IDs[], String a_values[],  int a_left, int a_right)
	{
		int index = a_left;
		for (int pointer = a_left; pointer < a_right; pointer++)
		{
			if (a_IDs[pointer].compareTo(a_IDs[a_right]) <= 0)
			{
				swap(a_IDs, a_values, index, pointer);
				index++;
			}
		}
		swap(a_IDs, a_values, index, a_right);
		return index;
	}

	private static void quicksort(String a_IDs[], String a_values[], int a_left, int a_right)
	{
		if (a_right > a_left)
		{
			int divisor = divide(a_IDs, a_values, a_left, a_right);
			quicksort(a_IDs, a_values, a_left, divisor - 1);
			quicksort(a_IDs, a_values, divisor + 1, a_right);
		}
	}
	
	public interface Comparable
	{
		public int compare(Object a_obj1, Object a_obj2);
	}

	public static class LongSortAsc implements Comparable
	{
		public int compare(Object a_obj1, Object a_obj2)
		{
			if(a_obj1 == null && a_obj2 == null) return 0;
			else if(a_obj1 == null) return -1;
			else if(a_obj2 == null) return 1;
			
			if(((Long) a_obj1).intValue() == Long.MAX_VALUE) return 1;
			if(((Long) a_obj2).intValue() == Long.MAX_VALUE) return -1;
			
			return (int) (((Long) a_obj1).longValue() - ((Long) a_obj2).longValue());
		}
	}
	
	public static class LongSortDesc implements Comparable
	{
		public int compare(Object a_obj1, Object a_obj2)
		{
			if(a_obj1 == null && a_obj2 == null) return 0;
			else if(a_obj1 == null) return 1;
			else if(a_obj2 == null) return -1;

			if(((Long) a_obj1).intValue() == Long.MAX_VALUE) return -1;
			if(((Long) a_obj2).intValue() == Long.MAX_VALUE) return 1;			
			
			return (int) (((Long) a_obj2).longValue() - ((Long) a_obj1).longValue());
		}
	}	
	
	public static class IntegerSortAsc implements Comparable
	{
		public int compare(Object a_obj1, Object a_obj2)
		{
			if(a_obj1 == null && a_obj2 == null) return 0;
			else if(a_obj1 == null) return -1;
			else if(a_obj2 == null) return 1;
			
			if(((Integer) a_obj1).intValue() == Integer.MAX_VALUE) return 1;
			if(((Integer) a_obj2).intValue() == Integer.MAX_VALUE) return -1;
			
			return (int) (((Integer) a_obj1).intValue() - ((Integer) a_obj2).intValue());
		}
	}
	
	public static class IntegerSortDesc implements Comparable
	{
		public int compare(Object a_obj1, Object a_obj2)
		{
			if(a_obj1 == null && a_obj2 == null) return 0;
			else if(a_obj1 == null) return 1;
			else if(a_obj2 == null) return -1;

			if(((Integer) a_obj1).intValue() == Integer.MAX_VALUE) return -1;
			if(((Integer) a_obj2).intValue() == Integer.MAX_VALUE) return 1;			
			
			return (int) (((Integer) a_obj2).intValue() - ((Integer) a_obj1).intValue());
		}
	}
	
	public static class StringSortAsc implements Comparable
	{
		public int compare(Object a_obj1, Object a_obj2)
		{
			if(a_obj1 == null && a_obj2 == null) return 0;
			else if(a_obj1 == null) return -1;
			else if(a_obj2 == null) return 1;
			
			return ((String)a_obj1).compareTo((String)a_obj2);
		}
	}
	
	public static void sort(Vector a_vec, Comparable c)
	{
		if(a_vec != null)
			quicksort(a_vec, 0, a_vec.size() - 1, c);
	}
	
	private static int divide(Vector a_vec, int a_left, int a_right, Comparable c)
	{
		int index = a_left;
		for (int pointer = a_left; pointer < a_right; pointer++)
		{
			if (c.compare(a_vec.elementAt(pointer), a_vec.elementAt(a_right)) <= 0)
			{
				swap(a_vec, index, pointer);
				index++;
			}
		}
		swap(a_vec, index, a_right);
		return index;
	}

	private static void quicksort(Vector a_vec, int a_left, int a_right, Comparable c)
	{
		if (a_right > a_left)
		{
			int divisor = divide(a_vec, a_left, a_right, c);
			quicksort(a_vec, a_left, divisor - 1, c);
			quicksort(a_vec, divisor + 1, a_right, c);
		}
	}
	
	private static void swap(Vector a_vec, int a, int b)
	{
		Object temp = a_vec.elementAt(a);
		a_vec.setElementAt(a_vec.elementAt(b), a);
		a_vec.setElementAt(temp, b);
	}

	/**
	 * Uses the Bubble Sort method to sort a vector of objects by comparing
	 * the output of the toString() method.
	 * @param a_vector a Vector
	 * @param buffer a buffer
	 * @param bufferIndices indices for the buffer
	 */
	private static void bubbleSortStrings(Vector a_vector, String buffer[], int bufferIndices[])
	{
		String temp;
		int tempIndex;

		for (int i = 1; i <= a_vector.size(); i++)
		{
			for (int j = a_vector.size() - 1; j > i; j--)
			{
				if (buffer[j].compareTo(buffer[j - 1]) < 0)
				{
					temp = buffer[j];
					tempIndex = bufferIndices[j];
					buffer[j] = buffer[j - 1];
					bufferIndices[j] = bufferIndices[j - 1];
					buffer[j - 1] = temp;
					bufferIndices[j - 1] = tempIndex;
				}
			}
		}
	}

	/**
	 * Tests if a character is an umlaut and, if yes, writes the umlaut in an ASCII form to
	 * the array of transformed umlauts at the specified position.
	 * @param a_character a character; must be lower case !
	 * @param a_transformedUmlauts an array of transformed umlauts
	 * @param a_position the position to write into the array of umlauts; if the character is not an umlaut,
	 * 'null' is written at this position, otherwise the character transformed into an ASCII form
	 * @return if the given character is an umlaut; false otherwise
	 */
	private static boolean isUmlaut(char a_character, String[] a_transformedUmlauts, int a_position)
	{
		switch (a_character)
		{
			case '\u00e4': a_transformedUmlauts[a_position] = "ae"; return true;
			case '\u00f6': a_transformedUmlauts[a_position] = "oe"; return true;
			case '\u00fc': a_transformedUmlauts[a_position] = "ue"; return true;
			default: a_transformedUmlauts[a_position] = null; return false;
		}
	}

	/**
	 * Converts a version string of the form xx.xx.xxx to a number
	 * @param a_version a version string of the form xx.xx.xxx
	 * @return the given version string as number
	 * @throws java.lang.NumberFormatException if the version has an illegal format
	 */
	public static long convertVersionStringToNumber(String a_version) throws NumberFormatException
	{
		if (a_version == null)
		{
			throw new NumberFormatException("Version string is null!");
		}

		long version = 0;
		StringTokenizer st = new StringTokenizer(a_version, ".");
		try
		{
			version = Long.parseLong(st.nextToken()) * 100000 + Long.parseLong(st.nextToken()) * 1000 +
				Long.parseLong(st.nextToken());
		}
		catch (NoSuchElementException a_e)
		{
			throw new NumberFormatException("Version string is too short!");
		}
		return version;
	}


	/**
	 * Since JDK 1.1.8 does not provide String.replaceAll(),
	 * this is an equivalent method.
	 */
	/*public static String replaceAll(String a_source, String a_toReplace, String a_replaceWith)
	{
		int position;

		while ( (position = a_source.indexOf(a_toReplace)) != -1)
		{
			int position2 = a_source.indexOf(a_replaceWith);
			if (a_replaceWith.indexOf(a_toReplace) != -1)
			{
				position2 += a_replaceWith.indexOf(a_toReplace);
			}
			if (position == position2)
			{
				break;
			}
			String before = a_source.substring(0, position);
			String after = a_source.substring(position + a_toReplace.length(), a_source.length());
			a_source = before + a_replaceWith + after;
		}

		return a_source;
	}*/
	
	public static String replaceAll(String a_source, String a_toReplace, String a_replaceWith)
	{
		return replaceAll(a_source, a_toReplace, a_replaceWith, null);
	}
	
	/**
	 * String replace algorithm. Strings specified in omit will not be replaced even if their prefices matches
	 * a_toReplace
	 * 
	 * @param a_source Source String
	 * @param a_toReplace substring which is to be replaced
	 * @param a_replaceWith string which should replace a_toReplace
	 * @param omit Strings that not be replaced when their prefix matches a_toReplace
	 * @return a new string with all replacements
	 */
	public static String replaceAll(String a_source, String a_toReplace, String a_replaceWith, String[] omit)
	{
		StringBuffer buf = new StringBuffer("");
		int index = a_source.indexOf(a_toReplace, 0);
		int lastIndex = 0;
		
		boolean replace = true;
		String omitTemp = null;
		
		while(index != -1)
		{
			replace = true;
			if(omit != null)
			{
				omitTemp = a_source.substring(index);
				for (int i = 0; i < omit.length; i++) 
				{	
					if(omitTemp.startsWith(omit[i]))
					{
						replace = false;
						break;
					}
				}
			}
			if(replace)
			{
				buf.append(a_source.substring(lastIndex, index));				
				buf.append(a_replaceWith);
				lastIndex = index + a_toReplace.length();
			}
			index = a_source.indexOf(a_toReplace, 
					(replace ? lastIndex : (index + a_toReplace.length())) );
		}
		
		buf.append(a_source.substring(lastIndex));
		return buf.toString();
	}
	
	public static String encodeWhiteSpaces(String stringWithWhitespaces)
	{
		/*replace any white space encodings with white spaces */
		StringBuffer encodeBuffer = new StringBuffer("");
		int whiteSpIndex = stringWithWhitespaces.indexOf(WHITESPACE, 0);
		int lastIx = 0;
		while(whiteSpIndex != -1)
		{
			encodeBuffer.append(stringWithWhitespaces.substring(lastIx, whiteSpIndex));				
			encodeBuffer.append(WHITESPACE_ENCODED);
			lastIx = whiteSpIndex+WHITESPACE.length();
			whiteSpIndex = stringWithWhitespaces.indexOf(WHITESPACE, (whiteSpIndex+1));
		}
		encodeBuffer.append(stringWithWhitespaces.substring(lastIx));
		return encodeBuffer.toString();
	}
	
	public static void closeStream(InputStream a_input)
	{
		if (a_input != null)
		{
			try
			{
				a_input.close();
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.ERR, LogType.FILE, a_e);
			}
		}
	}
	
	public static void closeStream(OutputStream a_input)
	{
		if (a_input != null)
		{
			try
			{
				a_input.close();
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.ERR, LogType.FILE, a_e);
			}
		}
	}
	
	public static void copyStream(InputStream a_input, OutputStream a_output)
		throws IOException
	{
		if (a_input == null)
		{
			throw new IOException("Input stream is null!");
		}
		if (a_output == null)
		{
			throw new IOException("Output stream is null!");
		}
		
		byte buffer[] = new byte[2048];
		int len = -1;
		while ( (len = a_input.read(buffer)) != -1)
		{
			a_output.write(buffer, 0, len);
		}
		a_input.close();
		a_output.flush();
		a_output.close();
	}
	
	/**
	 * Uses the reflection API to get the value of a static field in the given class, if the field
	 * is present.
	 * @param a_class a Class
	 * @param a_fieldName the field to read the value from
	 * @return the value of a static field in the given class or null if the value or field is not present
	 */
	public static String getStaticFieldValue(Class a_class, String a_fieldName)
	{
		String fieldValue = null;
		try
		{
			Field field = a_class.getField(a_fieldName);
			fieldValue = (String) field.get(null);
		}
		catch (Exception ex)
		{
		}

		return fieldValue;
	}
	
	public static String colonizeSKI(String a_ski)
	{
		StringBuffer buff = new StringBuffer();
		
		for(int i = 0; i < a_ski.length(); i++)
		{
			buff.append(a_ski.charAt(i));
			
			if((i + 1) % 2 == 0 && i != a_ski.length() - 1)
			{
				buff.append(":");
			}
		}
		
		return buff.toString();
	}
	
	public static InfoServiceDBEntry[] createDefaultInfoServices(
			String [] a_defaultNames, String[] a_defaultHostNames, int[][] a_defaultPortNumbers) throws Exception
	{
		Vector listeners;
		InfoServiceDBEntry[] entries = new InfoServiceDBEntry[a_defaultNames.length];
		for (int i = 0; i < entries.length; i++)
		{
			listeners = new Vector(a_defaultPortNumbers[i].length);
			for (int j = 0; j < a_defaultPortNumbers[i].length; j++)
			{
				listeners.addElement(new ListenerInterface(a_defaultHostNames[i],
						a_defaultPortNumbers[i][j]));
			}
			entries[i] = new InfoServiceDBEntry(
					a_defaultNames[i], a_defaultNames[i], listeners, false, true, 0, 0, false);
			entries[i].markAsBootstrap();
		}

		return entries;
	}
	
	
	
	public static String formatKbitPerSecValueWithUnit(long c)
	{
		return formatKbitPerSecValueWithUnit(c, MAX_FORMAT_ALL);
	}

	public static String formatKbitPerSecValueWithUnit(long c, int a_maxFormat)
	{
		return formatKbitPerSecValueWithoutUnit(c, a_maxFormat) + " " + formatKbitPerSecValueOnlyUnit(c, a_maxFormat);
	}
	
	public static String formatKbitPerSecValueOnlyUnit(long c)
	{
		return formatKbitPerSecValueOnlyUnit(c, MAX_FORMAT_ALL);
	}
	
	public static String formatKbitPerSecValueOnlyUnit(long c, int a_maxFormat)
	{
		if (c < 1000 || a_maxFormat < MAX_FORMAT_MBIT_PER_SEC)
		{
			return JAPMessages.getString("kbit/s");
		}
		else if (c < 1000000 || a_maxFormat < MAX_FORMAT_GBIT_PER_SEC)
		{
			return JAPMessages.getString("Mbit/s");
		}
		return JAPMessages.getString("Gbit/s");		
	}
	
	public static String formatKbitPerSecValueWithoutUnit(long c)
	{
		return formatKbitPerSecValueWithoutUnit(c, MAX_FORMAT_ALL);
	}
	
	public static String formatKbitPerSecValueWithoutUnit(long c, int a_maxFormat)
	{
		DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(JAPMessages.getLocale());
		double d = c;
		if (c < 1000 || a_maxFormat < MAX_FORMAT_MBIT_PER_SEC)
		{
			df.applyPattern("#,####");
		}
		else if (c < 1000000 || a_maxFormat < MAX_FORMAT_GBIT_PER_SEC)
		{
			d /= 1000.0;
			df.applyPattern("#,##0.0");
		}
		else
		{
			d /= 1000000.0;
			df.applyPattern("#,##0.0");
		}
		return df.format(d);
	}

	/** Returns the desired unit for this amount of Bytes (Bytes, kBytes, MBytes,GBytes)*/
	public static String formatBytesValueWithUnit(long c)
	{
		return formatBytesValueWithUnit(c, MAX_FORMAT_ALL);
	}

	public static String formatBytesValueWithUnit(long c, int a_maxFormat)
	{
		return formatBytesValueWithoutUnit(c, a_maxFormat) + " " + formatBytesValueOnlyUnit(c, a_maxFormat);
	}


	public static String formatBytesValueOnlyUnit(long c)
	{
		return formatBytesValueOnlyUnit(c, MAX_FORMAT_ALL);
	}

	public static String formatBytesValueOnlyUnit(long c, int a_maxFormat)
	{
		if (c < 1000 || a_maxFormat < MAX_FORMAT_KBYTES)
		{
			return JAPMessages.getString("Byte");
		}
		else if (c < 1000000 || a_maxFormat < MAX_FORMAT_MBYTES)
		{
			return JAPMessages.getString("kByte");
		}
		else if (c < 1000000000 || a_maxFormat < MAX_FORMAT_GBYTES)
		{
			return JAPMessages.getString("MByte");
		}
		return JAPMessages.getString("GByte");
	}

	public static String formatBytesValueWithoutUnit(long c)
	{
		return formatBytesValueWithoutUnit(c, MAX_FORMAT_ALL);
}

	/** Returns a formated number which respects different units (Bytes, kBytes, MBytes, GBytes)*/
	public static String formatBytesValueWithoutUnit(long c, int a_maxFormat)
	{
		DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(JAPMessages.getLocale());
		double d = c;
		if (c < 1000 || a_maxFormat < MAX_FORMAT_KBYTES)
		{
			df.applyPattern("#,####");
		}
		else if (c < 1000000 || a_maxFormat < MAX_FORMAT_MBYTES)
		{
			d /= 1000.0;
			df.applyPattern("#,##0.0");
		}
		else if (c < 1000000000 || a_maxFormat < MAX_FORMAT_GBYTES)
		{
			d /= 1000000.0;
			df.applyPattern("#,##0.0");
		}
		else
		{
			d /= 1000000000.0;
			df.applyPattern("#,##0.0");
		}
		return df.format(d);
	}	
	
	public static String toHTMLEntities(String buf)
	{
		final StringBuffer b = new StringBuffer();
		for (int i = 0; i < buf.length(); i++) 
		{
			final char ch = buf.charAt(i);
			if (!(ch >= '\u0000' && ch <= '\u007f')) 
			{
				// an UTF-8 character to be changed
				b.append("&#").append(Integer.toString((int)ch)).append(";");
			} 
			else 
			{
				b.append(ch);
			}
		}
		return b.toString();
	}
}

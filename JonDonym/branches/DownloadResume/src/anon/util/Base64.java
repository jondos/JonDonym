package anon.util;


/**
 * Encodes and decodes to and from Base64 notation.
 *
 * <p>
 * Change Log:
 * </p>
 * <ul>
 *  <li>v1.4 - Added helper methods to read/write files.</li>
 *  <li>v1.3.6 - Fixed OutputStream.flush() so that 'position' is reset.</li>
 *  <li>v1.3.5 - Added flag to turn on and off line breaks. Fixed bug in input stream
 *      where last buffer being read, if not completely full, was not returned.</li>
 *  <li>v1.3.4 - Fixed when "improperly padded stream" error was thrown at the wrong time.</li>
 *  <li>v1.3.3 - Fixed I/O streams which were totally messed up.</li>
 * </ul>
 *
 * <p>
 * I am placing this code in the Public Domain. Do with it as you will.
 * This software comes with no guarantees or warranties but with
 * plenty of well-wishing instead!
 * Please visit <a href="http://iharder.net/xmlizable">http://iharder.net/xmlizable</a>
 * periodically to check for updates or to contribute improvements.
 * </p>
 *
 * @author Robert Harder
 * @author rob@iharder.net
 * @version 1.4
 */
public class Base64
{
	public static final String BEGIN_TAG = "-----BEGIN ";
	public static final String END_TAG = "-----END ";
	public static final String TAG_END_SEQUENCE = "-----";

	/** Specify encoding (value is <tt>true</tt>). */
	public final static boolean ENCODE = true;

	/** Specify decoding (value is <tt>false</tt>). */
	public final static boolean DECODE = false;

	/** Specify that data should be compressed (value is <tt>true</tt>). */
	public final static boolean COMPRESS = true;

	/** Specify that data should not be compressed (value is <tt>false</tt>). */
	public final static boolean DONT_COMPRESS = false;

	/** Maximum line length (76) of Base64 output. */
	private final static int MAX_LINE_LENGTH = 76;

	/** The equals sign (=) as a byte. */
	private final static byte EQUALS_SIGN = (byte) '=';

	/** The new line character (\n) as a byte. */
	private final static byte NEW_LINE = (byte) '\n';

	/** The 64 valid Base64 values. */
	private final static byte[] ALPHABET =
		{
		(byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G',
		(byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N',
		(byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U',
		(byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z',
		(byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g',
		(byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n',
		(byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u',
		(byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z',
		(byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5',
		(byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) '+', (byte) '/'
	};

	/**
	 * Translates a Base64 value to either its 6-bit reconstruction value
	 * or a negative number indicating some other meaning.
	 **/
	private final static byte[] DECODABET =

		{

		-9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal  0 -  8

		-5, -5, // Whitespace: Tab and Linefeed

		-9, -9, // Decimal 11 - 12

		-5, // Whitespace: Carriage Return

		-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 - 26

		-9, -9, -9, -9, -9, // Decimal 27 - 31

		-5, // Whitespace: Space

		-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42

		62, // Plus sign at decimal 43

		-9, -9, -9, // Decimal 44 - 46

		63, // Slash at decimal 47

		52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero through nine

		-9, -9, -9, // Decimal 58 - 60

		-1, // Equals sign at decimal 61

		-9, -9, -9, // Decimal 62 - 64

		0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, // Letters 'A' through 'N'

		14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'O' through 'Z'

		-9, -9, -9, -9, -9, -9, // Decimal 91 - 96

		26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, // Letters 'a' through 'm'

		39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Letters 'n' through 'z'

		-9, -9, -9, -9 // Decimal 123 - 126

		/*,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 127 - 139
			 -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 140 - 152

			 -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 153 - 165

			 -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 166 - 178

			 -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 179 - 191

			 -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 192 - 204

			 -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 205 - 217

			 -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 218 - 230

			 -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 231 - 243

			 -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9         // Decimal 244 - 255 */

	};

//	private final static byte BAD_ENCODING = -9; // Indicates error in encoding

	private final static byte WHITE_SPACE_ENC = -5; // Indicates white space in encoding

	private final static byte EQUALS_SIGN_ENC = -1; // Indicates equals sign in encoding

	/** Defeats instantiation. */

	private Base64()
	{}

	/* ********  E N C O D I N G   M E T H O D S  ******** */





	/**
	 * Encodes the first three bytes of array <var>threeBytes</var>

	 * and returns a four-byte array in Base64 notation.

	 *

	 * @param threeBytes the array to convert

	 * @return four byte array in Base64 notation.

	 * @since 1.3

	 */

/*	private static byte[] encode3to4(byte[] threeBytes)

	{
		return encode3to4(threeBytes, 3);

	} // end encodeToBytes
*/
	/**
	 * Encodes up to the first three bytes of array <var>threeBytes</var>

	 * and returns a four-byte array in Base64 notation.

	 * The actual number of significant bytes in your array is

	 * given by <var>numSigBytes</var>.

	 * The array <var>threeBytes</var> needs only be as big as

	 * <var>numSigBytes</var>.

	 *

	 * @param threeBytes the array to convert

	 * @param numSigBytes the number of significant bytes in your array

	 * @return four byte array in Base64 notation.

	 * @since 1.3

	 */

/*	private static byte[] encode3to4(byte[] threeBytes, int numSigBytes)

	{
		byte[] dest = new byte[4];

		encode3to4(threeBytes, 0, numSigBytes, dest, 0);

		return dest;

	}
*/
	/**
	 * Encodes up to three bytes of the array <var>source</var>

	 * and writes the resulting four Base64 bytes to <var>destination</var>.

	 * The source and destination arrays can be manipulated

	 * anywhere along their length by specifying

	 * <var>srcOffset</var> and <var>destOffset</var>.

	 * This method does not check to make sure your arrays

	 * are large enough to accomodate <var>srcOffset</var> + 3 for

	 * the <var>source</var> array or <var>destOffset</var> + 4 for

	 * the <var>destination</var> array.

	 * The actual number of significant bytes in your array is

	 * given by <var>numSigBytes</var>.

	 *

	 * @param source the array to convert

	 * @param srcOffset the index where conversion begins

	 * @param numSigBytes the number of significant bytes in your array

	 * @param destination the array to hold the conversion

	 * @param destOffset the index where output will be put

	 * @return the <var>destination</var> array

	 * @since 1.3

	 */

	private static byte[] encode3to4(

		byte[] source, int srcOffset, int numSigBytes,

		byte[] destination, int destOffset)

	{

		//           1         2         3

		// 01234567890123456789012345678901 Bit position

		// --------000000001111111122222222 Array position from threeBytes

		// --------|    ||    ||    ||    | Six bit groups to index ALPHABET

		//          >>18  >>12  >> 6  >> 0  Right shift necessary

		//                0x3f  0x3f  0x3f  Additional AND



		// Create buffer with zero-padding if there are only one or two

		// significant bytes passed in the array.

		// We have to shift left 24 in order to flush out the 1's that appear

		// when Java treats a value as negative that is cast from a byte to an int.

		int inBuff = (numSigBytes > 0 ? ( (source[srcOffset] << 24) >>> 8) : 0)

			| (numSigBytes > 1 ? ( (source[srcOffset + 1] << 24) >>> 16) : 0)

			| (numSigBytes > 2 ? ( (source[srcOffset + 2] << 24) >>> 24) : 0);

		switch (numSigBytes)

		{

			case 3:

				destination[destOffset] = ALPHABET[ (inBuff >>> 18)];

				destination[destOffset + 1] = ALPHABET[ (inBuff >>> 12) & 0x3f];

				destination[destOffset + 2] = ALPHABET[ (inBuff >>> 6) & 0x3f];

				destination[destOffset + 3] = ALPHABET[ (inBuff) & 0x3f];

				return destination;

			case 2:

				destination[destOffset] = ALPHABET[ (inBuff >>> 18)];

				destination[destOffset + 1] = ALPHABET[ (inBuff >>> 12) & 0x3f];

				destination[destOffset + 2] = ALPHABET[ (inBuff >>> 6) & 0x3f];

				destination[destOffset + 3] = EQUALS_SIGN;

				return destination;

			case 1:

				destination[destOffset] = ALPHABET[ (inBuff >>> 18)];

				destination[destOffset + 1] = ALPHABET[ (inBuff >>> 12) & 0x3f];

				destination[destOffset + 2] = EQUALS_SIGN;

				destination[destOffset + 3] = EQUALS_SIGN;

				return destination;

			default:

				return destination;

		} // end switch

	} // end encode3to4

	/**
	 * Serializes an object and returns the Base64-encoded

	 * version of that serialized object. If the object

	 * cannot be serialized or there is another error,

	 * the method will return <tt>null</tt>.

	 *

	 * @param serializableObject The object to encode

	 * @return The Base64-encoded object

	 * @since 1.4

	 */

/*	public static String encodeObject(java.io.Serializable serializableObject)

	{

		return encodeObject(serializableObject, true);

	} // end encodeObject
*/
	/**
	 * Serializes an object and returns the Base64-encoded

	 * version of that serialized object. If the object

	 * cannot be serialized or there is another error,

	 * the method will return <tt>null</tt>.

	 *

	 * @param serializableObject The object to encode

	 * @param breakLines Break lines at 80 characters or less.

	 * @return The Base64-encoded object

	 * @since 1.4

	 */

/*	public static String encodeObject(java.io.Serializable serializableObject, boolean breakLines)

	{

		java.io.ByteArrayOutputStream baos = null;

		java.io.OutputStream b64os = null;

		java.io.ObjectOutputStream oos = null;

		try

		{

			baos = new java.io.ByteArrayOutputStream();

			b64os = new Base64.OutputStream(baos, Base64.ENCODE, breakLines);

			oos = new java.io.ObjectOutputStream(b64os);

			oos.writeObject(serializableObject);

		} // end try

		catch (java.io.IOException e)

		{

			e.printStackTrace();

			return null;

		} // end catch

		finally

		{

			try
			{
				oos.close();
			}
			catch (Exception e)
			{}

			try
			{
				b64os.close();
			}
			catch (Exception e)
			{}

			try
			{
				baos.close();
			}
			catch (Exception e)
			{}

		} // end finally

		return new String(baos.toByteArray());

	} // end encode
*/

   /**
	* Creates the begin tag for a base 64 encoded string.
	* @param strBase64TagName the base 64 tag name
	* @return the begin tag for a base 64 encoded string
	*/
   public static String createBeginTag(String strBase64TagName)
   {
	   return Base64.BEGIN_TAG + strBase64TagName + Base64.TAG_END_SEQUENCE + "\n";
   }

   /**
	* Creates the end tag for a base 64 encoded string.
	* @param strBase64TagName the base 64 tag name
	* @return the end tag for a base 64 encoded string
	*/
   public static String createEndTag(String strBase64TagName)
   {
	   return "\n" + Base64.END_TAG + strBase64TagName + Base64.TAG_END_SEQUENCE + "\n";
   }


	/**
	 * Encodes a byte array into Base64 notation.

	 * Equivalen to calling

	 * <code>encodeBytes( source, 0, source.length )</code>

	 *

	 * @param source The data to convert

	 * @since 1.4

	 */

	public static String encodeBytes(byte[] source)

	{

		return encode(source, true);

	} // end encodeBytes

	/**
	 * Encodes a byte array into Base64 notation.

	 * Equivalen to calling

	 * <code>encodeBytes( source, 0, source.length )</code>

	 *

	 * @param source The data to convert

	 * @param breakLines Break lines at 80 characters or less.
	 *
	 * @return the encoded bytes or null

	 * @since 1.4

	 */

	public static String encode(byte[] source, boolean breakLines)

	{
		if(source==null)
			return null;
		return encode(source, 0, source.length, breakLines);

	} // end encodeBytes

	/**
	 * Encodes a byte array into Base64 notation.

	 *

	 * @param source The data to convert

	 * @param off Offset in array where conversion should begin

	 * @param len Length of data to convert

	 * @since 1.4

	 */

	public static String encode(byte[] source, int off, int len)

	{

		return encode(source, off, len, true);

	} // end encodeBytes

	/**
	 * Encodes a byte array into Base64 notation.

	 *

	 * @param source The data to convert

	 * @param off Offset in array where conversion should begin

	 * @param len Length of data to convert

	 * @param breakLines Break lines at 80 characters or less.

	 * @since 1.4

	 */

	public static String encode(byte[] source, int off, int len, boolean breakLines)

	{

		int len43 = len * 4 / 3;

		byte[] outBuff = new byte[ (len43) // Main 4:3

			+ ( (len % 3) > 0 ? 4 : 0) // Account for padding

			+ (breakLines ? (len43 / MAX_LINE_LENGTH) : 0)]; // New lines

		int d = 0;

		int e = 0;

		int len2 = len - 2;

		int lineLength = 0;

		for (; d < len2; d += 3, e += 4)

		{

			encode3to4(source, d + off, 3, outBuff, e);

			lineLength += 4;

			if (breakLines && lineLength == MAX_LINE_LENGTH)

			{

				outBuff[e + 4] = NEW_LINE;

				e++;

				lineLength = 0;

			} // end if: end of line

		} // en dfor: each piece of array

		if (d < len)

		{

			encode3to4(source, d + off, len - d, outBuff, e);

			e += 4;

		} // end if: some padding needed

		return new String(outBuff, 0, e);

	} // end encodeBytes

	/**
	 * Encodes a string in Base64 notation with line breaks

	 * after every 75 Base64 characters.

	 * Of course you probably only need to encode a string

	 * if there are non-ASCII characters in it such as many

	 * non-English languages.

	 *

	 * @param s the string to encode

	 * @return the encoded string

	 * @since 1.3

	 */

	public static String encodeString(String s)

	{

		return encodeString(s, true);

	} // end encodeString

	/**
	 * Encodes a string in Base64 notation with line breaks

	 * after every 75 Base64 characters.

	 * Of course you probably only need to encode a string

	 * if there are non-ASCII characters in it such as many

	 * non-English languages.

	 *

	 * @param s the string to encode

	 * @param breakLines Break lines at 80 characters or less.

	 * @return the encoded string or null

	 * @since 1.3

	 */

	public static String encodeString(String s, boolean breakLines)

	{
		if(s==null)
			return null;
		return encode(s.getBytes(), breakLines);

	} // end encodeString

	/**
	 * Reads a file and either encodes or decodes it.

	 *

	 * @param file The file to read

	 * @param encode Whether or not to encode file as it is read.

	 * @return The encoded/decoded file or <tt>null</tt> if there was an error.

	 * @since 1.4

	 */

/*	public static byte[] readFile(String file, boolean encode)

	{

		return readFile(new java.io.File(file), encode);

	} // end readFile
*/
	/**
	 * Reads a file and either encodes or decodes it.

	 *

	 * @param file The file to read

	 * @param encode Whether or not to encode file as it is read.

	 * @return The encoded/decoded file or <tt>null</tt> if there was an error.

	 * @since 1.4

	 */

/*	public static byte[] readFile(java.io.File file, boolean encode)

	{

		byte[] data = new byte[100];

		byte[] returnValue = null;

		int nextIndex = 0;

		int b = -1;

		Base64.InputStream bis = null;

		try
		{

			bis = new Base64.InputStream(

				new java.io.BufferedInputStream(

				new java.io.FileInputStream(file)), encode);

			while ( (b = bis.read()) >= 0)
			{

				// Resize array?

				if (nextIndex >= data.length)
				{

					byte[] temp = new byte[data.length << 1];

					System.arraycopy(data, 0, temp, 0, data.length);

					data = temp;

				} // end if: resize array

				data[nextIndex++] = (byte) b;

			} // end while: each byte

			returnValue = new byte[nextIndex];

			System.arraycopy(data, 0, returnValue, 0, nextIndex);

		} // end try

		catch (java.io.IOException e)
		{

			returnValue = null;

		} // end catch

		finally
		{

			try
			{
				bis.close();
			}
			catch (Exception e)
			{}

		} // end finally

		return returnValue;

	} // end readFile
*/
	/**
	 * Writes a byte array to a file either encoding

	 * it or decoding it as specified.

	 *

	 * @param data The array to write to a file.

	 * @param file The file to write to.

	 * @param encode Whether or not to encode the data.

	 * @return Whether or not the write was a success.

	 * @since 1.4

	 */

/*	public static boolean writeFile(byte[] data, String file, boolean encode)

	{

		return writeFile(data, 0, data.length, new java.io.File(file), encode);

	} // end writeFile
*/
	/**
	 * Writes a byte array to a file either encoding

	 * it or decoding it as specified.

	 *

	 * @param data The array to write to a file.

	 * @param file The file to write to.

	 * @param encode Whether or not to encode the data.

	 * @return Whether or not the write was a success.

	 * @since 1.4

	 */

/*	public static boolean writeFile(byte[] data, java.io.File file, boolean encode)

	{

		return writeFile(data, 0, data.length, file, encode);

	} // end writeFile
*/
	/**
	 * Writes a byte array to a file either encoding

	 * it or decoding it as specified.

	 *

	 * @param data The array to write to a file.

	 * @param offset The offset where the "real" data begins.

	 * @param length The amount of data to write.

	 * @param file The file to write to.

	 * @param encode Whether or not to encode the data.

	 * @return Whether or not the write was a success.

	 * @since 1.4

	 */
/*
	public static boolean writeFile(byte[] data, int offset, int length, java.io.File file, boolean encode)

	{

		Base64.OutputStream bos = null;

		boolean success = false;

		try
		{

			bos = new Base64.OutputStream(

				new java.io.BufferedOutputStream(

				new java.io.FileOutputStream(file)), encode);

			bos.write(data, offset, length);

			success = true;

		} // end try

		catch (java.io.IOException e)
		{

			success = false;

		} // end catch

		finally
		{

			try
			{
				bos.close();
			}
			catch (Exception e)
			{}

		} // end finally

		return success;

	} // end writeFile
*/
	/**
	 * Simple helper method that Base64-encodes a file

	 * and returns the encoded string or <tt>null</tt>

	 * if there was an error.

	 *

	 * @param rawfile The file to read

	 * @return The encoded file or <tt>null</tt> if there was an error.

	 * @since 1.4

	 */

/*	public static String encodeFromFile(String rawfile)

	{

		byte[] ebytes = readFile(rawfile, ENCODE);

		return ebytes == null ? null : new String(ebytes);

	} // end encodeFromFile
*/
	/**
	 * Simple helper method that Base64-decodes a file

	 * and returns the decoded data or <tt>null</tt>

	 * if there was an error.

	 *

	 * @param encfile The file to read

	 * @return The decoded file or <tt>null</tt> if there was an error.

	 * @since 1.4

	 */

/*	public static byte[] decodeFromFile(String encfile)

	{

		return readFile(encfile, DECODE);

	} // end encodeFromFile
*/
	/**
	 * Simple helper method that Base64-encodes data to a file.

	 *

	 * @param rawdata The data to write.

	 * @param file The file to read.

	 * @return Whether or not the write was a success.

	 * @since 1.4

	 */

/*	public static boolean encodeToFile(byte[] rawdata, String file)

	{

		return writeFile(rawdata, file, ENCODE);

	} // end encodeFromFile
*/
	/**
	 * Simple helper method that Base64-decodes data to a file.
	 *
	 * @param encdata The data to write.
	 * @param file The file to read.
	 * @return Whether or not the write was a success.
	 * @since 1.4
	 */
/*	public static boolean decodeToFile(byte[] encdata, String file)

	{

		return writeFile(encdata, file, DECODE);

	} // end encodeFromFile
*/
	/* ********  D E C O D I N G   M E T H O D S  ******** */





	/**
	 * Decodes the first four bytes of array <var>fourBytes</var>

	 * and returns an array up to three bytes long with the

	 * decoded values.

	 *

	 * @param fourBytes the array with Base64 content

	 * @return array with decoded values

	 * @since 1.3

	 */

/*	private static byte[] decode4to3(byte[] fourBytes)

	{

		byte[] outBuff1 = new byte[3];

		int count = decode4to3(fourBytes, 0, outBuff1, 0);

		byte[] outBuff2 = new byte[count];

		for (int i = 0; i < count; i++)
		{

			outBuff2[i] = outBuff1[i];

		}

		return outBuff2;

	}
*/
	/**
	 * Decodes four bytes from array <var>source</var>

	 * and writes the resulting bytes (up to three of them)

	 * to <var>destination</var>.

	 * The source and destination arrays can be manipulated

	 * anywhere along their length by specifying

	 * <var>srcOffset</var> and <var>destOffset</var>.

	 * This method does not check to make sure your arrays

	 * are large enough to accomodate <var>srcOffset</var> + 4 for

	 * the <var>source</var> array or <var>destOffset</var> + 3 for

	 * the <var>destination</var> array.

	 * This method returns the actual number of bytes that

	 * were converted from the Base64 encoding.

	 *

	 *

	 * @param source the array to convert

	 * @param srcOffset the index where conversion begins

	 * @param destination the array to hold the conversion

	 * @param destOffset the index where output will be put

	 * @return the number of decoded bytes converted

	 * @since 1.3

	 */

	private static int decode4to3(byte[] source, int srcOffset, byte[] destination, int destOffset)

	{

		// Example: Dk==

		if (source[srcOffset + 2] == EQUALS_SIGN)

		{

			// Two ways to do the same thing. Don't know which way I like best.

			//int outBuff =   ( ( DECODABET[ source[ srcOffset    ] ] << 24 ) >>>  6 )

			//              | ( ( DECODABET[ source[ srcOffset + 1] ] << 24 ) >>> 12 );

			int outBuff = ( (DECODABET[source[srcOffset]] & 0xFF) << 18)

				| ( (DECODABET[source[srcOffset + 1]] & 0xFF) << 12);

			destination[destOffset] = (byte) (outBuff >>> 16);

			return 1;

		}

		// Example: DkL=

		else if (source[srcOffset + 3] == EQUALS_SIGN)

		{

			// Two ways to do the same thing. Don't know which way I like best.

			//int outBuff =   ( ( DECODABET[ source[ srcOffset     ] ] << 24 ) >>>  6 )

			//              | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )

			//              | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 );

			int outBuff = ( (DECODABET[source[srcOffset]] & 0xFF) << 18)

				| ( (DECODABET[source[srcOffset + 1]] & 0xFF) << 12)

				| ( (DECODABET[source[srcOffset + 2]] & 0xFF) << 6);

			destination[destOffset] = (byte) (outBuff >>> 16);

			destination[destOffset + 1] = (byte) (outBuff >>> 8);

			return 2;

		}

		// Example: DkLE

		else

		{

			try
			{

				// Two ways to do the same thing. Don't know which way I like best.

				//int outBuff =   ( ( DECODABET[ source[ srcOffset     ] ] << 24 ) >>>  6 )

				//              | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )

				//              | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 )

				//              | ( ( DECODABET[ source[ srcOffset + 3 ] ] << 24 ) >>> 24 );

				int outBuff = ( (DECODABET[source[srcOffset]] & 0xFF) << 18)

					| ( (DECODABET[source[srcOffset + 1]] & 0xFF) << 12)

					| ( (DECODABET[source[srcOffset + 2]] & 0xFF) << 6)

					| ( (DECODABET[source[srcOffset + 3]] & 0xFF));

				destination[destOffset] = (byte) (outBuff >> 16);

				destination[destOffset + 1] = (byte) (outBuff >> 8);

				destination[destOffset + 2] = (byte) (outBuff);

				return 3;

			}
			catch (Exception e)
			{

				System.out.println("" + source[srcOffset] + ": " + (DECODABET[source[srcOffset]]));

				System.out.println("" + source[srcOffset + 1] + ": " + (DECODABET[source[srcOffset + 1]]));

				System.out.println("" + source[srcOffset + 2] + ": " + (DECODABET[source[srcOffset + 2]]));

				System.out.println("" + source[srcOffset + 3] + ": " + (DECODABET[source[srcOffset + 3]]));

				return -1;

			} //e nd catch

		}

	} // end decodeToBytes

	/**
	 * Decodes data from Base64 notation.

	 *

	 * @param s the string to decode

	 * @return the decoded data

	 * @since 1.4

	 */

	public static byte[] decode(String s)

	{

		byte[] bytes = s.getBytes();

		return decode(bytes, 0, bytes.length);

	} // end decode

	/**
	 * Decodes data from Base64 notation and

	 * returns it as a string.

	 * Equivlaent to calling

	 * <code>new String( decode( s ) )</code>

	 *

	 * @param s the strind to decode

	 * @return The data as a string

	 * @since 1.4

	 */

	public static String decodeToString(String s)

	{
		return new String(decode(s));

	} // end decodeToString

	/**
	 * Attempts to decode Base64 data and deserialize a Java

	 * Object within. Returns <tt>null if there was an error.

	 *

	 * @param encodedObject The Base64 data to decode

	 * @return The decoded and deserialized object

	 * @since 1.4

	 */

/*	public static Object decodeToObject(String encodedObject)

	{

		byte[] objBytes = decode(encodedObject);

		java.io.ByteArrayInputStream bais = null;

		java.io.ObjectInputStream ois = null;

		try

		{

			bais = new java.io.ByteArrayInputStream(objBytes);

			ois = new java.io.ObjectInputStream(bais);

			return ois.readObject();

		} // end try

		catch (java.io.IOException e)

		{

			e.printStackTrace();

			return null;

		} // end catch

		catch (java.lang.ClassNotFoundException e)

		{

			e.printStackTrace();

			return null;

		} // end catch

		finally

		{

			try
			{
				bais.close();
			}
			catch (Exception e)
			{}

			try
			{
				ois.close();
			}
			catch (Exception e)
			{}

		} // end finally

	} // end decodeObject
*/
	/**
	 * Decodes Base64 content in byte array format and returns

	 * the decoded byte array.

	 *

	 * @param source The Base64 encoded data

	 * @param off    The offset of where to begin decoding

	 * @param len    The length of characters to decode

	 * @return decoded data

	 * @since 1.3

	 */

	public static byte[] decode(byte[] source, int off, int len)

	{

		int len34 = len * 3 / 4;

		byte[] outBuff = new byte[len34]; // Upper limit on size of output

		int outBuffPosn = 0;

		byte[] b4 = new byte[4];

		int b4Posn = 0;

		int i = 0;

		byte sbiCrop = 0;

		byte sbiDecode = 0;

		for (i = off; i < off + len; i++)

		{

			sbiCrop = (byte) (source[i] & 0x7f); // Only the low seven bits

			sbiDecode = DECODABET[sbiCrop];

			if (sbiDecode >= WHITE_SPACE_ENC) // White space, Equals sign or better

			{

				if (sbiDecode >= EQUALS_SIGN_ENC)

				{

					b4[b4Posn++] = sbiCrop;

					if (b4Posn > 3)

					{

						outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn);

						b4Posn = 0;

						// If that was the equals sign, break out of 'for' loop

						if (sbiCrop == EQUALS_SIGN)
						{

							break;
						}

					} // end if: quartet built

				} // end if: equals sign or better

			} // end if: white space, equals sign or better

			else

			{

				System.err.println("Bad Base64 input character at " + i + ": " + source[i] + "(decimal)");

				return null;

			} // end else:

		} // each input character

		byte[] out = new byte[outBuffPosn];

		System.arraycopy(outBuff, 0, out, 0, outBuffPosn);

		return out;

	} // end decode

	/* ********  I N N E R   C L A S S   I N P U T S T R E A M  ******** */







	/**
	 * A {@link Base64#InputStream} will read data from another

	 * {@link java.io.InputStream}, given in the constructor,

	 * and encode/decode to/from Base64 notation on the fly.

	 *

	 * @see Base64

	 * @see java.io.FilterInputStream

	 * @since 1.3

	 */


} // end class Base64

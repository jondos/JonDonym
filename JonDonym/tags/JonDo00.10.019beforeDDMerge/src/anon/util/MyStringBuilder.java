package anon.util;

final public class MyStringBuilder
{
	private char[] value;
	private int aktPos;
	private int capacity;
	public MyStringBuilder(int len)
	{
		value = new char[len];
		aktPos = 0;
		capacity = len;
	}

	public void append(String s)
	{
		int len = s.length();
		if (s.length() > capacity)
		{
			capacity = len + value.length + 512;
			char[] tmpValue = new char[capacity];
			System.arraycopy(value, 0, tmpValue, 0, aktPos);
			value = tmpValue;
			capacity -= aktPos;
		}
		s.getChars(0, len, value, aktPos);
		aktPos += len;
		capacity -= len;
	}

	public void append(int i)
	{
		append(Integer.toString(i));
	}

	public void append(long i)
	{
		append(Long.toString(i));
	}

	public void setLength(int i)
	{
		aktPos = i;
	}

	public String toString()
	{
		return new String(value, 0, aktPos);
	}
}

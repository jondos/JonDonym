package anon.tor.util;

public final class Base16 
{
	public static String encode(byte[] bytes) 
	{
		StringBuffer buff = new StringBuffer();
		
		for (int i = 0; i < bytes.length; i++) 
		{
			int b = 0x0f & (bytes[i] >> 4);
			buff.append(encodeByte((byte) b));
			int a = 0x0f & bytes[i];
			buff.append(encodeByte((byte) a));
		}
		return buff.toString();
	}
	
	 private static char encodeByte(byte b) 
	 {
		 switch(b) 
		 {
		  	case (byte) 0:  return '0';
		 	case (byte) 1:  return '1';
		 	case (byte) 2:  return '2';
		 	case (byte) 3:  return '3';
		 	case (byte) 4:  return '4';
		 	case (byte) 5:  return '5';
		  	case (byte) 6:  return '6';
		  	case (byte) 7:  return '7';
		  	case (byte) 8:  return '8';
		  	case (byte) 9:  return '9';
		  	case (byte) 10: return 'A';
		  	case (byte) 11: return 'B';
		  	case (byte) 12: return 'C';
		 	case (byte) 13: return 'D';
		 	case (byte) 14: return 'E';
		 	case (byte) 15: return 'F';
		 	default:        return '0';
		 }
	}
}

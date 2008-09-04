package infoservice;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.GregorianCalendar;

import anon.crypto.DSAKeyPair;
import anon.crypto.PKCS12;
import anon.crypto.Validity;
import anon.crypto.X509DistinguishedName;
import anon.crypto.X509Extensions;
import anon.crypto.X509SubjectKeyIdentifier;

public class KeyGenTest
{

	public static void generateKeys(String isName, String passwd) throws IOException
	{
		PKCS12 ownCertificate;
		String strInfoServiceName;
		if (isName != null && isName.length() > 0)
		{
			strInfoServiceName = isName;
		}
		else
		{
			System.out.print("Please enter a name for the InfoService: ");
			BufferedReader din = new BufferedReader(new InputStreamReader(System.in));
			strInfoServiceName = din.readLine();
		}
		String strPasswd = "";
		if (passwd == null)
		{
			System.out.print("Please enter a password to protect the private key of the InfoService: ");
			BufferedReader din = new BufferedReader(new InputStreamReader(System.in));
			strPasswd = din.readLine();
		}
		else
		{
			strPasswd = passwd;
		}
		System.out.println("Key generation started!");
		DSAKeyPair keyPair = DSAKeyPair.getInstance(new SecureRandom(), 1024, 80);
		FileOutputStream out1 = new FileOutputStream("private.pfx");
		FileOutputStream out2 = new FileOutputStream("public.cer");
		X509Extensions extensions = new X509Extensions(new X509SubjectKeyIdentifier(keyPair.getPublic()));
		ownCertificate = new PKCS12(new X509DistinguishedName("cn=" + strInfoServiceName), keyPair,
									new Validity(new GregorianCalendar(), 5), extensions);
		ownCertificate.store(out1, strPasswd.toCharArray());
		ownCertificate.getX509Certificate().store(out2);
		out1.close();
		out2.close();
		System.out.println("Key generation finished!");
	}
}

package jpi.util;

import java.io.FileInputStream;
import org.w3c.dom.Document;
import anon.crypto.tinytls.TinyTLS;
import anon.crypto.JAPCertificate;
import java.net.Socket;
import anon.pay.HttpClient;
import anon.pay.xml.XMLErrorMessage;
import anon.util.XMLUtil;

public class ChargeClient
{
	public ChargeClient(String a_file, JAPCertificate a_biCert)
	{
		try
		{
			FileInputStream f = new FileInputStream(a_file);
			Document doc = XMLUtil.readXMLDocument(f);
			try
			{
				f.close();
			}
			catch (Exception ex2)
			{}
			TinyTLS tls = new TinyTLS("132.199.130.150", 9950);

			tls.setSoTimeout(30000);
			tls.setRootKey(a_biCert.getPublicKey());
			tls.startHandshake();
			Socket socket = tls;
			HttpClient httpClient = new HttpClient(socket);
			httpClient.writeRequest("POST", "externalCharge", doc.getDocumentElement().toString());
			doc = httpClient.readAnswer();
			XMLErrorMessage m = new XMLErrorMessage(doc);
			if (m.getErrorCode() == XMLErrorMessage.ERR_OK)
			{
				System.out.println("Ok");
			}
			else
			{
				System.out.println("Error! (maybe password wrong?)");
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

	}

	public static void main(String[] args)
	{
		JAPCertificate cert = null;
		try
		{
			cert = JAPCertificate.getInstance(new FileInputStream(args[1]));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (cert != null)
		{
			ChargeClient chargeclient = new ChargeClient(args[0], cert);
		}
		else
		{
			System.out.println("Cert null!");
		}
	}
}

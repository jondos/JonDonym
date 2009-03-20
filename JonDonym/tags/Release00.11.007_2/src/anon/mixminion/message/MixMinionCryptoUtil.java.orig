package anon.mixminion.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.bouncycastle.crypto.digests.SHA1Digest;

import anon.crypto.MyAES;
import anon.util.ByteArrayUtil;
import anon.util.ZLibTools;

/**
 * Some Helping Methods for the MixMinion
 * @author Stefan Roenisch and Jens Kempe
 *
 */
public class MixMinionCryptoUtil
{

    /**
     * Creates an octet-array fills with zeros
     * @param len int
     * @return the array byte[]
     */
    static byte[] zeroArray(int len) {
        byte[] ret = new byte[len];
        for (int i=0; i<len;i++) {
            ret[i] = new Integer(0).byteValue();
        }
        return ret;
    }

    /**
     * Creates an octet-array fills with random Bytes
     * @param len int
     * @return the array byte[]
     */
    public static byte[] randomArray(int len) {
        byte[] ret = new byte[len];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(ret);
        return ret;
    }


    /**
     * xor of two byte[]
     * @param one byte[]
     * @param two byte[]
     * @return the xor, if not the same length: null
     */
    static byte[] xor(byte[] one, byte[] two) {
        if (one.length != two.length)
        	{
        	return null;
        	}
        else {
            byte[] result = new byte[one.length];
            short n =(short) 0;
            while (n<one.length) {
                result[n] = (byte) (one[n]^two[n]);
                n++;
            }

        return result;
        }
    }


    /**
     * SHA1Digest of x
     * @param x byte[]
     * @return the digest byte[]
     */
    public static byte[] hash(byte[] x)
    {
        SHA1Digest myDigest = new SHA1Digest();
        myDigest.update(x, 0, x.length);
        byte[] ret = new byte[myDigest.getDigestSize()];
        myDigest.doFinal(ret, 0);

        return ret;
    }

    /**
     * encrypts a given array with aes in counter mode
     * @author Stefan
     * @param key byte[]
     * @param plain byte[]
     * @return the ciphered array byte[]
     */
    static byte[] aes_ctr(byte[] key, byte[] plain,int len)
    {
        MyAES engine = new MyAES();

        byte[] data = new byte[len];
        try {
            engine.init(true,key);
            engine.processBytesCTR(plain,0,data,0,len);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return data;
    }


    /**
     * Die Encrypt-Funktion nach der MixMinion-Spec
     */
    static byte[] Encrypt(byte[] K, byte[] M)
    {
//      - Encrypt(K, M) = M ^ PRNG(K,Len(M)) - The encryption of an octet array
//      M with our stream cipher, using key K.  (Encrypt(Encrypt(M)) = M.)
        return xor(M, createPRNG(K, M.length));
    }


    /**
     * Creates a octet-array using Cryptographic Stream generator
     * @param key byte[]
     * @param len int
     * @return the array byte[]
     */
    static byte[] createPRNG(byte[] key, int len) {

    	return aes_ctr(key, zeroArray(len),len);
    }


    /**
     * Die SPRP_Encrypt-Funktion nach der MixMinion-Spec
     * @param k
     * @param M
     * @return
     */
    static byte[] SPRP_Encrypt(byte[] K, byte[] M)
    {
//    3.1.1.3. Super-pseudorandom permutation
//
//    To encrypt an octet array so that any change in the encrypted
//    value will make the decryption look like random bits, we use an
//    instance of the LIONESS SPRP, with SHA-1 for a hash and the
//    stream described in 3.1.1.2 above.
//
//    Thus, in the notation described below, we encrypt a message M with
//    a key K as follows:
//        K1 = K
//        K2 = K ^ [00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01]
//        K3 = K ^ [00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02]
//        K4 = K ^ [00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03]
//        L := M[0:20]
//        R := M[20:Len(M)-20]
//        R := Encrypt( Hash(K1 | L | K1)[0:16], R )
//        L := L ^ Hash(K2 | R | K2)
//        R := Encrypt( Hash(K3 | L | K3)[0:16], R )
//        L := L ^ Hash(K4 | R | K4)
//        SPRP_Encrypt(K, M) = L | R

        byte[] h1 = MixMinionCryptoUtil.zeroArray(20);
        h1[19] = new Integer(1).byteValue();
        byte[] h2 = MixMinionCryptoUtil.zeroArray(20);
        h2[19] = new Integer(2).byteValue();
        byte[] h3 = MixMinionCryptoUtil.zeroArray(20);
        h3[19] = new Integer(3).byteValue();

        byte[] K1 = K;
        byte[] K2 = xor(K, h1);
        byte[] K3 = xor(K, h2);
        byte[] K4 = xor(K, h3);
        byte[] L = ByteArrayUtil.copy(M,0,20);
        byte[] R = ByteArrayUtil.copy(M,20,M.length-20);
        R = Encrypt( ByteArrayUtil.copy(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(K1,L,K1)), 0, 16), R );
        L = xor(L,MixMinionCryptoUtil.hash(ByteArrayUtil.conc(K2,R,K2)));
        R = Encrypt( ByteArrayUtil.copy(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(K3,L,K3)), 0, 16), R );
        L = xor(L,MixMinionCryptoUtil.hash(ByteArrayUtil.conc(K4,R,K4)));

        return ByteArrayUtil.conc(L,R);
    }


    /**
     * this Method is for compressing some Data, with the ZLIB-Standard
     */
    static byte[] compressData(byte[] message)
    {
        byte[] compress = ZLibTools.compress(message);
        if (compress[0]!=0x78 || compress[1]+256!=0xDA)
            throw new RuntimeException("The Compressed Messege didn't start with 0x78DA");

        return compress;
    }

    
    /**
     * this Method is for compressing some Data
     */
    private static byte[] ZIPcompressData(byte[] message)
    {
        byte[] compress = null;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zip;
        
        try
        {
            zip = new ZipOutputStream(baos);
            zip.setLevel(9);
            zip.setMethod(ZipOutputStream.DEFLATED);
            ZipEntry e = new ZipEntry("MixMinionZip"); //Name darf anscheind nicht leer sein
            zip.putNextEntry(e);
            zip.write(message);
            zip.flush();
            zip.close();
            baos.flush();
            baos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        compress = baos.toByteArray();

        // only for Testing, if the Compression was ok.
        if (!ByteArrayUtil.equal(message, ZIPextractData(compress)))
            throw new RuntimeException("Something with Compression/Decompression was wrong!");
        
        return compress;
    }
    
    
    /**
     * this Method is for extracting some Data 
     */
    private static byte[] ZIPextractData(byte[] payload)
    {
        byte[] message = null;
        
        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        ZipInputStream zip;
        try {
            zip = new ZipInputStream(bais);
            ZipEntry e = zip.getNextEntry(); //sonst Null-Pointer-Exception

            boolean go = true;
            int size = -1;
            while (go)
            {
                size++;
                int c = zip.read();
                byte[] b = {(byte) c};
                if (c != -1) message = ByteArrayUtil.conc(message, b);
                else go = false;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        

        return message;
    }

    
    /**
     * this Method is for compressing some Data
     */
    private static byte[] GZIPcompressData(byte[] message)
    {
        byte[] payload = null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try
        {
            gzip = new GZIPOutputStream(baos);
            gzip.write(message);
            gzip.flush();
            gzip.close();
            baos.flush();
            baos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        payload = baos.toByteArray();
        return payload;
    }

    /**
     * this Method is for extracting some Data
     */
    private static byte[] GZIPextractData(byte[] payload)
    {
        byte[] message = null;

        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        GZIPInputStream gzip;
        try {
            gzip = new GZIPInputStream(bais);

            boolean go = true;
            int size = -1;
            while (go)
            {
                size++;
                int c = gzip.read();
                byte[] b = {(byte) c};
                if (c != -1) message = ByteArrayUtil.conc(message, b);
                else go = false;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return message;
    }
}

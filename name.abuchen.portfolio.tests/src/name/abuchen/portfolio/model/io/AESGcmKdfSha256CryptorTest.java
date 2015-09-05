package name.abuchen.portfolio.model.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.io.AESGcmKdfSha256Cryptor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AESGcmKdfSha256CryptorTest
{
    Client client;

    @Before
    public void createClient()
    {
        client = new Client();
        client.addAccount(new Account());
        client.addAccount(new Account());
        client.addPortfolio(new Portfolio());
        client.addPortfolio(new Portfolio());

        Security security = new Security();
        security.setName("Some security"); //$NON-NLS-1$
        client.addSecurity(security);
    }    

    @Test
    public void testCryptor() throws IOException 
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //
        char[] password = new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
        AESGcmKdfSha256Cryptor saveCryptor = new AESGcmKdfSha256Cryptor(password);
        //
        saveCryptor.save(client, 0, baos);
        //
        byte[] crypt = baos.toByteArray();
        //
        Assert.assertArrayEquals(Arrays.copyOfRange(crypt, 0, saveCryptor.getSignature().length), saveCryptor.getSignature());
        //
        AESGcmKdfSha256Cryptor loadCryptor = new AESGcmKdfSha256Cryptor(password);
        //
        Client copyOfClient = loadCryptor.load(new ByteArrayInputStream(crypt));
        //
        Assert.assertNotNull(copyOfClient);
        
    }
    
}

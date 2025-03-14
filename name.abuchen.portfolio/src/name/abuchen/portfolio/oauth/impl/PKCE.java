package name.abuchen.portfolio.oauth.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.oauth.AuthenticationException;

public class PKCE
{
    public static final String CODE_CHALLENGE_METHOD = "S256"; //$NON-NLS-1$

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"; //$NON-NLS-1$
    private static final int CODE_VERIFIER_LENGTH = 128;
    private static final String CODE_CHALLENGE_ALGORITHM = "SHA-256"; //$NON-NLS-1$

    private final String codeVerifier;
    private final String codeChallenge;

    private PKCE(String codeVerifier, String codeChallenge)
    {
        this.codeVerifier = codeVerifier;
        this.codeChallenge = codeChallenge;
    }

    public String getCodeVerifier()
    {
        return codeVerifier;
    }

    public String getCodeChallenge()
    {
        return codeChallenge;
    }

    public static PKCE generate() throws AuthenticationException
    {
        try
        {
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);
            return new PKCE(codeVerifier, codeChallenge);
        }
        catch (Exception e)
        {
            throw new AuthenticationException(Messages.OAuthErrorGeneratingPKCE, e);
        }
    }

    private static String generateCodeVerifier()
    {
        SecureRandom random = new SecureRandom();
        StringBuilder verifier = new StringBuilder(CODE_VERIFIER_LENGTH);

        for (int ii = 0; ii < CODE_VERIFIER_LENGTH; ii++)
        {
            int index = random.nextInt(CHARACTERS.length());
            verifier.append(CHARACTERS.charAt(index));
        }

        return verifier.toString();
    }

    private static String generateCodeChallenge(String codeVerifier) throws AuthenticationException
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance(CODE_CHALLENGE_ALGORITHM);
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new AuthenticationException(CODE_CHALLENGE_ALGORITHM, e);
        }
    }
}

package name.abuchen.portfolio.util.com.jenkov.io;

/**
 */
public class MockTokenResolver implements ITokenResolver {

    @Override
    public String resolveToken(String tokenName) {
        if("token1".equals(tokenName)) {
            return "123";
        }
        if("token2".equals(tokenName)) {
            return "";
        }

        return "";
    }
}

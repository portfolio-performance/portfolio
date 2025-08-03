package name.abuchen.portfolio.online.impl;

import java.util.HashMap;
import java.util.Map;

public class PortfolioReportCoins
{
    public static class Coin
    {
        private final String name;
        private final String onlineId;
        private final String tickerSymbol;
        private final String coindId;

        public Coin(String name, String onlineId, String tickerSymbol, String coindId)
        {
            this.name = name;
            this.onlineId = onlineId;
            this.tickerSymbol = tickerSymbol;
            this.coindId = coindId;
        }

        public String getName()
        {
            return name;
        }

        public String getOnlineId()
        {
            return onlineId;
        }

        public String getTickerSymbol()
        {
            return tickerSymbol;
        }

        public String getCoinGeckoCoindId()
        {
            return coindId;
        }
    }

    @SuppressWarnings("nls")
    private static final Coin[] COINS = new Coin[] {
                    new Coin("Polygon", "060518ff-1f9d-433c-80e2-635086201b1d", "matic", "matic-network"),
                    new Coin("Chainlink", "64744786-b049-49e3-8c62-2180bc84fae7", "link", "chainlink"),
                    new Coin("Ondo", "d55a30f8-c507-406d-a817-e970c1ec749f", "ondo", "ondo-finance"),
                    new Coin("Bittensor", "1693a877-32d6-4c42-880a-83755826c848", "tao", "bittensor"),
                    new Coin("NEAR Protocol", "01570071-8048-4977-9487-1363d6f89b10", "near", "near"),
                    new Coin("Shiba Inu", "bfcea04e-41c9-47ca-8bed-7b640aede1ef", "shib", "shiba-inu"),
                    new Coin("Algorand", "f5e44047-a3bc-4a6d-a609-90821f423a34", "algo", "algorand"),
                    new Coin("XRP", "03a32943-4c1f-4d7e-aef9-52399ff8e133", "xrp", "ripple"),
                    new Coin("Bitcoin", "ed2b7e46-9f36-4e27-854e-8bd1728dd1b5", "btc", "bitcoin"),
                    new Coin("Bitcoin Cash", "76551ed5-d4ba-4dab-a9b4-3ea6ed013c7f", "bch", "bitcoin-cash"),
                    new Coin("Bitpanda Ecosystem", "77a85356-a708-4cd5-a23a-66f6a9e08f9a", "best",
                                    "bitpanda-ecosystem-token"),
                    new Coin("Artificial Superintelligence Alliance", "e4b8d88f-8b62-43f2-b21a-b4dca17d7ec7", "fet",
                                    "fetch-ai"),
                    new Coin("Hedera", "a8259d8c-2454-4369-a708-799df95658d1", "hbar", "hedera-hashgraph"),
                    new Coin("Render", "41e734c6-87f0-4ac9-bbaf-73700c42a3a0", "render", "render-token"),
                    new Coin("Sui", "0422bb7e-fd4d-4528-8fdb-b1dc6b308a54", "sui", "sui"),
                    new Coin("TRON", "d88f809e-d086-4208-aacb-55680ee20283", "trx", "tron"),
                    new Coin("Tether", "ae347542-5c40-42b2-b5ae-0a00221c0947", "usdt", "tether"),
                    new Coin("VeChain", "f2276d42-1366-494b-85e5-62f20a939d17", "vet", "vechain"),
                    new Coin("Stellar", "02ea37a8-52e5-4e8b-a0fa-a476f52c817b", "xlm", "stellar"),
                    new Coin("Aave", "80967b53-da14-4639-b5db-d06ec3c4f096", "aave", "aave"),
                    new Coin("Ethereum", "68ec1d0f-60f9-4c43-8a3c-0304f1af8834", "eth", "ethereum"),
                    new Coin("Polkadot", "07d13fba-90a3-4f22-9903-25bd3bc56390", "dot", "polkadot"),
                    new Coin("Cardano", "a3293f5c-d6a3-4d8e-bc5a-8318b152b623", "ada", "cardano"),
                    new Coin("Litecoin", "dbf10d3b-b329-4875-8910-7b801a71ba0d", "ltc", "litecoin"),
                    new Coin("Solana", "4953dc68-5a9a-4f83-ad1b-b1ecd0ac56f6", "sol", "solana"),
                    new Coin("Dogecoin", "ab9a7b8c-5d3e-4e12-9960-e8fae7522814", "doge", "dogecoin") };

    private final Map<String, Coin> migrations;

    public PortfolioReportCoins()
    {
        migrations = new HashMap<>();

        for (Coin coin : COINS)
        {
            migrations.put(coin.onlineId, coin);
            migrations.put(coin.onlineId.replace("-", ""), coin); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public boolean contains(String onlineId)
    {
        return migrations.containsKey(onlineId);
    }

    public Coin getCoinByOnlineId(String onlineId)
    {
        return migrations.get(onlineId);
    }
}

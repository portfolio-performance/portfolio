package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class PostfinancePDFExtractor extends AbstractPDFExtractor
{
    public PostfinancePDFExtractor(Client client)
    {
        /***
         * Postfinance offers three accounts with different currencies (CHF, EUR, USD)
         * There are two possibilities to buy shares of foreign currencies:
         * - Transfer money from CHF account to EUR/USD account and buy it in foreign currency
         * - Buy EUR/USD shares from CHF account directly (actual exchange rate will be taken) 
         */

        super(client);

        addBankIdentifier("PostFinance"); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addDividendsTransaction();
        addCapitalGainTransaction();
        addFeeTransaction();
        addInterestTransaction();
        addKontoauszugGiro();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "PostFinance AG"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Börsentransaktion: Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("^Börsentransaktion: Kauf (.*)$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name", "isin", "shares", "currency", "transactionCurrency", "amount")
                        .find("Titel Ort der Ausführung")
                        .match("^(?<name>.*) ISIN: (?<isin>\\S*) (.*)$")
                        .match("^(?<shares>[\\d+',.]*) ([\\d+',.]*) (?<currency>\\w{3}+) ([\\d+',.]*)$")
                        .match("^Zu Ihren Lasten (?<transactionCurrency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(v.get("transactionCurrency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("feecurrency", "fee").optional()
                        .match("^Kommission (?<feecurrency>\\w{3}+) (?<fee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money fee = Money.of(asCurrencyCode(v.get("feecurrency")), asAmount(v.get("fee")));
                            if (fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, fee));
                        })

                        .section("taxcurrency", "tax").optional()
                        .match("^Abgabe \\(Eidg. Stempelsteuer\\) (?<taxcurrency>\\w{3}+) (?<tax>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("taxcurrency")), asAmount(v.get("tax")));
                            if (tax.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .section("stockfeecurrency", "stockfee").optional()
                        .match("^Börsengebühren (?<stockfeecurrency>\\w{3}+) (?<stockfee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money stock_fee = Money.of(asCurrencyCode(v.get("stockfeecurrency")), asAmount(v.get("stockfee")));
                            if (stock_fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, stock_fee));
                        })

                        // buy shares directly from CHF account
                        .section("amount", "currency", "exchangeRate", "forexCurrency", "forexAmount").optional()
                        .match("^Total (?<forexCurrency>\\w{3}+) (?<forexAmount>[\\d+',.]*)$")
                        .match("^Wechselkurs (?<exchangeRate>[\\d+',.]*)$")
                        .match("^(?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forexAmount")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            if (forex.getCurrencyCode()
                                            .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                            {
                                
                                t.getPortfolioTransaction()
                                                .addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                            }
                        })

                        .section("date")
                        .match("^Betrag belastet auf Kontonummer (\\d+), Valutadatum (?<date>\\d+\\.\\d+\\.\\d{4})$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Börsentransaktion: Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("^Börsentransaktion: Verkauf (.*)$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("name", "isin", "shares", "currency")
                        .find("Titel Ort der Ausführung")
                        .match("^(?<name>.*) ISIN: (?<isin>\\S*) (.*)$")
                        .match("^(?<shares>[\\d+',.]*) ([\\d+',.]*) (?<currency>\\w{3}+) ([\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("feecurrency", "fee").optional()
                        .match("^Kommission (?<feecurrency>\\w{3}+) (?<fee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money fee = Money.of(asCurrencyCode(v.get("feecurrency")), asAmount(v.get("fee")));
                            if (fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, fee));
                        })

                        .section("taxcurrency", "tax").optional()
                        .match("^Abgabe \\(Eidg. Stempelsteuer\\) (?<taxcurrency>\\w{3}+) (?<tax>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("taxcurrency")), asAmount(v.get("tax")));
                            if (tax.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .section( "stockfeecurrency", "stockfee").optional()
                        .match("^Börsengebühren (?<stockfeecurrency>\\w{3}+) (?<stockfee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money stock_fee = Money.of(asCurrencyCode(v.get("stockfeecurrency")), asAmount(v.get("stockfee")));
                            if (stock_fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, stock_fee));
                        })

                        .section("amount")
                        .match("^Zu Ihren Gunsten (\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("date")
                        .match("^Betrag gutgeschrieben auf Ihrer Kontonummer (\\d+), Valutadatum (?<date>\\d+\\.\\d+\\.\\d{4})$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addDividendsTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Block block = new Block("^Dividende Unsere Referenz(.*)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .oneOf(
                                        // There are two kinds of dividend exports
                                        // 1st:
                                        // ISIN: <isin>
                                        // <name> NKN: 2560588 <shares>
                                        section -> section.attributes("name", "isin", "shares")
                                        .match("^ISIN: (?<isin>\\S*)$")
                                        .match("^(?<name>.*)NKN: [\\d+]* (?<shares>[\\d+',.]*)$")
                                        .assign((t, v) -> {
                                            t.setSecurity(getOrCreateSecurity(v));
                                            t.setShares(asShares(v.get("shares")));
                                        }),
                                        // 2nd:
                                        // <name> ISIN: <isin>NKN: 3291273 <shares>
                                        section -> section.attributes("name", "isin", "shares")
                                        .match("^(?<name>.*) ISIN: (?<isin>\\S*)NKN: [\\d+]* (?<shares>[\\d+',.]*)$")
                                        .assign((t, v) -> {
                                            t.setSecurity(getOrCreateSecurity(v));
                                            t.setShares(asShares(v.get("shares")));
                                        })
                        )

                        .section("date")
                        .match("^Ausführungsdatum (?<date>\\d+\\.\\d+\\.\\d{4})$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .section("gross", "grosscurrency", "amount", "currency")
                        .match("^Betrag (?<grosscurrency>\\w{3}+) (?<gross>[\\d+',.]*)$")
                        .match("^Total (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.getSecurity().setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            long gross = asAmount(v.get("gross"));
                            long tax = gross - t.getAmount();
                            Unit unit = new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")), tax));
                            if (unit.getAmount().getCurrencyCode().equals(t.getCurrencyCode()))
                                t.addUnit(unit);
                        })

                        .wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addCapitalGainTransaction()
    {
        DocumentType type = new DocumentType("Kapitalgewinn");
        this.addDocumentTyp(type);

        Block block = new Block("^Kapitalgewinn Unsere Referenz(.*)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .oneOf(
                                        // There are two kinds of dividend exports
                                        // 1st:
                                        // ISIN: <isin>
                                        // <name> NKN: 2560588 <shares>
                                        section -> section.attributes("name", "isin", "shares")
                                        .match("^ISIN: (?<isin>\\S*)$")
                                        .match("^(?<name>.*)NKN: [\\d+]* (?<shares>[\\d+',.]*)$")
                                        .assign((t, v) -> {
                                            t.setSecurity(getOrCreateSecurity(v));
                                            t.setShares(asShares(v.get("shares")));
                                        }),
                                        // 2nd:
                                        // <name> ISIN: <isin>NKN: 3291273 <shares>
                                        section -> section.attributes("name", "isin", "shares")
                                        .match("^(?<name>.*) ISIN: (?<isin>\\S*)NKN: [\\d+]* (?<shares>[\\d+',.]*)$")
                                        .assign((t, v) -> {
                                            t.setSecurity(getOrCreateSecurity(v));
                                            t.setShares(asShares(v.get("shares")));
                                        })
                        )

                        .section("date")
                        .match("^Ausführungsdatum (?<date>\\d+\\.\\d+\\.\\d{4})$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .section("currency", "amount")
                        .match("^Total (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.getSecurity().setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addFeeTransaction()
    {
        DocumentType type = new DocumentType("Jahresgebühr");
        this.addDocumentTyp(type);

        Block block = new Block("^Jahresgebühr (.*)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.FEES);
                            return transaction;
                        })

                        .section("date", "amount", "currency")
                        .find("^Jahresgebühr (.*)")
                        .match("^Valutadatum (?<date>\\d+.\\d+.\\d{4}+)$")
                        .match("^Betrag belastet (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                        .wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addInterestTransaction()
    {
        DocumentType type = new DocumentType("Zinsabschluss");
        this.addDocumentTyp(type);

        Block block = new Block("^Zinsabschluss (.*)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.INTEREST);
                            return transaction;
                        })

                        .section("date", "amountGross", "amountNet", "currency")
                        .find("^Zinsabschluss (.*)")
                        .match("^(Kontonummer|IBAN){1} (.*) (?<currency>[A-Z]{3}+)\\s?")
                        .match("^(?i:BRUTTOZINS) (?<amountGross>[\\d+',.]*)(\\s.*)?")
                        .match("^(?i:NETTOZINS) (?<amountNet>[\\d+',.]*)(\\s.*)?")
                        .match("^(?<date>\\d+.\\d+.\\d{4}+) Kontostand (.*)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amountNet")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            processTaxEntry(t, v, type);
                        })
                        .wrap(t -> {
                            if (t.getAmount()>0)
                            {
                                return new TransactionItem(t);
                            }
                            else
                            {
                                return null;
                            }
                        }));
    }

    @SuppressWarnings("nls")
    private void addKontoauszugGiro()
    {
        DocumentType type = new DocumentType("Kontoauszug", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^(Kontonummer|IBAN){1} (.*) (?<currency>[A-Z]{3}+)\\s?");
            
            for (String line : lines)
            {
                Matcher mCurrency = pCurrency.matcher(line);

                if (mCurrency.matches())
                {
                    context.put("currency", mCurrency.group("currency"));
                    break;
                }
            }

        });
        this.addDocumentTyp(type);

        String removalPattern = "^(\\d{2}.\\d{2}.\\d{2}\\s)?(E-FINANCE|AUFTRAG.+IRECT|ESR|GIRO.+OST|GIRO.+ANK|ÜBERTRAG AUF KONTO|ÜBERTRAG A UF K ONTO|AUFTRAG.+ASISLASTSCHRIFT|KAUF.+IENSTLEISTUNG(.+\\.\\d{4}){0,1}|KAUF.+HOPPING(.+\\.\\d{4}){0,1}|GIRO INTERNATIONAL \\(SEPA\\)|BARGELDBEZUG(.+\\.\\d{4})?|ONLINE-SHOPPING|TWINT.+(ENDEN|DIENSTLEISTUNG)){1}.*?\\s(?<amount>[\\d+',.\\s]+)\\s(?<date>\\d{2}.\\d{2}.\\d{2}+)\\s?([\\d+',.\\s]+)?$"; 
        Block removalBlock = new Block(removalPattern);
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.REMOVAL);
                            return entry;
                        })

                        .section("date", "amount")
                        .match(removalPattern)
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));     
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        String depositPattern = "^(\\d{2}.\\d{2}.\\d{2}\\s)?(GIRO AUSLAND|GIRO AUS ONLINE-SIC \\d{3,4}|GIRO.+ONTO|ÜBERTRAG AUS KONTO|ÜBERTRAG A US K ONTO|GUTSCHRIFT.+HOPPING|GUTSCHRIFT.+REMDBANK \\d{3,4}|GUTSCHRIFT.+REMDBANK|. EINZAHLUNGSSCHEIN\\/QR-ZAHLTEIL){1}.*?\\s(?<amount>[\\d+',.\\s]+)\\s(?<date>\\d{2}.\\d{2}.\\d{2}+)(\\s)?([\\d+',.\\s]+)?$";
        Block depositBlock = new Block(depositPattern);
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DEPOSIT);
                            return entry;
                        })

                        .section("date", "amount")
                        .match(depositPattern)
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        String transferPattern = "^(\\d{2}.\\d{2}.\\d{2}\\s)?(ÜBERTRAG)?\\s(?<amount>[\\d+',.\\s]+)\\s(?<date>\\d{2}.\\d{2}.\\d{2}+)\\s?([\\d+',.\\s]+)?$";
        Block transferBlock = new Block(transferPattern);
        type.addBlock(transferBlock);
        transferBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            return entry;
                        })

                        .section("date", "amount")
                        .match(transferPattern)
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));
                        })
                        .section("direction").optional()
                        .match("^(?<direction>AUS|AUF) KONTO.*")
                        .assign((t, v) -> {
                            AccountTransaction.Type transactionType = v.get("direction").equals("AUF") ? AccountTransaction.Type.REMOVAL : AccountTransaction.Type.DEPOSIT;
                            t.setType(transactionType);
                        })

                        .wrap(TransactionItem::new));

        String feePattern = "^(\\d{2}.\\d{2}.\\d{2}\\s)?(FÜR DIE KONTOFÜHRUNG|PREIS.+ONTOFÜHRUNG|PREIS.*SCHALTER|JAHRESPREIS LOGIN|FÜR KONTOAUSZUG PAPIER|FÜR GIRO INTERNATIONAL \\(SEPA\\)){1}.*?\\s+(?<amount>[\\d+',.\\s]+)\\s(?<date>\\d{2}.\\d{2}.\\d{2}+)(\\s)?([\\d+',.\\s]+)?$";
        Block feeBlock = new Block(feePattern);
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.FEES);
                            return entry;
                        })

                        .section("date", "amount")
                        .match(feePattern)
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));     
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));
                        })

                        .wrap(t -> {
                            if (t.getAmount()>0)
                            {
                                return new TransactionItem(t);
                            }
                            else
                            {
                                return null;
                            }
                        }));

        String interestPattern = "^(\\d{2}.\\d{2}.\\d{2}\\s)?(ZINSABSCHLUSS.+\\d{4,6}){1}.*?\\s(?<amount>[\\d+',.\\s]+)\\s(?<date>\\d{2}.\\d{2}.\\d{2}+)(\\s)?([\\d+',.\\s]+)?$";
        Block interestBlock = new Block(interestPattern);
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.INTEREST);
                            return entry;
                        })

                        .section("date", "amount")
                        .match(interestPattern)
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));     
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));
                        })

                        .wrap(t -> {
                            if (t.getAmount()>0)
                            {
                                return new TransactionItem(t);
                            }
                            else
                            {
                                return null;
                            }
                        }));
    }

    @SuppressWarnings("nls")
    private void processTaxEntry(name.abuchen.portfolio.model.Transaction transaction, Map<String, String> v, DocumentType type)
    {
        long amountGross = asAmount(v.get("amountGross"));
        long amountNet = asAmount(v.get("amountNet"));
        long tax = amountGross - amountNet;
        
        if(tax > 0)
        {
            Money monetaryTax = Money.of(asCurrencyCode(v.get("currency")), tax);
            PDFExtractorUtils.checkAndSetTax(monetaryTax, transaction, type);
        }
    }

    @Override
    protected long asAmount(String value)
    {
        value = value.trim().replaceAll(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected long asShares(String value)
    {
        value = value.trim().replaceAll(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        value = value.trim().replaceAll(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}

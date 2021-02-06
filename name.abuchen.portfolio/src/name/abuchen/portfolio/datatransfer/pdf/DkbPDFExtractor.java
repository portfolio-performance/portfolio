package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

@SuppressWarnings("nls")
public class DkbPDFExtractor extends AbstractPDFExtractor
{
    private static final String EXCHANGE_RATE = "exchangeRate"; //$NON-NLS-1$
    private static final String FLAG_WITHHOLDING_TAX_FOUND  = "exchangeRate"; //$NON-NLS-1$

    public DkbPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier(""); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addInterestTransaction();
        addDividendTransaction();
        addInvestmentEarningTransaction();
        addRemoveTransaction();
        addTransferOutTransaction();
        addParticipationcertificateEarningTransaction();
        addParticipationcertificateRefundTransaction();
        addInvestmentPayoutTransaction();
        addBuyTransactionFundsSavingsPlan();
        addAdvanceTaxTransaction();
        addGiroTransactions();
    }

    @Override
    public String getPDFAuthor()
    {
        return "DKB AG"; //$NON-NLS-1$
    }

    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung \\b(Kauf?.+|Ausgabe.+)\\b");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapier Abrechnung \\b(Kauf?.+|Ausgabe.+)\\b");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });
        block.set(pdfTransaction);
        pdfTransaction.section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{1,})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !(notation.startsWith("St") && notation.endsWith("ck")))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("date", "amount").match("(^Schlusstag)(/-Zeit)? (?<date>\\d+.\\d+.\\d{4}+) (.*)")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Verkauf|Rücknahme Investmentfonds)");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapier Abrechnung (Verkauf|Rücknahme Investmentfonds).*");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });
        block.set(pdfTransaction);
        pdfTransaction.section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2,})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !(notation.startsWith("St") && notation.endsWith("ck")))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares(asShares(v.get("shares")) / 100);
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("amount", "currency") //
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<currency>\\w{3}+)(.*)")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("date").optional() //
                        .match("^Den Gegenwert buchen wir mit Valuta (?<date>\\d+.\\d+.\\d{4}+) (.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                        })

                        .section("date").optional() //
                        .match("(^Schlusstag)(/-Zeit)? (?<date>\\d+.\\d+.\\d{4}+) (.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                        })
                        
                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(type, pdfTransaction);
        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addInterestTransaction()
    {
        DocumentType type = new DocumentType("Zinsgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Zinsgutschrift");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        });
        block.set(pdfTransaction);
        pdfTransaction.section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2,})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !(notation.startsWith("St") && notation.endsWith("ck")))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares(asShares(v.get("shares")) / 100);
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("date", "amount")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(.*) (?<currency>\\w{3}+)")
                        .match("(^Den Betrag buchen wir mit Wertstellung) (?<date>\\d+.\\d+.\\d{4}+) zu Gunsten des Kontos (.*)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(type, pdfTransaction);
        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addDividendTransaction()
    {
        DocumentType type = createDocumentType("Dividendengutschrift");
        //DocumentType type = new DocumentType("Dividendengutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Dividendengutschrift|Ertragsgutschrift.*");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        });
        block.set(pdfTransaction);
        pdfTransaction.section("shares", "name", "nameContinued", "isin", "wkn") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(^St\\Dck) (?<shares>[\\d,.]*) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .match("(?<nameContinued>.*)") //
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("date", "amount")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2,})?)(.*) (?<currency>\\w{3}+)")
                        .match("(^Lagerstelle) (.*)")
                        .match("(^Den Betrag buchen wir mit Wertstellung) (?<date>\\d+.\\d+.\\d{4}+) zu Gunsten des Kontos (.*)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(type, pdfTransaction);
        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addInvestmentEarningTransaction()
    {
        addTransaction("Investmenterträge", "Gutschrift von Investmenterträgen", AccountTransaction.Type.DIVIDENDS);
    }

    private void addRemoveTransaction()
    {
        DocumentType type = new DocumentType("ung");
        this.addDocumentTyp(type);

        Block block = new Block("Gesamtkündigung|Teilrückzahlung mit Nennwertänderung"
                        + "|Teilliquidation mit Nennwertreduzierung|Einlösung bei Gesamtfälligkeit");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.getPortfolioTransaction().setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            entry.getAccountTransaction().setType(AccountTransaction.Type.TRANSFER_IN);
            return entry;
        });
        block.set(pdfTransaction);
        pdfTransaction.section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2,})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !(notation.startsWith("St") && notation.endsWith("ck")))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares(asShares(v.get("shares")) / 100);
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setSecurity(getOrCreateSecurity(v));
                            // Merken für evtl. Steuerrückerstattung:
                            type.getCurrentContext().put("isin", v.get("isin"));
                        })

                        .section("date", "amount", "sign")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?[+-]) (?<currency>\\w{3}+)(.*)")
                        .match("(^Den Betrag buchen wir mit Valuta) (?<date>\\d+.\\d+.\\d{4}+) zu (?<sign>Gunsten|Lasten) des Kontos (.*)")
                        .assign((t, v) -> {
                            String sign = v.get("sign");
                            if ("Lasten".equalsIgnoreCase(sign))
                            {
                                t.getPortfolioTransaction().setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                                t.getAccountTransaction().setType(AccountTransaction.Type.TRANSFER_OUT);
                            }
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(type, pdfTransaction);
        addFeesSectionsTransaction(pdfTransaction);
        addTaxReturnBlock(type);
    }

    private void addTransferOutTransaction()
    {
        DocumentType type = new DocumentType("Depotbuchung - Belastung");
        this.addDocumentTyp(type);

        Block block = new Block("Depotbuchung - Belastung");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.TRANSFER_OUT);
                            return entry;
                        })

                        .section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2,})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !(notation.startsWith("St") && notation.endsWith("ck")))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares(asShares(v.get("shares")) / 100);
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("date").match("(^Valuta) (?<date>\\d+.\\d+.\\d{4}+) (.*)").assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(
                                            t.getPortfolioTransaction().getSecurity().getCurrencyCode()));
                        })

                        .wrap(BuySellEntryItem::new));
    }

    private void addParticipationcertificateEarningTransaction()
    {
        addTransaction("Genussschein", "Ausschüttung aus Genussschein", AccountTransaction.Type.DIVIDENDS);
    }

    private void addParticipationcertificateRefundTransaction()
    {
        addTransaction("Gutschrift", "Kapitalrückzahlung", AccountTransaction.Type.DEPOSIT);
    }

    private void addInvestmentPayoutTransaction()
    {
        addTransaction("Investmentfonds", "Ausschüttung Investmentfonds", AccountTransaction.Type.DIVIDENDS);
    }
    
    private void addAdvanceTaxTransaction()
    {
        addTransaction("Vorabpauschale", "Vorabpauschale Investmentfonds", AccountTransaction.Type.TAXES);
    }
    
    private void addTransaction(String documentTypeString, String blockMarkerString,
                    AccountTransaction.Type transactiontype)
    {
        DocumentType type = createDocumentType(documentTypeString);
        this.addDocumentTyp(type);

        Block block = new Block(blockMarkerString);
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(transactiontype);
            return transaction;
        });
        block.set(pdfTransaction);

        pdfTransaction.oneOf(
                        section -> section.attributes("shares", "name", "isin", "wkn", "currency")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(^St\\Dck) (?<shares>[\\d,.]*) (?<name>.*)$") //
                        .match(".*") //
                        .match("(?<isin>[^ ]*) \\((?<wkn>.*)\\)$") //
                        .match("^Ertrag pro St. [\\d,.]* (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));
                        }),

                        section -> section.attributes("shares", "name", "nameContinued", "isin", "wkn")
                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                                        .match("(^St\\Dck) (?<shares>[\\d,.]*) (?<name>.*) (?<isin>[^ ]*) \\((?<wkn>.*)\\)$")
                                        .match("(?<nameContinued>.*)") //
                                        .assign((t, v) -> {
                                            t.setShares(asShares(v.get("shares")));
                                            t.setSecurity(getOrCreateSecurity(v));
                                        }))

                        .section("date", "amount")
                        .match("(^Ausmachender Betrag) (?<amount>[\\d,.]*)(.*) (?<currency>\\w{3}+)")
                        .match("(^Lagerstelle) (.*)")
                        .match("(^Den Betrag buchen wir mit Wertstellung) (?<date>\\d+.\\d+.\\d{4}+) zu (Gunsten|Lasten) des Kontos (.*)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(type, pdfTransaction);
        addFeesSectionsTransaction(pdfTransaction);
    }

    private DocumentType createDocumentType(String documentTypeString)
    {
        DocumentType type = new DocumentType(documentTypeString, (context, lines) -> {
            Pattern pattern = Pattern
                            .compile("Devisenkurs (?<term>\\w{3}+) / (?<base>\\w{3}+) (?<exchangeRate>[\\d,.]*)(.*)");
            for (String line : lines)
            {
                Matcher m = pattern.matcher(line);
                if (m.matches())
                    context.put(EXCHANGE_RATE, m.group("exchangeRate"));
            }
        });
        return type;
    }
    
    private void addBuyTransactionFundsSavingsPlan()
    {   
        final DocumentType type = new DocumentType("Halbjahresabrechnung Sparplan", (context, lines) -> {
            Pattern pSecurity = Pattern.compile("(?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$");
            Pattern pCurrency = Pattern.compile("^(?<currency>\\w{3}+) in .*$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pSecurity.matcher(line);
                if (m.matches())
                {
                    context.put("isin", m.group("isin"));
                    context.put("wkn", m.group("wkn"));
                    context.put("name", m.group("name"));
                }
                m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group("currency"));
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        
        Block blockTransaction = new Block("Kauf [\\d,]+ .*");
        type.addBlock(blockTransaction);
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });
        blockTransaction.set(pdfTransaction);

        pdfTransaction

                        .section("amount", "shares", "date", "fee").optional()
                        .match("^Kauf (?<amount>[\\d,]+) [\\d]{2,10}\\/.* (?<shares>[\\d,]+) (?<date>\\d+.\\d+.\\d{4}+) .*")
                        .match(".*Provision.* (?<fee>[\\d,]+) .*")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setSecurity(getOrCreateSecurity(context));
                            t.setDate(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));  
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));
                        })
        
                        .section("amount", "shares", "date").optional()
                        .match("^Kauf (?<amount>[\\d,]+) [\\d]{2,10}\\/.* (?<shares>[\\d,]+) (?<date>\\d+.\\d+.\\d{4}+) .*")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setSecurity(getOrCreateSecurity(context));
                            t.setDate(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);


    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(DocumentType documentType, T pdfTransaction)
    {
        pdfTransaction.section("tax", "currency").optional()
                        .match("^Kapitalertragsteuer (.*) (\\w{3}+) (?<tax>[\\d.-]+,\\d+)(-) (?<currency>\\w{3}+)")
                        .assign((t, v) -> addTax(documentType, t, v, "tax"))

                        .section("soli", "currency").optional()
                        .match("^Solidarit(.*) (\\w{3}+) (?<soli>[\\d.-]+,\\d+)(-) (?<currency>\\w{3}+)")
                        .assign((t, v) -> addTax(documentType, t, v, "soli"))

                        .section("kirchenst", "currency").optional()
                        .match("^Kirchensteuer (.*) (\\w{3}+) (?<kirchenst>[\\d.-]+,\\d+)(-) (?<currency>\\w{3}+)")
                        .assign((t, v) -> addTax(documentType, t, v, "kirchenst"))

                        .section("quellensteinbeh", "currency").optional()
                        .match("^Einbehaltene Quellensteuer(.*) (\\w{3}+) (?<quellensteinbeh>[\\d.]+,\\d+)(-) (?<currency>\\w{3}+)")
                        .assign((t, v) ->  {
                            documentType.getCurrentContext().put(FLAG_WITHHOLDING_TAX_FOUND, "true");
                            addTax(documentType, t, v, "quellensteinbeh");
                        })
                        
                        .section("quellenstanr", "currency").optional()
                        .match("^Anrechenbare Quellensteuer(.*) (\\w{3}+) (?<quellenstanr>[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> addTax(documentType, t, v, "quellenstanr"))

                        .section("quellenstrueck", "currency").optional()
                        .match("^(.*)ckforderbare Quellensteuer (?<quellenstrueck>[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> addTax(documentType, t, v, "quellenstrueck"));
    }

    private void addTax(DocumentType documentType, Object t, Map<String, String> v, String taxtype)
    {
        // Wenn es 'Einbehaltene Quellensteuer' gibt, dann die weiteren
        // Quellensteuer-Arten nicht berücksichtigen.
        // Die Berechnung der Gesamt-Quellensteuer anhand der anrechenbaren- und
        // der rückforderbaren Steuer kann ansonsten zu Rundungsfehlern führen.
        if (checkWithholdingTax(documentType, taxtype))
        {

            name.abuchen.portfolio.model.Transaction tx = getTransaction(t);

            String currency = asCurrencyCode(v.get("currency"));
            long amount = asAmount(v.get(taxtype));

            if (!currency.equals(tx.getCurrencyCode()) && documentType.getCurrentContext().containsKey(EXCHANGE_RATE))
            {
                BigDecimal rate = BigDecimal.ONE.divide( //
                                asExchangeRate(documentType.getCurrentContext().get(EXCHANGE_RATE)), 10,
                                RoundingMode.HALF_DOWN);

                currency = tx.getCurrencyCode();
                amount = rate.multiply(BigDecimal.valueOf(amount)).setScale(0, RoundingMode.HALF_DOWN).longValue();
            }

            tx.addUnit(new Unit(Unit.Type.TAX, Money.of(currency, amount)));
        }
    }
    
    private boolean checkWithholdingTax(DocumentType documentType, String taxtype)
    {
        if (Boolean.valueOf(documentType.getCurrentContext().get(FLAG_WITHHOLDING_TAX_FOUND)))
        {
            if ("quellenstanr".equalsIgnoreCase(taxtype) || ("quellenstrueck".equalsIgnoreCase(taxtype)))
            { 
                return false; 
            }
        }
        return true;
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T pdfTransaction)
    {
        pdfTransaction.section("fee", "currency").optional()
                        .match("(^Provision) (?<fee>\\d{1,3}(\\.\\d{3})*(,\\d{2})?[-]) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            getTransaction(t).addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")))));
                        })

                        .section("fee", "currency").optional()
                        .match("(^Transaktionsentgelt Börse) (?<fee>\\d{1,3}(\\.\\d{3})*(,\\d{2})?[-]) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            getTransaction(t).addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")))));
                        })
                        
                        .section("fee", "currency").optional()
                        .match(".*Namensaktien (?<fee>\\d{1,3}(\\.\\d{3})*(,\\d{2})?[-]) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            getTransaction(t).addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")))));
                        })

                        .section("fee", "currency").optional()
                        .match("(^Übertragungs-/Liefergebühr) (?<fee>\\d{1,3}(\\.\\d{3})*(,\\d{2})?[-]) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            getTransaction(t).addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")))));
                        });
    }

    private name.abuchen.portfolio.model.Transaction getTransaction(Object t)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            return ((name.abuchen.portfolio.model.Transaction) t);
        }
        else
        {
            return ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction();
        }
    }

    private void addTaxReturnBlock(DocumentType type)
    {
        // optional: Steuererstattung
        Block block = new Block("^Kapitalertragsteuer (.*) (\\w{3}+) ([\\d.-]+,\\d+)(\\+) (\\w{3}+)(.*)");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAX_REFUND);
                            return entry;
                        })

                        .section("tax", "currency").optional()
                        .match("^Kapitalertragsteuer (.*) (\\w{3}+) (?<tax>[\\d.-]+,\\d+)(\\+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("tax")));
                        }).section("soli", "currency").optional()
                        .match("^Solidarit(.*) (\\w{3}+) (?<soli>[\\d.-]+,\\d+)(\\+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("soli")));
                        }).section("kirchenst", "currency").optional()
                        .match("^Kirchensteuer (.*) (\\w{3}+) (?<kirchenst>[\\d.-]+,\\d+)(\\+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("kirchenst")));
                        })

                        .section("date")
                        .match("(^Den Betrag buchen wir mit Valuta) (?<date>\\d+.\\d+.\\d{4}+) zu Lasten des Kontos (.*)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            v.put("isin", type.getCurrentContext().get("isin"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(TransactionItem::new));
    }
    
    private void addGiroTransactions()
    {
        DocumentType type = new DocumentType("Kontoauszug Nummer", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^(Bu.Tag Wert Wir haben für Sie gebucht Belastung in )(\\w{3})(.*)$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(2));
                }
            }
            Pattern pYear = Pattern.compile("^(Kontoauszug Nummer )(\\d+)( / \\d+ vom )(\\d+.\\d+.)(?<year>\\d+) bis (\\d+.\\d+.\\d+)$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("nr", m.group(2));
                    // Read year
                    context.put("year", m.group(5));
                }
            }
        });
        this.addDocumentTyp(type);

        Block removalblock = new Block("(\\d+.\\d+.) (\\d+.\\d+.) (Überweisung|Dauerauftrag|Kartenzahlung) ([\\d.]+,\\d{2})");
        type.addBlock(removalblock);
        removalblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.REMOVAL);
                            return entry;
                        })

                        .section("day", "month", "value")
                        .match("(\\d+.\\d+.) (?<day>\\d+).(?<month>\\d+). (Überweisung|Dauerauftrag|Kartenzahlung) (?<value>[\\d.]+,\\d{2})")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            // since year is not within the date correction necessary in first receipt of year
                            if (context.get("nr").compareTo("001") == 0  && v.get("month").compareTo("01") == 0)
                            {
                                Integer year = Integer.parseInt(context.get("year")) + 1;
                                t.setDateTime(asDate(v.get("day")+"."+v.get("month")+"."+year.toString()));
                            }
                            else 
                            {
                                t.setDateTime(asDate(v.get("day")+"."+v.get("month")+"."+context.get("year")));                                
                            }
                            t.setAmount(asAmount(v.get("value")));
                            t.setCurrencyCode(context.get("currency"));
                        })

                        .wrap(TransactionItem::new));
        
        Block depositblock = new Block("(\\d+.\\d+.) (\\d+.\\d+.) ((Lohn, Gehalt, Rente)|(Zahlungseingang)) ([\\d.]+,\\d{2})");
        type.addBlock(depositblock);
        depositblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DEPOSIT);
                            return entry;
                        })

                        .section("day", "month", "value")
                        .match("(\\d+.\\d+.) (?<day>\\d+).(?<month>\\d+). ((Lohn, Gehalt, Rente)|(Zahlungseingang)) (?<value>[\\d.]+,\\d{2})")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            if (context.get("nr").compareTo("001") == 0  && v.get("month").compareTo("01") == 0)
                            {
                                Integer year = Integer.parseInt(context.get("year")) + 1;
                                t.setDateTime(asDate(v.get("day")+"."+v.get("month")+"."+year.toString()));
                            }
                            else 
                            {
                                t.setDateTime(asDate(v.get("day")+"."+v.get("month")+"."+context.get("year")));                                
                            }
                            t.setAmount(asAmount(v.get("value")));
                            t.setCurrencyCode(context.get("currency"));
                        })

                        .wrap(TransactionItem::new));
    }

    @Override
    public String getLabel()
    {
        return "DKB"; //$NON-NLS-1$
    }
}
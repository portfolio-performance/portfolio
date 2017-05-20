package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
import java.util.Map;

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
    public DkbPDFExtractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier(""); //$NON-NLS-1$

        addBuyTransaction();
        addBuyTransactionFund();
        addSellTransaction();
        addInterestTransaction();
        addDividendTransaction();
        addRemoveTransaction();
        addTransferOutTransaction();
    }

    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapier Abrechnung Kauf");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });
        block.set(pdfTransaction);
        pdfTransaction
                        .section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
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

                        .section("date", "amount")
                        .match("(^Schlusstag)(/-Zeit)? (?<date>\\d+.\\d+.\\d{4}+) (.*)")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addBuyTransactionFund()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung Ausgabe Investmentfonds");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapier Abrechnung Ausgabe Investmentfonds");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });
        block.set(pdfTransaction);
        pdfTransaction
                        .section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{4})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
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

                        .section("date", "amount")
                        .match("(^Schlusstag)(/-Zeit)? (?<date>\\d+.\\d+.\\d{4}+) (.*)")
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
        DocumentType type = new DocumentType("Wertpapier Abrechnung Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapier Abrechnung Verkauf");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });
        block.set(pdfTransaction);
        pdfTransaction
                        .section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
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
                        .match("(^Schlusstag)(/-Zeit)? (?<date>\\d+.\\d+.\\d{4}+) (.*)")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<currency>\\w{3}+)(.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction);
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
        pdfTransaction
                        .section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
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
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);
        addTaxesSectionsTransaction(pdfTransaction);
        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Dividendengutschrift");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        });
        block.set(pdfTransaction);
        pdfTransaction.section("shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(^St\\Dck) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("date", "amount")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(.*) (?<currency>\\w{3}+)")
                        .match("(^Lagerstelle) (.*)")
                        .match("(^Den Betrag buchen wir mit Wertstellung) (?<date>\\d+.\\d+.\\d{4}+) zu Gunsten des Kontos (.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);
        addTaxesSectionsTransaction(pdfTransaction);
        addFeesSectionsTransaction(pdfTransaction);
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
        pdfTransaction
                        .section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
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
        addTaxesSectionsTransaction(pdfTransaction);
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
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
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

                        .section("date")
                        .match("(^Valuta) (?<date>\\d+.\\d+.\\d{4}+) (.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(t.getPortfolioTransaction().getSecurity()
                                            .getCurrencyCode()));
                        })

                        .wrap(BuySellEntryItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T pdfTransaction)
    {
        pdfTransaction.section("tax").optional()
                        .match("^Kapitalertragsteuer (.*) (\\w{3}+) (?<tax>[\\d.-]+,\\d+)(-) (?<currency>\\w{3}+)")
                        .assign((t, v) -> addTax(t, v, "tax"))

                        .section("soli").optional()
                        .match("^Solidarit(.*) (\\w{3}+) (?<soli>[\\d.-]+,\\d+)(-) (?<currency>\\w{3}+)")
                        .assign((t, v) -> addTax(t, v, "soli"))

                        .section("kirchenst").optional()
                        .match("^Kirchensteuer (.*) (\\w{3}+) (?<kirchenst>[\\d.-]+,\\d+)(-) (?<currency>\\w{3}+)")
                        .assign((t, v) -> addTax(t, v, "kirchenst"))

                        .section("quellenst")
                        .optional()
                        .match("^Anrechenbare Quellensteuer(.*) (\\w{3}+) (?<quellenst>[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> addTax(t, v, "quellenst"));

    }

    private void addTax(Object t, Map<String, String> v, String taxtype)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            ((name.abuchen.portfolio.model.Transaction) t).addUnit(new Unit(Unit.Type.TAX, //
                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get(taxtype)))));
        }
        else
        {
            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, //
                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get(taxtype)))));
        }
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T pdfTransaction)
    {
        pdfTransaction.section("fees")
                        .optional()
                        .match("(^Provision) (?<fees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?[-])(.*)")
                        .assign((t, v) -> getTransaction(t).addUnit(
                                        new Unit(Unit.Type.FEE, Money.of(asCurrencyCode(v.get("currency")),
                                                        asAmount(v.get("fees"))))));
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

                        .section("tax", "currency")
                        .optional()
                        .match("^Kapitalertragsteuer (.*) (\\w{3}+) (?<tax>[\\d.-]+,\\d+)(\\+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("tax")));
                        })
                        .section("soli", "currency")
                        .optional()
                        .match("^Solidarit(.*) (\\w{3}+) (?<soli>[\\d.-]+,\\d+)(\\+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("soli")));
                        })
                        .section("kirchenst", "currency")
                        .optional()
                        .match("^Kirchensteuer (.*) (\\w{3}+) (?<kirchenst>[\\d.-]+,\\d+)(\\+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("kirchenst")));
                        })

                        .section("date")
                        .match("(^Den Betrag buchen wir mit Valuta) (?<date>\\d+.\\d+.\\d{4}+) zu Lasten des Kontos (.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            v.put("isin", type.getCurrentContext().get("isin"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(TransactionItem::new));
    }

    @Override
    public String getLabel()
    {
        return "DKB"; //$NON-NLS-1$
    }
}

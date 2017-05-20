package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
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
public class OnvistaPDFExtractor extends AbstractPDFExtractor
{

    public OnvistaPDFExtractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier(""); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addChangeTransaction();
        addPayingTransaction();
        addDividendTransaction();
        // addBackOfProfitsTransaction();
        addTransferInTransaction();
        addCapitalReductionTransaction();
        addCapitalIncreaseTransaction();
        addAddDididendRightsTransaction();
        addRemoveDididendRightsTransaction();
        addExchangeTransaction();
        addCompensationTransaction();
        addDepositTransaction();
        addAccountStatementTransaction();
    }

    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wir haben für Sie gekauft");
        this.addDocumentTyp(type);

        Block block = new Block("Wir haben für Sie gekauft(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares") //
                        .find("Nominal Kurs") //
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency") //
                        .match("Handelstag (?<date>\\d+.\\d+.\\d{4}+) (.*)")
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Lasten(\\s*)$")
                        // 14.01.2015 172306238 EUR 59,55
                        // Wert Konto-Nr. Betrag zu Ihren Lasten
                        // 01.06.2011 172306238 EUR 6,40
                        .match("(\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new BuySellEntryItem(t));
        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Wir haben für Sie verkauft");
        this.addDocumentTyp(type);

        Block block = new Block("Wir haben für Sie verkauft(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            // Merken für evtl. Steuerrückerstattung:
                            type.getCurrentContext().put("isin", v.get("isin"));
                        })

                        .section("notation", "shares").find("Nominal Kurs")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency")
                        .match("Handelstag (?<date>\\d+.\\d+.\\d{4}+) (.*)")
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Gunsten(\\s*)$")
                        .match("(\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new BuySellEntryItem(t));
        addTaxesSectionsTransaction(pdfTransaction);
        addFeesSectionsTransaction(pdfTransaction);
        addTaxReturnBlock(type);
    }

    private void addChangeTransaction()
    {
        DocumentType type = new DocumentType("Bestätigung");
        this.addDocumentTyp(type);

        Block block = new Block("Bestätigung(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares") //
                        .find("Nominal Kurs")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency") //
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Lasten(\\s*)$")
                        // 14.01.2015 172306238 EUR 59,55
                        // Wert Konto-Nr. Betrag zu Ihren Lasten
                        // 01.06.2011 172306238 EUR 6,40
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }).wrap(t -> new BuySellEntryItem(t));
        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addPayingTransaction()
    {
        DocumentType type = new DocumentType("Gutschriftsanzeige");
        this.addDocumentTyp(type);

        Block block = new Block("Gutschriftsanzeige(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung (.*) ISIN") //
                        .match("(?<name>.*) (.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares") //
                        .find("Nominal Einlösung(.*)$") //
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency") //
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Gunsten$")
                        // 17.11.2014 172306238 EUR 51,85
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new BuySellEntryItem(t));
        addTaxesSectionsTransaction(pdfTransaction);
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Erträgnisgutschrift");
        this.addDocumentTyp(type);

        // Erträgnisgutschrift allein ist nicht gut hier, da es schon in der
        // Kopfzeile steht..
        Block block = new Block("Dividendengutschrift.*|Kupongutschrift.*|Erträgnisgutschrift.*(\\d+.\\d+.\\d{4})");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        });

        block.set(pdfTransaction);
        pdfTransaction

                        .section("name", "isin") //
                        .find("Gattungsbezeichnung(.*) ISIN")
                        // Commerzbank AG Inhaber-Aktien o.N. DE000CBK1001
                        // 5,5% TUI AG Wandelanl.v.2009(2014) 17.11.2014
                        // 17.11.2010 DE000TUAG117
                        .match("(?<name>.*?) (\\d+.\\d+.\\d{4} ){0,2}(?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares", "date", "amount", "currency")
                        // .find("Nominal (Ex-Tag )?Zahltag (.*etrag pro
                        // .*)?(Zinssatz.*)?")
                        // STK 25,000 17.05.2013 17.05.2013 EUR 0,700000
                        // Leistungen aus dem steuerlichen Einlagenkonto (§27
                        // KStG) EUR 17,50
                        .match("(?<notation>^\\w{3}+) (?<shares>[\\d.]+(,\\d*)?) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+)?(.*)")
                        .match("(?<date>\\d+.\\d+.\\d{4}+)?(\\d{6,12})?(.{7,58} )?(?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !"STK".equalsIgnoreCase(notation))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares(asShares(v.get("shares")) / 100);
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction);

        // optional: Reinvestierung in:
        block = new Block("Reinvestierung.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("date")
                        .match("(^\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+)?(.*)")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("name", "isin") //
                        .find("Die Dividende wurde wie folgt in neue Aktien reinvestiert:")
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares", "amount", "currency") //
                        .find("Nominal Reinvestierungspreis")
                        // STK 25,000 EUR 0,700000
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount((asAmount(v.get("amount")) * asAmount(v.get("shares")) / 100));
                        })

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    private void addTransferInTransaction()
    {
        DocumentType type = new DocumentType("Wir erhielten zu Gunsten Ihres Depots");
        this.addDocumentTyp(type);

        Block block = new Block("Wir erhielten zu Gunsten Ihres Depots(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.TRANSFER_IN);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares", "date") //
                        .find("Nominal Schlusstag Wert")
                        // STK 28,000 02.12.2011 02.12.2011
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(
                                            t.getPortfolioTransaction().getSecurity().getCurrencyCode()));
                        })

                        .wrap(t -> new BuySellEntryItem(t));
        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addCapitalReductionTransaction()
    {
        DocumentType type = new DocumentType("Kapitalherabsetzung");
        this.addDocumentTyp(type);

        Block block = new Block("(Aus|Ein)buchung:(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<PortfolioTransaction>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("date")
                        .optional()
                        // STK 55,000 24.04.2013
                        .match("(^\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (?<date>\\d+.\\d+.\\d{4}+)?(.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            type.getCurrentContext().put("date", v.get("date"));
                        })

                        .section("name", "isin")
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("transactiontype")
                        .match("^(?<transactiontype>.*buchung:)(.*)")
                        .assign((t, v) -> {
                            String transactiontype = v.get("transactiontype");
                            if ("Einbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            }
                            else if ("Ausbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            }
                            else
                            {
                                ; // TODO: evtl. Warnung/Hinweis ausgeben?
                            }
                        })

                        // Nominal Ex-Tag
                        // STK 55,000 24.04.2013
                        .section("notation", "shares")
                        .find("Nominal(.*)")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                            if (t.getDate() == null)
                            {
                                t.setDate(asDate(type.getCurrentContext().get("date")));
                            }
                        })

                        .wrap(t -> new TransactionItem(t));
    }

    private void addCapitalIncreaseTransaction()
    {
        DocumentType type = new DocumentType("Kapitalerhöhung");
        this.addDocumentTyp(type);

        Block block = new Block("Kapitalerhöhung(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<PortfolioTransaction>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("date")
                        // Frankfurt am Main, 06.04.2011
                        .match("(.*), (?<date>\\d+.\\d+.\\d{4}+)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                        })

                        .section("name", "isin")
                        .find("Einbuchung:(\\s*)")
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // Nominal
                        // STK 55,000
                        .section("notation", "shares")
                        .find("Nominal(.*)")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                        })

                        .wrap(t -> new TransactionItem(t));
    }

    private void addAddDididendRightsTransaction()
    {
        DocumentType type = new DocumentType("Einbuchung von Rechten für die");
        this.addDocumentTyp(type);

        Block block = new Block("Einbuchung von Rechten für die(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<PortfolioTransaction>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        });

        block.set(pdfTransaction);

        pdfTransaction.section("date")
                        // Frankfurt am Main, 25.05.2016
                        .match("(.*), (?<date>\\d+.\\d+.\\d{4}+)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                        })

                        .section("name", "isin")
                        .find("Einbuchung:(\\s*)")
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares")
                        .find("Nominal(.*)")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                        })

                        .wrap(t -> new TransactionItem(t));
    }

    private void addRemoveDididendRightsTransaction()
    {
        DocumentType type = new DocumentType("Wertlose Ausbuchung");
        this.addDocumentTyp(type);

        Block block = new Block("Wertlose Ausbuchung(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<PortfolioTransaction>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            return entry;
        });

        block.set(pdfTransaction);

        pdfTransaction
                        .section("name", "isin")
                        .find("Ausbuchung:(\\s*)")
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares", "date")
                        .find("Nominal Ex-Tag")
                        // STK 25,000 21.06.2016
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (?<date>\\d+.\\d+.\\d{4}+)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                            t.setDate(asDate(v.get("date")));
                        })

                        .wrap(t -> new TransactionItem(t));
    }

    private void addExchangeTransaction()
    {
        DocumentType type = new DocumentType("Umtausch");
        this.addDocumentTyp(type);

        Block block = new Block("(Aus|Ein)buchung:(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<PortfolioTransaction>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            return entry;
        });

        block.set(pdfTransaction);

        pdfTransaction.section("name", "isin")
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            // Merken für evtl. Steuerrückerstattung:
                            type.getCurrentContext().put("isin", v.get("isin"));
                        })

                        .section("transactiontype")
                        .match("^(?<transactiontype>.*buchung:)(.*)")
                        .assign((t, v) -> {
                            String transactiontype = v.get("transactiontype");
                            if ("Einbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            }
                            else if ("Ausbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            }
                            else
                            {
                                ; // TODO: evtl. Warnung/Hinweis ausgeben?
                            }
                        })

                        .section("date")
                        .find("(.*)(Schlusstag|Ex-Tag|Wert Konto-Nr.*)")
                        .match("(.*)(^|\\s+)(?<date>\\d+.\\d+.\\d{4}+)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                        })

                        .section("currency", "amount")
                        .optional()
                        // Wert Betrag zu Ihren Gunsten
                        // EUR 0,00
                        // oder
                        // Wert Konto-Nr. Betrag zu Ihren Lasten
                        // 23.11.2015 172306238 EUR 12,86
                        .find("(.*)(Schlusstag|Ex-Tag|Wert Konto-Nr.*|Wert Betrag zu Ihren.*)")
                        .match("(^|\\s+)(\\d+\\.\\d+\\.\\d{4}+)?(\\s)?(\\d+)?(\\s)?(?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("notation", "shares")
                        .find("Nominal(.*)")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(t.getSecurity()
                                            .getCurrencyCode()));
                        })

                        .wrap(t -> {
                            return new TransactionItem(t);
                        });

        addTaxesSectionsTransaction(pdfTransaction);
        addTaxReturnBlock(type);
        addTaxBlock(type);
    }

    private void addCompensationTransaction()
    {
        DocumentType type = new DocumentType("Abfindung");
        this.addDocumentTyp(type);

        Block block = new Block("Ausbuchung(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin")
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("transactiontype")
                        .match("^(?<transactiontype>.*buchung:)(.*)")
                        .assign((t, v) -> {
                            String transactiontype = v.get("transactiontype");
                            if ("Einbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.getAccountTransaction().setType(AccountTransaction.Type.BUY);
                                t.getPortfolioTransaction().setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            }
                            else if ("Ausbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.getAccountTransaction().setType(AccountTransaction.Type.SELL);
                                t.getPortfolioTransaction().setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            }
                            else
                            {
                                ; // TODO: evtl. Warnung/Hinweis ausgeben?
                            }
                        })

                        .section("notation", "shares", "date")
                        .find("Nominal Ex-Tag")
                        // STK 25,000 11.06.2013
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (?<date>\\d+.\\d+.\\d{4}+)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(t.getPortfolioTransaction().getSecurity()
                                            .getCurrencyCode()));
                        })

                        .section("date", "amount", "currency")
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Gunsten(\\s*)$")
                        // 11.06.2013 172306238 EUR 17,50
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new BuySellEntryItem(t));
        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addDepositTransaction()
    {
        final DocumentType type = new DocumentType("Depotauszug", (context, lines) -> {
            Pattern pDate = Pattern.compile("Depotauszug per (\\d+.\\d+.\\d{4}+)?(.*)");
            Pattern pCurrency = Pattern.compile("(.*)Bewertung in[ ]+(\\w{3}+)");
            // read the current context here
                        for (String line : lines)
                        {
                            Matcher m = pDate.matcher(line);
                            if (m.matches())
                            {
                                context.put("date", m.group(1));
                            }
                            m = pCurrency.matcher(line);
                            if (m.matches())
                            {
                                context.put("currency", m.group(2));
                            }
                        }
                    });
        this.addDocumentTyp(type);

        Block block = new Block("(^\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<PortfolioTransaction>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        });

        block.set(pdfTransaction);
        // Die WP-Bezeichnung muss hier leider über mehrere Zeilen hinweg
        // zusammengesucht werden, da im Depotauszug-PDF-Extrakt leider
        // (zumindest teilweise) Zeilenumbrüche innerhalb des Namens sind... (s.
        // Beispiel-Datei: OnvistaDepotauszug.txt)
        pdfTransaction
                        .section("notation", "shares", "nameP1").optional()
                        // STK 4,000 Porsche Automobil
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (?<nameP1>.*)")
                        .assign((t, v) -> {
                            type.getCurrentContext().put("nameP1", v.get("nameP1"));

                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })
                        .section("nameP3").optional()
                        .find("(^\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (.*)")
                        // Inhaber-Vorzugsakti
                        .match("^(?<nameP3>^.*?)(\\s*)")
                        .assign((t, v) -> {
                                        type.getCurrentContext().put("nameP3", v.get("nameP3"));
                        })
                        .section("nameP2", "isin").optional()
                        // Holding SE DE000PAH0038 Girosammelverwahrung 59,3400
                        .match("(?<nameP2>.* )(?<isin>\\w{12}+) (.*)")
                        .assign((t, v) -> {
                            type.getCurrentContext().put("nameP2", v.get("nameP2"));
                            type.getCurrentContext().put("isin", v.get("isin"));
                        })
                        //
                        .section("nameP4").optional()
                        .find("^(.*) (\\w{12}+) (.*)")
                        // en o.St.o.N
                        .match("^(?<nameP4>^.*\\.*)$")
                        .assign((t, v) -> {
                            type.getCurrentContext().put("nameP4", v.get("nameP4"));
                        })

                        .section("combine")
                        .match("(?<combine>.*)")
                        .assign((t, v) -> {
                            String name = type.getCurrentContext().get("nameP1") + 
                                          type.getCurrentContext().get("nameP2") +
                                          type.getCurrentContext().get("nameP3") + 
                                          type.getCurrentContext().get("nameP4");
                            v.put("isin", type.getCurrentContext().get("isin"));
                            if (name.indexOf(v.get("isin")) > -1)
                            {
                                name = name.substring(0, name.indexOf(v.get("isin")));
                            }
                            if (name.indexOf("STK ") > -1)
                            {
                                name = name.substring(0, name.indexOf("STK "));
                            }
                            // WP-Bezeichnung nachbearbeiten, kann doppelte
                            // Leerzeichen enthalten...
                            name = name.replaceAll("\\s+", " ");
                            // oder auch überflüssige Nullen (00)...
                            name = name.replaceAll("0+%", "%");
                            // oder <Leerzeichen><Punkt> ( .)
                            name = name.replaceAll("\\s+\\.", ".");
                            v.put("name", name);

                            t.setSecurity(getOrCreateSecurity(v));
                            // System.out.println("WP: Stk-T: " + t.getShares()
                            // + " " + t.getSecurity().getIsin() + " "
                            // + name + "\n");
                            if (t.getDate() == null)
                            {
                                t.setDate(asDate(type.getCurrentContext().get("date")));
                            }
                            if (t.getCurrencyCode() == null)
                            {
                                t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                            }
                        })

                        .wrap(t -> new TransactionItem(t));
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG Nr.", (context, lines) -> {
            Pattern pYear = Pattern.compile("KONTOAUSZUG Nr. \\d+ per \\d+.\\d+.(\\d{4}+)?(.*)");
            Pattern pCurrency = Pattern.compile("(.*)Customer Cash Account[ ]+(\\w{3}+)");
            // read the current context here
                        for (String line : lines)
                        {
                            Matcher m = pYear.matcher(line);
                            if (m.matches())
                            {
                                context.put("year", m.group(1));
                            }
                            m = pCurrency.matcher(line);
                            if (m.matches())
                            {
                                context.put("currency", m.group(2));
                            }
                        }
                    });
        this.addDocumentTyp(type);

        // 31.10. 31.10. REF: 000017304356 37,66
        Block block = new Block("^\\d+\\.\\d+\\.\\s+\\d+\\.\\d+\\.\\s+REF:\\s+\\d+\\s+[\\d.-]+,\\d+[+-]?(.*)");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DEPOSIT);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("valuta", "amount", "sign")
                        .match("^\\d+\\.\\d+\\.\\s+(?<valuta>\\d+\\.\\d+\\.)\\s+REF:\\s+\\d+\\s+(?<amount>[\\d.-]+,\\d+)(?<sign>[+-]?)(.*)")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            String date = v.get("valuta");
                            if (date != null)
                            {
                                // create a long date from the year in the
                                // context
                                t.setDate(asDate(date + context.get("year")));
                            }
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            // check for withdrawals
                            String sign = v.get("sign");
                            if ("-".equals(sign))
                            {
                                // change type for withdrawals
                                t.setType(AccountTransaction.Type.REMOVAL);
                            }
                        })

                        // Feintuning Buchungstyp...
                        .section("postingtype")
                        .find("\\d+\\.\\d+\\.\\s+\\d+\\.\\d+\\. REF:(.*)")
                        .match("(?<postingtype>.*?)")
                        .assign((t, v) -> {
                            String postingtype = v.get("postingtype");
                            if (postingtype != null)
                            {
                                switch (postingtype)
                                {
                                    case "Wertpapierkauf":
                                    case "Umtausch/Bezug":
                                    case "Sollbuchung HSBC":
                                        t.setType(AccountTransaction.Type.BUY);
                                        break;
                                    case "Wertpapierverkauf":
                                    case "Spitze Verkauf":
                                    case "Habenbuchung HSBC":
                                    case "Tilgung":
                                        t.setType(AccountTransaction.Type.SELL);
                                        break;
                                    case "Zinsen/Dividenden":
                                        t.setType(AccountTransaction.Type.DIVIDENDS);
                                        break;
                                    case "AbgSt. Optimierung":
                                        t.setType(AccountTransaction.Type.TAX_REFUND);
                                        break;
                                }
                            }
                        })
                        .wrap(t -> {
                            // Buchungen, die bereits durch den Import von
                            // WP-Abrechnungen abgedeckt sind (sein sollten),
                            // hier ausklammern, sonst hat man sie doppelt im
                            // Konto:
                            if (t.getType() != AccountTransaction.Type.DIVIDENDS
                                            && t.getType() != AccountTransaction.Type.BUY
                                            && t.getType() != AccountTransaction.Type.SELL
                                            && t.getType() != AccountTransaction.Type.TAX_REFUND)
                            {
                                return new TransactionItem(t);
                            }
                            return null;
                        });
    }


    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T pdfTransaction)
    {
        pdfTransaction.section("tax", "withheld", "sign")
                        .optional()
                        .match("(?<withheld>\\w+|^)(\\s*)Kapitalertragsteuer(\\s*)(?<currency>\\w{3}+)(\\s+)(?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(?<sign>-|\\s+|$)?")
                        .assign((t, v) -> {
                            if ("-".equalsIgnoreCase(v.get("sign"))
                                            || "einbehaltene".equalsIgnoreCase(v.get("withheld")))
                            {
                                if (t instanceof name.abuchen.portfolio.model.Transaction)
                                {
                                    ((name.abuchen.portfolio.model.Transaction) t)
                                                    .addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                                }
                                else
                                {
                                    ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                    new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")),
                                                                    asAmount(v.get("tax")))));
                                }
                            }
                        })
                        .section("soli", "withheld", "sign")
                        .optional()
                        .match("(?<withheld>\\w+|^)(\\s*)Solidaritätszuschlag(\\s*)(?<currency>\\w{3}+)(\\s+)(?<soli>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(?<sign>-|\\s+|$)?")
                        .assign((t, v) -> {
                            if ("-".equalsIgnoreCase(v.get("sign"))
                                            || "einbehaltener".equalsIgnoreCase(v.get("withheld")))
                            {
                                if (t instanceof name.abuchen.portfolio.model.Transaction)
                                {
                                    ((name.abuchen.portfolio.model.Transaction) t).addUnit(new Unit(
                                                    Unit.Type.TAX,
                                                    Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("soli")))));
                                }
                                else
                                {
                                    ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                    new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("soli")))));
                                }
                            }
                        })
                        .section("kirchenst", "withheld", "sign")
                        .optional()
                        .match("(?<withheld>\\w+|^)(\\s*)Kirchensteuer(\\s*)(?<currency>\\w{3}+)(\\s+)(?<kirchenst>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(?<sign>-|\\s+|$)?")
                        .assign((t, v) -> {
                            if ("-".equalsIgnoreCase(v.get("sign"))
                                            || "einbehaltene".equalsIgnoreCase(v.get("withheld")))
                            {
                                if (t instanceof name.abuchen.portfolio.model.Transaction)
                                {
                                    ((name.abuchen.portfolio.model.Transaction) t).addUnit(new Unit(Unit.Type.TAX,
                                                    Money.of(asCurrencyCode(v.get("currency")),
                                                                    asAmount(v.get("kirchenst")))));
                                }
                                else
                                {
                                    ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                    new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("kirchenst")))));
                                }
                            }
                        });
    }

    private void addFeesSectionsTransaction(Transaction<BuySellEntry> pdfTransaction)
    {
        pdfTransaction.section("brokerage").optional()
                        .match("(^.*)(Orderprovision) (\\w{3}+) (?<brokerage>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("brokerage"))))))
                        .section("stockfees").optional()
                        .match("(^.*) (B\\Drsengeb\\Dhr) (\\w{3}+) (?<stockfees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("stockfees"))))))
                        .section("stockfees2").optional()
                        .match("(^.*) (Handelsplatzgeb\\Dhr) (\\w{3}+) (?<stockfees2>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("stockfees2"))))))
                        .section("agent").optional()
                        .match("(^.*)(Maklercourtage)(\\s+)(\\w{3}+) (?<agent>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("agent"))))));

    }

    private void addTaxReturnBlock(DocumentType type)
    {

        // optional: Steuererstattung
        Block block = new Block("Steuerausgleich nach § 43a Abs. 3 Satz 2 EStG:(.*)");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAX_REFUND);
                            return entry;
                        })

                        .section("tax")
                        .optional()
                        .match("^Kapitalertragsteuer (?<currency>\\w{3}+) (?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("tax")));
                        })
                        .section("soli")
                        .optional()
                        .match("^Solidaritätszuschlag (?<currency>\\w{3}+) (?<soli>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("soli")));
                        })
                        .section("kirchenst")
                        .optional()
                        .match("^Kirchensteuer (?<currency>\\w{3}+) (?<kirchenst>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("kirchenst")));
                        })

                        .section("date", "currency")
                        .find("Wert(\\s+)Konto-Nr.(\\s+)Abrechnungs-Nr.(\\s+)Betrag zu Ihren Gunsten(\\s*)$")
                        // Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren
                        // Gunsten
                        // 06.05.2013 172306238 56072633 EUR 3,05
                        .match("(^|\\s+)(?<date>\\d+\\.\\d+\\.\\d{4}+)(\\s)(\\d+)?(\\s)?(\\d+)?(\\s)(?<currency>\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            v.put("isin", type.getCurrentContext().get("isin"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

    private void addTaxBlock(DocumentType type)
    {

        // optional: Steuer dem Konto buchen
        Block block = new Block("(Kapitalertragsteuer)(.*)-$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAXES);
                            return entry;
                        })

                        .section("tax")
                        .optional()
                        .match("^Kapitalertragsteuer (?<currency>\\w{3}+) (?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("tax")));
                        })
                        .section("soli")
                        .optional()
                        .match("^Solidaritätszuschlag (?<currency>\\w{3}+) (?<soli>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("soli")));
                        })
                        .section("kirchenst")
                        .optional()
                        .match("^Kirchensteuer (?<currency>\\w{3}+) (?<kirchenst>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("kirchenst")));
                        })

                        .section("date", "currency")
                        .optional()
                        .find("Wert(\\s+)Konto-Nr.(\\s+)Betrag zu Ihren Lasten(\\s*)$")
                        // Wert Konto-Nr. Betrag zu Ihren Lasten
                        // 23.11.2015 172306238 EUR 12,86
                        .match("(^|\\s+)(?<date>\\d+\\.\\d+\\.\\d{4}+)(\\s)(\\d+)(\\s)(?<currency>\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            v.put("isin", type.getCurrentContext().get("isin"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "Onvista"; //$NON-NLS-1$
    }
}

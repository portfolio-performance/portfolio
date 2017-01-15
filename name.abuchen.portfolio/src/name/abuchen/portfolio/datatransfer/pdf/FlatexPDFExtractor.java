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

public class FlatexPDFExtractor extends AbstractPDFExtractor
{

    public FlatexPDFExtractor(Client client) throws IOException
    {
        super(client);

        addBuySellTransaction();
        addBuyTransaction();
        addDepositAndWithdrawalTransaction();
        addDividendTransaction();
        addSellTransaction();
        addTransferInTransaction();
        addTransferOutTransaction();
        addRemoveTransaction();
        addRemoveNewFormatTransaction();
        addOverdraftinterestTransaction();
        addTaxoptimisationTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Sammelabrechnung (Wertpapierkauf/-verkauf)");
        this.addDocumentTyp(type);

        Block block = new Block("Nr.(\\d*)/(\\d*)  Kauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })

                        .section("wkn", "isin", "name")
                        .match("Nr.[0-9A-Za-z]*/(\\d*)  Kauf *(?<name>[^(]*) \\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "date")
                        .match("^davon ausgef\\. *: (?<shares>[.\\d]+,\\d*) St\\. *Schlusstag *: *(?<date>\\d+\\.\\d+\\.\\d{4}+), \\d+:\\d+ Uhr")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setDate(asDate(v.get("date")));

                        })

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional() //
                        .match(".* Provision *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(t -> new BuySellEntryItem(t)));

        block = new Block("Nr.(\\d*)/(\\d*)  Verkauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*)  Verkauf *(?<name>[^(]*) \\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "date")
                        .match("^davon ausgef\\. *: (?<shares>[.\\d]+,\\d*) St\\. *Schlusstag *: *(?<date>\\d+.\\d+.\\d{4}+), \\d+:\\d+ Uhr")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setDate(asDate(v.get("date")));

                        })

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional() //
                        .match(".* Provision *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("tax", "currency").optional() //
                        .match(".* \\*\\*Einbeh. Steuer *: *(?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("taxreturn", "currency")
                        .optional()
                        .match(".* \\*\\*Einbeh. Steuer *: *(?<taxreturn>-[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxreturn")));
                        })

                        .wrap(t -> new BuySellEntryItem(t)));
        addTaxReturnBlock(type);
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung Kauf Fonds/Zertifikate");
        this.addDocumentTyp(type);

        Block block = new Block(" *biw AG *");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })

                        .section("date").match(".*Schlusstag *(?<date>\\d+.\\d+.\\d{4}).*") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("wkn", "isin", "name")
                        .match("Nr.[0-9A-Za-z]*/(\\d*)  Kauf *(?<name>[^(]*) \\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares") //
                        .match("^Ausgeführt *(?<shares>[\\.\\d]+(,\\d*)?) *St\\..*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .match(" * Endbetrag *(?<currency>\\w{3}+) *(?<amount>[\\d.-]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional()
                        //
                        .match(".* Provision *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* Eigene Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* \\*Fremde Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDepositAndWithdrawalTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug Nr:", (context, lines) -> {
            Pattern pYear = Pattern.compile("Kontoauszug Nr:[ ]*\\d+/(\\d+).*");
            Pattern pCurrency = Pattern.compile("Kontow.hrung:[ ]+(\\w{3}+)");
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
                    context.put("currency", m.group(1));
                }
            }
        });
        this.addDocumentTyp(type);

        // deposit, add value to account
        // 01.01. 01.01. xyz 123,45+
        Block block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+.berweisung[ ]+[\\d.-]+,\\d+[+-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DEPOSIT);
            return t;
        })

        .section("valuta", "amount", "sign")
                        .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+.berweisung[ ]+(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                        .assign((t, v) -> {
                                Map<String, String> context = type.getCurrentContext();
                                String date = v.get("valuta");
                                if (date != null)
                                {
                                    // create a long date from the year in the
                                    // context
                                    t.setDate(asDate(date + context.get("year")));
                                }
                                t.setNote(v.get("text"));
                                t.setAmount(asAmount(v.get("amount")));
                                t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                                //check for withdrawals
                                String sign=v.get("sign");
                                if("-".equals(sign))
                                {
                                    //change type for withdrawals
                                    t.setType(AccountTransaction.Type.REMOVAL);
                                }
                        }).wrap(t -> new TransactionItem(t)));
        
        // fees for foreign dividends, subtract value from account
        block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+Geb.hr Kapitaltransaktion Ausland[ ]+[\\d.-]+,\\d+[-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })
        .section("valuta", "amount")
        .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+Geb.hr Kapitaltransaktion Ausland[ ]+(?<amount>[\\d.-]+,\\d+)[-]")
        .assign((t, v) -> {
                Map<String, String> context = type.getCurrentContext();
                String date = v.get("valuta");
                if (date != null)
                {
                    // create a long date from the year in the
                    // context
                    t.setDate(asDate(date + context.get("year")));
                }
                t.setNote(v.get("text"));
                t.setAmount(asAmount(v.get("amount")));
                t.setCurrencyCode(asCurrencyCode(context.get("currency")));
        }).wrap(t -> new TransactionItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type1 = new DocumentType("Dividendengutschrift");
        DocumentType type2 = new DocumentType("Ertragsmitteilung");
        DocumentType type3 = new DocumentType("Zinsgutschrift");
        this.addDocumentTyp(type1);
        this.addDocumentTyp(type2);
        this.addDocumentTyp(type3);

        Block block = new Block("Ihre Depotnummer.*");
        type1.addBlock(block);
        type2.addBlock(block);
        type3.addBlock(block);
        block.set(new Transaction<AccountTransaction>()
                        //
                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("wkn", "isin", "name")
                        .match("Nr\\.(\\d*) * (?<name>[^(]*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares") //
                        .match("^St\\.[^:]+: *(?<shares>[\\.\\d]+(,\\d*)?).*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("date") //
                        .match("Valuta * : *(?<date>\\d+.\\d+.\\d{4}+).*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(t -> new TransactionItem(t)));
    }
    
    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block(" *biw AG *");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        })

                        .section("date").match(".*Valuta *(?<date>\\d+.\\d+.\\d{4}).*") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*)  Verkauf *(?<name>[^(]*) \\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "notation")
                        .match("^Ausgeführt *(?<shares>[\\.\\d]+(,\\d*)?) *(?<notation>St\\.|\\w{3}+).*") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("St."))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("amount", "currency")
                        .match(".* Endbetrag(\\s+)(?<currency>\\w{3}+)(\\s+)(?<amount>[\\d.]+,\\d+)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional()
                        //
                        .match(".* Provision *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* Eigene Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* \\*Fremde Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }
    
    @SuppressWarnings("nls")
    private void addTransferInTransaction()
    {
        DocumentType type = new DocumentType("Depoteingang");
        this.addDocumentTyp(type);

        Block block = new Block(" *biw AG *");
        type.addBlock(block);
        block.set(new Transaction<PortfolioTransaction>().subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        })
                        .section("date").match("Datum(\\s*):(\\s+)(?<date>\\d+.\\d+.\\d{4})") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("isin", "name")
                        .match("Depoteingang *(?<name>[^(]*) \\((?<isin>[^/]*)\\)") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "notation")
                        .match("^Stk\\.\\/Nominale(\\s*):(\\s+)(?<shares>[\\.\\d]+(,\\d*)?) *(?<notation>St\\.|\\w{3}+)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("Stk"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("rate", "currency")
                        .match("^Kurs(\\s*):(\\s+)(?<rate>[\\d.]+,\\d+)(\\s+)(?<currency>\\w{3}+)(.*)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount((asAmount(v.get("rate")) * t.getShares())/100/100/100); //TODO/Workaround: Die Abrechnung ist hier komisch, der Kurs wird in der Beispiel Datei in EUR angegeben, müsste aber eigentlich in Prozent sein...
                        })

                        .section("fee", "currency").optional()
                        //
                        .match(".* Provision *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* Eigene Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* \\*Fremde Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(t -> new TransactionItem(t)));
    }

    @SuppressWarnings("nls")
    private void addTransferOutTransaction()
    {
        DocumentType type = new DocumentType("Depotausgang");
        this.addDocumentTyp(type);

        Block block = new Block("Depotausgang(.*)");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        })
                        .section("date").match("Fälligkeitstag(\\s*):(\\s+)(?<date>\\d+.\\d+.\\d{4})(.*)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("isin", "name")
                        .match("Depotausgang *(?<name>[^(]*) \\((?<isin>[^/]*)\\)") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "notation")
                        .match("^Stk\\.\\/Nominale(\\s*):(\\s+)(?<shares>[\\.\\d]+(,\\d*)?) *(?<notation>St\\.|\\w{3}+)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("Stk"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("amount", "currency")
                        .match("(.*)Geldgegenwert\\*\\*(.*)(\\s*):(\\s*)(?<amount>[\\d.]+,\\d+)(\\s+)(?<currency>\\w{3}+)(.*)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional()
                        //
                        .match(".* Provision *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* Eigene Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* \\*Fremde Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))
                                        
                        .section("tax", "currency").optional() //
                        .match("(.*)Einbeh. Steuer(.*):(\\s*)(?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }
    
    @SuppressWarnings("nls")
    private void addRemoveTransaction()
    {
        DocumentType type = new DocumentType("Bestandsausbuchung");
        this.addDocumentTyp(type);

        Block block = new Block("Bestandsausbuchung(.*)");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        })
                        .section("date").match("Fälligkeitstag(\\s*):(\\s+)(?<date>\\d+.\\d+.\\d{4})(.*)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("isin", "name")
                        .match("Bestandsausbuchung *(?<name>[^(]*) \\((?<isin>[^/]*)\\)") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "notation")
                        //Stk./Nominale**: 2.000,000000 Stk       Einbeh. Steuer*:              0,00 EUR
                        .match("^Stk\\.\\/Nominale(.*):(\\s+)(?<shares>[\\.\\d]+(,\\d*)?)(\\s*)(?<notation>\\w{3}+)(.*)[Einbeh]+(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("Stk"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("amount", "currency").optional()
                        .match("(.*)Geldgegenwert\\*\\*(.*)(\\s*):(\\s*)(?<amount>[\\d.]+,\\d+)(\\s+)(?<currency>\\w{3}+)(.*)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .section().optional()
                        .match("(.*)Bestand haben wir wertlos ausgebucht(.*)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(t.getAccountTransaction().getSecurity().getCurrencyCode());
                            t.setAmount(0L);
                            t.getPortfolioTransaction().setType(PortfolioTransaction.Type.TRANSFER_OUT);
                            t.setType(PortfolioTransaction.Type.TRANSFER_OUT);
                        })

                        .section("fee", "currency").optional()
                        //
                        .match(".* Provision *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* Eigene Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* \\*Fremde Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))
                                        
                        .section("tax", "currency").optional() //
                        .match("(.*)Einbeh. Steuer(.*):(\\s*)(?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }
    
    //since ~2015
    @SuppressWarnings("nls")
    private void addRemoveNewFormatTransaction()
    {
        DocumentType type = new DocumentType("Gutschrifts- / Belastungsanzeige");
        this.addDocumentTyp(type);

        Block block = new Block("Kundennummer(.*)");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        })
        
                        //WKN     ISIN          Wertpapierbezeichnung           Anzahl
                        //SG0WRD  DE000SG0WRD3  SG EFF. TURBOL ZS               83,00
                        //Sehr geehrter 
                        .section("wkn", "isin", "name", "shares")
                        //.match("(?s)WKN(\\s+)ISIN(\\s+)Wertpapierbezeichnung(\\s+)Anzahl(.{1})(?<wkn>\\w{6}+)(\\s+)(?<isin>\\w{12}+)(\\s+)(?<name>.*?)(\\s+)(?<shares>[\\.\\d]+(,\\d*)?)(.*)(Sehr geehrte.*)") //
                        .match("(?s)(?<wkn>\\w{6}+)(\\s+)(?<isin>\\w{12}+)(\\s+)(?<name>.*?)(\\s+)(?<shares>[\\.\\d]+(,\\d*)?)(.*)") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })
                        .section("date").match("Fälligkeitstag(\\s*):(\\s+)(?<date>\\d+.\\d+.\\d{4})(.*)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount", "currency").optional()
                        .match("(.*)Geldgegenwert(\\*{1,3})(.*)(\\s*):(\\s*)(?<amount>[\\d.]+,\\d+)(\\s+)(?<currency>\\w{3}+)(.*)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .section().optional()
                        .match("(.*)Bestand haben wir wertlos ausgebucht(.*)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(t.getAccountTransaction().getSecurity().getCurrencyCode());
                            t.setAmount(0L);
                            t.getPortfolioTransaction().setType(PortfolioTransaction.Type.TRANSFER_OUT);
                            t.setType(PortfolioTransaction.Type.TRANSFER_OUT);
                        })

                        .section("fee", "currency").optional()
                        //
                        .match(".* Provision *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* Eigene Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* \\*Fremde Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))
                                        
                        .section("tax", "currency").optional() //
                        .match("(.*)Einbeh. Steuer(.*):(\\s*)(?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addOverdraftinterestTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug Nr:", (context, lines) -> {
            Pattern pYear = Pattern.compile("Kontoauszug Nr:[ ]*\\d+/(\\d+).*");
            Pattern pCurrency = Pattern.compile("Kontow.hrung:[ ]+(\\w{3}+)");
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
                                context.put("currency", m.group(1));
                            }
                        }
                    });
        this.addDocumentTyp(type);

        Block block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+Zinsabschluss[ ]+(.*)");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()
                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.INTEREST_CHARGE);
                            return t;
                        })

                        .section("valuta", "amount")
                        .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+Zinsabschluss[ ]+(\\d+.\\d+.\\d{4})(\\s+)-(\\s+)(\\d+.\\d+.\\d{4})(\\s+)(?<amount>[\\d.-]+,\\d+[+-])")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            String date = v.get("valuta");
                            if (date != null)
                            {
                                // create a long date from the year in the
                                // context
                                t.setDate(asDate(date + context.get("year")));
                            }
                            t.setNote(v.get("text"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                        }).wrap(t -> {
                            if (t.getAmount() != 0) { return new TransactionItem(t); }
                            return null;
                        }));
    }
    
    @SuppressWarnings("nls")
    private void addTaxoptimisationTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug Nr:", (context, lines) -> {
            Pattern pYear = Pattern.compile("Kontoauszug Nr:[ ]*\\d+/(\\d+).*");
            Pattern pCurrency = Pattern.compile("Kontow.hrung:[ ]+(\\w{3}+)");
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
                                context.put("currency", m.group(1));
                            }
                        }
                    });
        this.addDocumentTyp(type);

        Block block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+Steuertopfoptimierung[ ]+(.*)");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()
                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.TAX_REFUND);
                            return t;
                        })

                        .section("valuta", "amount", "sign")
                        .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+Steuertopfoptimierung[ ]+(\\d{4})(\\s+)(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            String date = v.get("valuta");
                            if (date != null)
                            {
                                // create a long date from the year in the
                                // context
                                t.setDate(asDate(date + context.get("year")));
                            }
                            t.setNote(v.get("text"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            String sign = v.get("sign");
                            if ("-".equals(sign))
                            {
                                // change type for payed Taxes
                                t.setType(AccountTransaction.Type.TAXES);
                            }
                        }).wrap(t -> {
                            if (t.getAmount() != 0) { return new TransactionItem(t); }
                            return null;
                        }));
    }

    @SuppressWarnings("nls")
    private void addTaxReturnBlock(DocumentType type)
    {

        // optional: Steuererstattung
        Block block = new Block("Nr.(\\d*)/(\\d*)  Verkauf.*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()
                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAX_REFUND);
                            return entry;
                        })

                        .section("taxreturn")
                        .optional()
                        .match(".* \\*\\*Einbeh. Steuer *: *(?<taxreturn>-[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("taxreturn")));
                        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*)  Verkauf *(?<name>[^(]*) \\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "date")
                        .match("^davon ausgef\\. *: (?<shares>[.\\d]+,\\d*) St\\. *Schlusstag *: *(?<date>\\d+.\\d+.\\d{4}+), \\d+:\\d+ Uhr")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setDate(asDate(v.get("date")));

                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0) { return new TransactionItem(t); }
                            return null;
                        }));
    }

    @Override
    public String getLabel()
    {
        return "flatex"; //$NON-NLS-1$
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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

public class JustTradePDFExtractor extends AbstractPDFExtractor
{
    private static final String FLAG_WITHHOLDING_TAX_FOUND  = "exchangeRate"; //$NON-NLS-1$
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("d LLL yyyy HH:mm:ss", //$NON-NLS-1$
                    Locale.GERMANY);

    public JustTradePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("justTRADE"); //$NON-NLS-1$

        addBuyTransactionOld();
        addBuySellTransaction();
        addDividendeTransaction();
    }

    private String stripTrailing(String value)
    {
        int right = value.length();
        if (value.endsWith(" ")) //$NON-NLS-1$
        {
            value = value.substring(0, right-1);
        }
        if (right == 0)
        {
            return ""; //$NON-NLS-1$
        }
        return value;
    }
    
    @Override
    public String getPDFAuthor()
    {
        return "Sutor Bank"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransactionOld()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung Kauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("Stück .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);
        pdfTransaction

                        .section("shares", "name", "isin", "wkn") //
                        .match("^Stück (?<shares>[\\d.,]+) (?<name>.*) (?<isin>\\S*) \\((?<wkn>\\S*)\\)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date") //
                        .match("Schlusstag\\/\\-Zeit (?<date>.{10}) .*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount", "currency")
                        .match("Kurswert (?<amount>[0-9.]+(\\,[0-9]{2}))- (?<currency>[A-Z]{3})") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {

        // layout of pdf changed
        DocumentType newType = new DocumentType("Transaktionsart: (Kauf|Verkauf)");
        this.addDocumentTyp(newType);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("Produktbezeichnung - .*", "Ausmachender Betrag:.*");
        newType.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);
        pdfTransaction

                        .section("name", "isin", "currency") //
                        .match("Produktbezeichnung - (?<name>.*)").match(".*")
                        .match("Internationale Wertpapierkennnummer \\(ISIN\\): (?<isin>\\S*)")
                        .match("Währung: (?<currency>[A-Z]{3})").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            newType.getCurrentContext().put("currency", v.get("currency"));
                        })

                        .section("date", "time") // length of date is 10 or 11, example: "2 Jun 2019" or "21 Jun 2019"
                        .match("Orderausführung Datum\\/Zeit: (?<date>.{11}).*(?<time>.{8}).*").assign((t, v) -> {
                            // TODO --> Work around DateTimeFormatter in Local.GERMAN looks like "Mär" not "Mrz" and in Local.ENGLISH like "Mar". 
                            // if length of date is 11, we need to strip the trailing blank                
                            LocalDateTime dateTime = LocalDateTime.parse(
                                            String.format("%s %s", stripTrailing(v.get("date").replace("Mrz", "Mär")), v.get("time")), DATE_TIME_FORMAT);
                            t.setDate(dateTime);
                        })
                        
                        // if type is "Verkauf" change from BUY to SELL
                        .section("type") //
                        .match("Transaktionsart: (?<type>\\w*)") //
                        .assign((t, v) -> {
                            if (v.get("type").equals("Verkauf")) 
                            {
                                t.setType(PortfolioTransaction.Type.SELL);
                            }
                        })
                        
                        .section("shares") //
                        .match("Stück\\/Nominale: (?<shares>[0-9.]+(\\,[0-9]{2})).*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount").match("Ausmachender Betrag: (\\W{1})(?<amount>([0-9.]+)\\,([0-9]{2}))") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(newType.getCurrentContext().get("currency")));
                        })

                        .section("tax").optional() //
                        .match("Kapitalertragssteuer: (\\W{1})(?<tax>([0-9.]+)\\,([0-9]{2}))") //
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                            .addUnit(new Unit(Unit.Type.TAX, Money.of(
                                                            asCurrencyCode(newType.getCurrentContext().get("currency")),
                                                            asAmount(v.get("tax")))));
                        })

                        .section("tax").optional() //
                        .match("Solidaritätszuschlag: (\\W{1})(?<tax>([0-9.]+)\\,([0-9]{2}))") //
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                            .addUnit(new Unit(Unit.Type.TAX, Money.of(
                                                            asCurrencyCode(newType.getCurrentContext().get("currency")),
                                                            asAmount(v.get("tax")))));
                        })

                        .section("tax").optional() //
                        .match("Kirchensteuer: (\\W{1})(?<tax>([0-9.]+)\\,([0-9]{2}))") //
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                            .addUnit(new Unit(Unit.Type.TAX, Money.of(
                                                            asCurrencyCode(newType.getCurrentContext().get("currency")),
                                                            asAmount(v.get("tax")))));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    @SuppressWarnings("nls")
    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividende [^pro]).*");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction

                // Produktbezeichnung - Kellogg Co.:
                // Internationale Wertpapierkennnummer US4878361082
                .section("name", "isin")
                .match("^Produktbezeichnung - (?<name>.*):")
                .match("^Internationale Wertpapierkennnummer (?<isin>.*)")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Anzahl/Nominale 30,00
                .section("shares")
                .match("^Anzahl\\/Nominale (?<shares>[\\d.]+,\\d+)")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                })

                // Ex Datum - Tag 01. März 2021
                .section("date")
                .match("^Ex Datum - Tag (?<date>\\d+. .* \\d{4})")
                .assign((t, v) -> {
                    // Formate the date from 01. März 2021 to 01.03.2021
                    v.put("date", DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.parse(v.get("date"), DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY))));
                    t.setDateTime(asDate(v.get("date")));
                })

                // Ausmachender Betrag EUR 12,15
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Einbehaltende Quellensteuer EUR 2,14
                .section("quellensteinbeh", "currency").optional()
                .match("^Einbehaltende Quellensteuer (?<currency>\\w{3}) (?<quellensteinbeh>[.,\\d]+)$")
                .assign((t, v) ->  {
                    type.getCurrentContext().put(FLAG_WITHHOLDING_TAX_FOUND, "true");
                    addTax(type, t, v, "quellensteinbeh");
                })

                // Anrechenbare Quellensteuer EUR 2,14
                .section("quellenstanr", "currency").optional()
                .match("^Anrechenbare Quellensteuer (?<currency>\\w{3}) (?<quellenstanr>[.,\\d]+)$")
                .assign((t, v) -> addTax(type, t, v, "quellenstanr"));
    }

    @SuppressWarnings("nls")
    private void addTax(DocumentType type, Object t, Map<String, String> v, String taxtype)
    {
        // Wenn es 'Einbehaltene Quellensteuer' gibt, dann die weiteren
        // Quellensteuer-Arten nicht berücksichtigen.
        if (checkWithholdingTax(type, taxtype))
        {
            ((name.abuchen.portfolio.model.Transaction) t)
                    .addUnit(new Unit(Unit.Type.TAX, 
                                    Money.of(asCurrencyCode(v.get("currency")), 
                                                    asAmount(v.get(taxtype)))));
        }
    }

    @SuppressWarnings("nls")
    private boolean checkWithholdingTax(DocumentType type, String taxtype)
    {
        if (Boolean.valueOf(type.getCurrentContext().get(FLAG_WITHHOLDING_TAX_FOUND)))
        {
            if ("quellenstanr".equalsIgnoreCase(taxtype))
            { 
                return false; 
            }
        }
        return true;
    }

    @Override
    public String getLabel()
    {
        return "Sutor justTRADE"; //$NON-NLS-1$
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class JustTradePDFExtractor extends AbstractPDFExtractor
{

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("d LLL yyyy HH:mm:ss", //$NON-NLS-1$
                    Locale.GERMANY);

    public JustTradePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("justTRADE"); //$NON-NLS-1$

        addBuyTransactionOld();
        addBuyTransaction();
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
    private void addBuyTransaction()
    {

        // layout of pdf changed
        DocumentType newType = new DocumentType("Transaktionsart: Kauf");
        this.addDocumentTyp(newType);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("Produktbezeichnung - .*");
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
                            // if length of date is 11, we need to strip the trailing blank
                            LocalDateTime dateTime = LocalDateTime.parse(
                                            String.format("%s %s", stripTrailing(v.get("date")), v.get("time")), DATE_TIME_FORMAT);
                            t.setDate(dateTime);
                        })

                        .section("shares") //
                        .match("Stück\\/Nominale: (?<shares>[0-9.]+(\\,[0-9]{2})).*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount").match("Ausmachender Betrag: (\\W{1})(?<amount>([0-9.]+)\\,([0-9]{2}))") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(newType.getCurrentContext().get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    @Override
    public String getLabel()
    {
        return "Sutor justTRADE"; //$NON-NLS-1$
    }
}

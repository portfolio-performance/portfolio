package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SimpelPDFExtractor extends AbstractPDFExtractor
{
    public SimpelPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("LU32888126");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Simpel S.A.";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Fondsabrechnung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^ (Kauf|Verkauf)  .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                        // Kauf Standortfonds Österreich 10.00 € 140.59 € 0.071
                        .section("type", "name", "amount", "kurs", "shares", "isin", "date", "shares1")
                        .match("^ (?<type>(Kauf|Verkauf))  (?<name>.*) (?<amount>[\\-\\.,\\d]+) €  (?<kurs>[\\-\\.,\\d]+) €  (?<shares>[\\d.,]+)$")
                        .match("^(?<isin>[\\w]{12}) (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}) (?<shares1>[\\d.,]+)$")
                        .assign((t, v) -> {
                            if (v.get("type").equals("Verkauf"))
                            {
                                t.setType(PortfolioTransaction.Type.SELL);
                            }

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setCurrencyCode("EUR");
                            t.setShares(asShares(normalizeAmount(v.get("shares"))));

                            t.setAmount(asAmount(normalizeAmount(v.get("amount"))));

                            t.setDate(asDate(v.get("date")));
                        })

                        // Auszahlungsbetrag
                        .section("amount").optional() //
                        .match("^Auszahlungsbetrag:  (?<amount>[\\-\\.,\\d]+) €") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(normalizeAmount(v.get("amount"))));
                        })

                        // Steuern
                        .section("tax").optional() //
                        .match("^abgef.hrte Kapitalertragssteuer: (?<tax>[\\-\\.,\\d]+) €") //
                        .assign((t, v) -> {
                            Money tax = Money.of("EUR", asAmount(normalizeAmount(v.get("tax"))));
                            PDFExtractorUtils.checkAndSetTax(tax, t.getPortfolioTransaction(), type);
                        })

                        // Auftragsnummer
                        .section("ordernum") //
                        .match("^Auftrags-Nummer: (?<ordernum>[\\d]+)$") //
                        .assign((t, v) -> {
                            t.setNote("Auftrags-Nummer: " + v.get("ordernum"));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Aussch.ttungsanzeige");
        this.addDocumentTyp(type);

        Block block = new Block("^.*Aussch.ttungsanzeige$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });
        block.set(pdfTransaction);

        pdfTransaction
                        // Fondsname: Standortfonds Deutschland Datum des
                        // Ertrags: 21.12.2021
                        // WKN / ISIN: AT0000A1Z882 Turnus: jährlich
                        .section("name", "date", "isin")
                        .match("^Fondsname: (?<name>.*) Datum des Ertrags: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})$")
                        .match("^WKN / ISIN: (?<isin>[\\w]{12}).*$").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setCurrencyCode("EUR");
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .section("amountPshare", "amount", "taxPshare", "tax", "total")
                        .match("^Aussch.ttung je Anteil: (?<amountPshare>[\\-\\.,\\d]+)$")
                        .match("^Aussch.ttung gesamt: (?<amount>[\\-\\.,\\d]+)$").match("^Abgeführte Steuern:$")
                        .match("^Kapitalertragssteuer .KESt. je Anteil: (?<taxPshare>[\\-\\.,\\d]+)$")
                        .match("^Kapitalertragssteuer .KESt. gesamt: (?<tax>[\\-\\.,\\d]+)$")
                        .match("^Zur Wiederveranlagung zur Verf.gung stehend: (?<total>[\\-\\.,\\d]+)$")
                        .assign((t, v) -> {
                            BigDecimal amountPerShare = asExchangeRate(normalizeAmount(v.get("amountPshare")));
                            BigDecimal amountTotal = asExchangeRate(normalizeAmount(v.get("amount")));

                            int sharesPrecision = Values.Share.precision() * 2;
                            BigDecimal sharesAmount = amountTotal.divide(amountPerShare, sharesPrecision,
                                            RoundingMode.HALF_UP);

                            t.setShares(asShares(normalizeAmount(sharesAmount.toPlainString())));

                            Money tax = Money.of("EUR", asAmount(normalizeAmount(v.get("tax"))));
                            PDFExtractorUtils.checkAndSetTax(tax, t, type);

                            t.setAmount(asAmount(normalizeAmount(v.get("total"))));
                        })

                        .wrap(TransactionItem::new);
    }

    private static String normalizeAmount(String amount)
    {
        return amount.replace(",", "").replace('.', ',');
    }
}

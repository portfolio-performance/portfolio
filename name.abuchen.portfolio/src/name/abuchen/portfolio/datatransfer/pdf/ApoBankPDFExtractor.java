package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class ApoBankPDFExtractor extends AbstractPDFExtractor
{
    public ApoBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("apoBank");

        addBuySellTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "apoBank - Deutsche Apotheker- und Ärztebank eG";
    }

    private void addDividendTransaction()
    {
        final var type = new DocumentType("Bardividende");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Wertpapierbezeichnung.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Wertpapierbezeichnung iShs VII-$ Trsy Bd 3-7yr U.ETF Registered Shs EUR DIS.Hgd
                        // o.N
                        // ISIN IE00BGPP6473
                        // WKN A2PDTT
                        // Verwahrart Wertpapierrechnung
                        // Lagerland Vereinigtes Königreich Grossbritannien und
                        // Nordirland
                        // Nominal/Stück 0,707 ST
                        // Währung EUR
                        // @formatter:on
                        .section("name", "nameContinued", "isin", "wkn", "currency") //
                        .match("^Wertpapierbezeichnung (?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^WKN (?<wkn>[A-Z0-9]{6})$") //
                        .match("^Währung (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Nominal/Stück 0,707 ST
                        // @formatter:on
                        .section("shares") //
                        .match("^Nominal\\/St.ck (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zahlungsdatum 27.08.2025
                        // @formatter:on
                        .section("date") //
                        .match("^Zahlungsdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag EUR 0,04
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<currency>[A-Z]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //
        // @formatter:off
                        // Kapitalertragsteuer EUR -0,02
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kapitalertragsteuer (?<currency>[A-Z]{3}) (\\-)?(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag EUR 0,00
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Solidarit.tszuschlag (?<currency>[A-Z]{3}) (\\-)?(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer EUR 0,00
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kirchensteuer (?<currency>[A-Z]{3}) (\\-)?(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Wertpapierabrechnung");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Transaktion: (Kauf|Verkauf)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Transaktion: Kauf
                        // Transaktion: Verkauf
                        // @formatter:on
                        .section("type") //
                        .match("^Transaktion: (?<type>Kauf|Verkauf)$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);

                        })

                        // @formatter:off
                        // Wertpapierbezeichnung: iShsII-EO Corp Bd ESG U.ETF Registered Shares o.N.
                        // ISIN: IE00BYZTVT56
                        // WKN: A142NT
                        // Kurswert: EUR -847,44
                        // @formatter:on
                        .section("name", "isin", "wkn", "currency") //
                        .match("^Wertpapierbezeichnung: (?<name>.*)$") //
                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^WKN: (?<wkn>[A-Z0-9]{6})$") //
                        .match("^Kurswert: (?<currency>[A-Z]{3}) (\\-)?[\\.,\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Nominal / Stück: 189
                        // @formatter:on
                        .section("shares") //
                        .match("^Nominal \\/ St.ck: (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag: 06.10.2022
                        // @formatter:on
                        .section("date") //
                        .match("^Schlusstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Gutschrift - EUR Privatkonto 3476831 EUR 5,63
                        // Belastung - EUR apoVV SMART Konto 76161273 EUR -847,44
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Gutschrift|Belastung) \\- .*(?<currency>[A-Z]{3}) (\\-)?(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Devisenkurs: EUR/USD 1,18205
                        // Gutschrift - EUR Privatkonto 3476831 EUR 5,63
                        // @formatter:on
                        .section("termCurrency", "baseCurrency", "exchangeRate", "gross").optional() //
                        .match("^Devisenkurs: (?<baseCurrency>[A-Z]{3})\\/(?<termCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^Gutschrift \\- .* [A-Z]{3} (?<gross>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap(BuySellEntryItem::new);
    }
}

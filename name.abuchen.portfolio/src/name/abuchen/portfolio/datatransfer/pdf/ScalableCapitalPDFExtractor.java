package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class ScalableCapitalPDFExtractor extends AbstractPDFExtractor
{
    public ScalableCapitalPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Scalable Capital GmbH");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Scalable Capital GmbH";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("Wertpapierabrechnung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^f.r (Kundenauftrag|Sparplanausf.hrung) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>(Kauf|Verkauf)) .* [\\.,\\d]+ Stk\\. .*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Kauf Vngrd Fds-ESG Dv.As-Pc Al ETF 3,00 Stk. 6,168 EUR 18,50 EUR
                        // Verkauf Scalable MSCI AC World Xtrackers (Acc) 1,00 Stk. 9,585 EUR 9,59 EUR
                        // IE0008T6IUX0
                        // @formatter:on
                        .section("name", "currency", "isin") //
                        .match("^(Kauf|Verkauf) (?<name>.*) [\\.,\\d]+ Stk\\. [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Kauf Vngrd Fds-ESG Dv.As-Pc Al ETF 3,00 Stk. 6,168 EUR 18,50 EUR
                        // Verkauf Scalable MSCI AC World Xtrackers (Acc) 1,00 Stk. 9,585 EUR 9,59 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^(Kauf|Verkauf) .* (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Ausführung 12.12.2024 13:12:51 Geschäft 36581526
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Ausf.hrung (?<date>[\\d]{2}\\.[\\w]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Total 19,49 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Typ LIMIT Order SCALsin78vS5CYz
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* Order (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Ord.-Nr.: " + trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*(Seite|Page) 1 \\/ [\\d]$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Berechtigtes Wertpapier AGNC Investment Corp.
                                        // ISIN US00123Q1040
                                        // 15.01.2025 15.01.2025 Gutschrift 0,12 USD 0,663129 0,08 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^Berechtigtes Wertpapier (?<name>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\d]{2}\\.[\\w]{2}\\.[\\d]{4} [\\d]{2}\\.[\\w]{2}\\.[\\d]{4} Gutschrift [\\.,\\d]+ (?<currency>[\\w]{3}).*$") //)
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Berechtigte Anzahl 0,663129
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Berechtigte Anzahl (?<shares>[\\.,\\d]+).*$") //")
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // 15.01.2025 15.01.2025 Gutschrift 0,12 USD 0,663129 0,08 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\w]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\w]{2}\\.[\\d]{4} Gutschrift [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\.,\\d]+ [\\w]{3}.*$") //"
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Gesamtbetrag 0,07 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Gesamtbetrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 15.01.2025 15.01.2025 Gutschrift 0,12 USD 0,663129 0,08 EUR
                                        // USD / EUR 1,0269
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^[\\d]{2}\\.[\\w]{2}\\.[\\d]{4} [\\d]{2}\\.[\\w]{2}\\.[\\d]{4} Gutschrift [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<gross>[\\.,\\d]+) [\\w]{3}.*$") //")
                                                        .match("^(?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getBaseCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Ausländische Quellensteuer -0,01 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Ausl.ndische Quellensteuer \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Ordergebühren +0,99 EUR
                        // Ordergebühren -0,99 EUR
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Ordergeb.hren [\\-|\\+](?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class PictetCieGruppeSAPDFExtractor extends AbstractPDFExtractor
{
    public PictetCieGruppeSAPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Banque Pictet & Cie SA");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Banque Pictet & Cie SA";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("STOCK EXCHANGE");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Transaction no\\.: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Sale" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(Purchase|Sale)) (\\-)?[\\.'\\d]+ .* [\\w]{3} [\\.'\\d]+$")
                .assign((t, v) -> {
                    if ("Sale".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Is type --> "Sale" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(Purchase|Sale)) [\\w]{3} (\\-)?[\\.'\\d]+ .* [\\.'\\d]+%$")
                .assign((t, v) -> {
                    if ("Sale".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // @formatter:off
                // Purchase 24'728.000 PIMCO GIS-GL.LO.DUR.R/R INS.USD-ACC at USD 11.97
                // ISIN: IE00BHZKQ946    Telekurs ID: 23335253          Value date 25.05.2022Booking date 24.05.2022
                // @formatter:on
                .section("name", "isin", "currency").optional()                .match("^(Purchase|Sale) (\\-)?[\\.'\\d]+ (?<name>.*) at (?<currency>[\\w]{3}) [\\.'\\d]+$")
                .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Purchase GBP 180'000 6.516% MS (SX5E/SPX) 22/23 at 100.00%
                // ISIN: XS2425022303    Telekurs ID: 110605684          Value date 28.03.2022Booking date 25.03.2022
                // @formatter:on
                .section("name", "isin", "currency").optional()
                .match("^(Purchase|Sale) (?<currency>[\\w]{3}) (\\-)?[\\.'\\d]+ (?<name>.*) at [\\.'\\d]+%$")
                .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                .oneOf(
                                // @formatter:off
                                // Purchase 24'728.000 PIMCO GIS-GL.LO.DUR.R/R INS.USD-ACC at USD 11.97
                                // Sale -74.620 AGIF-CHINA A-SHARES PT GBP-ACC. at GBP 1'565.99
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^(Purchase|Sale) (\\-)?(?<shares>[\\.'\\d]+) .* at [\\w]{3} [\\.'\\d]+$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // Purchase GBP 180'000 6.516% MS (SX5E/SPX) 22/23 at 100.00%
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^(Purchase|Sale) [\\w]{3} (\\-)?(?<shares>[\\.'\\d]+) (?<name>.*) at [\\.'\\d]+%$")
                                        .assign((t, v) -> {
                                            // Percentage quotation, workaround for bonds
                                            BigDecimal shares = asExchangeRate(v.get("shares"));
                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                        })
                        )

                // Order date 20.05.2022 at 05:06:32 pm
                .section("date", "time")
                .match("^Order date (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) at (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2} (am|pm))$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                .oneOf(
                                // Net amount USD -296'598.18 OtherCustodian STATE STREET FUND SERVICES
                                // Net amount GBP 116'726.92 Execution price GBP 1'565.99Gross amount GBP 116'854.17
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Net amount (?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.'\\d]+).*$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Net amount OtherGBP -196'855.38 Custodian BNP PARIBAS SECURITIES
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Net amount (Other([\\s]+)?)?(?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.'\\d]+).*$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // Transaction no.: 781327840 | Publication date: 24.05.2022
                .section("note").optional()
                .match("^(?<note>Transaction no\\.: [\\d]+) .*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("SECURITY EVENT");
        this.addDocumentTyp(type);

        Block block = new Block("^Transaction no\\.: .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // UBS(LUX)-MSCI WOR.SOC.RES.A USD-INC 2'420 GeneralTrade date 01.08.2022
                // ISIN: LU0629459743    Telekurs ID: 13042150          Value date 04.08.2022Booking date 04.08.2022
                // 2'304.81 Quantity held 2'420Net amount USD Income per unit USD 0.9524Gross amount USD 2'304.81
                .section("name", "isin", "currency")
                .match("^(?<name>.*) [\\.'\\d]+ GeneralTrade date [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                .match("^.* Income per unit (?<currency>[\\w]{3}) [\\.'\\d]+.*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // UBS(LUX)-MSCI WOR.SOC.RES.A USD-INC 2'420 GeneralTrade date 01.08.2022
                .section("shares")
                .match("^.* (?<shares>[\\.'\\d]+) GeneralTrade date [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // ISIN: LU0629459743    Telekurs ID: 13042150          Value date 04.08.2022Booking date 04.08.2022
                .section("date")
                .match("^.*Booking date (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // 2'304.81 Quantity held 2'420Net amount USD Income per unit USD 0.9524Gross amount USD 2'304.81
                .section("amount", "currency")
                .match("^(?<amount>[\\.'\\d]+) .*Net amount (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Transaction no.: 797075451 | Publication date: 04.08.2022
                .section("note").optional()
                .match("^(?<note>Transaction no\\.: [\\d]+) .*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Swiss stamp duty USD -443.99 Execution date 24.05.2022 at 09:51:27 amExecuted quantity 24'728.000
                .section("currency", "tax").optional()
                .match("^Swiss stamp duty (?<currency>[\\w]{3}) \\-(?<tax>[\\.'\\d]+).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Transaction taxes GBP -1.04
                .section("currency", "tax").optional()
                .match("^Transaction taxes (?<currency>[\\w]{3}) \\-(?<tax>[\\.'\\d]+).*$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Brokerage USD -160.03 Execution price USD 11.97Gross amount USD -295'994.16
                .section("fee", "currency").optional()
                .match("^Brokerage (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Execution fees GBP -95.65 Gross amount GBP -136'647.03
                .section("fee", "currency").optional()
                .match("^Execution fees (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Third party settlement fees GBP -3.20 OtherCustodian EUROCLEAR BANK S.A / N.V.
                .section("fee", "currency").optional()
                .match("^Third party settlement fees (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}

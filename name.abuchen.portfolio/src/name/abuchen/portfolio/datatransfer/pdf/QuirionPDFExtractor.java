package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetTax;

import java.text.MessageFormat;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class QuirionPDFExtractor extends AbstractPDFExtractor
{
    public QuirionPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Quirin Privatbank AG"); //$NON-NLS-1$

        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Quirin Privatbank AG"; //$NON-NLS-1$
    }

    private void addDepotStatementTransaction()
    {
        DocumentType type = new DocumentType("Kontoauszug");
        this.addDocumentTyp(type);

        Block buySellBlock = new Block("^Wertpapier (Kauf|Verkauf), Ref.: [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?[\\.,\\d]+ [\\w]{3}$");
        type.addBlock(buySellBlock);
        buySellBlock.setMaxSize(4);
        buySellBlock.set(new Transaction<BuySellEntry>()

                .subject(() -> {
                    BuySellEntry entry = new BuySellEntry();
                    entry.setType(PortfolioTransaction.Type.BUY);
                    return entry;
                })

                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Wertpapier (?<type>(Kauf|Verkauf)), Ref\\.: [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Wertpapier Kauf, Ref.: 133305911 03.06.2020 05.06.2020 -408,26 EUR
                // AIS-Amundi MSCI EMERG.MARKETS Namens-Anteile C Cap.EUR
                // o.N.
                // LU1681045370, ST 102,054
                .section("name", "nameContinued", "isin", "currency").optional()
                .match("^Wertpapier (Kauf|Verkauf), Ref\\.: [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .match("^(?<name>.*)$")
                .match("^(?<nameContinued>.*)$")
                .match("^(?<isin>[\\w]{12}), ST (\\-)?[\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Wertpapier Kauf, Ref.: 174680182 10.12.2020 14.12.2020 -428,06 EUR
                // Xtr.(IE)MSCI World Value Registered Shares 1C USD o.N.
                // IE00BL25JM42, ST 16,091
                .section("name", "isin", "currency").optional()
                .match("^Wertpapier (Kauf|Verkauf), Ref\\.: [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .match("^(?<name>.*)$")
                .match("^(?<isin>[\\w]{12}), ST (\\-)?[\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // IE00BCBJG560, ST 3,648
                // IE00B42THM37, ST -10,763
                .section("shares")
                .match("^[\\w]{12}, ST (\\-)?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Wertpapier Kauf, Ref.: 133305911 03.06.2020 05.06.2020 -408,26 EUR
                .section("date")
                .match("^Wertpapier (Kauf|Verkauf), Ref\\.: [\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Wertpapier Kauf, Ref.: 133305911 03.06.2020 05.06.2020 -408,26 EUR
                .section("amount", "currency").optional()
                .match("^Wertpapier (Kauf|Verkauf), Ref\\.: [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(BuySellEntryItem::new));

        Block dividendeBlock = new Block("^(Ertr.gnisabrechnung|Thesaurierung), Ref.: [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?[\\.,\\d]+ [\\w]{3}$");
        type.addBlock(dividendeBlock);
        dividendeBlock.setMaxSize(5);
        dividendeBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.DIVIDENDS);

                   // If we have multiple entries in the document,
                   // then the "taxCurrency" flag must be removed.
                    type.getCurrentContext().remove("taxCurrency");

                    return entry;
                })

                // Is type --> "Thesaurierung" change from DIVIDENDS to TAXES
                .section("type").optional()
                .match("^(?<type>(Ertr.gnisabrechnung|Thesaurierung)), Ref\\.: [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Thesaurierung"))
                    {
                        t.setType(AccountTransaction.Type.TAXES);
                    }
                })

                // Thesaurierung, Ref.: 111727648 25.02.2020 03.01.2020 -0,49 EUR
                // AIS-Amundi MSCI EMERG.MARKETS Namens-Anteile C Cap.EUR
                // o.N.
                // LU1681045370, ST 449,231
                .section("name", "nameContinued", "isin", "currency").optional()
                .match("^(Ertr.gnisabrechnung|Thesaurierung), Ref\\.: [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .match("^(?<name>.*)$")
                .match("^(?<nameContinued>.*)$")
                .match("^(?<isin>[\\w]{12}), ST [\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Erträgnisabrechnung, Ref.: 108905624 30.01.2020 29.01.2020 5,35 EUR
                // iShsIII-EO Corp Bd 1-5yr U.ETF Registered Shares o.N.
                // IE00B4L60045, ST 21,296
                .section("name", "isin", "currency")
                .match("^(Ertr.gnisabrechnung|Thesaurierung), Ref\\.: [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .match("^(?<name>.*)$")
                .match("^(?<isin>[\\w]{12}), ST [\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // IE00B4L60045, ST 21,296
                .section("shares")
                .match("^[\\w]{12}, ST (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Erträgnisabrechnung, Ref.: 108905624 30.01.2020 29.01.2020 5,35 EUR
                // Thesaurierung, Ref.: 111353113 25.02.2020 02.01.2020 -0,40 EUR
                .section("date")
                .match("^(Ertr.gnisabrechnung|Thesaurierung), Ref\\.: [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (\\-)?[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Erträgnisabrechnung, Ref.: 108905624 30.01.2020 29.01.2020 5,35 EUR
                .section("amount", "currency").optional()
                .match("^(Ertr.gnisabrechnung|Thesaurierung), Ref\\.: [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // The exchange rate is missing in the documents.
                // If taxes or fees are specified in foreign currency,
                // they cannot be converted.
                // 
                // The transaction is not recorded
                //  
                // Erträgnisabrechnung, Ref.: 119876044 09.04.2020 08.04.2020 0,94 EUR
                // KEST: USD -0,22, SOLI: USD -0,01
                // @formatter:on

                // KEST: EUR -1,81, SOLI: EUR -0,09
                .section("currency", "tax").optional()
                .match("^Ertr.gnisabrechnung, .*$")
                .match("^KEST: (?<currency>[\\w]{3}) \\-(?<tax>[\\.,\\d]+)( .*)?$")
                .assign((t, v) -> {
                    Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                    
                    if (t.getMonetaryAmount().getCurrencyCode().equals(tax.getCurrencyCode()))
                        checkAndSetTax(tax, t, type);
                    else
                    {
                        t.setNote(MessageFormat.format(Messages.MsgNoExchangeRateAvailableForConversionTaxFee, tax.getCurrencyCode(),
                                        t.getMonetaryAmount().getCurrencyCode()));

                        type.getCurrentContext().put("taxCurrency", v.get("currency"));   
                    }
                })

                // KEST: EUR -1,81, SOLI: EUR -0,09
                .section("currency", "tax").optional()
                .match("^Ertr.gnisabrechnung, .*$")
                .match("^.*, SOLI: (?<currency>[\\w]{3}) \\-(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                    
                    if (t.getMonetaryAmount().getCurrencyCode().equals(tax.getCurrencyCode()))
                        checkAndSetTax(tax, t, type);
                    else
                    {
                        t.setNote(MessageFormat.format(Messages.MsgNoExchangeRateAvailableForConversionTaxFee, tax.getCurrencyCode(),
                                        t.getMonetaryAmount().getCurrencyCode()));

                        type.getCurrentContext().put("taxCurrency", v.get("currency"));   
                    }
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                    {
                        if (t.getNote() == null || !t.getNote().equals(MessageFormat.format(Messages.MsgNoExchangeRateAvailableForConversionTaxFee, type.getCurrentContext().get("taxCurrency"),
                                        t.getMonetaryAmount().getCurrencyCode())))
                            return new TransactionItem(t);
                        else
                            return new NonImportableItem(MessageFormat.format(Messages.MsgNoExchangeRateAvailableForConversionTaxFee, type.getCurrentContext().get("taxCurrency"),
                                            t.getMonetaryAmount().getCurrencyCode()));
                    }
                    return null;
                }));

        // @formatter:off
        // Kontoübertrag 1197537 28.05.2019 28.05.2019 3.000,00 EUR
        // Sammelgutschrift 19.12.2019 19.12.2019 5.000,00 EUR
        // Überweisungsgutschrift Inland 27.12.2019 27.12.2019 2.000,00 EUR
        // Interne Buchung 31.01.2020 31.01.2020 2,84 EUR
        // @formatter:on
        Block depositBlock = new Block("^(Konto.bertrag [\\d]+|"
                        + "Sammelgutschrift|"
                        + "Interne Buchung|"
                        + ".berweisungsgutschrift Inland) "
                        + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                        + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                        + "[\\.,\\d]+ [\\w]{3}");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                .section("note1", "note2", "date", "amount", "currency")
                .match("^(?<note1>(Konto.bertrag [\\d]+|"
                                + "Sammelgutschrift|"
                                + "Interne Buchung|"
                                + ".berweisungsgutschrift Inland)) "
                                + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                + "(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})")
                .match("^(?<note2>Ref\\.: [\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note1") + " " + v.get("note2"));
                })

                .wrap(t -> new TransactionItem(t)));

        // @formatter:off
        // Rücküberweisung Inland 23.12.2019 19.12.2019 -5.002,84 EUR
        // @formatter:on
        Block removalBlock = new Block("^R.cküberweisung Inland [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\-[\\.,\\d]+ [\\w]{3}$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.REMOVAL);
                    return t;
                })

                .section("note1", "note2", "date", "amount")
                .match("^(?<note1>R.cküberweisung Inland) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                + "\\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^(?<note2>Ref\\.: [\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note1") + " " + v.get("note2"));
                })

                .wrap(t -> new TransactionItem(t)));

        // @formatter:off
        // Steueroptimierung 12.06.2020 12.06.2020 36,82 EUR
        // @formatter:on
        Block taxReturnBlock = new Block("^Steueroptimierung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [\\w]{3}$");
        type.addBlock(taxReturnBlock);
        taxReturnBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                .section("note1", "note2", "date", "amount", "currency")
                .match("^(?<note1>Steueroptimierung) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                + "(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^(?<note2>Ref\\.: [\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note1") + " " + v.get("note2"));
                })

                .wrap(t -> new TransactionItem(t)));

        // @formatter:off
        // Vermögensverwaltungshonorar 31.08.2019 31.08.2019 -5,75 EUR
        // Vermögensverwaltungshonorar 0000000000, 01.09.2019 - 30.09.2019 30.09.2019 30.09.2019 -6,98 EUR
        // @formatter:on
        Block feesBlock = new Block("^Verm.gensverwaltungshonorar( .*)? [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\- )?[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\-[\\.,\\d]+ [\\w]{3}$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.FEES);
                    return t;
                })

                .section("note1", "note2", "date", "amount", "currency")
                .match("^(?<note1>Verm.gensverwaltungshonorar)( .*)? [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\- )?"
                                + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                + "\\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^(?<note2>Ref\\.: [\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note1") + " " + v.get("note2"));
                })

                .wrap(t -> new TransactionItem(t)));
    }
}

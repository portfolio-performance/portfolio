package name.abuchen.portfolio.datatransfer.pdf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Isin;

@SuppressWarnings("nls")
public class KFintechPDFExtractor extends AbstractPDFExtractor
{
    private static final String INR = "INR";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.US);

    public KFintechPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("KFINTECH");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "KFintech";
    }

    private void addAccountStatementTransaction()
    {
        // the ISIN sometimes is wrapped to the next line - therefore pick both
        // fragments and stitch them back together in getOrCreateSecurity if
        // necessary

        var isinRange = new Block("^Folio No: .*") //
                        .asRange(section -> section //
                                        .optional() //
                                        .attributes("isin1", "isin2") //
                                        .match("^.* ISIN: (?<isin1>[A-Z0-9]+).*$") //
                                        .match("(?<isin2>[A-Z0-9]*).*"));

        var nameRange = new Block("^Folio No: .*") //
                        .asRange(section -> section //
                                        .attributes("name") //
                                        .match("^[A-Z0-9]*-(?<name>.*)$"));

        final DocumentType type = new DocumentType("Consolidated Account Statement", isinRange, nameRange);

        this.addDocumentTyp(type);

        addPurchase(type);
        addCreationOfUnits(type);
        addSale(type);
        addSwitchInAndOut(type);
        addDividendPayment(type);
    }

    private void addPurchase(final DocumentType type)
    {
        Block purchase = new Block("^..-...-.... .*Purchase.*");
        type.addBlock(purchase);
        purchase.setMaxSize(10);
        purchase.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .section("date", "amount", "units") //
                        .optional() //
                        .documentRange("isin1", "isin2", "name") //
                        .match("^(?<date>[\\d]{2}-[\\w]{3}-[\\d]{4}) " //
                                        + ".*Purchase.* " //
                                        + "(?<amount>[.,\\d]+) " //
                                        + "(?<units>[.,\\d]+) " //
                                        + "[.,\\d]+ [.,\\d]+$")
                        .assign((t, v) -> {

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(LocalDate.parse(v.get("date"), DATE_FORMAT).atStartOfDay());
                            t.setCurrencyCode(INR);
                            t.setAmount(asAmount(v.get("amount")));
                            t.setShares(asShares(v.get("units")));
                        })

                        .section("date", "tax") //
                        .optional() //
                        .match("^(?<date>[\\d]{2}-[\\w]{3}-[\\d]{4}) " //
                                        + ".*Stamp Duty.* " //
                                        + "(?<tax>[.,\\d]+)$")
                        .assign((t, v) -> {

                            var date = LocalDate.parse(v.get("date"), DATE_FORMAT).atStartOfDay();
                            var tax = asAmount(v.get("tax"));

                            var tx = t.getPortfolioTransaction();

                            if (date.equals(tx.getDateTime()) && tax > 0 && tx.getAmount() > tax)
                            {
                                tx.addUnit(new Unit(Unit.Type.TAX, Money.of(INR, tax)));
                                t.setAmount(tx.getAmount() + tax);
                            }
                        })

                        .wrap(e -> e.getPortfolioTransaction().getSecurity() == null ? null : new BuySellEntryItem(e)));
    }

    private void addCreationOfUnits(final DocumentType type)
    {
        // the creation of units appears to be some kind of a delivery of units
        // to start the opening balance. It does not contain a valuation, only
        // the number units. We create it with 1 INR.

        Block creation = new Block("^..-...-.... .*Creation of units.*");
        type.addBlock(creation);
        creation.setMaxSize(10);
        creation.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .section("date", "units", "note") //
                        .optional() //
                        .documentRange("isin1", "isin2", "name") //
                        .match("^(?<date>[\\d]{2}-[\\w]{3}-[\\d]{4}) " //
                                        + "(?<note>Creation of units.*) " //
                                        + "(?<units>[.,\\d]+) " //
                                        + "[.,\\d]+$")
                        .assign((t, v) -> {

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(LocalDate.parse(v.get("date"), DATE_FORMAT).atStartOfDay());
                            t.setCurrencyCode(INR);
                            t.setAmount(Values.Amount.factorize(1));
                            t.setShares(asShares(v.get("units")));

                            t.setNote(v.get("note"));
                        })

                        .wrap(e -> e.getPortfolioTransaction().getSecurity() == null ? null : new BuySellEntryItem(e)));
    }

    private void addSale(final DocumentType type)
    {
        Block sale = new Block("^..-...-.... .*(Redemption|Payment - Units Extinguished).*");
        type.addBlock(sale);
        sale.setMaxSize(10);
        sale.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.SELL);
                            return portfolioTransaction;
                        })

                        .section("date", "amount", "units", "note") //
                        .optional() //
                        .documentRange("isin1", "isin2", "name") //
                        .match("^(?<date>[\\d]{2}-[\\w]{3}-[\\d]{4}) " //
                                        + "(?<note>(Redemption|Payment - Units Extinguished).*) " //
                                        + "\\((?<amount>[.,\\d]+)\\) " //
                                        + "\\((?<units>[.,\\d]+)\\) " //
                                        + "[.,\\d]+ [.,\\d]+$")
                        .assign((t, v) -> {

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(LocalDate.parse(v.get("date"), DATE_FORMAT).atStartOfDay());
                            t.setCurrencyCode(INR);
                            t.setAmount(asAmount(v.get("amount")));
                            t.setShares(asShares(v.get("units")));

                            t.setNote(v.get("note"));
                        })

                        .wrap(e -> e.getPortfolioTransaction().getSecurity() == null ? null : new BuySellEntryItem(e)));
    }

    private void addSwitchInAndOut(DocumentType type)
    {
        Block switchOut = new Block("^..-...-.... .*(Switch.Out).*");
        type.addBlock(switchOut);
        switchOut.setMaxSize(10);
        switchOut.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.SELL);
                            return portfolioTransaction;
                        })

                        .section("date", "amount", "units", "note") //
                        .optional() //
                        .documentRange("isin1", "isin2", "name") //
                        .match("^(?<date>[\\d]{2}-[\\w]{3}-[\\d]{4}) " //
                                        + "(?<note>.*Switch.Out.*) " //
                                        + "\\((?<amount>[.,\\d]+)\\) " //
                                        + "\\((?<units>[.,\\d]+)\\) " //
                                        + "[.,\\d]+ [.,\\d]+$")
                        .assign((t, v) -> {

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(LocalDate.parse(v.get("date"), DATE_FORMAT).atStartOfDay());
                            t.setCurrencyCode(INR);
                            t.setAmount(asAmount(v.get("amount")));
                            t.setShares(asShares(v.get("units")));

                            t.setNote(v.get("note"));
                        })

                        .section("date", "fee") //
                        .optional() //
                        .match("^(?<date>[\\d]{2}-[\\w]{3}-[\\d]{4}) " //
                                        + ".*STT Paid.* " //
                                        + "(?<fee>[.,\\d]+)$")
                        .assign((t, v) -> {

                            var date = LocalDate.parse(v.get("date"), DATE_FORMAT).atStartOfDay();
                            var tax = asAmount(v.get("fee"));

                            var tx = t.getPortfolioTransaction();

                            if (date.equals(tx.getDateTime()) && tax > 0 && tx.getAmount() > tax)
                            {
                                tx.addUnit(new Unit(Unit.Type.FEE, Money.of(INR, tax)));
                                t.setAmount(tx.getAmount() - tax);
                            }
                        })

                        .wrap(e -> e.getPortfolioTransaction().getSecurity() == null ? null : new BuySellEntryItem(e)));

        Block switchIn = new Block("^..-...-.... .*Switch.In.*");
        type.addBlock(switchIn);
        switchIn.setMaxSize(10);
        switchIn.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .section("date", "amount", "units", "note") //
                        .optional() //
                        .documentRange("isin1", "isin2", "name") //
                        .match("^(?<date>[\\d]{2}-[\\w]{3}-[\\d]{4}) " //
                                        + "(?<note>.*Switch.In.*) " //
                                        + "(?<amount>[.,\\d]+) " //
                                        + "(?<units>[.,\\d]+) " //
                                        + "[.,\\d]+ [.,\\d]+$")
                        .assign((t, v) -> {

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(LocalDate.parse(v.get("date"), DATE_FORMAT).atStartOfDay());
                            t.setCurrencyCode(INR);
                            t.setAmount(asAmount(v.get("amount")));
                            t.setShares(asShares(v.get("units")));

                            t.setNote(v.get("note"));
                        })

                        .section("date", "tax") //
                        .optional() //
                        .match("^(?<date>[\\d]{2}-[\\w]{3}-[\\d]{4}) " //
                                        + ".*Stamp Duty.* " //
                                        + "(?<tax>[.,\\d]+)$")
                        .assign((t, v) -> {

                            var date = LocalDate.parse(v.get("date"), DATE_FORMAT).atStartOfDay();
                            var tax = asAmount(v.get("tax"));

                            var tx = t.getPortfolioTransaction();

                            if (date.equals(tx.getDateTime()) && tax > 0 && tx.getAmount() > tax)
                            {
                                tx.addUnit(new Unit(Unit.Type.TAX, Money.of(INR, tax)));
                                t.setAmount(tx.getAmount() + tax);
                            }
                        })

                        .wrap(e -> e.getPortfolioTransaction().getSecurity() == null ? null : new BuySellEntryItem(e)));

    }

    private void addDividendPayment(final DocumentType type)
    {
        Block dividend = new Block("^..-...-.... .*Payout.*");
        type.addBlock(dividend);
        dividend.setMaxSize(10);
        dividend.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("date", "amount") //
                        .optional() //
                        .documentRange("isin1", "isin2", "name") //
                        .match("^(?<date>[\\d]{2}-[\\w]{3}-[\\d]{4}) " //
                                        + ".*Payout.* " //
                                        + "(?<amount>[.,\\d]+)$")
                        .assign((t, v) -> {

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDateTime(LocalDate.parse(v.get("date"), DATE_FORMAT).atStartOfDay());
                            t.setCurrencyCode(INR);
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("date", "tax") //
                        .optional() //
                        .match("^(?<date>[\\d]{2}-[\\w]{3}-[\\d]{4}) " //
                                        + ".*TDS on Above.* " //
                                        + "(?<tax>[.,\\d]+)$")
                        .assign((t, v) -> {

                            var date = LocalDate.parse(v.get("date"), DATE_FORMAT).atStartOfDay();
                            var tax = asAmount(v.get("tax"));

                            if (date.equals(t.getDateTime()) && tax > 0 && t.getAmount() > tax)
                            {
                                t.addUnit(new Unit(Unit.Type.TAX, Money.of(INR, tax)));
                                t.setAmount(t.getAmount() - tax);
                            }

                        })

                        .wrap(t -> t.getSecurity() == null ? null : new TransactionItem(t)));
    }

    @Override
    protected Security getOrCreateSecurity(Map<String, String> values)
    {
        // because the ISIN could be wrapped on multiple lines, we attempt to
        // stitch them back together

        var isin = values.get("isin1");
        if (Isin.isValid(isin))
        {
            values.put("isin", isin);
        }
        else
        {
            isin += values.get("isin2");
            values.put("isin", isin);
        }

        // remove the optional "- ISIN" from the instrument name

        var name = values.get("name");
        var p = name.indexOf(" - ISIN");
        if (p > 0)
            name = name.substring(0, p);

        p = name.indexOf("Registrar");
        if (p > 0)
            name = name.substring(0, p);

        values.put("name", name);

        // add the default currency

        values.put("currency", INR);

        return super.getOrCreateSecurity(values);
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "IN");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "IN");
    }

}

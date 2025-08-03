package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

/**
 * @formatter:off
 * @implNote Sydbank A/S is a DKK-based financial services company.
 *           The currency is DKK.
 *
 * @implSpec Dividend transactions:
 *           The currency is missing in the dividend statements, so we set it to DKK.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class SydbankASPDFExtractor extends AbstractPDFExtractor
{
    private static final String DKK = "DKK";

    public SydbankASPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Sydbank A/S");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Sydbank A/S";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("Du har den .* \\(UTC: .*\\) solgt:");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Sydbank A\\/S.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "solgt" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Du har den .* \\(UTC: .*\\) (?<type>solgt):.*$") //
                        .assign((t, v) -> {
                            if ("solgt".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // ISIN-kode DK0010068006
                                        // Du har den 02.02.24 kl. 10:53:55 (UTC: 09:53:55.688) solgt: Sparinvest Danske Aktier KL A
                                        // DKK 26.036,89
                                        // I alt
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^ISIN\\-kode (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Du har den .* \\(UTC: .*\\) solgt: (?<name>.*)$") //
                                                        .find("I alt") //
                                                        .match("^(?<currency>[A-Z]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Antal stk Fondskurs Kursværdi
                        // 120 217,3000000 26.076,00
                        // @formatter:on
                        .section("shares") //
                        .find("Antal stk Fondskurs Kursv.rdi") //
                        .match("^(?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Du har den 02.02.24 kl. 10:53:55 (UTC: 09:53:55.688) solgt: Sparinvest Danske Aktier KL A
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Du har den (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) kl. (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) \\(UTC: .*\\) solgt:.*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))))

                        // @formatter:off
                        // I alt
                        // DKK 26.036,89
                        // @formatter:on
                        .section("currency", "amount") //
                        .find("I alt") //
                        .match("^(?<currency>[A-Z]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // FONVPA/Refnr. 1195520/E
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*\\/Refnr\\. (?<note>[\\d]+\\/.*)$") //
                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        var type = new DocumentType("Udbytte\\-meddelelse");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.*Sydbank A\\/S.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 10.02.2025 6010520 Sparinvest Korte Obligationer KL A
                        // ISIN-kode: DK0060105203
                        // @formatter:on
                        .section("name", "isin") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<name>.*)$") //
                        .match("^ISIN\\-kode: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> {
                            v.put("currency", asCurrencyCode(DKK));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Beholdning 209 Stk. Udbytte 104,50
                        // @formatter:on
                        .section("shares") //
                        .match("^Beholdning (?<shares>[\\.,\\d]+) Stk\\. Udbytte [\\.,\\d]+$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Afkast i alt kr. 104,50 indsættes den 07.02.2025 på konto 4375-6141965465.
                        // @formatter:on
                        .section("date") //
                        .match("^Afkast i alt kr\\. [\\.,\\d]+ inds.ttes den (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Afkast i alt kr. 104,50 indsættes den 07.02.2025 på konto 4375-6141965465.
                        // @formatter:on
                        .section("amount") //
                        .match("^Afkast i alt kr\\. (?<amount>[\\.,\\d]+).*") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(DKK));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // DEPUBV/Refnr. 1195520/E
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*\\/Refnr\\. (?<note>[\\d]+\\/.*)$") //
                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kurtage
                        // 0,15 % af 26.076,00 39,11-
                        // (Handelsomkostninger i alt DKK 39,11)
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .find("Kurtage") //
                        .match("^[\\.,\\d]+ % af [\\.,\\d]+ (?<fee>[\\.,\\d]+)\\-$") //
                        .match("^\\(Handelsomkostninger i alt (?<currency>[A-Z]{3}) [\\.,\\d]+\\)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

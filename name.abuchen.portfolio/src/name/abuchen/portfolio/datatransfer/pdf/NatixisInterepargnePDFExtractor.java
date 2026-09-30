package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @implNote Natixis Interépargne manages French employee savings plans (PEE,
 *           PERCOL). The documents do not contain any security identifier
 *           (ISIN, WKN), therefore the securities are matched by name only.
 * @implSpec All amounts are reported in EUR. The currency sign is rendered as
 *           "¤" by the PDF conversion, therefore the currency is fixed.
 */
@SuppressWarnings("nls")
public class NatixisInterepargnePDFExtractor extends AbstractPDFExtractor
{
    private static final String EUR = "EUR";

    public NatixisInterepargnePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Natixis Interépargne");
        addBankIdentifier("interepargne.natixis.com");

        addProfitSharingTransaction();
        addArbitrageTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Natixis Interépargne";
    }

    private void addProfitSharingTransaction()
    {
        // @formatter:off
        // PEE - Plan d'Epargne Entreprise
        // @formatter:on
        var planRange = new Block("^[A-Z]{3,} \\- .*$") //
                        .asRange(section -> section //
                                        .attributes("plan") //
                                        .match("^(?<plan>[A-Z]{3,}) \\- .*$"));

        // @formatter:off
        // Investissement de votre prime d'intéressement au titre de l'exercice 2024
        // @formatter:on
        var yearRange = new Block("^Investissement de votre prime d.int.ressement au titre de l.exercice [\\d]{4}$") //
                        .asRange(section -> section //
                                        .attributes("year") //
                                        .match("^Investissement de votre prime d.int.ressement au titre de l.exercice (?<year>[\\d]{4})$"));

        final var type = new DocumentType("Votre prime d.int.ressement", planRange, yearRange);
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        // @formatter:off
        // Formatting:
        // Placement | Votre versement | Abondement de votre entreprise | Date de valorisation | Valeur de part | Nombre de parts | Date de disponibilité
        // -------------------------------------
        // SELECTION DNCA MIXTE ISR (I) 236,35 104,08 20/03/2025 21,60729 15,2883 01/06/2030
        // Prélèvements sociaux à déduire - 10,09
        // @formatter:on
        var firstRelevantLine = new Block("^.* [\\d\\s]+,[\\d]{2} [\\d\\s]+,[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]+ [\\d]{2}\\/[\\d]{2}\\/[\\d]{4}$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(2);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // SELECTION DNCA MIXTE ISR (I) 236,35 104,08 20/03/2025 21,60729 15,2883 01/06/2030
                        // @formatter:on
                        .section("name") //
                        .match("^(?<name>.*?) [\\d\\s]+,[\\d]{2} [\\d\\s]+,[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]+ [\\d]{2}\\/[\\d]{2}\\/[\\d]{4}$") //
                        .assign((t, v) -> {
                            v.put("currency", asCurrencyCode(EUR));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // SELECTION DNCA MIXTE ISR (I) 236,35 104,08 20/03/2025 21,60729 15,2883 01/06/2030
                        // @formatter:on
                        .section("shares") //
                        .match("^.*? [\\d\\s]+,[\\d]{2} [\\d\\s]+,[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d\\s]+,[\\d]+ (?<shares>[\\d\\s]+,[\\d]+) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // SELECTION DNCA MIXTE ISR (I) 236,35 104,08 20/03/2025 21,60729 15,2883 01/06/2030
                        // @formatter:on
                        .section("date") //
                        .match("^.*? [\\d\\s]+,[\\d]{2} [\\d\\s]+,[\\d]{2} (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]+ [\\d]{2}\\/[\\d]{2}\\/[\\d]{4}$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // The amount is the sum of the employee payment and the
                        // employer contribution (abondement).
                        // @formatter:off
                        // SELECTION DNCA MIXTE ISR (I) 236,35 104,08 20/03/2025 21,60729 15,2883 01/06/2030
                        // @formatter:on
                        .section("payment", "contribution") //
                        .match("^.*? (?<payment>[\\d\\s]+,[\\d]{2}) (?<contribution>[\\d\\s]+,[\\d]{2}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]+ [\\d]{2}\\/[\\d]{2}\\/[\\d]{4}$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(EUR));
                            t.setAmount(asAmount(v.get("payment")) + asAmount(v.get("contribution")));
                        })

                        // @formatter:off
                        // PEE - Plan d'Epargne Entreprise
                        // Investissement de votre prime d'intéressement au titre de l'exercice 2024
                        // SELECTION DNCA MIXTE ISR (I) 236,35 104,08 20/03/2025 21,60729 15,2883 01/06/2030
                        // @formatter:on
                        .section("availability").optional() //
                        .documentRange("plan", "year") //
                        .match("^.*? [\\d\\s]+,[\\d]{2} [\\d\\s]+,[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]+ (?<availability>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4})$") //
                        .assign((t, v) -> {
                            var note = concatenate("Intéressement " + trim(v.get("year")), trim(v.get("plan")), " | ");
                            note = concatenate(note, "Disponibilité " + trim(v.get("availability")), " | ");
                            t.setNote(note);
                        })

                        // @formatter:off
                        // Prélèvements sociaux à déduire - 10,09
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Pr.l.vements sociaux . d.duire \\- (?<tax>[\\d\\s]+,[\\d]{2})$") //
                        .assign((t, v) -> {
                            var tax = Money.of(asCurrencyCode(EUR), asAmount(v.get("tax")));
                            checkAndSetTax(tax, t, type.getCurrentContext());
                        })

                        .wrap(BuySellEntryItem::new);
    }

    private void addArbitrageTransaction()
    {
        // @formatter:off
        // PERCOL - PER Collectif
        // @formatter:on
        var planRange = new Block("^[A-Z]{3,} \\- .*$") //
                        .asRange(section -> section //
                                        .attributes("plan") //
                                        .match("^(?<plan>[A-Z]{3,}) \\- .*$"));

        // @formatter:off
        // DESINVESTISSEMENT
        // INVESTISSEMENT
        // @formatter:on
        var typeRange = new Block("^(DESINVESTISSEMENT|INVESTISSEMENT)$") //
                        .asRange(section -> section //
                                        .attributes("type") //
                                        .match("^(?<type>(DESINVESTISSEMENT|INVESTISSEMENT))$"));

        final var type = new DocumentType("Arbitrage de vos avoirs", typeRange, planRange);
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        // @formatter:off
        // Formatting:
        // Date de valorisation | Valeur de la part | Nombre de parts | Vos avoirs
        // Placement
        // -------------------------------------
        // 25/11/2024 26,26826 9,2267 242,37
        // IMPACT ISR RENDEMENT SOLID I
        // @formatter:on
        var firstRelevantLine = new Block("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]{2}$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(2);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // 25/11/2024 26,26826 9,2267 242,37
                        // IMPACT ISR RENDEMENT SOLID I
                        // @formatter:on
                        .section("name") //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]{2}$") //
                        .match("^(?<name>.*)$") //
                        .assign((t, v) -> {
                            v.put("currency", asCurrencyCode(EUR));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // DESINVESTISSEMENT
                        // 25/11/2024 26,26826 9,2267 242,37
                        // @formatter:on
                        .section("date", "shares", "amount") //
                        .documentRange("type") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\d\\s]+,[\\d]+ (?<shares>[\\d\\s]+,[\\d]+) (?<amount>[\\d\\s]+,[\\d]{2})$") //
                        .assign((t, v) -> {
                            // Is type --> "DESINVESTISSEMENT" change from BUY to SELL
                            if ("DESINVESTISSEMENT".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);

                            t.setDate(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(EUR));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // PERCOL - PER Collectif
                        // 25/11/2024 26,26826 9,2267 242,37
                        // @formatter:on
                        .section("date").optional() //
                        .documentRange("plan") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]{2}$") //
                        .assign((t, v) -> t.setNote(concatenate("Arbitrage", trim(v.get("plan")), " | ")))

                        .wrap(BuySellEntryItem::new);
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "fr", "FR");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "fr", "FR");
    }
}

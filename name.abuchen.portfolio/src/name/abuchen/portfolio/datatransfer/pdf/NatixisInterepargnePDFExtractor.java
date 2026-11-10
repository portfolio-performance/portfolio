package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
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

    /**
     * Valid French amount with space as group separator, e.g. "236,35" or
     * "1 181,76"
     */
    private static final Pattern FRENCH_AMOUNT = Pattern.compile("^[\\d]{1,3}(\\s[\\d]{3})*,[\\d]{2}$");

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
        var firstRelevantLine = new Block("^.* [\\d]{1,3}(\\s[\\d]{3})*,[\\d]{2} [\\d]{1,3}(\\s[\\d]{3})*,[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]+ [\\d]{2}\\/[\\d]{2}\\/[\\d]{4}$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(2);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .oneOf( //
                                        // @formatter:off
                                        // SELECTION DNCA MIXTE ISR (I) 236,35 104,08 20/03/2025 21,60729 15,2883 01/06/2030
                                        // Prélèvements sociaux à déduire - 10,09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("nameAndAmounts", "date", "price", "shares", "tax") //
                                                        .match("^(?<nameAndAmounts>.*) (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<price>[\\d\\s]+,[\\d]+) (?<shares>[\\d\\s]+,[\\d]+) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4}$") //
                                                        .match("^Pr.l.vements sociaux . d.duire \\- (?<tax>[\\d]{1,3}(\\s[\\d]{3})*,[\\d]{2})$") //
                                                        .assign((t, v) -> assignProfitSharing(t, v, type)),
                                        // @formatter:off
                                        // SELECTION DNCA MIXTE ISR (I) 236,35 104,08 20/03/2025 21,60729 15,2883 01/06/2030
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("nameAndAmounts", "date", "price", "shares") //
                                                        .match("^(?<nameAndAmounts>.*) (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<price>[\\d\\s]+,[\\d]+) (?<shares>[\\d\\s]+,[\\d]+) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4}$") //
                                                        .assign((t, v) -> assignProfitSharing(t, v, type)))

                        // @formatter:off
                        // PEE - Plan d'Epargne Entreprise
                        // Investissement de votre prime d'intéressement au titre de l'exercice 2024
                        // SELECTION DNCA MIXTE ISR (I) 236,35 104,08 20/03/2025 21,60729 15,2883 01/06/2030
                        // @formatter:on
                        .section("availability").optional() //
                        .documentRange("plan", "year") //
                        .match("^.* [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d\\s]+,[\\d]+ [\\d\\s]+,[\\d]+ (?<availability>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4})$") //
                        .assign((t, v) -> {
                            var note = concatenate("Intéressement " + trim(v.get("year")), trim(v.get("plan")), " | ");
                            note = concatenate(note, "Disponibilité " + trim(v.get("availability")), " | ");
                            t.setNote(note);
                        })

                        .wrap(BuySellEntryItem::new);
    }

    /**
     * The fund line starts with the fund name, followed by the employee
     * payment and the employer contribution (abondement). As the fund name can
     * end with a number (e.g. "AVENIR RETRAITE 2030" or "S&P 500"), the
     * boundary between name and amounts is ambiguous. Therefore all splits
     * with valid French amounts are evaluated and the one is chosen whose
     * invested amount (payment + contribution - social charges) is closest to
     * the value of the shares (shares x price).
     */
    private void assignProfitSharing(BuySellEntry t, ParsedData v, DocumentType type)
    {
        var shares = asShares(v.get("shares"));
        var price = ExtractorUtils.convertToNumberBigDecimal(v.get("price"), Values.Share, "fr", "FR");
        var tax = v.get("tax") != null ? asAmount(v.get("tax")) : 0L;

        var sharesValue = price.multiply(BigDecimal.valueOf(shares)) //
                        .divide(Values.Share.getBigDecimalFactor()) //
                        .multiply(Values.Amount.getBigDecimalFactor()) //
                        .setScale(0, RoundingMode.HALF_UP).longValue();

        var tokens = v.get("nameAndAmounts").split(" ", -1);

        String name = null;
        var amount = 0L;
        var deviation = Long.MAX_VALUE;

        for (var nameEnd = 1; nameEnd < tokens.length - 1; nameEnd++)
        {
            for (var paymentEnd = nameEnd + 1; paymentEnd < tokens.length; paymentEnd++)
            {
                var payment = String.join(" ", Arrays.copyOfRange(tokens, nameEnd, paymentEnd));
                var contribution = String.join(" ", Arrays.copyOfRange(tokens, paymentEnd, tokens.length));

                if (!FRENCH_AMOUNT.matcher(payment).matches() || !FRENCH_AMOUNT.matcher(contribution).matches())
                    continue;

                var candidateAmount = asAmount(payment) + asAmount(contribution);
                var candidateDeviation = Math.abs(candidateAmount - tax - sharesValue);

                if (candidateDeviation < deviation)
                {
                    name = String.join(" ", Arrays.copyOfRange(tokens, 0, nameEnd));
                    amount = candidateAmount;
                    deviation = candidateDeviation;
                }
            }
        }

        if (name == null)
            throw new IllegalArgumentException("Unable to separate fund name and amounts: " + v.get("nameAndAmounts"));

        v.put("name", trim(name));
        v.put("currency", asCurrencyCode(EUR));

        t.setSecurity(getOrCreateSecurity(v));
        t.setDate(asDate(v.get("date")));
        t.setShares(shares);
        t.setCurrencyCode(asCurrencyCode(EUR));
        t.setAmount(amount);

        if (tax != 0L)
            checkAndSetTax(Money.of(asCurrencyCode(EUR), tax), t, type.getCurrentContext());
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

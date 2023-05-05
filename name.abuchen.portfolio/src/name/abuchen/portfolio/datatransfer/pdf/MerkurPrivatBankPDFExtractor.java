package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.DocumentContext;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class MerkurPrivatBankPDFExtractor extends AbstractPDFExtractor
{
    private static final String isJointAccount = "isJointAccount"; //$NON-NLS-1$

    BiConsumer<DocumentContext, String[]> jointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("Anteilige Berechnungsgrundlage .* \\(50,00 %\\).*"); //$NON-NLS-1$
        Boolean bJointAccount = false;

        for (String line : lines)
        {
            Matcher m = pJointAccount.matcher(line);
            if (m.matches())
            {
                context.put(isJointAccount, Boolean.TRUE.toString());
                bJointAccount = true;
                break;
            }
        }

        if (!bJointAccount)
            context.put(isJointAccount, Boolean.FALSE.toString());
    };

    public MerkurPrivatBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Am Marktplatz 10 · 97762 Hammelburg"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "MERKUR PRIVATBANK KGaA"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf)", jointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        // Handshake for tax refund transaction
        Map<String, String> context = type.getCurrentContext();

        Block firstRelevantLine = new Block("^Wertpapier Abrechnung (Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .section("type").optional()
                .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf)).*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                .section("name", "isin", "wkn", "name1", "currency")
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                .match("^(?<name1>.*)$")
                .match("^Kurswert [\\.,\\d]+(\\-)? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));

                    context.put("name", v.get("name"));
                    context.put("isin", v.get("isin"));
                    context.put("wkn", v.get("wkn"));
                })

                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));

                    // Handshake, if there is a tax refund
                    context.put("shares", v.get("shares"));
                })

                .section("date", "time")
                .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)([\\-|\\+])? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .section("note").optional()
                .match("^(?<note>(Limit|Stoplimit) .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                .section("fee", "currency").optional()
                .match("^Provision [\\.,\\d]+ % .* (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                .section("fee", "currency").optional()
                .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                .section("fee", "currency").optional()
                .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                .section("fee", "currency").optional()
                .match("^.bertragungs-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                .section("fee", "currency").optional()
                .match("^Eigene Spesen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                .section("amount", "percentageFee").optional()
                .match("^Anlage (?<amount>[\\.,\\d]+) (?<percentageFee>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("baseCurrency")));

                    BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                    BigDecimal amount = asBigDecimal(v.get("amount"));

                    if (percentageFee.compareTo(BigDecimal.ZERO) != 0)
                    {
                        // fxFee = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                        BigDecimal fxFee = amount
                                        .divide(percentageFee.divide(BigDecimal.valueOf(100))
                                                        .add(BigDecimal.ONE), Values.MC)
                                        .multiply(percentageFee, Values.MC);

                        Money fee = Money.of(asCurrencyCode(v.get("currency")),
                                        fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                        checkAndSetFee(fee, t, type.getCurrentContext());
                    }
                })

                .section("amount", "percentageFee").optional()
                .match("^[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4} Wiederanlage (?<amount>[\\.,\\d]+) (?<percentageFee>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("baseCurrency")));

                    BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                    BigDecimal amount = asBigDecimal(v.get("amount"));

                    if (percentageFee.compareTo(BigDecimal.ZERO) != 0)
                    {
                        // fxFee = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                        BigDecimal fxFee = amount
                                        .divide(percentageFee.divide(BigDecimal.valueOf(100))
                                                        .add(BigDecimal.ONE), Values.MC)
                                        .multiply(percentageFee, Values.MC);

                        Money fee = Money.of(asCurrencyCode(v.get("currency")),
                                        fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                        checkAndSetFee(fee, t, type.getCurrentContext());
                    }
                });
    }

    private Security getSecurity(Map<String, String> context, Integer startTransactionLine)
    {
        for (String key : context.keySet())
        {
            String[] parts = key.split("_"); //$NON-NLS-1$
            if (parts[0].equalsIgnoreCase("security")) //$NON-NLS-1$
            {
                if (startTransactionLine >= Integer.parseInt(parts[3]) && startTransactionLine <= Integer.parseInt(parts[4]))
                {
                    // returns security name, isin, security currency
                    return new Security(parts[1], context.get(key), parts[2]);
                }
            }
        }
        return null;
    }

    private static class Security
    {
        public Security(String name, String isin, String currency)
        {
            this.name = name;
            this.isin = isin;
            this.currency = currency;
        }

        private String name;
        private String isin;
        private String currency;

        public String getName()
        {
            return name;
        }

        public String getIsin()
        {
            return isin;
        }

        public String getCurrency()
        {
            return currency;
        }
    }
}

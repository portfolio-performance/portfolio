package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class StarMoneyPDFExtractor extends AbstractPDFExtractor
{
    public StarMoneyPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("StarMoney"); //$NON-NLS-1$

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "StarMoney (Sparkasse)"; //$NON-NLS-1$
    }


    private void addAccountStatementTransaction()
    {
        DocumentType type = new DocumentType("Kontoauszug", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^.* Umsatz \\((?<currency>[\\w]{3})\\)$");
            // read the current context here
            for (String line : lines)
            {
                Matcher mCurrency = pCurrency.matcher(line);
                if (mCurrency.matches())
                    context.put("currency", asCurrencyCode(mCurrency.group("currency")));
            }
        });
        this.addDocumentTyp(type);

        // @formatter:off
        // 08.07 08.07.08 Überweisung -103,75
        // 08.07 08.07.08 Zahlungseingang 100,00
        // 30.09 30.09.08 Lohn, Gehalt, Rente 1.414,06
        // 25.09 25.09.08 Lastschrift -21,98
        // 01.09 01.09.15 Basis-Lastschrift -119,00
        // 31.08 31.08.15 Kartenzahlung -54,69
        // 17.12 17.12.12 Geldautomat -750,00
        // @formatter:on
        Block depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2} "
                        + "(Zahlungseingang"
                        + "|Lastschrift"
                        + "|.berweisung"
                        + "|Lohn, Gehalt, Rente"
                        + "|Basis\\-Lastschrift"
                        + "|Kartenzahlung"
                        + "|Geldautomat)"
                        + " ([\\-])?[\\.,\\d]+$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.DEPOSIT);
                    return entry;
                })

                .section("date", "note", "type", "amount").optional()
                .match("^[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) (?<note>.*) (?<type>([\\s\\-]+)?)(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    // Is type is "-" change from DEPOSIT to REMOVAL
                    if ("-".equals(trim(v.get("type"))))
                        t.setType(AccountTransaction.Type.REMOVAL);
 
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(context.get("currency"));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        Block interestBlock = new Block("^Abrechnungszeitraum vom .*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.INTEREST);
                    return entry;
                })

                .oneOf(
                                // @formatter:off
                                // Abrechnungszeitraum vom 01.07.2008 bis 30.09.2008
                                // Zinsen für Konto-/Kreditüberziehungen                                0,15S
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2", "date", "amount", "type")
                                        .match("^(?<note1>Abrechnungszeitraum) .* (?<note2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}))$")
                                        .match("^Zinsen f.r .* (?<amount>[\\.,\\d]+)(?<type>[S|H])$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            // Is type is "S" change from INTEREST to INTEREST_CHARGE
                                            if ("S".equals(v.get("type")))
                                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                                            t.setDateTime(asDate(v.get("date")));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(context.get("currency"));
                                            t.setNote(v.get("note1") + " " + v.get("note2"));
                                        })
                                ,
                                // @formatter:off
                                // Abrechnungszeitraum vom 01.10.2013 bis 31.12.2013
                                // Zinsen für Dispositionskredit                                        0,74-
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2", "date", "amount", "type")
                                        .match("^(?<note1>Abrechnungszeitraum) .* (?<note2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}))$")
                                        .match("^Zinsen f.r .* (?<amount>[\\.,\\d]+)(?<type>[\\-|\\+])$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            // Is type is "S" change from INTEREST to INTEREST_CHARGE
                                            if ("-".equals(v.get("type")))
                                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                                            t.setDateTime(asDate(v.get("date")));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(context.get("currency"));
                                            t.setNote(v.get("note1") + " " + v.get("note2"));
                                        })
                                ,
                                // @formatter:off
                                // Abrechnungszeitraum vom 01.10.2013 bis 31.12.2013
                                // Abrechnung 31.12.2012                                                0,00H
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2", "date", "amount")
                                        .match("^(?<note1>Abrechnungszeitraum) .* (?<note2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}))$")
                                        .match("^Abrechnung .* (?<amount>[\\.,\\d]+)H$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            t.setDateTime(asDate(v.get("date")));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(context.get("currency"));
                                            t.setNote(v.get("note1") + " " + v.get("note2"));
                                        })
                                ,
                                // @formatter:off
                                // Abrechnungszeitraum vom 01.10.2013 bis 31.12.2013
                                // Abrechnung 31.12.2012                                                0,00
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2", "date", "amount")
                                        .match("^(?<note1>Abrechnungszeitraum) .* (?<note2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}))$")
                                        .match("^Abrechnung .* (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            t.setDateTime(asDate(v.get("date")));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(context.get("currency"));
                                            t.setNote(v.get("note1") + " " + v.get("note2"));
                                        })
                        )

                .wrap(t -> {
                    TransactionItem item = new TransactionItem(t);
                    if (t.getCurrencyCode() != null && t.getAmount() == 0)
                        item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);
                    return item;
                }));

        Block feesBlock = new Block("^Entgelte vom .*$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.FEES);
                    return entry;
                })

                .oneOf(
                                // @formatter:off
                                // Entgelte vom 01.08.2015 bis 31.08.2015                               6,20-
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2", "date", "amount", "type")
                                        .match("^(?<note1>Entgelte) .* (?<note2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})) .* (?<amount>[\\.,\\d]+)(?<type>\\-)$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            t.setDateTime(asDate(v.get("date")));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(context.get("currency"));
                                            t.setNote(v.get("note1") + " " + v.get("note2"));
                                        })
                                ,
                                // @formatter:off
                                // Entgelte vom 01.11.2012 bis 30.11.2012                               4,30S
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2", "date", "amount", "type")
                                        .match("^(?<note1>Entgelte) .* (?<note2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})) .* (?<amount>[\\.,\\d]+)(?<type>S)$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            t.setDateTime(asDate(v.get("date")));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(context.get("currency"));
                                            t.setNote(v.get("note1") + " " + v.get("note2"));
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // abzüglich 100% Nachlass auf        1,80- 1,80+         1,80+
                                // @formatter:on
                                section -> section
                                        .attributes("amount")
                                        .match("^abz.glich .* Nachlass .* (?<amount>[\\.,\\d]+)\\+$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            Money discount = Money.of(context.get("currency"), asAmount(v.get("amount")));

                                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(discount));
                                        })
                                ,
                                // @formatter:off
                                // abzüglich 100% Nachlass auf        1,80S               1,80H         1,80H
                                // @formatter:on
                                section -> section
                                        .attributes("amount")
                                        .match("^abz.glich .* Nachlass .* (?<amount>[\\.,\\d]+)H$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            Money discount = Money.of(context.get("currency"), asAmount(v.get("amount")));

                                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(discount));
                                        })
                        )

                .wrap(t -> {
                    TransactionItem item = new TransactionItem(t);
                    if (t.getCurrencyCode() != null && t.getAmount() == 0)
                        item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);
                    return item;
                }));
    }
}

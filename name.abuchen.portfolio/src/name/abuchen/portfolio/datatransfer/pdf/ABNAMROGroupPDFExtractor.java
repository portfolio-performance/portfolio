package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class ABNAMROGroupPDFExtractor extends AbstractPDFExtractor
{
    public ABNAMROGroupPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("ABN AMRO Bank N.V.");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "ABN AMRO Group / MoneYou";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug", //
                        documentContext -> documentContext //
                                        .oneOf( //
                                                        // @formatter:off
                                                        // Tagesgeldkonto (alle Betra¨ge in EUR) 63,28 28.06.2019 85.288,02
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^Tagesgeldkonto \\(alle Betr.* in (?<currency>[A-Z]{3})\\).*$") //
                                                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))),
                                                        // @formatter:off
                                                        // Tagesgeldkonto 100,00 24.10.2011 100,00
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^Tagesgeldkonto (?<currency>.*)$") //
                                                                        .assign((ctx, v) -> ctx.put("currency",
                                                                                        CurrencyUnit.EUR)))

                                        .optionalOneOf( //
                                                        // @formatter:off
                                                        // 31.12.2012 01.01.2013 5000510765 Abgeltungssteuer 49,74
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("taxDate1", "tax1").multipleTimes() //
                                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<taxDate1>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* Abgeltungssteuer[\\s]{1,}(?<tax1>[\\.,\\d]+)$") //
                                                                        .assign((ctx, v) -> {
                                                                            ctx.put("taxDate1", v.get("taxDate1"));
                                                                            ctx.put("tax1", v.get("tax1"));
                                                                        }))

                                        .optionalOneOf( //
                                                        // @formatter:off
                                                        // 31.12.2012 01.01.2013 5000510765 Solidarita¨tszuschlag 2,73
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("taxDate2", "tax2") //
                                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<taxDate2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* Solidarit.*tszuschlag[\\s]{1,}(?<tax2>[\\.,\\d]+)$") //
                                                                        .assign((ctx, v) -> {
                                                                            ctx.put("taxDate2", v.get("taxDate2"));
                                                                            ctx.put("tax2", v.get("tax2"));
                                                                        }))

                                        .optionalOneOf( //
                                                        // @formatter:off
                                                        // 31.12.2012 01.01.2013 5000510765 Kirchensteuer X,XX
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("taxDate3", "tax3") //
                                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<taxDate3>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* Kirchensteuer[\\s]{1,}(?<tax3>[\\.,\\d]+)$") //
                                                                        .assign((ctx, v) -> {
                                                                            ctx.put("taxDate3", v.get("taxDate3"));
                                                                            ctx.put("tax3", v.get("tax3"));
                                                                        })));

        this.addDocumentTyp(type);

        // @formatter:off
        // 24.10.2011 19.10.2011 12030000-001 Zahlungseingang 100,00
        // 11.10.2012 11.10.2012 B2D11BI5S00A Ru¨ckzahlung Ihres Festgeldes 50.000,00
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (Zahlungseingang|Ru.ckzahlung Ihres Festgeldes)[\\s]{1,}[\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* " //
                                        + "(Zahlungseingang" //
                                        + "|Ru.ckzahlung Ihres Festgeldes)[\\s]{1,}" //
                                        + "(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 16.12.2011 16.12.2011 12030000-001 Zahlungsausgang 2.000,00
        // 11.04.2012 11.04.2012 B2D11BI5S00A Abschluss eines Festgeldes 50.000,0
        // @formatter:on
        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (Zahlungsausgang|Abschluss eines Festgeldes)[\\s]{1,}[\\.,\\d]+$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* " //
                                        + "(Zahlungsausgang" //
                                        + "|Abschluss eines Festgeldes)[\\s]{1,}" //
                                        + "(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 28.06.2019 01.07.2019 DE5050324040 Ihre Zinsabrechnung 63,28
        // 30.12.2011 01.01.2012 5000510765 Ihre Tagesgeldzinsen 114,34
        // 11.10.2012 11.10.2012 B2D11BI5S00A Zinszahlung Festgeld 596,33
        //
        // 31.12.2012 01.01.2013 5000510765 Abgeltungssteuer 49,74
        // 31.12.2012 01.01.2013 5000510765 Solidarita¨tszuschlag 2,73
        // 31.12.2012 01.01.2013 5000510765 Ihre Tagesgeldzinsen 198,97
        // @formatter:on
        Block interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (Ihre (Zinsabrechnung|Tagesgeldzinsen)|Zinszahlung Festgeld)[\\s]{1,}[\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .documentContextOptionally("taxDate1", "taxDate2", "taxDate3", "tax1", "tax2", "tax3")
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* " //
                                        + "(Ihre (Zinsabrechnung|Tagesgeldzinsen)" //
                                        + "|Zinszahlung Festgeld)[\\s]{1,}" //
                                        + "(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            if (v.containsKey("taxDate1") && v.containsKey("tax1")
                                            && t.getDateTime().equals(asDate(v.get("taxDate1"))))
                            {
                                Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax1")));
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                            }

                            if (v.containsKey("taxDate2") && v.containsKey("tax2")
                                            && t.getDateTime().equals(asDate(v.get("taxDate2"))))
                            {
                                Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax2")));
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                            }

                            if (v.containsKey("taxDate3") && v.containsKey("tax3")
                                            && t.getDateTime().equals(asDate(v.get("taxDate3"))))
                            {
                                Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax3")));
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                            }
                        })

                        .wrap(TransactionItem::new));
    }
}

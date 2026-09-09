package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
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
        addBankIdentifier("Bij- en afschrijvingen");

        addAccountStatementTransaction();
        addAccountStatementTransaction_Format02();
    }

    @Override
    public String getLabel()
    {
        return "ABN AMRO Group / MoneYou";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug", //
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
        var depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (Zahlungseingang|Ru.ckzahlung Ihres Festgeldes)[\\s]{1,}[\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* " //
                                        + "(?<note>Zahlungseingang" //
                                        + "|Ru.ckzahlung Ihres Festgeldes)[\\s]{1,}" //
                                        + "(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // The PDF text contains a broken umlaut, e.g.
                            // "Ru¨ckzahlung" instead of "Rückzahlung"
                            t.setNote(trim(v.get("note")).replace("u¨", "ü"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 16.12.2011 16.12.2011 12030000-001 Zahlungsausgang 2.000,00
        // 11.04.2012 11.04.2012 B2D11BI5S00A Abschluss eines Festgeldes 50.000,0
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (Zahlungsausgang|Abschluss eines Festgeldes)[\\s]{1,}[\\.,\\d]+$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.REMOVAL))

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* " //
                                        + "(?<note>Zahlungsausgang" //
                                        + "|Abschluss eines Festgeldes)[\\s]{1,}" //
                                        + "(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(v.get("note")));
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
        var interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (Ihre (Zinsabrechnung|Tagesgeldzinsen)|Zinszahlung Festgeld)[\\s]{1,}[\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .documentContextOptionally("taxDate1", "taxDate2", "taxDate3", "tax1", "tax2", "tax3") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* " //
                                        + "(?<note>Ihre (Zinsabrechnung|Tagesgeldzinsen)" //
                                        + "|Zinszahlung Festgeld)[\\s]{1,}" //
                                        + "(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(v.get("note")));

                            if (v.containsKey("taxDate1") && v.containsKey("tax1")
                                            && t.getDateTime().equals(asDate(v.get("taxDate1"))))
                            {
                                var tax = Money.of(v.get("currency"), asAmount(v.get("tax1")));
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                            }

                            if (v.containsKey("taxDate2") && v.containsKey("tax2")
                                            && t.getDateTime().equals(asDate(v.get("taxDate2"))))
                            {
                                var tax = Money.of(v.get("currency"), asAmount(v.get("tax2")));
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                            }

                            if (v.containsKey("taxDate3") && v.containsKey("tax3")
                                            && t.getDateTime().equals(asDate(v.get("taxDate3"))))
                            {
                                var tax = Money.of(v.get("currency"), asAmount(v.get("tax3")));
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                            }
                        })

                        .wrap(TransactionItem::new));
    }

    private void addAccountStatementTransaction_Format02()
    {
        final var type = new DocumentType("Bij\\- en afschrijvingen", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Saldo 02-12-2025 €  0,00 Totaal afgeschreven Totaal bijgeschreven
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Saldo [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} (?<currency>\\p{Sc}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 02-03-2026 DEPOSIT INV. FUND VANG FTSE WLD 251,00
        // FONDSCODE 098020 PER 02/03 UNIT
        // 1,7308 @ EUR 145,02
        // @formatter:on
        var buySellBlock = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} DEPOSIT INV\\. FUND .* [\\.,\\d]+$");
        buySellBlock.setMaxSize(3);
        type.addBlock(buySellBlock);
        buySellBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .section("date", "name", "amount", "wkn") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) DEPOSIT INV\\. FUND (?<name>.*) (?<amount>[\\.,\\d]+)$") //
                        .match("^FONDSCODE (?<wkn>[A-Z0-9]{6}) PER [\\d]{2}\\/[\\d]{2} UNIT.*$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // FONDSCODE 098020 PER 25/08 UNIT 1,241
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^FONDSCODE [A-Z0-9]{6} PER [\\d]{2}\\/[\\d]{2} UNIT (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // 1,7308 @ EUR 145,02
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) @ [A-Z]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .wrap(BuySellEntryItem::new));

        // @formatter:off
        // 31-03-2026 DIVIDEND VANG FTSE WLD DIS 0,69
        // FONDSCODE 098020 31.03.2026 OVER
        // PART 1,7308 PAID WITH EUR 0,39893917
        // @formatter:on
        var dividendBlock = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} DIVIDEND .* [\\.,\\d]+$");
        dividendBlock.setMaxSize(3);
        type.addBlock(dividendBlock);
        dividendBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .section("date", "name", "amount", "wkn", "shares") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) DIVIDEND (?<name>.*) (?<amount>[\\.,\\d]+)$") //
                        .match("^FONDSCODE (?<wkn>[A-Z0-9]{6}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} OVER$") //
                        .match("^PART (?<shares>[\\.,\\d]+) PAID WITH [A-Z]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 01-03-2026 SEPA Overboeking IBAN: 251,00
        // yL65EsYr4346691434 BIC: INGBNL2A
        //
        // 16-06-2026 SEPA Overboeking IBAN: 90,00
        // rF40QtOR2279550554 BIC: ABNANL2A
        // @formatter:on
        var depositRemovalBlock_Format01 = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} SEPA Overboeking IBAN: [\\.,\\d]+$");
        depositRemovalBlock_Format01.setMaxSize(2);
        type.addBlock(depositRemovalBlock_Format01);
        depositRemovalBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<note>SEPA Overboeking) IBAN: (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .section("bic").optional() //
                        .match("^.* BIC: (?<bic>[A-Z0-9]{8,11})$") //
                        .assign((t, v) -> {
                            // Money can only be withdrawn to the own ABN AMRO
                            // account. Is bic --> "ABNANL2A" change from
                            // DEPOSIT to REMOVAL
                            if ("ABNANL2A".equals(trim(v.get("bic"))))
                                t.setType(AccountTransaction.Type.REMOVAL);
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 19-08-2026 /TRTP/SEPA 300,00
        // OVERBOEKING/IBAN/od00qqfz2703081661
        // /BIC/INGBNL2A/NAME/Hr fc wd
        //
        // 12-06-2026 /TRTP/SEPA 100,00
        // Overboeking/IBAN/NL37ABNA0562843558/B
        // IC/ABNANL2A/NAME/ABN AMRO
        // @formatter:on
        var depositBlock_Format02 = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} \\/TRTP\\/SEPA [\\.,\\d]+$");
        depositBlock_Format02.setMaxSize(1);
        type.addBlock(depositBlock_Format02);
        depositBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) \\/TRTP\\/SEPA (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));

                            // The booking text is spread over several lines,
                            // therefore we set the note manually
                            t.setNote("SEPA Overboeking");
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 02-04-2026 ACCOUNT BALANCED CREDIT INTEREST 0,02
        // 0,02Cfrom 31.12.2025 to 31.03.2026
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} ACCOUNT BALANCED CREDIT INTEREST [\\.,\\d]+$");
        interestBlock.setMaxSize(2);
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) ACCOUNT BALANCED CREDIT INTEREST (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("note").optional() //
                        .match("^[\\.,\\d]+C(?<note>from [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} to [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 12-06-2026 ABNAMRO Investments See your invoice for 0,05
        // details
        // @formatter:on
        var feesBlock = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} ABNAMRO Investments See your invoice for [\\.,\\d]+$");
        feesBlock.setMaxSize(1);
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<note>ABNAMRO Investments) See your invoice for (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }
}

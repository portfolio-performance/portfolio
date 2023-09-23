package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.DocumentContext;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class INGDiBaPDFExtractor extends AbstractPDFExtractor
{
    private static final String IS_JOINT_ACCOUNT = "isJointAccount";

    BiConsumer<DocumentContext, String[]> jointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("KapSt anteilig [\\d]{2},[\\d]{2} %.*");

        for (String line : lines)
        {
            if (pJointAccount.matcher(line).matches())
            {
                context.putBoolean(IS_JOINT_ACCOUNT, true);
                break;
            }
        }
    };

    public INGDiBaPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("ING-DiBa AG");

        addBuySellTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "ING-DiBa AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Wertpapierabrechnung "
                        + "(Kauf|"
                        + "Kauf aus Sparplan|"
                        + "Kauf aus Wiederanlage Fondsaussch.ttung|"
                        + "Bezug|"
                        + "Verkauf|"
                        + "Verk\\. Teil\\-\\/Bezugsr\\.)|"
                        + "R.ckzahlung|"
                        + "Einl.sung)", jointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Wertpapierabrechnung "
                        + "(Kauf|"
                        + "Kauf Einmalanlage|"
                        + "Kauf aus Sparplan|"
                        + "Kauf aus Wiederanlage Fondsaussch.ttung|"
                        + "Bezug|"
                        + "Verkauf|"
                        + "Verk. Teil\\-\\/Bezugsr\\.)|"
                        + "R.ckzahlung|"
                        + "Einl.sung)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(Wertpapierabrechnung )?(?<type>(Kauf|"
                                + "Kauf Einmalanlage|"
                                + "Kauf aus Sparplan|"
                                + "Bezug|"
                                + "Verkauf|"
                                + "Verk. Teil\\-\\/Bezugsr\\.)|"
                                + "R.ckzahlung|"
                                + "Einl.sung)$")
                .assign((t, v) -> {
                    if ("Verkauf".equals(v.get("type"))
                            || "Verk. Teil-/Bezugsr.".equals(v.get("type"))
                            || "Rückzahlung".equals(v.get("type"))
                            || "Einlösung".equals(v.get("type")))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // @formatter:off
                // ISIN (WKN) DE0002635307 (263530)
                // Wertpapierbezeichnung iSh.STOXX Europe 600 U.ETF DE
                // Inhaber-Anteile
                // Kurswert EUR 4.997,22
                // @formatter:on
                .section("isin", "wkn", "name", "name1", "currency")
                .match("^ISIN \\(WKN\\) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                .match("^Wertpapierbezeichnung (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominale") && !v.get("name1").startsWith("Zinstermin"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                .oneOf(
                                // @formatter:off
                                // Nominale Stück 14,00
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^Nominale St.ck (?<shares>[\\.,\\d]+)$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // Nominale 11,00 Stück
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^Nominale (?<shares>[\\.,\\d]+) St.ck$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // Nominale EUR 1.000,00
                                section -> section
                                        .attributes("shares")
                                        .match("^Nominale [\\w]{3} (?<shares>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            // Percentage quotation, workaround for bonds
                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                        })
                        )

                // @formatter:off
                // Ausführungstag / -zeit 17.11.2015 um 16:17:32 Uhr
                // Schlusstag / -zeit 20.03.2012 um 19:35:40 Uhr
                // @formatter:on
                .section("time").optional()
                .match("^(Ausf.hrungstag|Schlusstag) \\/ \\-zeit [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // @formatter:off
                // Ausführungstag 15.12.2015
                // Schlusstag / -zeit 20.03.2012 um 19:35:40 Uhr
                // Fälligkeit 25.05.2017
                // @formatter:on
                .section("date").multipleTimes()
                .match("^(Ausf.hrungstag|Schlusstag|F.lligkeit)( \\/ -zeit)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})( .*)?$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // @formatter:off
                // Endbetrag zu Ihren Lasten EUR 533,39
                // Endbetrag zu Ihren Gunsten EUR 1.887,64
                // Endbetrag EUR 256,66
                // @formatter:on
                .section("currency", "amount")
                .match("^Endbetrag( zu Ihren (Lasten|Gunsten))? (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // Zwischensumme USD 1.503,75
                // umger. zum Devisenkurs EUR 1.311,99 (USD = 1,146163)
                // Endbetrag zu Ihren Lasten EUR 1.335,07
                // @formatter:on
                .section("fxGross", "gross", "termCurrency", "exchangeRate", "baseCurrency").optional()
                .match("^Zwischensumme [\\w]{3} (?<fxGross>[\\.,\\d]+)$")
                .match("^.* Devisenkurs [\\w]{3} (?<gross>[\\.,\\d]+) \\((?<termCurrency>[\\w]{3}) = (?<exchangeRate>[\\.,\\d]+)\\)$")
                .match("^Endbetrag( zu Ihren (Lasten|Gunsten))? (?<baseCurrency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                    Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // Ordernummer 12345678.001
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Ordernummer .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .optionalOneOf(
                                // @formatter:off
                                // Diese Order wurde mit folgendem Limit / -typ erteilt: 38,10 EUR
                                // Diese Order wurde mit folgendem Limit / -typ erteilt: 57,00 EUR / Dynamisches Stop / Abstand 6,661 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^Diese Order wurde mit folgendem (?<note1>Limit) .*: (?<note2>[\\.,\\d]+ [\\w]{3})( .*)?$")
                                        .assign((t, v) -> {
                                            if (t.getNote() != null)
                                                t.setNote(t.getNote() + " | " + v.get("note1") + ": " + v.get("note2"));
                                            else
                                                t.setNote(v.get("note1") + ": " + v.get("note2"));
                                        })
                                ,
                                // @formatter:off
                                // Rückzahlung
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(?<note>R.ckzahlung)$")
                                        .assign((t, v) -> {
                                            if (t.getNote() != null)
                                                t.setNote(t.getNote() + " | " + v.get("note"));
                                            else
                                                t.setNote(v.get("note"));
                                        })
                                ,
                                // @formatter:off
                                // Stückzinsen EUR 0,10 (Zinsvaluta 17.11.2022 357 Tage)
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(?<note>St.ckzinsen .*)$")
                                        .assign((t, v) -> {
                                            if (t.getNote() != null)
                                                t.setNote(t.getNote() + " | " + v.get("note"));
                                            else
                                                t.setNote(v.get("note"));
                                        })
                        )

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Ertragsgutschrift|Zinsgutschrift)", jointAccount);
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Ertragsgutschrift|Zinsgutschrift).*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Sie erhalten eine neue Abrechnung.
                .section("type").optional()
                .match("^(?<type>Sie erhalten eine neue Abrechnung\\.)$")
                .assign((t, v) -> v.getTransactionContext().put(FAILURE,
                                Messages.MsgErrorOrderCancellationUnsupported))

                .oneOf(
                                // @formatter:off
                                // ISIN (WKN) US5801351017 (856958)
                                // Wertpapierbezeichnung McDonald's Corp.
                                // Registered Shares DL-,01
                                // Zins-/Dividendensatz 0,94 USD
                                // @formatter:on
                                section -> section
                                        .attributes("isin", "wkn", "name", "name1", "currency")
                                        .match("^ISIN \\(WKN\\) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                                        .match("^Wertpapierbezeichnung (?<name>.*)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^(Zins\\-\\/Dividendensatz|(Ertragsaussch.ttung|Vorabpauschale) per St.ck) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Nominale"))
                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                                ,
                                // @formatter:off
                                // ISIN (WKN) DE000A1PGUT9 (A1PGUT)
                                // Wertpapierbezeichnung 7,25000% posterXXL AG
                                // Inh.-Schv.v.2012(2015/2017)
                                // Nominale 1.000,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("isin", "wkn", "name", "name1", "currency")
                                        .match("^ISIN \\(WKN\\) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                                        .match("^Wertpapierbezeichnung (?<name>.*)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^Nominale [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                        )

                // @formatter:off
                // Nominale 66,00 Stück
                // Nominale 1.000,00 EUR
                // @formatter:on
                .section("shares", "notation")
                .match("^Nominale (?<shares>[\\.,\\d]+) (?<notation>(St.ck|[\\w]{3}))$")
                .assign((t, v) -> {
                    if (v.get("notation") != null && !"Stück".equalsIgnoreCase(v.get("notation")))
                    {
                        // Percentage quotation, workaround for bonds
                        BigDecimal shares = asBigDecimal(v.get("shares"));
                        t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                    }
                    else
                    {
                        t.setShares(asShares(v.get("shares")));
                    }
                })

                // @formatter:off
                // Valuta 15.12.2016
                // @formatter:on
                .section("date")
                .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Gesamtbetrag zu Ihren Gunsten EUR 44,01
                .section("amount", "currency").optional()
                .match("^Gesamtbetrag zu Ihren Gunsten (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // If the total amount is negative, then we change the
                // transaction type from DIVIDENDS to TAXES.

                // Gesamtbetrag zu Ihren Lasten EUR - 20,03
                .section("currency", "amount").optional()
                .match("^Gesamtbetrag zu Ihren Lasten (?<currency>[\\w]{3}) \\- (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (v.getTransactionContext().get(FAILURE) == null)
                        t.setType(AccountTransaction.Type.TAXES);

                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // Brutto USD 62,04
                // Umg. z. Dev.-Kurs (1,049623) EUR 50,24
                //
                // Brutto USD - 54,00
                // Umg. z. Dev.-Kurs (1,084805) EUR - 37,33
                // @formatter:on
                .section("termCurrency", "fxGross", "exchangeRate", "baseCurrency").optional()
                .match("^Brutto (?<termCurrency>[\\w]{3}) (\\- )?(?<fxGross>[\\.,\\d]+)$")
                .match("^Umg\\. z\\. Dev\\.\\-Kurs \\((?<exchangeRate>[\\.,\\d]+)\\) (?<baseCurrency>[\\w]{3}) (\\- )?[\\.,\\d]+$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                .conclude(ExtractorUtils.fixGrossValueA())

                .wrap((t, ctx) -> {
                    TransactionItem item = new TransactionItem(t);

                    if (ctx.getString(FAILURE) != null)
                        item.setFailureMessage(ctx.getString(FAILURE));

                    return item;
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Vorabpauschale$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAXES);
                    return t;
                })

                // @formatter:off
                // ISIN (WKN) IE00BKPT2S34 (A2P1KU)
                // Wertpapierbezeichnung iShsIII-Gl.Infl.L.Gov.Bd U.ETF
                // Reg. Shs HGD EUR Acc. oN
                // Nominale 378,00 Stück
                // Vorabpauschale per Stück 0,00245279 EUR
                // @formatter:on
                .section("isin", "wkn", "name", "name1", "currency")
                .match("^ISIN \\(WKN\\) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                .match("^Wertpapierbezeichnung (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Vorabpauschale per St.ck [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominale"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // @formatter:off
                // Nominale 378,00 Stück
                // @formatter:on
                .section("shares")
                .match("^Nominale (?<shares>[\\.,\\d]+) .*")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Valuta 04.01.2021
                // @formatter:on
                .section("date")
                .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Gesamtbetrag zu Ihren Lasten EUR - 0,16
                // @formatter:on
                .section("currency", "amount")
                .match("^Gesamtbetrag zu Ihren Lasten (?<currency>[\\w]{3}) \\- (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(t -> {
                    TransactionItem item = new TransactionItem(t);

                    if (t.getCurrencyCode() != null && t.getAmount() == 0)
                        item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);
                    return item;
                });
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug .*[\\d]{4}", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Buchung Buchung / Verwendungszweck Betrag (EUR)
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Buchung Buchung \\/ Verwendungszweck Betrag \\((?<currency>[\\w]{3})\\)$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 13.07.2016 Ueberweisung Mustermann -5.000,00
        // 13.02.2020 Gutschrift/Dauerauftrag Max Mustermann 1,01
        // 16.02.2020 Lastschrift XYZ GmbH -10,00
        // @formatter:on
        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                        + "(Ueberweisung" //
                        + "|Dauerauftrag\\/Terminueberw\\." //
                        + "|Lastschrift) " //
                        + ".* \\-[\\.,\\d]+$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.REMOVAL);
                            return entry;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>Ueberweisung" //
                                        + "|Dauerauftrag\\/Terminueberw\\." //
                                        + "|Lastschrift) " //
                                        + ".* \\-(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if ("Ueberweisung".equals(v.get("note")))
                                v.put("note", "Überweisung");

                            if ("Dauerauftrag/Terminueberw.".equals(v.get("note")))
                                v.put("note", "Dauerauftrag/Terminüberweisung");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 27.06.2016 Gutschrift Max Mustermann 10.000,00
        // 14.02.2020 Dauerauftrag/Terminueberw. Max Mustermann -30,00
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                        + "(Gutschrift" //
                        + "|Gutschrift\\/Dauerauftrag) " //
                        + ".* [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DEPOSIT);
                            return entry;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>Gutschrift" //
                                        + "|Gutschrift\\/Dauerauftrag) " //
                                        + ".* (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 01.01.2016 bis 14.06.2016 0,50%  Zins 0,40
        // 15.06.2016 bis 31.12.2016 0,35%  Zins 5,22
        // @formatter:on
        Block interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* Zins [\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.INTEREST);
                            return entry;
                        })

                        .section("note1", "date", "note2", "amount") //
                        .documentContext("currency") //
                        .match("^(?<note1>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})) " //
                                        + "(?<note2>[\\,\\d]+%) .* Zins " //
                                        + "(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note1") + " (" + v.get("note2") + ")");
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 30.12.2016 Kapitalertragsteuer -1,38
        // 30.12.2016 Solidaritätszuschlag -0,07
        // 30.12.2016 Kirchensteuer -0,11
        // @formatter:on
        Block taxesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                        + "(Kapitalertrags(s)?teuer" //
                        + "|Solidarit.tszuschlag" //
                        + "|Kirchensteuer) \\-[\\.,\\d]+$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAXES);
                            return entry;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>Kapitalertrags(s)?teuer" //
                                        + "|Solidarit.tszuschlag" //
                                        + "|Kirchensteuer) " //
                                        + "\\-(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Kapitalertragsteuer (Account)
                // Kapitalertragsteuer 25,00 % EUR 18,32
                // Kapitalertragsteuer 25,00% EUR 5,91
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kapitalerstragsteuer (Joint Account)
                // KapSt anteilig 50,00 % 24,45% EUR 79,46
                // KapSt anteilig 50,00 % 24,45 % EUR 79,46
                // @formatter:on
                .section("currency1", "tax1", "currency2", "tax2").optional()
                .match("^KapSt anteilig [\\.,\\d]+([\\s]+)?% [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^KapSt anteilig [\\.,\\d]+([\\s]+)?% [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // @formatter:off
                // Solidaritätszuschlag (Account)
                // Solidarit‰tszuschlag 5,50 % EUR 1,00
                // Solidaritätszuschlag 5,50% EUR 0,32
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Solitaritätszuschlag (Joint Account)
                // Solidaritätszuschlag 5,50% EUR 4,37
                // Solidaritätszuschlag 5,50 % EUR 4,37
                // @formatter:on
                .section("currency1", "tax1", "currency2", "tax2").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // @formatter:off
                // Kirchensteuer (Account)
                // Kirchensteuer 5,50 % EUR 1,00
                // Kirchensteuer 5,50% EUR 0,32
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kirchensteuer (Joint Account)
                // Kirchensteuer 9,00 % EUR 7,15
                // Kirchensteuer 9,00% EUR 7,15
                // @formatter:on
                .section("currency1", "tax1", "currency2", "tax2").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // @formatter:off
                // QuSt 15,00 % (EUR 8,87) USD 9,31
                // @formatter:on
                .section("currency", "withHoldingTax").optional()
                .match("^QuSt [\\.,\\d]+([\\s]+)?% \\((?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+)\\) [\\w]{3} [\\.,\\d]+$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // @formatter:off
                // QuSt 30,00 % EUR 16,50
                // @formatter:on
                .section("currency", "withHoldingTax").optional()
                .match("^QuSt [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // @formatter:off
                // Franz. Transaktionssteuer 0,30% EUR 2,52
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Franz\\. Transaktionssteuer [\\.,\\d]+% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Handelsplatzgebühr EUR 2,50
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^Handelsplatzgeb.hr (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Provision EUR 9,90
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^Provision (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Handelsentgelt EUR 3,00
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^Handelsentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Börsenentgelt EUR 0,39
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^B.rsenentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Variables Transaktionsentgelt EUR 2,82
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^Variables Transaktionsentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Kurswert EUR 52,63
                // Rabatt EUR - 2,63
                // Der regul.re Ausgabeaufschlag von 5,263% ist im Kurs enthalten.
                // @formatter:on
                .section("currency", "amount", "discountCurrency", "discount", "percentageFee").optional()
                .match("^Kurswert (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .match("^Rabatt (?<discountCurrency>[\\w]{3}) \\- (?<discount>[\\.,\\d]+)$")
                .match("^Der regul.re Ausgabeaufschlag von (?<percentageFee>[\\.,\\d]+)% .*$")
                .assign((t, v) -> {
                    BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                    BigDecimal amount = asBigDecimal(v.get("amount"));
                    Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                    if (percentageFee.compareTo(BigDecimal.ZERO) != 0 && discount.isPositive())
                    {
                        // @formatter:off
                        // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                        // @formatter:on
                        BigDecimal fxFee = amount
                                        .divide(percentageFee.divide(BigDecimal.valueOf(100))
                                                        .add(BigDecimal.ONE), Values.MC)
                                        .multiply(percentageFee, Values.MC);

                        Money fee = Money.of(asCurrencyCode(v.get("currency")),
                                        fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                        // fee = fee - discount
                        fee = fee.subtract(discount);

                        checkAndSetFee(fee, t, type.getCurrentContext());
                    }
                });
    }
}
package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.Locale;

import name.abuchen.portfolio.Messages;
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

/**
 * @formatter:off
 * @implNote Liechtensteinische Landesbank AG
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class LiechtensteinischeLBPDFExtractor extends AbstractPDFExtractor
{
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public LiechtensteinischeLBPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Liechtensteinische Landesbank");
        addBankIdentifier("Liechtensteinische");

        addBuySellTransaction();
        addDividendeTransaction();
        addDeliveryInOutBoundTransaction();
        addFixedTermDepositTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Liechtensteinische Landesbank AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("(Storno: )?B.rsenabrechnung \\- Ihr (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Storno: )?B.rsenabrechnung \\- Ihr (Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(Storno: )?B.rsenabrechnung \\- Ihr (?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Storno: Börsenabrechnung - Ihr Verkauf
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Storno): B.rsenabrechnung \\- Ihr (Kauf|Verkauf).*$") //
                        .assign((t, v) -> v.markAsFailure(Messages.MsgErrorTransactionOrderCancellationUnsupported))

                        .oneOf( //
                                        // @formatter:off
                                        // Auftragsnummer XXXXXXXXX
                                        // Ant Plenum CAT Bond Fund Klasse -P CHF-
                                        // Kurs CHF 104.42
                                        // ISIN LI0290349492
                                        // Valorennummer 29034949
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "wkn") //
                                                        .find("Auftragsnummer .*") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^Kurs (?<currency>[A-Z]{3}) [\\.'\\d]+$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Valorennummer (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Auftragsnummer 343253140
                                        // 3.875% Treasury Nts United States 2023-30.04.25
                                        // Anzahl / Nominal USD 7'500.00
                                        // Kurs 99.75 %
                                        // ISIN US91282CGX39
                                        // Valorennummer 126441977
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "wkn") //
                                                        .find("Auftragsnummer .*") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^Anzahl \\/ Nominal (?<currency>[A-Z]{3}) [\\.'\\d]+$") //
                                                        .match("^Kurs [\\.'\\d]+ %$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Valorennummer (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Anzahl / Nominal 1.394011
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Anzahl \\/ Nominal (?<shares>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Anzahl / Nominal USD 7'500.00
                                        // Kurs 99.75 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Anzahl \\/ Nominal [A-Z]{3} (?<shares>[\\.'\\d]+)$") //
                                                        .match("^Kurs [\\.'\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            // Percentage quotation, workaround for bonds
                                                            t.setShares(asBondNominal(v.get("shares")));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Abschlussdatum 09.11.2022 / 10:15:38 CET
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Abschlussdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Abschlussdatum 17.11.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Abschlussdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Zu Ihren Lasten Valuta 22. November 2023 CHF 145.56
                        // Zu Ihren Gunsten Valuta 7. November 2023 CHF 55.10
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Zu Ihren (Lasten|Gunsten) Valuta [\\d]{1,2}\\. .* [\\d]{4} (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Bruttobetrag EUR 48.47
                        // Umrechnungskurs EUR/CHF 0.964296
                        //
                        // Bruttobetrag ZAR 34'080.00
                        // Umrechnungskurs EUR/ZAR 19.179094
                        // @formatter:on
                        .section("fxCurrency", "fxGross", "baseCurrency", "termCurrency", "exchangeRate").optional()
                        .match("^Bruttobetrag (?<fxCurrency>[A-Z]{3}) (?<fxGross>[\\.'\\d]+)$") //
                        .match("^Umrechnungskurs (?<baseCurrency>[A-Z]{3})\\/(?<termCurrency>[A-Z]{3}) (?<exchangeRate>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            // The exchange rate is quoted with the account currency either
                            // as base or as term currency, depending on the document.
                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                            ExtrExchangeRate rate = fixExchangeRateQuotedPer100Units(asExchangeRate(v), fxGross,
                                            t.getPortfolioTransaction().getMonetaryAmount());
                            type.getCurrentContext().putType(rate);

                            Money gross = rate.convert(t.getPortfolioTransaction().getCurrencyCode(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftragsnummer XXXXXXXXX
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Marchzinsen 25 Tage USD 20.07
                        // @formatter:on
                        .section("note1", "note2", "note3").optional() //
                        .match("^(?<note1>Marchzinsen [\\d]+ Tage) (?<note3>[A-Z]{3}) (?<note2>(\\-)?[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), v.get("note1"), " | "));
                            t.setNote(concatenate(t.getNote(), v.get("note2"), ": "));
                            t.setNote(concatenate(t.getNote(), v.get("note3"), " "));
                        })

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("(Bardividende \\(Ordentliche Dividende\\)" //
                        + "|Wahldividende" //
                        + "|R.ckzahlung von Reserven aus Kapitaleinlagen)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        // @formatter:off
        // The heading is repeated on the following pages without any booking
        // data, therefore the end of the block is required.
        // @formatter:on
        Block firstRelevantLine = new Block("^(Bardividende \\(Ordentliche Dividende\\)" //
                        + "|Wahldividende" //
                        + "|R.ckzahlung von Reserven aus Kapitaleinlagen)$", //
                        "^Den Betrag haben wir gutgeschrieben.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .oneOf( //
                                        // @formatter:off
                                        // Auftragsnummer 623950393
                                        // Reg Shs Healthpeak Pptys Inc
                                        // ISIN US42250P1030
                                        // Valorennummer 50880191
                                        // Zahlungswert USD 0.30
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "wkn") //
                                                        .find("Auftragsnummer .*") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Valorennummer (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .match("^Zahlungswert (?<currency>[A-Z]{3}) [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Auftragsnummer XXXXXXXXX
                                        // Reg Shs Pearson PLC
                                        // ISIN GB0006776081
                                        // Valorennummer 400018
                                        // Zahlungswert GBP 0.07 pro Stück
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "wkn") //
                                                        .find("Auftragsnummer .*") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Valorennummer (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .match("^Zahlungswert (?<currency>[A-Z]{3}) [\\.'\\d]+ pro St.ck$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Ihr Bestand per 06.11.2023 25.114744 Stück
                        // @formatter:on
                        .section("shares")
                        .match("^Ihr Bestand per .* (?<shares>[\\.'\\d]+) St.ck$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zu Ihren Gunsten Valuta 20. November 2023 CHF 5.65
                        // @formatter:on
                        .section("date") //
                        .match("^Zu Ihren Gunsten Valuta (?<date>[\\d]{1,2}\\. .* [\\d]{4}) [A-Z]{3} [\\.'\\d]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ex-Datum 06.11.2023
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Ex\\-Datum (?<exDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        // @formatter:off
                        // Zu Ihren Gunsten Valuta 20. November 2023 CHF 5.65
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Zu Ihren Gunsten Valuta (?<date>[\\d]{1,2}\\. .* [\\d]{4}) (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Bruttobetrag USD 7.53
                        // Umrechnungskurs USD/CHF 0.882477
                        // @formatter:on
                        .section("fxCurrency", "fxGross", "baseCurrency", "termCurrency", "exchangeRate").optional()
                        .match("^Bruttobetrag (?<fxCurrency>[A-Z]{3}) (?<fxGross>[\\.'\\d]+)$") //
                        .match("^Umrechnungskurs (?<baseCurrency>[A-Z]{3})\\/(?<termCurrency>[A-Z]{3}) (?<exchangeRate>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            // The exchange rate is quoted with the account currency either
                            // as base or as term currency, depending on the document.
                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                            ExtrExchangeRate rate = fixExchangeRateQuotedPer100Units(asExchangeRate(v), fxGross,
                                            t.getMonetaryAmount());
                            type.getCurrentContext().putType(rate);

                            Money gross = rate.convert(t.getCurrencyCode(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftragsnummer 623950393
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDeliveryInOutBoundTransaction()
    {
        final var type = new DocumentType("Depoteingang");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Depoteingang$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new PortfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND))

                        // @formatter:off
                        // Reg Shs Naspers Ltd -N-
                        // Anzahl / Nominal 50 Stück
                        // ISIN ZAE000015889
                        // Valorennummer 104977
                        // Währung ZAR
                        // @formatter:on
                        .section("name", "isin", "wkn", "currency") //
                        .find("Wir haben Ihrem Depot folgende Werte beigef.gt:") //
                        .match("^(?<name>.*)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Valorennummer (?<wkn>[A-Z0-9]{5,9})$") //
                        .match("^W.hrung (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(0L);
                        })

                        // @formatter:off
                        // Anzahl / Nominal 50 Stück
                        // @formatter:on
                        .section("shares") //
                        .match("^Anzahl \\/ Nominal (?<shares>[\\.'\\d]+) St.ck$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Abschlussdatum 04.11.2022
                        // @formatter:on
                        .section("date") //
                        .match("^Abschlussdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Auftragsnummer 556171599
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new TransactionItem(t);
                        });
    }

    private void addFixedTermDepositTransaction()
    {
        final var type = new DocumentType("Festgeldanlage");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Festgeldanlage$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.REMOVAL))

                        // @formatter:off
                        // Zu Ihren Lasten Valuta 6. Juni 2023 HUF 3'770'000.00
                        // @formatter:on
                        .section("date", "currency", "amount") //
                        .match("^Zu Ihren Lasten Valuta (?<date>[\\d]{1,2}\\. .* [\\d]{4}) (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftragsnummer 305856191
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // The money is only transferred to the fixed-term deposit,
                        // therefore the transaction is skipped.
                        // @formatter:on
                        .wrap(t -> new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired));
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug in", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Kontoauszug in EUR 01.12.2023 - 31.12.2023
                                        // @formatter:on
                                        .section("currency", "year") //
                                        .match("^Kontoauszug in (?<currency>[A-Z]{3}) [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}).*$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("currency", asCurrencyCode(v.get("currency")));
                                            ctx.put("year", v.get("year"));
                                        }));

        this.addDocumentTyp(type);

        // @formatter:off
        // 01.02. Gutschrift 01.02. 465.86 10'522.58
        // XXXXX XXXXX
        // Auftragsnummer: XXXXXXXXX
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. Gutschrift [\\d]{2}\\.[\\d]{2}\\..*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "amount", "note") //
                        .documentContext("currency", "year") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. Gutschrift (?<date>[\\d]{2}\\.[\\d]{2}\\.) (?<amount>[\\.'\\d]+) [\\.'\\d]+$") //
                        .match("^(?<note>Auftragsnummer: .*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 28.06. Gebühren 30.06. 215.09 1.35
        // All-in-Gebühr
        // Periode 14.05.2024-30.06.2024
        // Auftragsnummer: 668110724
        // @formatter:on
        Block feesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. Geb.hren [\\d]{2}\\.[\\d]{2}\\..*$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))

                        .section("date", "amount", "note1", "note2") //
                        .documentContext("currency", "year") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. Geb.hren (?<date>[\\d]{2}\\.[\\d]{2}\\.) (?<amount>[\\.'\\d]+) [\\.'\\d]+$") //
                        .match("^(?<note1>.*[Gg]eb.hr)$") //
                        .match("^(?<note2>Auftragsnummer: .*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(concatenate(trim(v.get("note2")), trim(v.get("note1")), " | "));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 28.06. Zinszahlung Callgeld 28.06. 70.70 72.05
        // Call Deposit CHF, 0.65%, 10.05.23 (1965781)
        // Auftragsnummer: 669550491
        // @formatter:on
        Block interestBlock_Format02 = new Block("^[\\d]{2}\\.[\\d]{2}\\. Zinszahlung .* [\\d]{2}\\.[\\d]{2}\\..*$");
        type.addBlock(interestBlock_Format02);
        interestBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("note1", "date", "amount", "note2") //
                        .documentContext("currency", "year") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<note1>Zinszahlung .*) (?<date>[\\d]{2}\\.[\\d]{2}\\.) (?<amount>[\\.'\\d]+) [\\.'\\d]+$") //
                        .match("^(?<note2>Auftragsnummer: .*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(concatenate(trim(v.get("note2")), trim(v.get("note1")), " | "));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Per 31. Dezember 2023
        // Abrechnungsperiode 30.09.2023-31.12.2023
        // Habenzins 456.60
        // @formatter:on
        Block interestBlock = new Block("^Per [\\d]{1,2}\\. .* [\\d]{4}$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "note1", "note2", "amount") //
                        .documentContext("currency") //
                        .match("^Per (?<date>[\\d]{1,2}\\. .* [\\d]{4})$") //
                        .match("^Abrechnungsperiode (?<note1>[\\d]{1,2}\\.[\\d]{2}\\.[\\d]{4})\\-(?<note2>[\\d]{1,2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .match("^Habenzins (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note1") + " - " + v.get("note2"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eidgenössische Stempelsteuer CHF 0.04
                        // Eidgenössische Stempelsteuer CHF -0.01
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Eidgen.ssische Stempelsteuer (?<currency>[A-Z]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Finanztransaktionssteuer Spanien EUR 0.10
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Finanztransaktionssteuer .* (?<currency>[A-Z]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // 15 % Quellensteuer USD -1.13
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^[\\d]+ % Quellensteuer (?<currency>[A-Z]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Lieferspesen EUR 25.22
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Lieferspesen (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Fremde Brokergebühren EUR 0.84
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^(Fremde )?Brokergeb.hren (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    /**
     * @formatter:off
     * Exchange rates of currencies with a small unit value (e.g. NOK, SEK, JPY) are
     * quoted per 100 units. The converted gross value is then about 100 times the
     * booked amount and the exchange rate has to be divided by 100.
     *
     * Bruttobetrag NOK 849.15
     * Umrechnungskurs NOK/CHF 7.815708   --> CHF 0.07815708 per NOK
     * Zu Ihren Gunsten Valuta 29. August 2025 CHF 49.78
     * @formatter:on
     */
    private ExtrExchangeRate fixExchangeRateQuotedPer100Units(ExtrExchangeRate rate, Money fxGross, Money amount)
    {
        if (amount.getAmount() == 0)
            return rate;

        Money gross = rate.convert(amount.getCurrencyCode(), fxGross);

        if (gross.getAmount() <= amount.getAmount() * 10)
            return rate;

        BigDecimal correctedRate = amount.getCurrencyCode().equals(rate.getTermCurrency()) //
                        ? rate.getRate().divide(HUNDRED, Values.MC) //
                        : rate.getRate().multiply(HUNDRED);

        return new ExtrExchangeRate(correctedRate, rate.getBaseCurrency(), rate.getTermCurrency());
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected long asBondNominal(String value)
    {
        return asBondNominal(value, Locale.forLanguageTag("de-CH"));
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}

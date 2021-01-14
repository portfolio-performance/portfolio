package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.NonImportableItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class QuirionPDFExtractor extends AbstractPDFExtractor
{
    public QuirionPDFExtractor(Client client)
    {
        super(client);


        addBankIdentifier("Quirin Privatbank AG"); //$NON-NLS-1$

        addPeriodenauszugTransactions();
    }

    @SuppressWarnings("nls")
    private void addPeriodenauszugTransactions()
    {
        final DocumentType type = new DocumentType("(Kontoauszug)", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("(Vermögensverwaltungskonto in )(\\w{3})\\n");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(1));
                }
            }
        });
        this.addDocumentTyp(type);

//Kontoübertrag 1197537 28.05.2019 28.05.2019 3.000,00 EUR
//Ref.: 86991330
        
//Sammelgutschrift 19.12.2019 19.12.2019 5.000,00 EUR
//DE00000000000000000000
//Ref.: 105892216
//AWV-Meldepflicht beachten - Tel.-Nr.08001234111
        
//Interne Buchung 31.01.2020 31.01.2020 2,84 EUR
//Ref.: 110934428
//Kulanzzahlung
        
//Überweisungsgutschrift Inland 27.12.2019 27.12.2019 2.000,00 EUR
//Ref.: 106509889
//Konto Inhaber DE00000000000000000000
//Verwendungszweck
        
        Block depositBlock = new Block(
                        "^((Kontoübertrag (\\d+))|(Sammelgutschrift)|(Interne Buchung)|(Überweisungsgutschrift Inland)) (\\d+\\.\\d+\\.\\d{4}) (\\d+\\.\\d+\\.\\d{4}) ([\\d.]+,\\d{2}) (\\w{3})");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DEPOSIT);
            return t;
        })

                        .section("valuta", "amount")
                        .match("^((Kontoübertrag (\\d+))|(Sammelgutschrift)|(Interne Buchung)|(Überweisungsgutschrift Inland)) (\\d+\\.\\d+\\.\\d{4}) (?<valuta>\\d+\\.\\d+\\.\\d{4}) (?<amount>[\\d.]+,\\d{2}) (\\w{3})")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            t.setDateTime(asDate(v.get("valuta")));
                            t.setAmount(asAmount(v.get("amount")));
                        }).wrap(t -> new TransactionItem(t)));

        
//Steueroptimierung 12.06.2020 12.06.2020 36,82 EUR
//Ref.: 135435928
        
        Block taxReturnBlock = new Block("^Steueroptimierung (\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) ([\\d.]+,\\d{2}).*");
        type.addBlock(taxReturnBlock);
        taxReturnBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.TAX_REFUND);
            return t;
        })

                        .section("amount", "currency", "date")
                        .match("^Steueroptimierung (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+) (?<amount>[\\d.]+,\\d{2}) (?<currency>\\w{3}).*")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new TransactionItem(t)));
        
        
//Rücküberweisung Inland 23.12.2019 19.12.2019 -5.002,84 EUR
//Ref.: 106317528
//Kontoinhaber DE00000000000000000000
//Verwendungszweck             laenge
//r Verrechnungskontonummer
//5fx3t8k485f104a-39000
        
        Block removalBlock = new Block(
                        "^(Rücküberweisung Inland) (\\d+\\.\\d+\\.\\d{4}) (\\d+\\.\\d+\\.\\d{4}) -([\\d.]+,\\d{2}) (\\w{3})");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.REMOVAL);
            return t;
        })

                        .section("valuta", "amount")
                        .match("^(Rücküberweisung Inland) (\\d+\\.\\d+\\.\\d{4}) (?<valuta>\\d+\\.\\d+\\.\\d{4}) -(?<amount>[\\d.]+,\\d{2}) (\\w{3})")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            t.setDateTime(asDate(v.get("valuta")));
                            t.setAmount(asAmount(v.get("amount")));
                        }).wrap(t -> new TransactionItem(t)));

        Block feesBlock = new Block(
                        "(Vermögensverwaltungshonorar) (\\d+,) (\\d+\\.\\d+\\.\\d{4}) - (\\d+\\.\\d+\\.\\d{4}) (\\d+\\.\\d+\\.\\d{4}) (\\d+\\.\\d+\\.\\d{4}) (-[\\d.]+,\\d{2}) (\\w{3})");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })

                        .section("valuta", "amount")
                        .match("(Vermögensverwaltungshonorar) (\\d+,) (\\d+\\.\\d+\\.\\d{4}) - (\\d+\\.\\d+\\.\\d{4}) (\\d+\\.\\d+\\.\\d{4}) (?<valuta>\\d+\\.\\d+\\.\\d{4}) -(?<amount>[\\d.]+,\\d{2}) (\\w{3})")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            t.setDateTime(asDate(v.get("valuta")));
                            t.setAmount(asAmount(v.get("amount")));
                        }).wrap(t -> new TransactionItem(t)));
        
        
//Wertpapier Kauf, Ref.: 174680182 10.12.2020 14.12.2020 -428,06 EUR
//Xtr.(IE)MSCI World Value Registered Shares 1C USD o.N.
//IE00BL25JM42, ST 16,091
        
//Wertpapier Kauf, Ref.: 133305911 03.06.2020 05.06.2020 -408,26 EUR
//AIS-Amundi MSCI EMERG.MARKETS Namens-Anteile C Cap.EUR
//o.N.
//LU1681045370, ST 102,054
        
        Block buyBlock = new Block("(Wertpapier Kauf, Ref.: \\d+) (\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) (-[\\d.]+,\\d{2}) (\\w{3}).*");
        type.addBlock(buyBlock);
        buyBlock.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })

                        .section("isin", "name")
                        .match("(Wertpapier Kauf, Ref.: \\d+) (\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) (-[\\d.]+,\\d{2}) (\\w{3}).*")
                        .match("^(?<name>.*)$")
                        .match("((^o.N.\\n)|(^))(?<isin>.{12}).*").assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            v.put("currency", context.get("currency"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("amount", "date", "shares")
                        .match("(Wertpapier Kauf, Ref.: \\d+) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+) -(?<amount>[\\d.]+,\\d{2}) (\\w{3}).*")
                        .match("^(.*)$").match("^(.{12}, ST) (?<shares>[\\d.,]+).*").assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDate(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        }).wrap(BuySellEntryItem::new));
        
        
//Thesaurierung, Ref.: 111353113 25.02.2020 02.01.2020 -0,40 EUR
//SPDR MSCI Wrld Small Cap U.ETF Registered Shares o.N.
//IE00BCBJG560, ST 28,476
//KEST: EUR -0,38, SOLI: EUR -0,02
        
//Thesaurierung, Ref.: 111727648 25.02.2020 02.01.2020 -0,49 EUR
//AIS-Amundi MSCI EMERG.MARKETS Namens-Anteile C Cap.EUR
//o.N.
//LU1681045370, ST 449,231
//KEST: EUR -0,47, SOLI: EUR -0,02
        
        Block taxpayblock = new Block("(Thesaurierung, Ref.: \\d+) (\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) (-[\\d.]+,\\d{2}) (\\w{3}).*");
        type.addBlock(taxpayblock);

        Transaction<AccountTransaction> taxVorabpauschaleTransaction = new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAXES);
                            return entry;
                        })

                        .section("name", "isin","date") //
                        .match("(Thesaurierung, Ref.: \\d+) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+) (-[\\d.]+,\\d{2}) (\\w{3}).*")
                        .match("^(?<name>.*)$")
                        .match("((^o.N.\\n)|(^))(?<isin>.{12}).*") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("tax", "currency").optional()
                        .match("(Thesaurierung, Ref.: \\d+) (\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) (?<tax>-[\\d.]+,\\d{2}) (?<currency>\\w{3}).*")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("tax")));
                        })

                        .wrap(t -> t.getAmount() != 0 ? new TransactionItem(t)
                                        : new NonImportableItem("Steuerpflichtige Vorabpauschale mit 0 "
                                                        + t.getCurrencyCode()));

        taxpayblock.set(taxVorabpauschaleTransaction);

        
//Erträgnisabrechnung, Ref.: 169958419 30.11.2020 27.11.2020 156,33 EUR
//Amundi Index Solu.-A.PRIME GL. Nam.-Ant.UCI.ETF DR USD Dis.oN
//LU1931974692, ST 481,99
//KEST: EUR -1,81, SOLI: EUR -0,09
        
//Erträgnisabrechnung, Ref.: 169992104 27.11.2020 27.11.2020 54,86 EUR
//Amundi I.S.-A.PRIME EURO CORP. Nam.-Ant.UC.ETF DR EUR
//Dis.oN
//LU1931975079, ST 189,172
//KEST: EUR -0,01
        
//Erträgnisabrechnung, Ref.: 87991231 27.06.2019 27.06.2019 11,54 EUR
//I.M.-I.S&P 500 UETF Reg.Shares Dist o.N.
//IE00BYML9W36, ST 106,167
//KEST: USD -2,83, SOLI: USD -0,15
        
        Block dividendBlock = new Block("^Erträgnisabrechnung, Ref.: \\d+ (\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) ([\\d.]+,\\d{2}).*");
        type.addBlock(dividendBlock);
        dividendBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DIVIDENDS);
            return t;
        })

                        .section("isin", "name").match("^Erträgnisabrechnung, Ref.: \\d+ (\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) ([\\d.]+,\\d{2})(.*)$")
                        .match("^(?<name>.*)$").match("((^Dis.oN\\n)|(^))(?<isin>.{12})(.*)").assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            v.put("currency", context.get("currency"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("amount", "date", "shares")
                        .match("^Erträgnisabrechnung, Ref.: \\d+ (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+) (?<amount>[\\d.]+,\\d{2})(.*)$")
                        .match("^(.*)$")
                        .match("((^Dis.oN\\n)|(^))(.{12}, ST) (?<shares>[\\d.,]+)(.*)")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        .section("tax", "currency").optional()
                        .match("^Erträgnisabrechnung, Ref.: \\d+ (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+) (?<amount>[\\d.]+,\\d{2})(.*)$")
                        .match("^(.*)$")
                        .match("^((Dis.oN\\n)|())(.{12}, ST) ([\\d.,]+)(.*)$")
                        .match("^(.*)(KEST: )(?<currency>\\w{3}) -(?<tax>[\\d.]+,\\d{2}).*")
                        .assign((t, v) -> {//t.addUnit(new Unit(Unit.Type.TAX, asAmount(v.get("tax"))));
                                            Money taxes = Money.of(asCurrencyCode("EUR"),
  //TODO other currencys not working                         Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))
                                                            asAmount(v.get("tax")));
                                            //.subtract(t.getMonetaryAmount()).toMoney();
                                          t.addUnit(new Unit(Unit.Type.TAX, taxes));})
                        //.assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX, asAmount(v.get("tax")))))
//                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("tax", "currency").optional()
                        .match("^Erträgnisabrechnung, Ref.: \\d+ (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+) (?<amount>[\\d.]+,\\d{2})(.*)$")
                        .match("^((Dis.oN\\n)|())(.{12}, ST) ([\\d.,]+)(.*)$")
                        .match("^(.*)(SOLI: )(?<currency>\\w{3}) -(?<tax>[\\d.]+,\\d{2})(.*)$")
                        .assign((t, v) -> {//t.addUnit(new Unit(Unit.Type.TAX, asAmount(v.get("tax"))));
                                            Money taxes = Money.of(asCurrencyCode("EUR"),
  //TODO other currencys not working                         Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))
                                                            asAmount(v.get("tax")));
                                            //.subtract(t.getMonetaryAmount()).toMoney();
                                          t.addUnit(new Unit(Unit.Type.TAX, taxes));})
                        .wrap(t -> new TransactionItem(t)));
        

//Wertpapier Kauf, Ref.: 85249245 08.05.2019 10.05.2019 -235,86 EUR
//SPDR MSCI Wrld Small Cap U.ETF Registered Shares o.N.
//IE00BCBJG560, ST 3,648
        
        Block sellBlock = new Block("(Wertpapier Verkauf, Ref.: \\d+) (\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) ([\\d.]+,\\d{2}) (\\w{3}).*");
        type.addBlock(sellBlock);
        sellBlock.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        })

                        .section("isin", "name", "amount", "date", "shares")
                        .match("(Wertpapier Verkauf, Ref.: \\d+) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+) (?<amount>[\\d.]+,\\d{2}) (\\w{3}).*")
                        .match("^(?<name>.*)$")
                        .match("^((Dis.oN\\n)|())((?<isin>.{12}), ST) -(?<shares>[\\d.,]+)(.*)")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            v.put("currency", context.get("currency"));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDate(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

//                        .section("amount", "date", "shares")
//                        .match("(Wertpapier Verkauf, Ref.: \\d+) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+) (?<amount>[\\d.]+,\\d{2}) (\\w{3}).*")
//                        .match("^(.*)$")
//                        .match("^(.{12}, ST) -(?<shares>[\\d.,]+)(.*)")
//                        .assign((t, v) -> {
//                            Map<String, String> context = type.getCurrentContext();
//                        })
                        
                        
//Erträgnisabrechnung, Ref.: 119876044 09.04.2020 08.04.2020 0,94 EUR
//Vanguard FTSE Japan UCITS ETF Registered Shares USD Dis.oN
//IE00B95PGT31, ST 31,633
//KEST: USD -0,22, SOLI: USD -0,01
                        .section("kest", "soli", "currency").optional()
                        .match("(Wertpapier Verkauf, Ref.: \\d+) (\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) ([\\d.]+,\\d{2}) (\\w{3}).*")
                        .match("^(?<name>.*)$")
                        .match("^((Dis.oN\\n)|())((?<isin>.{12}), ST) -(?<shares>[\\d.,]+)(.*)")
                        .match("^(KEST: )(?<currency>\\w{3}) -(?<kest>[\\d.]+,\\d{2})((, SOLI: EUR -(?<soli>[\\d.]+,\\d{2}))|(\\n))")
//TODO bug: If no tax info is available it catches a value from another block.
//E.g. this section is followed by Steueroptimierung and then Erträgnisabrechnung. 1,9 EUR will be added to Wertpapier Verkauf and also Erträgnisabrechnung 
//I do not understand why. minimal example:
                        
//Wertpapier Verkauf, Ref.: 106663542 03.01.2020 07.01.2020 110,86 EUR
//Dimensional Fds-Emerg.Mkts Va. Registered Shares EUR Dis.o.N.
//IE00B42THM37, ST -10,763
//Steueroptimierung 03.01.2020 03.01.2020 0,40 EUR
//Ref.: 107410183
//Erträgnisabrechnung, Ref.: 108905624 30.01.2020 29.01.2020 5,35 EUR
//iShsIII-EO Corp Bd 1-5yr U.ETF Registered Shares o.N.
//IE00B4L60045, ST 21,296
//KEST: EUR -1,81, SOLI: EUR -0,09
                        
                        .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode("EUR"), asAmount(v.get("kest")))));
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode("EUR"), asAmount(v.get("soli")))));
                            })
//TODO other currencys not working                         Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))
                        
                        .wrap(BuySellEntryItem::new));
        
        

    }

    @Override
    public String getLabel()
    {
        return "Quirion"; //$NON-NLS-1$
    }
}

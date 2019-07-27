package name.abuchen.portfolio.json;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import name.abuchen.portfolio.json.impl.LocalDateSerializer;
import name.abuchen.portfolio.json.impl.LocalTimeSerializer;
import name.abuchen.portfolio.model.TransactionPair;

public class JClient
{
    private static final Gson GSON = new GsonBuilder() //
                    .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
                    .registerTypeAdapter(LocalTime.class, new LocalTimeSerializer())
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();

    private int version = 1;

    private List<JTransaction> transactions;

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public Stream<JTransaction> getTransactions()
    {
        return transactions == null ? Stream.empty() : transactions.stream();
    }

    public void addTransaction(JTransaction transaction)
    {
        if (transactions == null)
            transactions = new ArrayList<>();

        transactions.add(transaction);
    }

    public String toJson()
    {
        return GSON.toJson(this);
    }

    public static JClient from(List<TransactionPair<?>> transactions)
    {
        JClient client = new JClient();
        transactions.stream().map(JTransaction::from).forEach(client::addTransaction);
        return client;
    }
}

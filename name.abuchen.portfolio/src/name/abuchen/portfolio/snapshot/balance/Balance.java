package name.abuchen.portfolio.snapshot.balance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;

public class Balance
{
    public static class Proposal
    {
        private final String category;
        private final List<Transaction> candidates;

        public Proposal(String category, List<Transaction> candidates)
        {
            this.category = category;
            this.candidates = candidates;
        }

        public String getCategory()
        {
            return category;
        }

        public List<Transaction> getCandidates()
        {
            return candidates;
        }
    }

    private final LocalDate date;
    private final Money value;
    private final List<Proposal> proposals = new ArrayList<>();

    public Balance(LocalDate date, Money value)
    {
        this.date = date;
        this.value = value;
    }

    public LocalDate getDate()
    {
        return date;
    }

    public Money getValue()
    {
        return value;
    }

    public void addProposal(Proposal proposal)
    {
        this.proposals.add(proposal);
    }

    public List<Proposal> getProposals()
    {
        return this.proposals;
    }
}

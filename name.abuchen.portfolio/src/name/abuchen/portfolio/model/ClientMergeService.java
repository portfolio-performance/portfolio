package name.abuchen.portfolio.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class ClientMergeService
{

    private int securitiesReused;
    private int securitiesAdded;
    private int accountsImported;
    private int portfoliosImported;
    private int investmentPlansImported;
    private int watchlistsImported;

    public ClientMergeResult merge(List<File> files, Map<File, char[]> passwords, IProgressMonitor monitor)
                    throws IOException
    {
        if (files == null || files.isEmpty())
            throw new IllegalArgumentException("files must not be empty"); //$NON-NLS-1$

        securitiesReused = 0;
        securitiesAdded = 0;
        accountsImported = 0;
        portfoliosImported = 0;
        investmentPlansImported = 0;
        watchlistsImported = 0;

        IProgressMonitor progress = monitor != null ? monitor : new NullProgressMonitor();

        File firstFile = files.get(0);
        Client target = ClientFactory.load(firstFile, passwords.get(firstFile), progress);

        for (int i = 1; i < files.size(); i++)
        {
            File sourceFile = files.get(i);
            Client source = ClientFactory.load(sourceFile, passwords.get(sourceFile), progress);
            mergeInto(target, source);
        }

        return new ClientMergeResult(target, files.size(), securitiesReused, securitiesAdded, accountsImported,
                        portfoliosImported, investmentPlansImported, watchlistsImported);
    }

    private void mergeInto(Client target, Client source)
    {

        Map<Security, Security> securityMapping = mergeSecurities(target, source);

        remapSecurityReferences(source, securityMapping);
        regenerateUUIDs(source);

        mergeAccounts(target, source);
        mergePortfolios(target, source);
        mergeWatchlists(target, source);
        mergeInvestmentPlans(target, source);

        // bewusst ignoriert:
        // - Taxonomies
        // - Dashboards
    }

    private Map<Security, Security> mergeSecurities(Client target, Client source)
    {
        List<Security> originalTargetSecurities = new java.util.ArrayList<>(target.getSecurities());

        Map<SecurityKey, Integer> targetKeyCounts = countSecurityKeys(originalTargetSecurities);
        Map<SecurityKey, Integer> sourceKeyCounts = countSecurityKeys(source.getSecurities());

        Map<SecurityKey, Security> uniqueTargetByKey = buildUniqueSecurityIndex(originalTargetSecurities,
                        targetKeyCounts);
        Map<Security, Security> securityMapping = new HashMap<>();

        for (Security sourceSecurity : source.getSecurities())
        {
            Security existing = findMatchingSecurity(sourceSecurity, source, originalTargetSecurities, sourceKeyCounts,
                            targetKeyCounts, uniqueTargetByKey);

            if (existing != null)
            {
                securityMapping.put(sourceSecurity, existing);
                securitiesReused++;
                continue;
            }

            target.addSecurity(sourceSecurity);
            securityMapping.put(sourceSecurity, sourceSecurity);
            securitiesAdded++;
        }

        return securityMapping;
    }

    private void remapSecurityReferences(Client source, Map<Security, Security> securityMapping)
    {
        remapAccountTransactions(source, securityMapping);
        remapPortfolioTransactions(source, securityMapping);
        remapWatchlists(source, securityMapping);
        remapInvestmentPlans(source, securityMapping);
    }

    private void remapAccountTransactions(Client source, Map<Security, Security> securityMapping)
    {
        for (Account account : source.getAccounts())
        {
            for (AccountTransaction transaction : account.getTransactions())
            {
                Security oldSecurity = transaction.getSecurity();
                Security mappedSecurity = securityMapping.get(oldSecurity);

                if (mappedSecurity != null && mappedSecurity != oldSecurity)
                    transaction.setSecurity(mappedSecurity);
            }
        }
    }

    private void remapPortfolioTransactions(Client source, Map<Security, Security> securityMapping)
    {
        for (Portfolio portfolio : source.getPortfolios())
        {
            for (PortfolioTransaction transaction : portfolio.getTransactions())
            {
                Security oldSecurity = transaction.getSecurity();
                Security mappedSecurity = securityMapping.get(oldSecurity);

                if (mappedSecurity != null && mappedSecurity != oldSecurity)
                    transaction.setSecurity(mappedSecurity);
            }
        }
    }

    private void remapWatchlists(Client source, Map<Security, Security> securityMapping)
    {
        for (Watchlist watchlist : source.getWatchlists())
        {
            List<Security> securities = watchlist.getSecurities();

            for (int i = 0; i < securities.size(); i++)
            {
                Security oldSecurity = securities.get(i);
                Security mappedSecurity = securityMapping.get(oldSecurity);

                if (mappedSecurity != null && mappedSecurity != oldSecurity)
                    securities.set(i, mappedSecurity);
            }
        }
    }

    private void remapInvestmentPlans(Client source, Map<Security, Security> securityMapping)
    {
        for (InvestmentPlan plan : source.getPlans())
        {
            Security oldSecurity = plan.getSecurity();
            Security mappedSecurity = securityMapping.get(oldSecurity);

            if (mappedSecurity != null && mappedSecurity != oldSecurity)
                plan.setSecurity(mappedSecurity);

            for (Transaction transaction : plan.getTransactions())
            {
                Security txSecurity = transaction.getSecurity();
                Security mappedTxSecurity = securityMapping.get(txSecurity);

                if (mappedTxSecurity != null && mappedTxSecurity != txSecurity)
                    transaction.setSecurity(mappedTxSecurity);
            }
        }
    }

    private void regenerateUUIDs(Client source)
    {
        regenerateAccountUUIDs(source);
        regeneratePortfolioUUIDs(source);
        regenerateInvestmentPlanTransactionUUIDs(source);
    }

    private void regenerateAccountUUIDs(Client source)
    {
        for (Account account : source.getAccounts())
        {
            account.generateUUID();

            for (AccountTransaction transaction : account.getTransactions())
                transaction.generateUUID();
        }
    }

    private void regeneratePortfolioUUIDs(Client source)
    {
        for (Portfolio portfolio : source.getPortfolios())
        {
            portfolio.generateUUID();

            for (PortfolioTransaction transaction : portfolio.getTransactions())
                transaction.generateUUID();
        }
    }

    private void regenerateInvestmentPlanTransactionUUIDs(Client source)
    {
        for (InvestmentPlan plan : source.getPlans())
        {
            for (Transaction transaction : plan.getTransactions())
                transaction.generateUUID();
        }
    }

    private void mergeAccounts(Client target, Client source)
    {
        for (Account account : source.getAccounts())
        {
            target.addAccount(account);
            accountsImported++;
        }
    }

    private void mergePortfolios(Client target, Client source)
    {
        for (Portfolio portfolio : source.getPortfolios())
        {
            target.addPortfolio(portfolio);
            portfoliosImported++;
        }
    }

    private void mergeWatchlists(Client target, Client source)
    {
        for (Watchlist watchlist : source.getWatchlists())
        {
            target.addWatchlist(watchlist);
            watchlistsImported++;
        }
    }

    private void mergeInvestmentPlans(Client target, Client source)
    {
        for (InvestmentPlan plan : source.getPlans())
        {
            target.addPlan(plan);
            investmentPlansImported++;
        }
    }

    private Map<SecurityKey, Integer> countSecurityKeys(List<Security> securities)
    {
        Map<SecurityKey, Integer> counts = new HashMap<>();

        for (Security security : securities)
        {
            for (SecurityKey key : buildKeysInPriorityOrder(security))
                counts.merge(key, 1, Integer::sum);
        }

        return counts;
    }

    private Map<SecurityKey, Security> buildUniqueSecurityIndex(List<Security> securities,
                    Map<SecurityKey, Integer> counts)
    {
        Map<SecurityKey, Security> index = new HashMap<>();

        for (Security security : securities)
        {
            for (SecurityKey key : buildKeysInPriorityOrder(security))
            {
                if (counts.getOrDefault(key, 0) == 1)
                    index.put(key, security);
            }
        }

        return index;
    }

    private Security findMatchingSecurity(Security sourceSecurity, Client source,
                    List<Security> originalTargetSecurities,
                    Map<SecurityKey, Integer> sourceCounts, Map<SecurityKey, Integer> targetCounts,
                    Map<SecurityKey, Security> uniqueTargetByKey)
    {
        for (SecurityKey key : buildKeysInPriorityOrder(sourceSecurity))
        {
            boolean uniqueInSource = sourceCounts.getOrDefault(key, 0) == 1;
            boolean uniqueInTarget = targetCounts.getOrDefault(key, 0) == 1;

            if (!uniqueInSource || !uniqueInTarget)
                continue;

            Security match = uniqueTargetByKey.get(key);

            if (match != null)
                return match;
        }

        return findByWknOnly(sourceSecurity, source, originalTargetSecurities);
    }

    private Security findByWknOnly(Security sourceSecurity, Client source, List<Security> originalTargetSecurities)
    {
        String sourceWkn = normalize(sourceSecurity.getWkn());

        if (sourceWkn == null)
            return null;

        int sourceMatches = 0;
        for (Security security : source.getSecurities())
        {
            if (sourceWkn.equals(normalize(security.getWkn())))
                sourceMatches++;
        }

        if (sourceMatches != 1)
            return null;

        Security targetMatch = null;
        int targetMatches = 0;

        for (Security security : originalTargetSecurities)
        {
            if (sourceWkn.equals(normalize(security.getWkn())))
            {
                targetMatches++;
                targetMatch = security;
            }
        }

        return targetMatches == 1 ? targetMatch : null;
    }

    private List<SecurityKey> buildKeysInPriorityOrder(Security security)
    {
        List<SecurityKey> keys = new java.util.ArrayList<>(6);

        SecurityKey fullKey = SecurityKey.forIsinWknCurrency(security);
        SecurityKey isinCurrencyKey = SecurityKey.forIsinCurrency(security);
        SecurityKey wknCurrencyKey = SecurityKey.forWknCurrency(security);
        SecurityKey isinWknKey = SecurityKey.forIsinWkn(security);
        SecurityKey isinOnlyKey = SecurityKey.forIsinOnly(security);

        if (fullKey != null)
            keys.add(fullKey);

        if (isinCurrencyKey != null)
            keys.add(isinCurrencyKey);

        if (wknCurrencyKey != null)
            keys.add(wknCurrencyKey);

        if (isinWknKey != null)
            keys.add(isinWknKey);

        if (isinOnlyKey != null)
            keys.add(isinOnlyKey);

        return keys;
    }

    private static String normalize(String value)
    {
        if (value == null)
            return null;

        String normalized = value.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    private enum SecurityMatchLevel
    {
        ISIN_WKN_CURRENCY, ISIN_CURRENCY, WKN_CURRENCY, ISIN_WKN, ISIN_ONLY, WKN_ONLY
    }

    private static final class SecurityKey
    {
        private final SecurityMatchLevel level;
        private final String isin;
        private final String wkn;
        private final String currency;

        private SecurityKey(SecurityMatchLevel level, String isin, String wkn, String currency)
        {
            this.level = level;
            this.isin = isin;
            this.wkn = wkn;
            this.currency = currency;
        }

        static SecurityKey forIsinWknCurrency(Security security)
        {
            String isin = normalize(security.getIsin());
            String wkn = normalize(security.getWkn());
            String currency = normalize(security.getCurrencyCode());

            if (isin == null || wkn == null || currency == null)
                return null;

            return new SecurityKey(SecurityMatchLevel.ISIN_WKN_CURRENCY, isin, wkn, currency);
        }

        static SecurityKey forIsinCurrency(Security security)
        {
            String isin = normalize(security.getIsin());
            String currency = normalize(security.getCurrencyCode());

            if (isin == null || currency == null)
                return null;

            return new SecurityKey(SecurityMatchLevel.ISIN_CURRENCY, isin, null, currency);
        }

        static SecurityKey forWknCurrency(Security security)
        {
            String wkn = normalize(security.getWkn());
            String currency = normalize(security.getCurrencyCode());

            if (wkn == null || currency == null)
                return null;

            return new SecurityKey(SecurityMatchLevel.WKN_CURRENCY, null, wkn, currency);
        }

        static SecurityKey forIsinWkn(Security security)
        {
            String isin = normalize(security.getIsin());
            String wkn = normalize(security.getWkn());

            if (isin == null || wkn == null)
                return null;

            return new SecurityKey(SecurityMatchLevel.ISIN_WKN, isin, wkn, null);
        }

        static SecurityKey forIsinOnly(Security security)
        {
            String isin = normalize(security.getIsin());

            if (isin == null)
                return null;

            return new SecurityKey(SecurityMatchLevel.ISIN_ONLY, isin, null, null);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(level, isin, wkn, currency);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;

            if (!(obj instanceof SecurityKey other))
                return false;

            return Objects.equals(level, other.level) && Objects.equals(isin, other.isin)
                            && Objects.equals(wkn, other.wkn)
                            && Objects.equals(currency, other.currency);
        }

        @Override
        public String toString()
        {
            return String.valueOf(level) + "|" + isin + "|" + wkn + "|" + currency; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
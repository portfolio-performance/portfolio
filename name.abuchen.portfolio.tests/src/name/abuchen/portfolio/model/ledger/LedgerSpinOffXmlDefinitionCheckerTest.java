package name.abuchen.portfolio.model.ledger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinitionRegistry;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEventParameterDefinition;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingTypeDefinitionRegistry;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerParameterRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerPostingRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerRequirementGroup;

@SuppressWarnings("nls")
public final class LedgerSpinOffXmlDefinitionCheckerTest
{
    private static final Path XML_EXAMPLE = Path
                    .of("name.abuchen.portfolio.tests", "src", "name", "abuchen", "portfolio", "model", "ledger",
                                    "ledger-v6-spin-off-siemens-energy-example.xml");

    /**
     * Checks the persistence scenario: siemens energy xml example matches static ledger definitions.
     * The loaded model must rebuild visible rows from ledger truth.
     * This protects compatibility data from becoming a second transaction source.
     */
    @Test
    public void testSiemensEnergyXmlExampleMatchesStaticLedgerDefinitions() throws IOException
    {
        var report = check(XML_EXAMPLE);

        System.out.println(report.markdown());

        assertTrue(report.structuralValidationOK());
        assertEquals(0, report.summary().unknownParameterCount());
        assertEquals(0, report.summary().valueKindMismatchCount());
        assertEquals(0, report.summary().notAllowedCount());
        assertEquals(0, report.summary().definitionGapCount());
        assertEquals(1, report.spinOffEntryCount());
    }

    public static Report check(Path xmlFile) throws IOException
    {
        xmlFile = resolve(xmlFile);

        var client = ClientFactory.load(Files.newInputStream(xmlFile));
        var structuralResult = LedgerStructuralValidator.validate(client.getLedger());
        var summary = new Summary();
        var sections = new ArrayList<String>();
        var codeDomainRows = new ArrayList<Row>();
        var spinOffEntryCount = 0;

        for (var entry : client.getLedger().getEntries())
        {
            if (entry.getType() == LedgerEntryType.SPIN_OFF)
                spinOffEntryCount++;

            var definition = entry.getType() == null ? null
                            : LedgerEntryDefinitionRegistry.lookup(entry.getType()).orElse(null);

            sections.add(entrySection(entry, definition, codeDomainRows, summary));

            for (var posting : entry.getPostings())
                sections.add(postingSection(entry, posting, definition, codeDomainRows, summary));
        }

        return new Report(xmlFile, structuralResult, sections, codeDomainRows, summary, spinOffEntryCount);
    }

    private static Path resolve(Path file)
    {
        var current = Path.of("").toAbsolutePath();

        while (current != null)
        {
            var candidate = current.resolve(file);

            if (Files.exists(candidate))
                return candidate;

            current = current.getParent();
        }

        return Path.of("").toAbsolutePath().resolve(file);
    }

    private static String entrySection(LedgerEntry entry, LedgerEntryDefinition definition,
                    List<Row> codeDomainRows, Summary summary)
    {
        var builder = new StringBuilder();
        var allowed = definition == null ? Set.<LedgerParameterType>of() : definition.getEntryParameterTypes();

        builder.append("## LedgerEntry `").append(entry.getUUID()).append("`\n\n");
        builder.append("* Entry type: `").append(entry.getType()).append("`\n");
        builder.append("* Definition: `")
                        .append(definition == null ? "missing" : definition.getEntryType().name())
                        .append("`\n\n");
        builder.append("### Entry Parameters\n\n");
        builder.append("| Parameter | ValueKind | Expected | Rule | Alternative group | Code domain | Result |\n");
        builder.append("| --- | --- | --- | --- | --- | --- | --- |\n");

        for (var parameter : entry.getParameters())
        {
            var type = parameter.getType();
            var definitionAllowed = type != null && allowed.contains(type);
            var rule = definition == null || type == null ? "-"
                            : parameterRule(definition.getEntryParameterRules(), type);
            var alternative = definition == null || type == null ? "-"
                            : alternativeGroups(definition.getAlternativeRequirementGroups(), type);
            var result = parameterResult(parameter, definitionAllowed, false, summary);

            appendParameterRow(builder, parameter, rule, alternative, result);
            appendCodeDomainRow(codeDomainRows, "entry " + entry.getUUID(), parameter, summary);
        }

        if (entry.getParameters().isEmpty())
            builder.append("| _none_ | | | | | | |\n");

        builder.append("\n");
        builder.append("Allowed entry parameters from `LedgerEntryDefinition`: ")
                        .append(formatTypes(allowed)).append("\n\n");

        return builder.toString();
    }

    private static String postingSection(LedgerEntry entry, LedgerPosting posting, LedgerEntryDefinition entryDefinition,
                    List<Row> codeDomainRows, Summary summary)
    {
        var builder = new StringBuilder();
        var entryPostingTypes = entryDefinition == null ? Set.<LedgerPostingType>of()
                        : entryDefinition.getPostingTypes();
        var entryPostingParameters = entryDefinition == null ? Set.<LedgerParameterType>of()
                        : entryDefinition.getPostingParameterTypes();
        var postingDefinition = posting.getType() == null ? null
                        : LedgerPostingTypeDefinitionRegistry.lookup(posting.getType()).orElse(null);
        var postingParameters = postingDefinition == null ? Set.<LedgerParameterType>of()
                        : postingDefinition.getComponentParameterTypes();
        var postingRule = entryDefinition == null || posting.getType() == null ? null
                        : postingRule(entryDefinition.getPostingRules(), posting.getType());
        var postingTypeAllowed = entryDefinition == null
                        || posting.getType() != null && entryPostingTypes.contains(posting.getType());

        if (entryDefinition != null && !postingTypeAllowed)
            summary.definitionGapCount++;

        builder.append("## LedgerPosting `").append(posting.getUUID()).append("`\n\n");
        builder.append("* Posting type: `").append(posting.getType()).append("`\n");
        builder.append("* Posting type allowed by entry definition: `")
                        .append(entryDefinition == null ? "not applicable" : postingTypeAllowed).append("`\n");
        builder.append("* Posting rule: `").append(postingRule == null ? "-" : postingRule.getRequirement())
                        .append("`\n\n");
        builder.append("### Posting Parameters\n\n");
        builder.append("| Parameter | ValueKind | Expected | EntryDefinition | PostingTypeDefinition | Rule | Code domain | Result |\n");
        builder.append("| --- | --- | --- | --- | --- | --- | --- | --- |\n");

        for (var parameter : posting.getParameters())
        {
            var type = parameter.getType();
            var entryAllowed = type != null && entryPostingParameters.contains(type);
            var postingAllowed = type != null && postingParameters.contains(type);
            var result = parameterResult(parameter, entryAllowed, postingAllowed, summary);
            var rule = postingRuleDescription(postingRule, type);

            builder.append("| `").append(type).append("` | `").append(parameter.getValueKind())
                            .append("` | `").append(type == null ? "<missing>" : type.getExpectedValueKind())
                            .append("` | ").append(definitionStatus(entryAllowed)).append(" | ")
                            .append(definitionStatus(postingAllowed)).append(" | ")
                            .append(escape(rule)).append(" | ")
                            .append(codeDomain(parameter)).append(" | ").append(result).append(" |\n");

            appendCodeDomainRow(codeDomainRows, "posting " + posting.getUUID(), parameter, summary);
        }

        if (posting.getParameters().isEmpty())
            builder.append("| _none_ | | | | | | | |\n");

        builder.append("\n");
        builder.append("Entry-definition posting parameter vocabulary: ")
                        .append(formatTypes(entryPostingParameters)).append("\n\n");
        builder.append("Posting-type definition parameter vocabulary: ")
                        .append(formatTypes(postingParameters)).append("\n\n");

        return builder.toString();
    }

    private static String parameterResult(LedgerParameter<?> parameter, boolean definitionAllowed,
                    boolean postingDefinitionAllowed, Summary summary)
    {
        var type = parameter.getType();

        if (type == null)
        {
            summary.unknownParameterCount++;
            return "`UNKNOWN_PARAMETER`";
        }

        if (parameter.getValueKind() == null || parameter.getValue() == null
                        || !type.supportsValueKind(parameter.getValueKind())
                        || !parameter.getValueKind().supportsValue(parameter.getValue()))
        {
            summary.valueKindMismatchCount++;
            return "`VALUE_KIND_MISMATCH`";
        }

        if (!definitionAllowed && !postingDefinitionAllowed)
        {
            summary.definitionGapCount++;
            return "`DEFINITION_GAP`";
        }

        summary.okCount++;

        if (definitionAllowed && postingDefinitionAllowed)
            return "`OK_BOTH_DEFINITIONS`";

        if (definitionAllowed)
            return "`OK_ENTRY_DEFINITION_ONLY`";

        return "`OK_POSTING_TYPE_DEFINITION_ONLY`";
    }

    private static void appendParameterRow(StringBuilder builder, LedgerParameter<?> parameter, String rule,
                    String alternative, String result)
    {
        var type = parameter.getType();

        builder.append("| `").append(type).append("` | `").append(parameter.getValueKind()).append("` | `")
                        .append(type == null ? "<missing>" : type.getExpectedValueKind()).append("` | ")
                        .append(escape(rule)).append(" | ").append(escape(alternative)).append(" | ")
                        .append(codeDomain(parameter)).append(" | ").append(result).append(" |\n");
    }

    private static void appendCodeDomainRow(List<Row> rows, String owner, LedgerParameter<?> parameter, Summary summary)
    {
        var type = parameter.getType();

        if (type == null || !type.hasCodeDomain())
            return;

        var value = String.valueOf(parameter.getValue());
        var domain = type.getCodeDomain();
        var allowed = domain.allows(value);

        if (!allowed)
            summary.notAllowedCount++;

        rows.add(new Row(owner, type.name(), value, domain.name(),
                        String.join(", ", domain.getAllowedCodes()), allowed ? "OK" : "NOT_ALLOWED"));
    }

    private static String codeDomain(LedgerParameter<?> parameter)
    {
        var type = parameter.getType();

        if (type == null || !type.hasCodeDomain())
            return "`-`";

        return "`" + type.getCodeDomain() + "`";
    }

    private static String parameterRule(Set<LedgerParameterRule> rules, LedgerParameterType type)
    {
        for (var rule : rules)
            if (rule.getParameterType() == type)
                return rule.getRequirement() + (rule.isRepeatable() ? ", repeatable" : "");

        return "-";
    }

    private static String postingRuleDescription(LedgerPostingRule postingRule, LedgerParameterType type)
    {
        if (postingRule == null || type == null)
            return "-";

        if (postingRule.getRequiredParameterTypes().contains(type))
            return "REQUIRED";

        if (postingRule.getOptionalParameterTypes().contains(type))
            return "OPTIONAL";

        return "-";
    }

    private static LedgerPostingRule postingRule(Set<LedgerPostingRule> rules, LedgerPostingType type)
    {
        for (var rule : rules)
            if (rule.getPostingType() == type)
                return rule;

        return null;
    }

    private static String alternativeGroups(Set<LedgerRequirementGroup> groups, LedgerParameterType type)
    {
        var names = groups.stream().filter(group -> group.getParameterTypes().contains(type))
                        .map(group -> group.getName() + " " + group.getRequirement())
                        .collect(Collectors.joining(", "));

        return names.isBlank() ? "-" : names;
    }

    private static String definitionStatus(boolean allowed)
    {
        return allowed ? "`allowed`" : "`not listed`";
    }

    private static String formatTypes(Set<LedgerParameterType> types)
    {
        if (types.isEmpty())
            return "`-`";

        return types.stream().map(type -> "`" + type.name() + "`").collect(Collectors.joining(", "));
    }

    private static String escape(String value)
    {
        return value == null || value.isBlank() ? "`-`" : "`" + value.replace("|", "\\|") + "`";
    }

    public record Report(Path xmlFile, LedgerStructuralValidator.ValidationResult structuralResult,
                    List<String> sections, List<Row> codeDomainRows, Summary summary, int spinOffEntryCount)
    {
        public boolean structuralValidationOK()
        {
            return structuralResult.isOK();
        }

        public String markdown()
        {
            var builder = new StringBuilder();

            builder.append("# Ledger Spin-Off XML Definition Check\n\n");
            builder.append("* XML file: `").append(xmlFile).append("`\n");
            builder.append("* XML structural load / LedgerStructuralValidator: `")
                            .append(structuralResult.isOK() ? "OK" : "NOT_OK").append("`\n");
            builder.append("* Spin-off entries: `").append(spinOffEntryCount).append("`\n\n");
            builder.append("This diagnostic reads the XML through `ClientFactory`, inspects `Client#getLedger()`, ")
                            .append("and compares the stored `LedgerParameter` values against the static ")
                            .append("Ledger definition registries. It reports required, optional, and alternative ")
                            .append("definition metadata, but it does not implement productive corporate-action ")
                            .append("completeness validation.\n\n");
            builder.append("## Summary\n\n");
            builder.append("| Counter | Value |\n");
            builder.append("| --- | ---: |\n");
            builder.append("| OK | ").append(summary.okCount()).append(" |\n");
            builder.append("| NOT_ALLOWED | ").append(summary.notAllowedCount()).append(" |\n");
            builder.append("| UNKNOWN_PARAMETER | ").append(summary.unknownParameterCount()).append(" |\n");
            builder.append("| VALUE_KIND_MISMATCH | ").append(summary.valueKindMismatchCount()).append(" |\n");
            builder.append("| DEFINITION_GAP | ").append(summary.definitionGapCount()).append(" |\n\n");
            builder.append("## Static Event Parameter Vocabulary\n\n");
            builder.append(formatTypes(EnumSet.copyOf(LedgerEventParameterDefinition.getParameterTypes())))
                            .append("\n\n");
            builder.append(String.join("\n", sections));
            builder.append("## Code Domain Results\n\n");
            builder.append("| Owner | ParameterType | XML value | CodeDomain | Allowed codes | Result |\n");
            builder.append("| --- | --- | --- | --- | --- | --- |\n");

            for (var row : codeDomainRows)
                builder.append("| ").append(escapeCell(row.owner())).append(" | `").append(row.parameterType())
                                .append("` | `").append(escapeCell(row.value())).append("` | `")
                                .append(row.codeDomain()).append("` | ")
                                .append(escapeCell(row.allowedCodes())).append(" | `").append(row.result())
                                .append("` |\n");

            if (codeDomainRows.isEmpty())
                builder.append("| _none_ | | | | | |\n");

            builder.append("\n## Boundary Statement\n\n");
            builder.append("The diagnostic separates XML structural load, generic `LedgerStructuralValidator` ")
                            .append("result, definition vocabulary checks, and code-domain checks. It does not ")
                            .append("change production validation and does not state that Portfolio Performance ")
                            .append("productively enforces corporate-action completeness.\n");

            return builder.toString();
        }

        private static String escapeCell(String value)
        {
            return value.replace("|", "\\|");
        }
    }

    public static final class Summary
    {
        private int okCount;
        private int notAllowedCount;
        private int unknownParameterCount;
        private int valueKindMismatchCount;
        private int definitionGapCount;

        public int okCount()
        {
            return okCount;
        }

        public int notAllowedCount()
        {
            return notAllowedCount;
        }

        public int unknownParameterCount()
        {
            return unknownParameterCount;
        }

        public int valueKindMismatchCount()
        {
            return valueKindMismatchCount;
        }

        public int definitionGapCount()
        {
            return definitionGapCount;
        }
    }

    public record Row(String owner, String parameterType, String value, String codeDomain, String allowedCodes,
                    String result)
    {
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;

/* package */final class PDFParser
{
    /* package */static class DocumentType
    {
        private List<Pattern> mustInclude = new ArrayList<>();

        private List<Block> blocks = new ArrayList<>();
        private Map<String, String> context = new HashMap<>();
        private BiConsumer<Map<String, String>, String[]> contextProvider;

        public DocumentType(List<Pattern> mustInclude)
        {
            this.mustInclude.addAll(mustInclude);
        }

        public DocumentType(String mustInclude)
        {
            this(mustInclude, null);
        }

        public DocumentType(String mustInclude, BiConsumer<Map<String, String>, String[]> contextProvider)
        {
            this.mustInclude.add(Pattern.compile(mustInclude));
            this.contextProvider = contextProvider;
        }

        public boolean matches(String text)
        {
            for (Pattern pattern : mustInclude)
            {
                if (!pattern.matcher(text).find())
                    return false;
            }

            return true;
        }

        public void addBlock(Block block)
        {
            blocks.add(block);
        }

        public List<Block> getBlocks()
        {
            return blocks;
        }

        /**
         * Gets the current context for this parse run.
         * 
         * @return current context map
         */
        public Map<String, String> getCurrentContext()
        {
            return context;
        }

        public void parse(String filename, List<Item> items, String text)
        {
            String[] lines = text.split("\\r?\\n"); //$NON-NLS-1$

            // reset context and parse it from this file
            context.clear();
            parseContext(context, lines);

            for (Block block : blocks)
                block.parse(filename, items, lines);
        }

        /**
         * Parses the current context.
         * 
         * @param context
         *            context map
         * @param lines
         *            content lines of the file
         */
        private void parseContext(Map<String, String> context, String[] lines)
        {
            // if a context provider is given call it, else parse the current
            // context in a subclass
            if (contextProvider != null)
            {
                contextProvider.accept(context, lines);
            }
        }
    }

    /* package */static class Block
    {
        private Pattern startsWith;
        private Pattern endsWith;
        private Transaction<?> transaction;

        public Block(String startsWith)
        {
            this(startsWith, null);
        }

        public Block(String startsWith, String endsWith)
        {
            this.startsWith = Pattern.compile(startsWith);

            if (endsWith != null)
                this.endsWith = Pattern.compile(endsWith);
        }

        public void set(Transaction<?> transaction)
        {
            this.transaction = transaction;
        }

        public void parse(String filename, List<Item> items, String[] lines)
        {
            List<Integer> blocks = new ArrayList<>();

            for (int ii = 0; ii < lines.length; ii++)
            {
                Matcher matcher = startsWith.matcher(lines[ii]);
                if (matcher.matches())
                    blocks.add(ii);
            }

            for (int ii = 0; ii < blocks.size(); ii++)
            {
                int startLine = blocks.get(ii);
                int endLine = ii + 1 < blocks.size() ? blocks.get(ii + 1) - 1 : lines.length - 1;

                // if an "endsWith" pattern exists, check if the block might end
                // earlier

                if (endsWith != null)
                {
                    endLine = findBlockEnd(lines, startLine, endLine);
                    if (endLine == -1)
                        continue;
                }

                transaction.parse(filename, items, lines, startLine, endLine);
            }
        }

        private int findBlockEnd(String[] lines, int startLine, int endLine)
        {
            for (int lineNo = startLine; lineNo <= endLine; lineNo++)
            {
                Matcher matcher = endsWith.matcher(lines[lineNo]);
                if (matcher.matches())
                    return lineNo;
            }

            return -1;
        }
    }

    /* package */static class Transaction<T>
    {
        private Supplier<T> supplier;
        private Function<T, Item> wrapper;
        private List<Section<T>> sections = new ArrayList<>();

        public Transaction<T> subject(Supplier<T> supplier)
        {
            this.supplier = supplier;
            return this;
        }

        public Section<T> section(String... attributes)
        {
            Section<T> section = new Section<>(this, attributes);
            sections.add(section);
            return section;
        }

        @SafeVarargs
        public final Transaction<T> oneOf(Function<Section<T>, Transaction<T>>... alternatives)
        {
            List<Section<T>> subSections = new ArrayList<>();
            for (Function<Section<T>, Transaction<T>> function : alternatives)
            {
                Section<T> s = new Section<>(this, null);
                function.apply(s);
                subSections.add(s);
            }

            sections.add(new Section<T>(this, null)
            {
                @Override
                public void parse(String filename, String[] lines, int lineNo, int lineNoEnd, T target)
                {
                    List<String> errors = new ArrayList<>();

                    for (Section<T> section : subSections)
                    {
                        try
                        {
                            section.parse(filename, lines, lineNo, lineNoEnd, target);

                            // if parsing was successful, then return
                            return;
                        }
                        catch (IllegalArgumentException ignore)
                        {
                            // try next sub-section
                            errors.add(ignore.getMessage());
                        }
                    }

                    throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorNoneOfSubSectionsMatched,
                                    String.valueOf(subSections.size()), String.join("; ", errors), lineNo + 1, //$NON-NLS-1$
                                    lineNoEnd + 1));
                }
            });
            return this;
        }

        public Transaction<T> wrap(Function<T, Item> wrapper)
        {
            this.wrapper = wrapper;
            return this;
        }

        public void parse(String filename, List<Item> items, String[] lines, int lineNoStart, int lineNoEnd)
        {
            T target = supplier.get();

            for (Section<T> section : sections)
                section.parse(filename, lines, lineNoStart, lineNoEnd, target);

            if (wrapper == null)
                throw new IllegalArgumentException("Wrapping function missing"); //$NON-NLS-1$

            Item item = wrapper.apply(target);
            if (item != null)
                items.add(item);
        }
    }

    /* package */static class Section<T>
    {
        private boolean isOptional = false;
        private boolean isMultipleTimes = false;
        private Transaction<T> transaction;
        private String[] attributes;
        private List<Pattern> pattern = new ArrayList<>();
        private BiConsumer<T, Map<String, String>> assignment;

        public Section(Transaction<T> transaction, String[] attributes)
        {
            this.transaction = transaction;
            this.attributes = attributes;
        }

        public Section<T> attributes(String... attributes)
        {
            this.attributes = attributes;
            return this;
        }

        public Section<T> optional()
        {
            this.isOptional = true;
            return this;
        }

        public Section<T> multipleTimes()
        {
            this.isMultipleTimes = true;
            return this;
        }

        public Section<T> find(String string)
        {
            pattern.add(Pattern.compile("^" + string + "$")); //$NON-NLS-1$ //$NON-NLS-2$
            return this;
        }

        public Section<T> match(String regex)
        {
            pattern.add(Pattern.compile(regex));
            return this;
        }

        public Transaction<T> assign(BiConsumer<T, Map<String, String>> assignment)
        {
            this.assignment = assignment;
            return transaction;
        }

        public void parse(String filename, String[] lines, int lineNo, int lineNoEnd, T target)
        {
            if (assignment == null)
                throw new IllegalArgumentException("Assignment function missing"); //$NON-NLS-1$

            Map<String, String> values = new HashMap<>();

            int patternNo = 0;
            boolean sectionFoundAtLeastOnce = false;
            for (int ii = lineNo; ii <= lineNoEnd; ii++)
            {
                Pattern p = pattern.get(patternNo);
                Matcher m = p.matcher(lines[ii]);
                if (m.matches())
                {

                    // extract attributes
                    extractAttributes(values, p, m);

                    // next pattern?
                    patternNo++;
                    if (patternNo >= pattern.size())
                    {
                        // all pattern matched:

                        if (values.size() != attributes.length)
                            throw new IllegalArgumentException(MessageFormat.format(
                                            Messages.MsgErrorMissingValueMatches, values.keySet().toString(),
                                            Arrays.toString(attributes), filename, lineNo + 1, lineNoEnd + 1));

                        assignment.accept(target, values);

                        // if there might be multiple occurrences that match,
                        // the found values need to be added and the search
                        // continues through all lines with the same patterns
                        if (isMultipleTimes)
                        {
                            // continue searching with first pattern
                            sectionFoundAtLeastOnce = true;
                            patternNo = 0;
                        }
                        else
                        {
                            break;
                        }
                    }
                }
            }

            if (patternNo < pattern.size() && !sectionFoundAtLeastOnce && !isOptional)
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorNotAllPatternMatched,
                                patternNo, pattern.size(), pattern.toString(), filename, lineNo + 1, lineNoEnd + 1));
        }

        private void extractAttributes(Map<String, String> values, Pattern p, Matcher m)
        {
            for (String attribute : attributes)
            {
                if (p.pattern().contains("<" + attribute + ">")) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    String v = m.group(attribute);
                    if (v != null)
                        values.put(attribute, v);
                }
            }
        }
    }

    private PDFParser()
    {
    }
}

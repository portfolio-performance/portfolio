package name.abuchen.portfolio.datatransfer.pdf;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.DocumentContext;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.model.TypedMap;

/* package */ final class PDFParser
{
    /* package */ static class DocumentType
    {
        private List<Pattern> mustInclude = new ArrayList<>();
        private List<Pattern> mustNotInclude = new ArrayList<>();

        private List<Block> blocks = new ArrayList<>();
        private DocumentContext context = new DocumentContext();
        private BiConsumer<DocumentContext, String[]> contextProvider;
        private Consumer<Transaction<DocumentContext>> contextBuilder;

        public DocumentType(String mustInclude)
        {
            this.mustInclude.add(Pattern.compile(mustInclude));
        }

        public DocumentType(String mustInclude, Consumer<Transaction<DocumentContext>> contextBuilder)
        {
            this.mustInclude.add(Pattern.compile(mustInclude));
            this.contextBuilder = contextBuilder;
        }

        public DocumentType(String mustInclude, String mustNotInclude)
        {
            this.mustInclude.add(Pattern.compile(mustInclude));
            this.mustNotInclude.add(Pattern.compile(mustNotInclude));
        }

        public DocumentType(String mustInclude, BiConsumer<DocumentContext, String[]> contextProvider)
        {
            this.mustInclude.add(Pattern.compile(mustInclude));
            this.contextProvider = contextProvider;
        }

        public DocumentType(String mustInclude, String mustNotInclude,
                        BiConsumer<DocumentContext, String[]> contextProvider)
        {
            this.mustInclude.add(Pattern.compile(mustInclude));
            this.mustNotInclude.add(Pattern.compile(mustNotInclude));
            this.contextProvider = contextProvider;
        }

        public boolean matches(String text)
        {
            // Check if the text matches the mustInclude patterns
            for (Pattern pattern : mustInclude)
            {
                if (!pattern.matcher(text).find())
                    return false;
            }

            // Check if matches mustNotInclude to skip processing
            for (Pattern pattern : mustNotInclude)
            {
                if (pattern.matcher(text).find())
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
        public DocumentContext getCurrentContext()
        {
            return context;
        }

        public void parse(String filename, List<Item> items, String text)
        {
            String[] lines = text.split("\\r?\\n"); //$NON-NLS-1$

            // reset context and parse it from this file
            context.clear();
            parseContext(filename, context, lines);

            for (Block block : blocks)
                block.parse(filename, context, items, lines);
        }

        /**
         * Parses the current context.
         * 
         * @param context
         *            context map
         * @param lines
         *            content lines of the file
         */
        private void parseContext(String filename, DocumentContext context, String[] lines)
        {
            // if a context provider is given call it, else parse the current
            // context in a subclass
            if (contextProvider != null)
            {
                contextProvider.accept(context, lines);
            }

            if (contextBuilder != null)
            {
                var builder = new Transaction<DocumentContext>() //
                                .subject(() -> context).wrap(ctx -> null);
                contextBuilder.accept(builder);
                builder.parse(filename, context, Collections.emptyList(), lines, 0, lines.length - 1);
            }
        }
    }

    /* package */static class Block
    {
        private Pattern startsWith;
        private Pattern endsWith;
        private int maxSize = -1;
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

        /**
         * Sets the maximum number of lines matched to this block
         */
        public void setMaxSize(int maxSize)
        {
            this.maxSize = maxSize;
        }

        public void set(Transaction<?> transaction)
        {
            this.transaction = transaction;
        }

        public void parse(String filename, DocumentContext documentContext, List<Item> items, String[] lines)
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

                if (maxSize >= 0)
                {
                    // the endLine is included in the parsing -> remove one
                    // number
                    endLine = Math.min(endLine, startLine + maxSize - 1);
                }

                transaction.parse(filename, documentContext, items, lines, startLine, endLine);
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
        private BiFunction<T, TypedMap, Item> wrapper;
        private List<Section<T>> sections = new ArrayList<>();
        private List<Consumer<T>> concludes = new ArrayList<>();

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

        /**
         * The document must contain one of the sections. The sections are
         * matched in order of the definition and the first matching section is
         * used. If no section is matching, the parsing is aborted.
         */
        @SafeVarargs
        public final Transaction<T> oneOf(Function<Section<T>, Transaction<T>>... alternatives)
        {
            return internalOneOf(false, alternatives);
        }

        /**
         * The document must contain at most one of the sections. The sections
         * are matched in order of the definition and the first matching section
         * is used. If no section is matching, parsing continues with the next
         * sections.
         */
        @SafeVarargs
        public final Transaction<T> optionalOneOf(Function<Section<T>, Transaction<T>>... alternatives)
        {
            return internalOneOf(true, alternatives);
        }

        @SafeVarargs
        private final Transaction<T> internalOneOf(boolean isOptional,
                        Function<Section<T>, Transaction<T>>... alternatives)
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
                public void parse(String filename, DocumentContext documentContext, String[] lines, int lineNo,
                                int lineNoEnd, TypedMap ctx, T target)
                {
                    List<String> errors = new ArrayList<>();

                    for (Section<T> section : subSections)
                    {
                        try
                        {
                            section.parse(filename, documentContext, lines, lineNo, lineNoEnd, ctx, target);

                            // if parsing was successful, then return
                            return;
                        }
                        catch (IllegalArgumentException ignore)
                        {
                            // try next sub-section
                            errors.add(ignore.getMessage());
                        }
                    }

                    if (!isOptional)
                        throw new IllegalArgumentException(MessageFormat.format(
                                        Messages.MsgErrorNoneOfSubSectionsMatched, String.valueOf(subSections.size()),
                                        String.join("; ", errors), lineNo + 1, //$NON-NLS-1$
                                        lineNoEnd + 1));
                }
            });
            return this;
        }

        public Transaction<T> conclude(Consumer<T> conclude)
        {
            this.concludes.add(conclude);
            return this;
        }

        public Transaction<T> wrap(Function<T, Item> wrapper)
        {
            return wrap((t, c) -> wrapper.apply(t));
        }

        public Transaction<T> wrap(BiFunction<T, TypedMap, Item> wrapper)
        {
            this.wrapper = wrapper;
            return this;
        }

        public void parse(String filename, DocumentContext documentContext, List<Item> items, String[] lines,
                        int lineNoStart, int lineNoEnd)
        {
            TypedMap txContext = new TypedMap();

            T target = supplier.get();

            for (Section<T> section : sections)
                section.parse(filename, documentContext, lines, lineNoStart, lineNoEnd, txContext, target);

            for (Consumer<T> conclude : concludes)
                conclude.accept(target);

            if (wrapper == null)
                throw new IllegalArgumentException("Wrapping function missing"); //$NON-NLS-1$

            Item item = wrapper.apply(target, txContext);
            if (item != null)
                items.add(item);
        }
    }

    /* package */static class ParsedData implements Map<String, String>
    {
        private final Map<String, String> base;
        private final int startLineNumber;
        private final int endLineNumber;
        private final String fileName;
        private final TypedMap txContext;

        private ParsedData(Map<String, String> base, int startLineNumber, int endLineNumber, String fileName,
                        TypedMap txContext)
        {
            this.base = base;
            this.startLineNumber = startLineNumber;
            this.endLineNumber = endLineNumber;
            this.fileName = fileName;
            this.txContext = txContext;
        }

        public int getStartLineNumber()
        {
            return startLineNumber;
        }

        public int getEndLineNumber()
        {
            return endLineNumber;
        }

        public String getFileName()
        {
            return fileName;
        }

        /**
         * Returns the transactions context, a hash map that exist for as long
         * as one transaction is parsed. It can be used to exchange data between
         * sections.
         */
        public TypedMap getTransactionContext()
        {
            return txContext;
        }

        @Override
        public String put(String key, String value)
        {
            return base.put(key, value);
        }

        @Override
        public int size()
        {
            return base.size();
        }

        @Override
        public boolean isEmpty()
        {
            return base.isEmpty();
        }

        @Override
        public boolean containsKey(Object key)
        {
            return base.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value)
        {
            return base.containsValue(value);
        }

        @Override
        public String get(Object key)
        {
            return base.get(key);
        }

        @Override
        public String remove(Object key)
        {
            return base.remove(key);
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> m)
        {
            base.putAll(m);
        }

        @Override
        public void clear()
        {
            base.clear();
        }

        @Override
        public Set<String> keySet()
        {
            return base.keySet();
        }

        @Override
        public Collection<String> values()
        {
            return base.values();
        }

        @Override
        public Set<Entry<String, String>> entrySet()
        {
            return base.entrySet();
        }
    }

    /* package */static class Section<T>
    {
        private boolean isOptional = false;
        private boolean isMultipleTimes = false;
        private Transaction<T> transaction;
        /** attributes extracted from regular pattern */
        private String[] attributes;
        /** attributes mixed in from the document context */
        private String[] documentAttributes;

        private List<Pattern> pattern = new ArrayList<>();
        private BiConsumer<T, ParsedData> assignment;

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

        public Section<T> documentContext(String... documentAttributes)
        {
            this.documentAttributes = documentAttributes;
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

        public Transaction<T> assign(BiConsumer<T, ParsedData> assignment)
        {
            this.assignment = assignment;
            return transaction;
        }

        public void parse(String filename, DocumentContext documentContext, String[] lines, int lineNo, int lineNoEnd,
                        TypedMap txContext, T target)
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

                        // enrich extracted values with context values
                        if (documentAttributes != null)
                        {
                            for (String attribute : documentAttributes)
                            {
                                if (!documentContext.containsKey(attribute))
                                {
                                    throw new IllegalArgumentException(MessageFormat.format(
                                                    Messages.MsgErrorMissingValueMatches, values.keySet().toString(),
                                                    attribute, filename, lineNo + 1, lineNoEnd + 1));
                                }

                                values.put(attribute, documentContext.get(attribute));
                            }
                        }

                        assignment.accept(target, new ParsedData(values, lineNo, lineNoEnd, filename, txContext));

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
                throw new IllegalArgumentException(Arrays.toString(attributes) + MessageFormat.format(Messages.MsgErrorNotAllPatternMatched,
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

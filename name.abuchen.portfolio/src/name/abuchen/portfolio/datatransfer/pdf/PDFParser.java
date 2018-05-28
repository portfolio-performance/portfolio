package name.abuchen.portfolio.datatransfer.pdf;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;

/* package */final class PDFParser
{
    /* package */static class DocumentType
    {
        private String mustInclude;
        private String mustExclude;

        private List<Block> blocks = new ArrayList<>();
        private Map<String, String> context = new HashMap<>();
        private BiConsumer<Map<String, String>, String[]> contextProvider;

        public DocumentType(String mustInclude)
        {
            this(mustInclude, null);
        }

        public DocumentType(String mustInclude, BiConsumer<Map<String, String>, String[]> contextProvider)
        {
            this.mustInclude = mustInclude;
            this.contextProvider = contextProvider;
        }

        public boolean matches(String text)
        {
            if (!text.contains(mustInclude))
                return false;

            if (mustExclude != null)
                return !text.contains(mustExclude);

            return true;
        }

        public void setMustExclude(String mustExclude)
        {
            this.mustExclude = mustExclude;
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
            parseContext(context, filename, lines);

            for (Block block : blocks)
                block.parse(filename, items, lines);
        }

        /**
         * Parses the current context and could be overridden in a subclass to
         * fill the context.
         * 
         * @param context
         *            context map
         * @param filename
         *            current filename
         * @param lines
         *            content lines of the file
         */
        protected void parseContext(Map<String, String> context, String filename, String[] lines)
        {
            // if a context provider is given call it, else parse the current
            // context in a subclass
            if (contextProvider != null)
            {
                contextProvider.accept(context, lines);
            }
        }

       public String getMustInclude()
       {
           return mustInclude;
       }
    }

    /* package */static class Block
    {
        private Pattern marker;
        private Transaction<?> transaction;

        public Block(String marker)
        {
            this.marker = Pattern.compile(marker);
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
                Matcher matcher = marker.matcher(lines[ii]);
                if (matcher.matches())
                    blocks.add(ii);
            }

            for (int ii = 0; ii < blocks.size(); ii++)
            {
                int startLine = blocks.get(ii);
                int endLine = ii + 1 < blocks.size() ? blocks.get(ii + 1) - 1 : lines.length - 1;

                transaction.parse(filename, items, lines, startLine, endLine);
            }
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

        public Transaction<T> wrap(Function<T, Item> wrapper)
        {
            this.wrapper = wrapper;
            return this;
        }

        public void parse(String filename, List<Item> items, String[] lines, int lineNoStart, int lineNoEnd)
        {
            T target = supplier.get();
            List<Entry<Integer, Boolean>> result = new ArrayList<>();

            for (Section<T> section : sections)
            {
                if(section.isGroup)
                {
                    try
                    {
                        section.parse(filename, lines, lineNoStart, lineNoEnd, target);
                        result.add(new AbstractMap.SimpleEntry<>(section.getGroupID(), true));
                    }
                    catch(Exception ex)
                    {
                        result.add(new AbstractMap.SimpleEntry<>(section.getGroupID(), false));
                    }
                }
                else
                {
                    section.parse(filename, lines, lineNoStart, lineNoEnd, target);
                }
            }
            
            result.stream()
                .collect(Collectors.groupingBy(Entry::getKey, Collectors.toList()))
                .forEach((k,v) -> 
                {
                    if(!v.stream().anyMatch(Entry::getValue))
                        throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorNoSectionOfGroupMatched, k));                 
                });            
            
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
        private boolean isGroup = false;
        private int groupID;
        private Transaction<T> transaction;
        private String[] attributes;
        private List<Pattern> pattern = new ArrayList<>();
        private BiConsumer<T, Map<String, String>> assignment;

        public Section(Transaction<T> transaction, String[] attributes)
        {
            this.transaction = transaction;
            this.attributes = attributes;
        }

        public Section<T> optional()
        {
            this.isOptional = true;
            return this;
        }

        public Section<T> grouping(int groupID)
        {
            this.isGroup = true;
            this.groupID = groupID;
            return this;
        }

        public boolean isGroup()
        {
            return this.isGroup;
        }

        public int getGroupID()
        {
            return this.groupID;
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
            Map<String, String> values = new HashMap<>();

            int patternNo = 0;
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
                        break;
                }
            }

            if (patternNo < pattern.size())
            {
                // if section is optional, ignore if patterns do not match
                if (isOptional)
                    return;

                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorNotAllPatternMatched,
                                patternNo, pattern.size(), pattern.toString(), filename));
            }

            if (values.size() != attributes.length)
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorMissingValueMatches,
                                values.keySet().toString(), Arrays.toString(attributes), filename));

            if (assignment == null)
                throw new IllegalArgumentException("Assignment function missing"); //$NON-NLS-1$

            assignment.accept(target, values);
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
    {}
}

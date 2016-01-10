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
        private String marker;
        private List<Block> blocks = new ArrayList<Block>();

        public DocumentType(String marker)
        {
            this.marker = marker;
        }

        public boolean matches(String text)
        {
            return text.contains(marker);
        }

        public void addBlock(Block block)
        {
            blocks.add(block);
        }

        public List<Block> getBlocks()
        {
            return blocks;
        }

        public void parse(String filename, List<Item> items, String text)
        {
            String[] lines = text.split("\\r?\\n"); //$NON-NLS-1$

            for (Block block : blocks)
                block.parse(filename, items, lines);
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
            for (int ii = 0; ii < lines.length; ii++)
            {
                Matcher matcher = marker.matcher(lines[ii]);
                if (matcher.matches())
                    transaction.parse(filename, items, lines, ii);
            }
        }
    }

    /* package */static class Transaction<T>
    {
        private Supplier<T> supplier;
        private Function<T, Item> wrapper;
        private List<Section<T>> sections = new ArrayList<Section<T>>();

        public Transaction<T> subject(Supplier<T> supplier)
        {
            this.supplier = supplier;
            return this;
        }

        public Section<T> section(String... attributes)
        {
            Section<T> section = new Section<T>(this, attributes);
            sections.add(section);
            return section;
        }

        public Transaction<T> wrap(Function<T, Item> wrapper)
        {
            this.wrapper = wrapper;
            return this;
        }

        public void parse(String filename, List<Item> items, String[] lines, int lineNo)
        {
            T target = supplier.get();

            for (Section<T> section : sections)
                section.parse(filename, items, lines, lineNo, target);

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
        private Transaction<T> transaction;
        private String[] attributes;
        private List<Pattern> pattern = new ArrayList<Pattern>();
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

        public void parse(String filename, List<Item> items, String[] lines, int lineNo, T target)
        {
            Map<String, String> values = new HashMap<String, String>();

            int patternNo = 0;
            for (int ii = lineNo; ii < lines.length; ii++)
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

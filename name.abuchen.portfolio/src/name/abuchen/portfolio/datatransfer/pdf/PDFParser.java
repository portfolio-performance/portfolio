package name.abuchen.portfolio.datatransfer.pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.Extractor.Item;

/* package */final class PDFParser
{
    /* package */static class DocumentType
    {
        private String mustInclude;
        private String mustExclude;

        List<Block> blocks = new ArrayList<>();
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
        Pattern marker;
        Transaction<?> transaction;

        public Block(String marker)
        {
            this.marker = Pattern.compile(marker);
        }

        public void set(Transaction<?> transaction)
        {
            this.transaction = transaction;
        }

    }

    /* package */static class Transaction<T>
    {
        Supplier<T> supplier;
        Function<T, Item> wrapper;
        List<Section<T>> sections = new ArrayList<>();

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
    }

    /* package */static class Section<T>
    {
        boolean isOptional = false;
        private Transaction<T> transaction;
        String[] attributes;
        List<Pattern> pattern = new ArrayList<>();
        BiConsumer<T, PDFExtractionContext> assignment;

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

        public Transaction<T> assign(BiConsumer<T, PDFExtractionContext> assignment)
        {
            this.assignment = assignment;
            return transaction;
        }

        void extractAttributes(Map<String, String> values, Pattern p, Matcher m)
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

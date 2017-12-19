package name.abuchen.portfolio.datatransfer.pdf;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.model.Client;

public abstract class AbstractPDFExtractor implements Extractor
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.GERMANY); //$NON-NLS-1$

    private final List<String> bankIdentifier = new ArrayList<>();
    private final List<DocumentType> documentTypes = new ArrayList<>();

    protected final void addDocumentTyp(DocumentType type)
    {
        this.documentTypes.add(type);
    }

    protected final void addBankIdentifier(String identifier)
    {
        this.bankIdentifier.add(identifier);
    }

    public List<String> getBankIdentifier()
    {
        return bankIdentifier;
    }

    public String getPDFAuthor()
    {
        return null;
    }

    @Override
    public String getFileExtension()
    {
        return "pdf"; //$NON-NLS-1$
    }
    
    @Override
    public List<Item> extract(Client client, List<Extractor.InputFile> files, List<Exception> errors)
    {
        SecurityCache securityCache = new SecurityCache(client);

        List<Item> results = new ArrayList<>();
        for (InputFile f : files)
        {
            PDFExtractionContext context = newExtractionContext(client, securityCache);
            if (!(f instanceof PDFInputFile))
                throw new IllegalArgumentException();

            PDFInputFile inputFile = (PDFInputFile) f;

            String text = inputFile.getText();
            results.addAll(extract(context, inputFile.getFile().getName(), text, errors));
        }

        results.addAll(securityCache.createMissingSecurityItems(results));

        return results;
    }

    protected PDFExtractionContext newExtractionContext(Client client, SecurityCache securityCache)
    {
        return new PDFExtractionContext(client, securityCache);
    }

    private final List<Item> extract(PDFExtractionContext context, String filename, String text, List<Exception> errors)
    {
        try
        {
            checkBankIdentifier(filename, text);

            List<Item> items = parseDocumentTypes(context, documentTypes, filename, text);

            if (items.isEmpty())
            {
                errors.add(new UnsupportedOperationException(
                                MessageFormat.format(Messages.PDFdbMsgCannotDetermineFileType, filename)));
            }

            for (Item item : items)
                item.getSubject().setNote(filename);

            return items;
        }
        catch (IllegalArgumentException e)
        {
            errors.add(new IllegalArgumentException(e.getMessage() + " @ " + filename, e)); //$NON-NLS-1$
            return Collections.emptyList();
        }
        catch (UnsupportedOperationException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }

    protected final List<Item> parseDocumentTypes(PDFExtractionContext context, List<DocumentType> list, String filename,
                    String text)
    {
        List<Item> items = new ArrayList<>();
        for (DocumentType type : list)
        {
            if (type.matches(text))
                context.parseDocumentType(type, filename, items, text);
        }
        return items;
    }

    private void checkBankIdentifier(String filename, String text)
    {
        if (bankIdentifier.isEmpty())
            bankIdentifier.add(getLabel());

        for (String identifier : bankIdentifier)
            if (text.contains(identifier))
                return;

        throw new UnsupportedOperationException( //
                        MessageFormat.format(Messages.PDFMsgFileNotSupported, filename, getLabel()));
    }

    /* protected */LocalDate asDate(String value)
    {
        return LocalDate.parse(value, DATE_FORMAT);
    }
}

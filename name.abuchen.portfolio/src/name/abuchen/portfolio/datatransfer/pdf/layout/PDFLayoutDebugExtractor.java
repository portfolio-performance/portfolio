package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.io.File;
import java.io.IOException;

import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutBcbcDebugTextExtractor;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentDocument;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentDocumentExtractor;

public final class PDFLayoutDebugExtractor
{
    public record PDFLayoutDebugSummary(int pages, int rows, int blocks, int tableCandidates,
                    int keyValueGroupCandidates, int multiSegmentRowCandidates, int textBlockCandidates)
    {
    }

    public String extract(File file) throws IOException
    {
        return extract(file, PDFLayoutDebugMode.STANDARD);
    }

    public String extract(File file, PDFLayoutDebugMode mode) throws IOException
    {
        PDFLayoutSegmentDocument document = new PDFLayoutSegmentDocumentExtractor().extract(file);
        PDFLayoutStructure structure = new PDFLayoutStructureBuilder().build(document);

        StringBuilder out = new StringBuilder();

        if (mode == PDFLayoutDebugMode.FULL)
        {
            out.append(new PDFLayoutBcbcDebugTextExtractor().extract(file));
            out.append('\n');
        }

        if (mode == PDFLayoutDebugMode.FULL || mode == PDFLayoutDebugMode.STRUCTURE)
        {
            out.append("==================================================\n"); //$NON-NLS-1$
            out.append("PDF DOCUMENT STRUCTURE\n"); //$NON-NLS-1$
            out.append("==================================================\n"); //$NON-NLS-1$
            out.append(new PDFLayoutDebugDocumentStructureBuilder().build(document));
            out.append('\n');

            out.append("==================================================\n"); //$NON-NLS-1$
            out.append("PDF DATA STRUCTURE\n"); //$NON-NLS-1$
            out.append("==================================================\n"); //$NON-NLS-1$
            out.append(new PDFLayoutDebugDataStructureBuilder().build(structure));
        }

        if (mode == PDFLayoutDebugMode.SUMMARY)
        {
            PDFLayoutDebugSummary summary = summarize(file);

            out.append("PDF LAYOUT SUMMARY\n"); //$NON-NLS-1$
            out.append("pages=").append(summary.pages()).append('\n'); //$NON-NLS-1$
            out.append("rows=").append(summary.rows()).append('\n'); //$NON-NLS-1$
            out.append("blocks=").append(summary.blocks()).append('\n'); //$NON-NLS-1$
            out.append("tableCandidates=").append(summary.tableCandidates()).append('\n'); //$NON-NLS-1$
            out.append("keyValueGroupCandidates=").append(summary.keyValueGroupCandidates()).append('\n'); //$NON-NLS-1$
            out.append("multiSegmentRowCandidates=").append(summary.multiSegmentRowCandidates()).append('\n'); //$NON-NLS-1$
            out.append("textBlockCandidates=").append(summary.textBlockCandidates()).append('\n'); //$NON-NLS-1$
        }

        if (mode == PDFLayoutDebugMode.STANDARD)
        {
            out.append("==================================================\n"); //$NON-NLS-1$
            out.append("PDF LAYOUT EVIDENCE\n"); //$NON-NLS-1$
            out.append("==================================================\n"); //$NON-NLS-1$
            out.append(new PDFLayoutDebugEvidenceBuilder().build(structure));
        }

        return out.toString();
    }

    public PDFLayoutDebugSummary summarize(File file) throws IOException
    {
        PDFLayoutSegmentDocument document = new PDFLayoutSegmentDocumentExtractor().extract(file);
        String yaml = new PDFLayoutDebugDocumentStructureBuilder().build(document);

        return new PDFLayoutDebugSummary(document.rows().stream().mapToInt(row -> row.page()).max().orElse(0),
                        document.rows().size(), document.blocks().size(), count(yaml, "table_candidate_"), //$NON-NLS-1$
                        count(yaml, "key_value_group_candidate_"), count(yaml, "multi_segment_row_candidate_"), //$NON-NLS-1$//$NON-NLS-2$
                        count(yaml, "text_block_candidate_")); //$NON-NLS-1$
    }

    private int count(String text, String token)
    {
        int count = 0;
        int offset = 0;

        while ((offset = text.indexOf(token, offset)) >= 0)
        {
            count++;
            offset += token.length();
        }

        return count;
    }
}

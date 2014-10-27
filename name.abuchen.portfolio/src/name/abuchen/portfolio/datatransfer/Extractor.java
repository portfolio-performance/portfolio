package comdirect;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public class Extractor {

	public enum ExtractType {
		GUTSCHRIFT, WERTPAPIERKAUF
	}

	public class ExtractResult {

		ExtractType type;
		Date date;
		String isin;
		Map<String, Object> attributes;

		public ExtractResult(ExtractType type, Date date, String isin) {
			super();
			this.type = type;
			this.date = date;
			this.isin = isin;
			attributes = new HashMap<String, Object>();
		}

		public void add(String key, Object value) {
			attributes.put(key, value);
		}

		public String toString() {
			return "" + type + "(" + isin + ")" + date;
		}
	}

	PDFTextStripper stripper;
	List<ExtractResult> results;
	DateFormat df;
	Pattern isinPattern;
	Matcher isinMatcher;
	NumberFormat format;

	public Extractor() {
		results = new ArrayList<ExtractResult>();
		df = new SimpleDateFormat("dd.MM.yyyy");
		isinPattern = Pattern.compile("[A-Z]{2}([A-Z0-9]){9}[0-9]");
		format = NumberFormat.getInstance(Locale.GERMANY);
		try {
			stripper = new PDFTextStripper();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void extract(Path path) {
		PDDocument doc;
		try {
			doc = PDDocument.load(path.toString());
			snif(FilenameUtils.getBaseName(path.toString()),
					stripper.getText(doc));
			doc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<ExtractResult> getResults() {
		return results;
	}

	private void snif(String filename, String text) {
		if (text.contains("Gutschrift fälliger Wertpapier-Erträge")) {
			snifErtragsgutschrift(text, filename);
		} else if (filename.contains("Wertpapierabrechnung_Kauf")) {
			snifWertpapierabrechnung(text, filename);
		} else {
			System.err.println("Could not snif type from text " + filename);
		}
	}

	private void snifWertpapierabrechnung(String text, String filename) {
		int tagPosition = text.indexOf("Geschäftstag");
		String tagString = text.substring(tagPosition + 20, tagPosition + 30);
		try {
			Date tag = df.parse(tagString);
			isinMatcher = isinPattern.matcher(text);
			String isin;
			if (isinMatcher.find()) {
				isin = isinMatcher.group();
			} else {
				throw new RuntimeException("ISIN could not be parsed");
			}
			int stueckLinePos = text
					.indexOf("\n", text.indexOf("Zum Kurs von"));
			String stText = text.substring(stueckLinePos + 6,
					stueckLinePos + 11);
			Number stueck = format.parse(stText);
			int kursEURPos = text.indexOf("EUR", stueckLinePos);
			String kursText = text.substring(kursEURPos + 5, kursEURPos + 11);
			Number kurs = format.parse(kursText);
			int totalEURPos = text.indexOf("EUR", kursEURPos + 11);
			String totalText = text.substring(totalEURPos + 18,
					totalEURPos + 23);
			Number total = format.parse(totalText);
			ExtractResult res = new ExtractResult(ExtractType.WERTPAPIERKAUF,
					tag, isin);
			res.add("Stueck", stueck);
			res.add("Kurs", kurs);
			res.add("Total", total);
			results.add(res);
		} catch (ParseException e) {
			System.err.println(filename);
			e.printStackTrace();
		}
	}

	private void snifErtragsgutschrift(String text, String filename) {
		// Thesaurierend? Do nothing!
		if (text.contains("Ertragsthesaurierung")) {
			return;
		}
		// Datum
		int perPosition = text.indexOf("per");
		String datumString = text.substring(perPosition + 4, perPosition + 14);
		Date d;
		String eurPart = "";
		try {
			d = df.parse(datumString);
			isinMatcher = isinPattern.matcher(text);
			String isin;
			if (isinMatcher.find()) {
				isin = isinMatcher.group();
			} else {
				throw new RuntimeException("ISIN could not be parsed");
			}
			// Loop the lines and try to find the EUR value
			String[] lines = text.split("\r\n|\r|\n");
			String eurLine = "";
			boolean snap = false;
			for (String line : lines) {
				if (snap) {
					eurLine = line;
					break;
				}
				if (line.contains("Zu Ihren Gunsten vor Steuern")) {
					snap = true;
				}
			}
			String[] parts = eurLine.split("EUR");
			eurPart = parts[parts.length - 1].trim();

			Number value = format.parse(eurPart);
			ExtractResult res = new ExtractResult(ExtractType.GUTSCHRIFT, d,
					isin);
			res.add("Wert", value);
			results.add(res);

		} catch (ParseException e) {
			System.err.println(filename);
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		Extractor e = new Extractor();
		try {
			DirectoryStream<Path> stream = Files.newDirectoryStream(Paths
					.get("/home/bastian/Google Drive/Comdirect_Postbox/"));
			for (Path child : stream) {
				File f = child.toFile();
				if (f.isDirectory()) {
				} else {
					if (!FilenameUtils.getExtension(child.toString()).equals(
							"pdf")) {
					} else {
						e.extract(child);
					}
				}

			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		for (ExtractResult result : e.getResults()) {
			System.out.println("Result: " + result);
		}
	}

}

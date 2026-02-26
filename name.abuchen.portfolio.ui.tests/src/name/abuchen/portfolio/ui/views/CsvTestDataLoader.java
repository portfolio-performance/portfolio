package name.abuchen.portfolio.ui.views;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVRecord;

import com.google.common.primitives.Doubles;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

class CsvTestDataLoader
{
    static Security loadSecurity(List<CSVRecord> records, DateTimeFormatter dateFormatter, String dateHeader,
                    NumberFormat numberFormat, String valueHeader) throws Exception
    {
        Security result = new Security();
        for (CSVRecord csvRecord : records)
        {
            String value = csvRecord.get(valueHeader);
            if (!value.isBlank())
            {
                result.addPrice(new SecurityPrice(LocalDate.parse(csvRecord.get(dateHeader), dateFormatter),
                                Values.Quote.factorize(numberFormat.parse(value).doubleValue())));
            }
        }
        return result;
    }

    static ChartLineSeriesAxes loadChartLineSeriesAxes(List<CSVRecord> records, DateTimeFormatter dateFormatter,
                    String dateHeader, NumberFormat numberFormat, String valueHeader) throws Exception
    {
        List<LocalDate> dates = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (CSVRecord csvRecord : records)
        {
            String value = csvRecord.get(valueHeader);
            if (!value.isBlank())
            {
                dates.add(LocalDate.parse(csvRecord.get(dateHeader), dateFormatter));
                values.add(numberFormat.parse(value).doubleValue());
            }
        }
        ChartLineSeriesAxes result = new ChartLineSeriesAxes();
        result.setDates(dates.toArray(new LocalDate[0]));
        result.setValues(Doubles.toArray(values));
        return result;
    }
}

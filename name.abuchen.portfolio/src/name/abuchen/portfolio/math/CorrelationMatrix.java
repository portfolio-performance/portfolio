package name.abuchen.portfolio.math;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;

import name.abuchen.portfolio.math.CorrelationMatrix.DataFrame;

public final class CorrelationMatrix<F extends DataFrame>
{
    public interface DataFrame
    {
        LocalDate[] dates();

        double[] values();

        IntPredicate filter();
    }

    public static class Correlation
    {
        private double r = Double.NaN;
        private int count;
        private LocalDate firstDate;
        private LocalDate lastDate;

        private Correlation()
        {
        }

        public double getR()
        {
            return r;
        }

        public int getCount()
        {
            return count;
        }

        public LocalDate getFirstDate()
        {
            return firstDate;
        }

        public LocalDate getLastDate()
        {
            return lastDate;
        }
    }

    private List<F> dataFrames = new ArrayList<F>();

    private Correlation data[][];

    public CorrelationMatrix(List<F> dataFrames)
    {
        this.dataFrames = dataFrames;

        this.data = new Correlation[dataFrames.size()][dataFrames.size()];

        int size = dataFrames.size();

        for (int x = 0; x < size - 1; x++)
        {
            for (int y = x + 1; y < size; y++)
            {
                this.data[x][y] = calculate(dataFrames.get(x), dataFrames.get(y));
            }
        }
    }

    private Correlation calculate(DataFrame frameX, DataFrame frameY)
    {
        LocalDate[] datesX = frameX.dates();
        double[] valuesX = frameX.values();
        if (datesX.length != valuesX.length)
            return new Correlation();

        LocalDate[] datesY = frameY.dates();
        double[] valuesY = frameY.values();
        if (datesY.length != valuesY.length)
            return new Correlation();

        Correlation answer = new Correlation();

        IntPredicate filterX = frameX.filter();
        IntPredicate filterY = frameY.filter();

        int n = 0;

        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double syy = 0.0;
        double sxy = 0.0;

        int indexX = 0;
        int indexY = 0;

        while (indexX < valuesX.length && indexY < valuesY.length)
        {
            if (!filterX.test(indexX))
            {
                indexX++;
            }
            else if (!filterY.test(indexY))
            {
                indexY++;
            }
            else if (datesX[indexX].equals(datesY[indexY]))
            {
                n++;
                double x = valuesX[indexX];
                double y = valuesY[indexY];

                sx += x;
                sy += y;
                sxx += x * x;
                syy += y * y;
                sxy += x * y;

                if (n == 1)
                    answer.firstDate = datesX[indexX];
                answer.lastDate = datesX[indexX];

                indexX++;
                indexY++;
            }
            else if (datesX[indexX].isBefore(datesY[indexY]))
            {
                indexX++;
            }
            else
            {
                indexY++;
            }
        }

        // covariation
        double cov = sxy / n - sx * sy / n / n;
        // standard error of x
        double sigmax = Math.sqrt(sxx / n - sx * sx / n / n);
        // standard error of y
        double sigmay = Math.sqrt(syy / n - sy * sy / n / n);

        // correlation is just a normalized covariation
        answer.r = cov / sigmax / sigmay;
        answer.count = n;

        return answer;
    }

    public List<F> getDataFrames()
    {
        return dataFrames;
    }

    public Optional<Correlation> getCorrelation(int x, int y)
    {
        return x == y ? Optional.empty() : Optional.of(x < y ? data[x][y] : data[y][x]);
    }
}

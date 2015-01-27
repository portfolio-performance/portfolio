package name.abuchen.portfolio.math;

import java.util.Date;

import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;

import org.junit.Test;

public class RiskTest
{
    
    @Test
    public void testDrawdown() {
        int size = 10;
        double[] values = new double[size];
        Date[] dates = new Date[size];
        for (int i=0;i<size;i++) {
            values[i] = i;
            dates[i] = new Date(2015,1,i+1);
        }
        Drawdown drawdown = new Drawdown(values, dates);
        assert(drawdown.getMagnitude() == 0d);
        assert(drawdown.getDuration().getStandardDays() == 0l);
        drawdown = new Drawdown(new double[] {2,2,0,1,2,3,4,5,6,7}, dates);
        assert(drawdown.getMagnitude() == 1d);
        assert(drawdown.getDuration().getStandardDays() == 3l);
    }
    
    @Test
    public void testVolatility() {
        Volatility volatility = new Volatility(new double[] {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1});
        assert(volatility.getStandardDeviation() == 0d);
        assert(volatility.getSemiDeviation() == 0d);
        volatility = new Volatility(new double[] {1,0,1,0,1,2,0});
        assert(volatility.getStandardDeviation() == 1d);
        assert(volatility.getSemiDeviation() == 0.5d);
        volatility = new Volatility(new double[] {1,2,2,4,4,2});
        assert(volatility.getStandardDeviation() == 0.5d);
    }

}

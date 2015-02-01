package name.abuchen.portfolio.math;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.util.Date;

import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;

import org.junit.Test;

public class RiskTest
{
    
    private Date[] getDates(int size) {
        Date[] dates = new Date[size];
        for (int i=0;i<size;i++) {
            dates[i] = new Date(2015,1,i+1);
        }
        return dates;
    }
    
    @Test
    public void testDrawdown() {
        int size = 10;
        Date[] dates = getDates(size);
        double[] values = new double[size];
        for (int i=0;i<size;i++) {
         values[i] = i;
        }
        Drawdown drawdown = new Drawdown(values, dates);
        //Every new value is a new peak, so there never is a drawdown and threfore the magnitude is 0
        assertThat(drawdown.getMagnitude(), is(0d));
        //Dradown duration is the longest duration between peaks. Every vlaue is a peak, so 1 day is
        //every time the duration. The fact that there is never a drawdown does not negate the duration
        assertThat(drawdown.getDuration().getStandardDays(), is(1l));
        drawdown = new Drawdown(new double[] {2,2,0,1,2,3,4,5,6,7}, dates);
        //values are interpreted as performance, so percentages
        //from 2 to 0, the difference is 2, so 200%
        assertThat(drawdown.getMagnitude(), is(2d));
        //the first peak is the first 2. The second 2 is not a peak, the next peak is the 3,
        //which is 5 days later
        assertThat(drawdown.getDuration().getStandardDays(), is(5l));
        drawdown = new Drawdown(new double[] {0, 0.1d, 0.21d, -0.395}, getDates(4));
        assertThat(drawdown.getMagnitude(), is(0.21+0.395));
    }
    
    @Test
    public void testVolatility() {
        Volatility volatility = new Volatility(new double[] {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1});
        assertThat(volatility.getStandardDeviation(), is(0d));
        assertThat(volatility.getSemiDeviation(), is(0d));
        volatility = new Volatility(new double[] {1,0,1,0,1,2,0});
        //The return sequence is [-1.0, 1.0, -1.0, 1.0, 1.0, -2.0]
        //The average return is the -1/6
        //Standard is the square root of the average of the square difference to the average return
        //for -1 ==> (-1--1/6)^2 = (5/6)^2 = 25/36
        //for 1 ==> (1--1/6)^2 = (7/6)^2 = 49/36
        //for -2 ==> (-2--1/6)^2 = (11/6)^2 = 121/36
        //Total square root of (2*25/36 + 3*49/36 + 121/36)/6
        //(318/216)^(1/2)
        assertThat(volatility.getStandardDeviation(), closeTo(Math.sqrt(318d/216), 1e10));
        //for semi deviation, only the returns lower than the average are counted
        //so only the 2 times -1 and the -2
        //root (2*25/36+121/36)/6 = (171/216/^(1/2)
        assertThat(volatility.getSemiDeviation(), closeTo(Math.sqrt(171d/216), 1e10));
        
        volatility = new Volatility(new double[] {1,2,2,4,4,2});
        assertThat(volatility.getStandardDeviation(), closeTo(Math.sqrt(220d/125), 1e10));
    }

}

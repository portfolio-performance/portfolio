package name.abuchen.portfolio.math;

import org.junit.Test;

public class RiskTest
{
    
    @Test
    public void testDrawdown() {
        double steady = Risk.calculateMaxDrawdown(new long[] {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1});
        assert(steady == 0d);
        double full = Risk.calculateMaxDrawdown(new long[] {2,2,0,1,2,3,4,5,6,7});
        assert(full == 1d);
        double many = Risk.calculateMaxDrawdown(new long[] {1,2,3,4,3,4,5,6,5,3,4,5,6,7,6,8,5,9});
        assert(many == 0.5d);
    }
    
    @Test
    public void testVolatility() {
        double steady = Risk.calculateAverageVolatility(new long[] {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1});
        assert(steady == 0d);
        double full = Risk.calculateAverageVolatility(new long[] {1,0,1,0,1,2,0});
        assert(full == 1d);
        double many = Risk.calculateAverageVolatility(new long[] {1,2,2,4,4,2});
        assert(many == 0.5d);
    }

}

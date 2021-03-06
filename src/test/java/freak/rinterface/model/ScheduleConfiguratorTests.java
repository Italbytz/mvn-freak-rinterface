package freak.rinterface.model;

import freak.core.control.ScheduleInterface;
import freak.module.searchspace.PointSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ScheduleConfiguratorTests {

    @Test
    public void testGetLTSSchedule() {
        double[] values = { 42, 37, 37, 28, 18, 18, 19, 20, 15, 14, 14, 13, 11, 12,  8,  7,  8,  8,  9, 15, 15, 80, 80, 75, 62, 62, 62, 62, 62, 58, 58, 58, 58, 58, 58, 50, 50, 50, 50, 50, 56, 70, 27, 27, 25, 24, 22, 23, 24, 24, 23, 18, 18, 17, 18, 19, 18, 18, 19, 19, 20, 20, 20, 89, 88, 90, 87, 87, 87, 93, 93, 87, 80, 89, 88, 82, 93, 89, 86, 72, 79, 80, 82, 91 };
        int[] dim = {21, 4};
        RDoubleMatrix stackloss = new RDoubleMatrix(values, dim);
        PointSet.setPointsSetFromR(true);
        ScheduleInterface schedule = ScheduleConfigurator.getLTSSchedule(stackloss, 0, false, 1, 10000, 0, 0);
        Assertions.assertNotNull(schedule);
    }
}

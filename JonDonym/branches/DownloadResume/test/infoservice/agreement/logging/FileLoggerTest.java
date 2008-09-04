package infoservice.agreement.logging;

import infoservice.agreement.simulator.AInfoService;
import infoservice.agreement.simulator.GoodInfoService;
import junit.framework.TestCase;

public class FileLoggerTest extends TestCase
{

    public void testWriteOut()
    {
        AInfoService is = new GoodInfoService("Guter", "", 0);
        FileLogger fl = new FileLogger(is);
        fl.writeOut("Hallo");
    }

}

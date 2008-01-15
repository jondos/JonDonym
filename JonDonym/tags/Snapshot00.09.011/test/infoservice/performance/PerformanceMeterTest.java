package infoservice.performance;

import java.lang.reflect.Constructor;

import junitx.framework.PrivateTestCase;

public class PerformanceMeterTest extends PrivateTestCase {

	public PerformanceMeterTest(String name) {
		super(name);
	}

	public void testPerformanceMeter() {
		Integer size = new Integer(1024);
		Integer intvl = new Integer(10000);
		Integer req = new Integer(2);
		try {
//			WORKAROUND: PrivateTestCase.newInstance seemingly can't handle Constructors with simple data type arguments.
//			Constructor c[] = PerformanceMeter.class.getConstructors();
//			Class[] paramTypes = new Class[] { Integer.class, Integer.class, Integer.class };
//			Object instance = new TestProxy().createInstance(PerformanceMeter.class.getConstructor(paramTypes), new Integer[] { size, intvl, req });
			Object instance = newInstanceWithKey("infoservice.performance.PerformanceMeter", "_int_int_int", new Integer[] { size, intvl, req });
			Object testdata = this.get(instance, "testdata");
			assertEquals(true, testdata instanceof byte[]);
			assertEquals(size.intValue(), ((byte[]) testdata).length);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testGetExitPointSocket() {
		fail("Not yet implemented");
	}

	public void testRun() {
		fail("Not yet implemented");
	}

}

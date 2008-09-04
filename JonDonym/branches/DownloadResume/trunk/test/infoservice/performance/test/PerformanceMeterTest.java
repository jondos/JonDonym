package infoservice.performance.test;

import infoservice.performance.PerformanceMeter;

import java.lang.reflect.Constructor;
import java.net.Socket;

import anon.infoservice.Database;
import anon.infoservice.MixCascade;

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
			assertEquals(true, instance instanceof PerformanceMeter);

			Object testdata = this.get(instance, "testdata");
			assertTrue(testdata instanceof byte[]);
			assertEquals(size.intValue(), ((byte[]) testdata).length);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testGetExitPointSocket() {
		try {
			Integer size = new Integer(1024);
			Integer intvl = new Integer(10000);
			Integer req = new Integer(2);

			Object instance = newInstanceWithKey("infoservice.performance.PerformanceMeter", "_int_int_int", new Integer[] { size, intvl, req });
			assertTrue(instance instanceof PerformanceMeter);

			Object socket = this.invoke(instance, "getExitPointSocket", new Object[] { new MixCascade("localhost", 6666) });
			assertTrue(socket instanceof Socket);
			assertEquals("toliman", ((Socket) socket).getInetAddress().getHostName());
			assertFalse(((Socket) socket).isClosed());
			assertTrue(((Socket) socket).isConnected());
			((Socket) socket).close();
		}
		catch(Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testRun() {
		try {
			Integer size = new Integer(1024);
			Integer intvl = new Integer(10000);
			Integer req = new Integer(2);
			Database db = Database.getInstance(MixCascade.class);
			MixCascade testcascade = new MixCascade("localhost", 6666);
			db.update(testcascade);
			assertNotNull(db.getEntryById(testcascade.getId()));

			Object instance = newInstanceWithKey("infoservice.performance.PerformanceMeter", "_int_int_int", new Integer[] { size, intvl, req });
			assertTrue(instance instanceof PerformanceMeter);

			this.invoke(instance, "run", new Object[] {} );

		}
		catch(Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

}

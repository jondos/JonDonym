package anon.tor.ordescription.test;

import junitx.framework.PrivateTestCase;

import anon.tor.ordescription.ORList;
import anon.tor.ordescription.PlainORListFetcher;
import anon.tor.ordescription.ORDescriptor;

/**
 * tests whether ORList
 * @author dhoske
 *
 */
public class ORListTest extends PrivateTestCase
{
	private ORList m_l;

	public ORListTest(String s)
	{
		super(s);
	}

	public void setUp()
	{
		m_l = new ORList(new PlainORListFetcher(PlainORListFetcherTest.TEST_DIR_ADDR,
			  	                                PlainORListFetcherTest.TEST_DIR_PORT));
	}

	public void tearDown()
	{

	}

	public void testAddRemove()
	{
		String name = "test-server";
		ORDescriptor ord = new ORDescriptor("test.de",name,9000,"Tor 0.1.1.26");

		m_l.add(ord);
		assertTrue(m_l.getByName(name) != null);

		m_l.remove(name);
		assertTrue(m_l.getByName(name) == null);
	}

	public void testUpdate()
	{
		/* test twice to check whether parseStatus
		 * removes the descriptors
		 */
		m_l.updateList();
		assertTrue(m_l.size() != 0);
		m_l.updateList();
		assertTrue(m_l.size() != 0);

		assertTrue(m_l.getByRandom() != null);
	}
}

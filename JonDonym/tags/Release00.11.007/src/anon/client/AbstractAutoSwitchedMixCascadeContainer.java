package anon.client;

import java.security.SignatureException;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

import anon.infoservice.AbstractMixCascadeContainer;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.pay.PayAccountsFile;

/**
 * This class returns a new random cascade from all currently available cascades every time
 * getNextCascade() is called. If all available cascades have been returned once, this class starts
 * again by choosing the random cascades from all available ones.
 * @author Rolf Wendolsky
 */
public abstract class AbstractAutoSwitchedMixCascadeContainer extends AbstractMixCascadeContainer
{
	private Hashtable m_alreadyTriedCascades;
	private Random m_random;
	private MixCascade m_initialCascade;
	private MixCascade m_currentCascade;
	private boolean m_bKeepCurrentCascade;
	private boolean m_bSkipInitialCascade;
	
	public AbstractAutoSwitchedMixCascadeContainer(boolean a_bSkipInitialCascade, MixCascade a_initialCascade)
	{
		m_bSkipInitialCascade = a_bSkipInitialCascade;
		m_alreadyTriedCascades = new Hashtable();
		m_random = new Random(System.currentTimeMillis());
		m_random.nextInt();
		m_initialCascade = a_initialCascade;
		m_bKeepCurrentCascade = false;
	}

	public final MixCascade getInitialCascade()
	{
		return m_initialCascade;
	}

	/**
	 * Explicitly chooses the next cascade at random. If auto-reconnect is active,
	 * this method is equal to getNextCascade().
	 * @return the next cascade at random
	 */
	public final MixCascade getNextRandomCascade()
	{
		return getNextCascade(true);
	}
	
	public final MixCascade getNextCascade()
	{
		return getNextCascade(false);
	}
	
	private final MixCascade getNextCascade(boolean a_bForceNextRandom)
	{
		synchronized (m_alreadyTriedCascades)
		{
			if (!isServiceAutoSwitched() && !a_bForceNextRandom)
			{
				m_alreadyTriedCascades.clear();
				m_bKeepCurrentCascade = false;
				if (m_currentCascade == null)
				{
					m_currentCascade = m_initialCascade;
				}
			}
			else if (m_bKeepCurrentCascade)
			{
				// do not check if this cascade has been used before
				m_bKeepCurrentCascade = false;
				if (m_currentCascade == null)
				{
					m_currentCascade = m_initialCascade;
				}
				if (m_currentCascade != null)
				{
					m_alreadyTriedCascades.put(m_currentCascade.getId(), m_currentCascade);
				}
			}
			else if (m_bSkipInitialCascade || m_initialCascade == null ||
					 m_alreadyTriedCascades.containsKey(m_initialCascade.getId()))
			{
				MixCascade currentCascade = null;
				Vector availableCascades;
				boolean forward = true;

				availableCascades = Database.getInstance(MixCascade.class).getEntryList();
				if (availableCascades.size() > 0)
				{
					int chosenCascadeIndex = m_random.nextInt();
					if (chosenCascadeIndex < 0)
					{
						// only positive numbers are allowed
						chosenCascadeIndex *= -1;
						// move backward
						forward = false;
					}

					// chose an index from the vector
					chosenCascadeIndex %= availableCascades.size();
					/* Go through all indices until a suitable MixCascade is found or the original index
					 * is reached.
					 */
					int i;
					for (i = 0; i < availableCascades.size(); i++)
					{
						currentCascade = (MixCascade) availableCascades.elementAt(chosenCascadeIndex);
						// this is the logic that decides whether to use a cascade or not
						if (!m_alreadyTriedCascades.containsKey(currentCascade.getId()))
						{
							m_alreadyTriedCascades.put(currentCascade.getId(), currentCascade);
							if (isSuitableCascade(currentCascade))
							{
								// found a suitable cascade
								break;
							}
						}
						if (forward)
						{
							chosenCascadeIndex = (chosenCascadeIndex + 1) % availableCascades.size();
						}
						else
						{
							chosenCascadeIndex -= 1;
							if (chosenCascadeIndex < 0)
							{
								chosenCascadeIndex = availableCascades.size() - 1;
							}
						}
					}
					if (i == availableCascades.size())
					{
						// no suitable cascade was found
						if (m_alreadyTriedCascades.size() == 0)
						{
							/** @todo Perhaps we should insert a timeout here? */
						}
						currentCascade = null;
					}
				}
				else if (m_initialCascade == null)
				{
					// no cascade is available
					return null;
				}
				if (currentCascade == null)
				{
					m_bSkipInitialCascade = false; // this is not the first call
					m_alreadyTriedCascades.clear();
					currentCascade = getNextCascade();
					if (currentCascade == null && m_initialCascade != null)
					{
						// fallback if there are really no cascades; take the initial cascade
						currentCascade = m_initialCascade;
						m_alreadyTriedCascades.put(m_initialCascade.getId(), m_initialCascade);
					}
				}
				m_currentCascade = currentCascade;
			}
			else
			{
				m_alreadyTriedCascades.put(m_initialCascade.getId(), m_initialCascade);
				m_currentCascade = m_initialCascade;
			}

			if (m_bSkipInitialCascade)
			{
				m_initialCascade = m_currentCascade;
			}
			// this only happens for the first call
			m_bSkipInitialCascade = false;
		}

		return m_currentCascade;
	}

	public abstract boolean isServiceAutoSwitched();
	public abstract boolean isReconnectedAutomatically();
	public abstract boolean isPaidServiceAllowed();

	private final boolean isSuitableCascade(MixCascade a_cascade)
	{
		if (a_cascade == null)
		{
			return false;
		}

		if (a_cascade.isPayment() && !TrustModel.getCurrentTrustModel().isPaymentForced() && // Force is stonger! 
				// if no valid account exists, do not allow automatic connections to
				// paid services if an editable account is active or if the user does not want to connect to them
			(PayAccountsFile.getInstance().getChargedAccount(a_cascade.getPIID()) == null &&
					(TrustModel.getCurrentTrustModel().isEditable() || !isPaidServiceAllowed())))
		{
			// do not connect to payment for new users
			return false;
		}

		if (m_initialCascade != null && m_bSkipInitialCascade && a_cascade.equals(m_initialCascade))
		{
			return false;
		}

		/*
		 * Cascade is not suitable if payment and the warning dialog is shown or no account is available
		 * Otherwise the user would have to answer a dialog which is not good for automatic connections.
		 */
		/*
		return isTrusted(a_cascade) && !(a_cascade.isPayment() &&
				 ( !JAPController.getInstance().getDontAskPayment() ||
				  PayAccountsFile.getInstance().getNumAccounts() == 0 ||
				  PayAccountsFile.getInstance().getActiveAccount() == null ||
				  PayAccountsFile.getInstance().getActiveAccount().getBalance().getCredit() == 0));*/
		return isTrusted(a_cascade);


	}
	public final MixCascade getCurrentCascade()
	{
		return m_currentCascade;
	}
	
	public final boolean setCurrentCascade(MixCascade a_cascade)
	{
		if (!isTrusted(a_cascade))
		{
			return false;
		}
		synchronized (m_alreadyTriedCascades)
		{
			m_bKeepCurrentCascade = true;
			m_currentCascade = a_cascade;
		}
		return true;
	}

	public final void keepCurrentService(boolean a_bKeepCurrentCascade)
	{
		synchronized (m_alreadyTriedCascades)
		{
			m_bKeepCurrentCascade = a_bKeepCurrentCascade;
		}
	}

	public final void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
	{
		TrustModel.getCurrentTrustModel().checkTrust(a_cascade);
	}
}

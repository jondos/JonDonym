package anon.pay;

import java.util.EventListener;

import anon.pay.xml.XMLErrorMessage;
import anon.util.captcha.IImageEncodedCaptcha;
import anon.util.captcha.ICaptchaSender;
import anon.infoservice.MixCascade;


/**
 * GUI classes can implement this interface and register with the Payment library
 * to be notified about payment specific events
 * @version 1.0
 * @author Bastian Voigt, Tobias Bayer
 */
public interface IPaymentListener extends EventListener
{
	/**
	 * The AI has signaled that the current cascade has to be payed for.
	 * @param acc PayAccount
	 */
	int accountCertRequested(MixCascade a_connectedCascade);

	/**
	 * The AI has signaled an error.
	 * @param acc PayAccount
	 * @param a_bIgnore do not force a user reaction
	 */
	void accountError(XMLErrorMessage msg, boolean a_bIgnore);

	/**
	 * The active account changed.
	 * @param acc PayAccount the account which is becoming active
	 */
	void accountActivated(PayAccount acc);

	/**
	 * An account was removed
	 * @param acc PayAccount the account which was removed
	 */
	void accountRemoved(PayAccount acc);

	/**
	 * An account was added
	 * @param acc PayAccount the new Account
	 */
	void accountAdded(PayAccount acc);

	/**
	 * The credit changed for the given account.
	 * @param acc PayAccount
	 */
	void creditChanged(PayAccount acc);

	/**
	 * Captcha retrieved
	 */
	void gotCaptcha(ICaptchaSender a_source, IImageEncodedCaptcha a_captcha);
	}

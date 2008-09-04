/*
 Copyright (c) 2000-2005, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
   may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package gui;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import anon.util.ClassUtil;

/**
 * This text field only accepts positive integers as input.
 * @author Rolf Wendolsky
 */
public final class JAPJIntField extends JTextField
{
	/** choose this value if the integer text field should have no upper bound */
	public static final int NO_MAXIMUM_BOUND = -1;

	public static final int ALLOW_ZEROS_NONE = 0;
	public static final int ALLOW_ZEROS_ONE = 1;
	public static final int ALLOW_ZEROS_UNTIL_BOUND = 2;

	private static final String MSG_NO_VALID_INTEGER = gui.JAPJIntField.class.getName() + "_noValidInteger";

	private IIntFieldBounds m_bounds;
	private boolean b_bAutoTransferFocus;

	/**
	 * Creates a new JAPJIntField with no bounds.
	 */
	public JAPJIntField()
	{
		this(NO_MAXIMUM_BOUND, false);
	}

	/**
	 * Creates a new JAPJIntField.
	 * @param a_maxValue the maximum int value that may be entered in the text field
	 */
	public JAPJIntField(int a_maxValue)
	{
		this(a_maxValue, false);
	}

	/**
	 * Creates a new JAPJIntField.
	 * @param a_maxValue the maximum int value that may be entered in the text field
	 * @param a_bAutoTransferFocus if the focus of the field should automatically be transfered if the
	 * maximum value would be exceeded with the next input
	 */
	public JAPJIntField(int a_maxValue, boolean a_bAutoTransferFocus)
	{
		this(new DefaultBounds(a_maxValue), a_bAutoTransferFocus);
	}

	/**
	 * Creates a new JAPJIntField.
	 * @param a_bounds the bounds (minimum/maximum value) for this integer text field
	 */
	public JAPJIntField(IIntFieldBounds a_bounds)
	{
		this(a_bounds, false);
	}

	/**
	 * Creates a new JAPJIntField.
	 * @param a_bounds the bounds (minimum/maximum value) for this integer text field
	 * @param a_bAutoTransferFocus if the focus of the field should automatically be transfered if the
	 * maximum value would be exceeded with the next input
	 */
	public JAPJIntField(IIntFieldBounds a_bounds, boolean a_bAutoTransferFocus)
	{
		super(parseNumberOfDigits(a_bounds.getMaximum()));

		if (a_bounds.getAllowZeros() < ALLOW_ZEROS_NONE || a_bounds.getAllowZeros() > ALLOW_ZEROS_UNTIL_BOUND)
		{
			throw new IllegalArgumentException("getAllowZeros() returned an illegal value: " +
				a_bounds.getAllowZeros());
		}

		m_bounds = a_bounds;
		b_bAutoTransferFocus = a_bAutoTransferFocus;
	}

	/**
	 * Sets the integer stored in this text field.
	 * @param a_integer the integer stored in this text field
	 */
	public void setInt(int a_integer)
	{
		setText(Integer.toString(a_integer));
	}

	/**
	 * Returns the integer stored in this text field.
	 * @return the integer stored in this text field
	 * @throws NumberFormatException if there is no valid integer in the text field
	 */
	public int getInt() throws NumberFormatException
	{
		int integer;
		Object[] arguments = new Object[2];

		if (getName() == null || getName().trim().length() == 0)
		{
			arguments[1] = ClassUtil.getShortClassName(getClass());
		}
		else
		{
			arguments[1] = getName();
		}

		try
		{
			integer = Integer.parseInt(getText());
			if (integer < 0 || ((m_bounds.getAllowZeros() == ALLOW_ZEROS_NONE) && (integer == 0)) ||
				(m_bounds.getMaximum() >= 0 && integer > m_bounds.getMaximum()))
			{
				arguments[0] = new Integer(integer);
			}
			else
			{
				return integer;
			}
		}
		catch (NumberFormatException a_e)
		{
			arguments[0] = getText();
		}

		throw new NumberFormatException(JAPMessages.getString(MSG_NO_VALID_INTEGER, arguments));
	}

	/**
	 * Updates the text field with the current bounds. If the number entered in the text field is not
	 * within the current bounds, it will be adapted to fit into the bounds (increase/decrease).
	 */
	public void updateBounds()
	{
		try
		{
			if (getInt() > m_bounds.getMaximum())
			{
				setInt(m_bounds.getMaximum());
			}
			if ((m_bounds.getAllowZeros() == ALLOW_ZEROS_NONE) && (getInt() == 0))
			{
				setInt(1);
			}
		}
		catch (NumberFormatException a_e)
		{
			if (m_bounds.getAllowZeros() > ALLOW_ZEROS_NONE)
			{
				setInt(0);
			}
			else
			{
				setInt(1);
			}
		}
	}

	/**
	 * This interface represents bounds for the integer text field, this means what numbers (max/min)
	 * are allowed to enter.
	 */
	public static interface IIntFieldBounds
	{
		/**
		 * Returns the number of allowed left-hand zeros as one of the following constants:
		 * <ul>
		 *   <li> ALLOW_ZEROS_NONE </li>
		 *   <li> ALLOW_ZEROS_ONE </li>
		 *   <li> ALLOW_ZEROS_UNTIL_BOUND </li>
		 * </ul>
		 * @return the number of allowed left-hand zeros as a constant
		 */
		int getAllowZeros();

		/**
		 * Returns the maximum value that is allowed to enter.
		 * @return the maximum value that is allowed to enter or a value smaller than zero if there is
		 * no upper bound
		 */
		int getMaximum();
	}

	public static abstract class AbstractIntFieldBounds implements IIntFieldBounds
	{
		private int m_maxValue;

		public AbstractIntFieldBounds(int a_maxValue)
		{
			m_maxValue = a_maxValue;
		}

		public final int getMaximum()
		{
			return m_maxValue;
		}
	}

	public static final class IntFieldUnlimitedZerosBounds extends AbstractIntFieldBounds
	{
		public IntFieldUnlimitedZerosBounds(int a_maxValue)
		{
			super(a_maxValue);
		}

		/**
		 * Allows left-hand zeros until the digit bound is reached.
		 * @return ALLOW_ZEROS_UNTIL_BOUND
		 */
		public int getAllowZeros()
		{
			return ALLOW_ZEROS_UNTIL_BOUND;
		}
	}

	/**
	 * This bound does not allow zeros.
	 */
	public static final class IntFieldWithoutZeroBounds extends AbstractIntFieldBounds
	{
		public IntFieldWithoutZeroBounds(int a_maxValue)
		{
			super(a_maxValue);
		}

		/**
		 * Left-hand zeros are not allowed.
		 * @return ALLOW_ZEROS_NONE
		 */
		public int getAllowZeros()
		{
			return ALLOW_ZEROS_NONE;
		}
	}

	protected final Document createDefaultModel()
	{
		return new IntDocument();
	}

	private static final class DefaultBounds extends AbstractIntFieldBounds
	{
		public DefaultBounds(int a_maximum)
		{
			super(a_maximum);
		}

		/**
		 * One left-hand zero is allowed.
		 * @return ALLOW_ZEROS_ONE
		 */
		public int getAllowZeros()
		{
			return ALLOW_ZEROS_ONE;
		}
	}

	private final class IntDocument extends PlainDocument
	{
		public void insertString(int offSet, String string, AttributeSet attributeSet)
			throws BadLocationException
		{
			int currentInt;

			if (string == null || string.trim().length() == 0)
			{
				return;
			}

			try
			{
				currentInt = Integer.parseInt(getText(0, getLength()) + string);
			}
			catch (NumberFormatException e)
			{
				return;
			}


			if (m_bounds.getAllowZeros() < ALLOW_ZEROS_UNTIL_BOUND)
			{
				// do not allow to write more than one zero
				if (currentInt < 10 && offSet > 0)
				{
					return;
				}
			}
			else
			{
				// do not write more than the length of the maximum bound
				if (m_bounds.getMaximum() >= 0 && parseNumberOfDigits(m_bounds.getMaximum()) < (offSet + 1))
				{
					return;
				}
			}



			// do not allow to write more than the maximum and check if zero is allowed
			if (!(m_bounds.getMaximum() >= 0 && currentInt > m_bounds.getMaximum()) &&
				(m_bounds.getAllowZeros() > ALLOW_ZEROS_NONE ||
				 (m_bounds.getAllowZeros() == ALLOW_ZEROS_NONE && currentInt > 0)))
			{
				super.insertString(offSet, string, attributeSet);
			}

			// if the maximum number length has been reached, move on to the next component
			if (m_bounds.getMaximum() >= 0 && b_bAutoTransferFocus && getLength() > 0 &&
				(offSet + 1) == parseNumberOfDigits(m_bounds.getMaximum()))
			{
				transferFocus();
			}
		}
	}

	private static int parseNumberOfDigits(int a_integer)
		{
			int digits;

			for (digits = 0; a_integer > 0; digits++, a_integer /= 10);

			if (digits == 0)
			{
				digits = 1;
			}

			return digits;
	}
}

/*
 Copyright (c) 2004, The JAP-Team
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
package anon.tor.cells;

/**
 * @author stefan
 *
 */

public abstract class Cell
{
	public final static int CELL_SIZE = 512;
	public final static int CELL_PAYLOAD_SIZE = 509;
	private int m_circID;
	private int m_command;
	protected byte[] m_payload;

	protected Cell(int command)
	{
		m_circID = 0;
		m_command = command;
		m_payload = new byte[CELL_PAYLOAD_SIZE];
	}

	protected Cell(int command, int circID)
	{
		this(command);
		this.m_circID = circID;
	}

	protected Cell(int command, int circID, byte[] payload)
	{
		this(command, circID);
		setPayload(payload, 0);
	}

	protected Cell(int command, int circID, byte[] payload, int offset)
	{
		this(command, circID);
		setPayload(payload, offset);
	}

	/**
	 * creates a fixed-sized cell<br>
	 * <br>
	 * 2   bytes - circID<br>
	 * 1   byte  - command<br>
	 * 509 bytes - payload
	 *
	 * @return
	 * composed cell
	 */
	public byte[] getCellData()
	{
		byte[] buff = new byte[CELL_SIZE];
		buff[0] = (byte) ( (m_circID >> 8) & 0x00FF);
		buff[1] = (byte) ( (m_circID) & 0x00FF);
		buff[2] = (byte) ( (m_command) & 0x00FF);
		System.arraycopy(m_payload, 0, buff, 3, CELL_PAYLOAD_SIZE);
		return buff;
	}

	/**
	 * returns the command of the cell
	 * @return
	 * command
	 */
	public int getCommand()
	{
		return m_command;
	}

	/**
	 * gets the circuit id
	 * @return
	 * ID
	 */
	public int getCircuitID()
	{
		return m_circID;
	}

	/**
	 * gets the payload of the cell
	 * @return
	 * payload
	 */
	public byte[] getPayload()
	{
		return m_payload;
	}

	/**
	 * sets the payload
	 * @param payload
	 * payload
	 * @param offset
	 * offset
	 */
	public void setPayload(byte[] payload, int offset)
	{
		int len = Math.min(CELL_PAYLOAD_SIZE, payload.length);
		System.arraycopy(payload, offset, m_payload, 0, len);
	}

	/**
	 * creates a cell with the given data
	 * @param cellData
	 * data
	 * @return
	 * a cell
	 */
	public static Cell createCell(byte[] cellData)
	{
		if (cellData.length != CELL_SIZE)
		{
			return null;
		}
		Cell cell = null;
		int cid = ( (cellData[0] & 0xFF) << 8) | (cellData[1] & 0xFF);
		int type = cellData[2] & 0xFF;
		switch (type)
		{
			case 2:
			{
				cell = new CreatedCell(cid, cellData, 3);
				break;
			}
			case 3:
			{
				cell = new RelayCell(cid, cellData, 3);
				break;
			}
			case 4:
			{
				cell = new DestroyCell(cid, cellData, 3);
				break;
			}
			case 0:
			{
				cell = new PaddingCell(cid, cellData, 3);
				break;
			}

			default:
			{
				cell = null;
			}
		}
		return cell;
	}

}

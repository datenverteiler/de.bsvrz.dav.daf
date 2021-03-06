/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2006 by Kappich Systemberatung Aachen
 * Copyright 2004 by Kappich+Kni� Systemberatung, Aachen
 * 
 * This file is part of de.bsvrz.dav.daf.
 * 
 * de.bsvrz.dav.daf is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dav.daf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with de.bsvrz.dav.daf; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.dav.daf.communication.lowLevel.telegrams;

import de.bsvrz.dav.daf.main.impl.CommunicationConstant;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Ermittlung der Telegrammlaufzeit. Mit diesem Systemtelegramm wird eine Anfrage zur Ermittlung der Telegrammlaufzeit zwischen zwei Datenverteiler beantwortet.
 * Die Ermittlung der Telegrammlaufzeit ist in beiden Richtungen m�glich (siehe Telegrammlaufzeitanfrage ). Der anfragende Kommunikationspartner schreibt seine
 * aktuelle Systemzeit in Millisekunden in das Systemzeitfeld. Diese Zeitangabe wird von dem anderen Kommunikationspartner in das Zeitfeld des Antworttelegramms
 * kopiert. Die Laufzeit (Hin- und R�ckweg) ermittelt sich durch die Differenz der aktuellen Systemzeit bei Erhalt des Antworttelegramms mit der Zeitangabe im
 * Telegramm.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5059 $
 */
public class TransmitterTelegramTimeAnswer extends DataTelegram {

	/** Die Zeit, die das Telegramm bisher f�r die Laufzeitermittlung ben�tigt hat. */
	private long _telegramTime;

	private long _roundTripTime = 0;

	public TransmitterTelegramTimeAnswer() {
		type = TRANSMITTER_TELEGRAM_TIME_ANSWER_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
	}

	public TransmitterTelegramTimeAnswer(long telegramRequestTime) {
		type = TRANSMITTER_TELEGRAM_TIME_ANSWER_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
		_telegramTime = telegramRequestTime;
		length = 8;
	}

	/**
	 * Gibt die Startzeit der Durchsatzpr�fungs-Anfrage zur�ck.
	 *
	 * @return die Startzeit der Durchsatzpr�fungs-Anfrage
	 */
	public final long getTelegramStartTime() {
		return _telegramTime;
	}

	/**
	 * Gibt die Zeit der Durchsatzpr�fungs-Anfrage zur�ck.
	 *
	 * @return die Zeit der Durchsatzpr�fungs-Anfrage
	 */
	public final long getRoundTripTime() {
		return _roundTripTime;
	}

	public final String parseToString() {
		String str = "Systemtelegramm Laufzeitsermittlung Antwort: \n";
		str += "Gesamte Zeit der ermittlung : " + _telegramTime + "\n";
		return str;
	}

	public final void write(DataOutputStream out) throws IOException {
		out.writeShort(length);
		out.writeLong(_telegramTime);
	}

	public final void read(DataInputStream in) throws IOException {
		int _length = in.readShort();
		_telegramTime = in.readLong();
		_roundTripTime = System.currentTimeMillis() - _telegramTime;
		length = 8;
		if(length != _length) {
			throw new IOException("Falsche Telegramml�nge");
		}
	}
}

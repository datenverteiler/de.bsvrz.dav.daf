/*
 * Copyright 2010 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.daf.main.config;

/**
 * Klasse, die eine R�ckgabe eines Konfigurationsbackupvorgangs enth�lt.
 */
public class SimpleBackupResult implements BackupResult {

	private final long _completed;
	private final long _failed;
	private final String _path;

	public SimpleBackupResult(final long completed, final long failed, final String path) {
		_completed = completed;
		_failed = failed;
		_path = path;
	}

	public long getCompleted() {
		return _completed;
	}

	public long getFailed() {
		return _failed;
	}

	public long getTotal() {
		return _completed + _failed;
	}

	public String getPath() {
		return _path;
	}

	@Override
	public String toString() {
		return "SimpleBackupResult{" + "_completed=" + _completed + ", _failed=" + _failed + ", _path='" + _path + '\'' + '}';
	}
}

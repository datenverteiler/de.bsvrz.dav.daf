/*
 * Copyright 2011 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.daf.util.fileBackedQueue;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

/**
 * Eine Queue, die Elemente beim �berschreiten eines Grenzwertes ins Dateisystem auslagert und dadurch sehr viele Elemente aufnehmen kann. Diese Queue ist
 * abgesehen vom Iterator threadsicher. Je nach Konstruktor verwendet die Queue einen benutzerdefinierten Serializer um die Objekte im Dateisystem abzulegen,
 * oder den Standard-Java-Serializer. Es gibt vordefinierte geerbte Queue-Klassen, die f�r einfache Datentypen optimierte Serializer benutzen. F�r weitere
 * Datentypen ist ein benutzerdefinierter {@link QueueSerializer} zu implementieren. Wichtig: nachdem die Queue nicht mehr gebraucht wird sollte {@link
 * #clear()} aufgerufen werden um nicht mehr gebrauchten Speicher auf der Festplatte freizugeben.<br />Diese Queue ist auf mehrere gleichzeitige Einf�gungen von
 * mehreren Threads spezialisiert. Das Auslesen von Objekten geht im allgemeinen sehr schnell, kann aber nur von einem Thread gleichzeitig durchgef�hrt
 * werden.<br />F�r das Speichern im Dateisystem werden die Objekte serialisiert. Das hei�t, die Objekte, die zu der Queue hinzugef�gt werden sind
 * m�glicherweise nicht identisch (im Sinne von Objektidentit�t) zu den Objekten, die aus der Queue ausgelesen werden.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9474 $
 * @see FileBackedLongQueue
 * @see FileBackedIntQueue
 * @see FileBackedShortQueue
 * @see FileBackedByteQueue
 * @see FileBackedStringQueue
 */
public class FileBackedQueue<E> extends AbstractQueue<E> {

	private final Queue<E> _memoryQueue;

	private final FileSystemQueue<E> _fileQueue;

	private volatile int _memoryUsed = 0;

	private final int _memoryCapacity;

	private final QueueSerializer<E> _queueSerializer;

	/**
	 * Erstellt eine neue Queue, die durch ein Dateisystem unterst�tzt wird und so recht gro� werden kann. Um die Objekte in das Dateisystem zu speichern, ist
	 * erforderlich, dass ein QueueSerializer angegeben wird, der die Objekte serialisiert und wieder deserialisiert.
	 *
	 * @param memoryCapacity     Wie viel Speicher in Bytes maximal im Arbeitsspeicher gehalten werden sollen. Es handelt sich um einen Richtwert, der geringf�gig
	 *                           (um die Gr��e eines Elements) �berschritten werden kann. Ber�cksichtigt wird hier nicht der tats�chliche Arbeitsspeicherverbrauch,
	 *                           sondern die Gr��e, die das Element verbrauchen w�rde, falls es serialisiert werden w�rde.
	 * @param filesystemCapacity Wie viel Speicher in Bytes maximal im Dateisystem gehalten werden sollen. Es handelt sich um einen Richtwert, der geringf�gig (um
	 *                           maximal die Gr��e eines Elements minus 1 Byte) �berschritten werden kann.
	 * @param queueSerializer    Klasse, die das Deserialisieren von Objekten �bernimmt.
	 */
	public FileBackedQueue(final int memoryCapacity, final long filesystemCapacity, final QueueSerializer<E> queueSerializer) {
		if(memoryCapacity < 1) throw new IllegalArgumentException("memoryCapacity muss > 0 sein.");
		if(filesystemCapacity < 1) throw new IllegalArgumentException("filesystemCapacity muss > 0 sein.");
		if(queueSerializer == null) throw new IllegalArgumentException("queueSerializer ist null.");
		_memoryCapacity = memoryCapacity;
		_memoryQueue = new ArrayDeque<E>();
		_queueSerializer = queueSerializer;
		_fileQueue = new FileSystemQueue<E>(filesystemCapacity, _queueSerializer);
	}

	/**
	 * Erstellt eine neue Queue, die durch ein Dateisystem unterst�tzt wird und so recht gro� werden kann. Wird dieser Konstruktor benutzt, wird der
	 * Standard-Java-Serializer benutzt. Dieser ist bei kleinen oder primitiven Datentypen ineffektiv und funktioniert bei nicht serialisierbaren Klassen nicht.
	 *
	 * @param memoryCapacity     Wie viel Speicher in Bytes maximal im Arbeitsspeicher gehalten werden sollen.
	 * @param filesystemCapacity Wie viel Speicher in Bytes maximal im Dateisystem gehalten werden sollen. Es handelt sich um einen Richtwert, der geringf�gig (um
	 *                           maximal die Gr��e eines Elements) �berschritten werden kann.
	 */
	public FileBackedQueue(final int memoryCapacity, final long filesystemCapacity) {
		if(memoryCapacity < 1) throw new IllegalArgumentException("memoryCapacity muss > 0 sein.");
		if(filesystemCapacity < 1) throw new IllegalArgumentException("filesystemCapacity muss > 0 sein.");
		_memoryCapacity = memoryCapacity;
		_memoryQueue = new ArrayDeque<E>();
		_queueSerializer = new ObjectQueueSerializer<E>();
		_fileQueue = new FileSystemQueue<E>(filesystemCapacity, _queueSerializer);
	}

	/**
	 * Returns an iterator over the elements contained in this collection.
	 * <p/>
	 * It is imperative that the user manually synchronize on the returned collection when iterating over it:
	 * <pre>
	 *  Collection c = myFileBackedQueue;
	 *     ...
	 *  synchronized(c) {
	 *      Iterator i = c.iterator(); // Must be in the synchronized block
	 *      while (i.hasNext())
	 *         foo(i.next());
	 *  }
	 * </pre>
	 * Failure to follow this advice may result in non-deterministic behavior.
	 *
	 * @return an iterator over the elements contained in this collection
	 */
	@Override
	public Iterator<E> iterator() {
		final Iterator<E> fileItr = _fileQueue.iterator();
		final Iterator<E> memItr = _memoryQueue.iterator();
		return new MergeItr(fileItr, memItr);
	}

	@Override
	public synchronized int size() {
		return _memoryQueue.size() + _fileQueue.size();
	}

	/**
	 * Inserts the specified element into this queue if it is possible to do so immediately without violating capacity restrictions. When using a
	 * capacity-restricted queue, this method is generally preferable to {@link #add}, which can fail to insert an element only by throwing an exception.
	 *
	 * @param e the element to add
	 *
	 * @return <tt>true</tt> if the element was added to this queue, else <tt>false</tt>
	 *
	 * @throws ClassCastException       if the class of the specified element prevents it from being added to this queue
	 * @throws NullPointerException     if the specified element is null and this queue does not permit null elements
	 * @throws IllegalArgumentException if some property of this element prevents it from being added to this queue
	 */
	public synchronized boolean offer(final E e) {
		final int size = _queueSerializer.getSize(e);
		_memoryUsed += size;
		while(isMemoryQueueTooFull()) {
			if(!moveFromMemoryQueueToFileQueue()) {
				_memoryUsed -= size;
				return false;
			}
		}
		_memoryQueue.offer(e);
		return true;
	}

	/**
	 * Verschiebt das head-Element der Memoryqueue ins Dateisystem
	 *
	 * @return false wenn der Dateisystemspeicher voll ist, sonst true.
	 */
	private synchronized boolean moveFromMemoryQueueToFileQueue() {
		// das Element aus dem Speicher auslesen
		final E entry = _memoryQueue.peek();
		if(entry == null) return true;

		boolean result = false;
		if(_fileQueue.offer(entry)) {
			// Erst wenn sicher ist, dass das Element ins Dateisystem passt, aus dem Speicher entfernen
			_memoryQueue.remove();
			_memoryUsed -= _queueSerializer.getSize(entry);
			result = true;
		}
		return result;
	}

	private boolean isMemoryQueueTooFull() {
		return _memoryUsed > _memoryCapacity;
	}

	/**
	 * Retrieves and removes the head of this queue, or returns <tt>null</tt> if this queue is empty.
	 *
	 * @return the head of this queue, or <tt>null</tt> if this queue is empty
	 */
	public synchronized E poll() {
		if(!_fileQueue.isEmpty()) {
			return _fileQueue.poll();
		}
		final E entry = _memoryQueue.poll();
		if(entry != null) {
			_memoryUsed -= _queueSerializer.getSize(entry);
		}
		return entry;
	}

	/**
	 * Retrieves, but does not remove, the head of this queue, or returns <tt>null</tt> if this queue is empty.
	 *
	 * @return the head of this queue, or <tt>null</tt> if this queue is empty
	 */
	public synchronized E peek() {
		if(!_fileQueue.isEmpty()) {
			return _fileQueue.peek();
		}
		return _memoryQueue.peek();
	}

	/**
	 * Gibt den Speicher zur�ck, den die Objekte im Arbeitsspeicher verwenden. Das ist nicht zwingend die Menge an Arbeitsspeicher, die wirklich verbraucht wird,
	 * sondern die Menge an Speicher, den die Objekte brauchen w�rden, w�ren sie in einer Datei serialisiert gespeichert. Dieser Wert kann kurzzeitig w�hrend des
	 * Einf�gens von Objekten getMemoryCapacity() �berschreiten.
	 *
	 * @return Menge an Speicher in Bytes
	 */
	public int getMemoryUsed() {
		return _memoryUsed;
	}

	/**
	 * Gibt die Kapazit�t des Caches in Bytes zur�ck, in dem die Objekte im Arbeitsspeicher gehalten werden. Das ist nicht zwingend die Menge an Arbeitsspeicher,
	 * die verbraucht wird, sondern die Menge an Speicher, den die Objekte brauchen w�rden, w�ren sie in einer Datei serialisiert gespeichert.
	 *
	 * @return Kapazit�t in Bytes
	 */
	public int getMemoryCapacity() {
		return _memoryCapacity;
	}

	/**
	 * Gibt den Festplattenplatz zur�ck, der von dieser Queue in Benutzung ist. Die Dateigr��e kann aufgrund von Fragmentierung usw. gr��er sein.
	 *
	 * @return Gr��e in Bytes
	 */
	public synchronized long getDiskUsed() {
		return _fileQueue.getDiskUsed();
	}

	/**
	 * Gibt den maximal genutzen Festplattenplatz zur�ck, der von dieser Queue verwendet wird. Dieser Wert kann von getDiskUsed() um maximal eine Objektgr��e
	 * �berschritten werden.
	 *
	 * @return Gr��e in Bytes
	 */
	public long getDiskCapacity() {
		return _fileQueue.getCapacity();
	}

	/** Removes all of the elements from this queue. The queue will be empty after this call returns. */
	@Override
	public synchronized void clear() {
		_fileQueue.clear();
		_memoryQueue.clear();
	}

	@Override
	public String toString() {
		return "fileBackedQueue{" + _memoryUsed + " bytes (" + _memoryQueue.size() + " Entries) im Speicher, " + _fileQueue + '}';
	}

	private class MergeItr implements Iterator<E> {

		private final Iterator<E> _itr1;

		private final Iterator<E> _itr2;

		public MergeItr(final Iterator<E> itr1, final Iterator<E> itr2) {
			_itr1 = itr1;
			_itr2 = itr2;
		}

		public boolean hasNext() {
			return _itr1.hasNext() || _itr2.hasNext();
		}

		public E next() {
			if(_itr1.hasNext()) return _itr1.next();
			return _itr2.next();
		}

		public void remove() {
			throw new UnsupportedOperationException("Nicht unterst�tzt");
		}
	}
}

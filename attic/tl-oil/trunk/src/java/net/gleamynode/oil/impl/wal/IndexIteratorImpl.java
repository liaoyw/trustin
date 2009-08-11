/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
/*
 * @(#) $Id: IndexIteratorImpl.java 38 2004-11-14 15:20:22Z trustin $
 */
package net.gleamynode.oil.impl.wal;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.lang.Validate;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;

import net.gleamynode.oil.IndexIterator;
import net.gleamynode.oil.OilException;
import net.gleamynode.oil.impl.wal.log.IndexPutLog;
import net.gleamynode.oil.impl.wal.log.IndexRemoveLog;


/**
 * {@link IndexImpl}�� �����ϰ� �ִ� key - value ���� ���ʴ�� ��ȸ�� �� �ִ� iterator.
 *
 * JDBC�� {@link java.sql.ResultSet}�� ������ ������� ��ȸ�� �� �ִ�.
 *
 * <pre>
 * IndexIterator it = null;
 * try {
 *         it = idx.iterator();
 *         while (it.next()) {
 *                 System.out.println(&quot;key: &quot; + it.key() + &quot; value: &quot; + it.value());
 *         }
 * } catch (PMFException e) {
 *         e.printStackTrace();
 * } finally {
 *         if (it != null) {
 *                 try {
 *                         it.close();
 *                 } catch (PMFException e) {
 *                 }
 *                 it = null;
 *         }
 * }
 * </pre>
 *
 * <h3>Fail-fast modification check</h3>
 * �ε����� ��ȸ ���߿� �ε����� ������ ������ ��� ���ܰ� �߻��Ѵ�. �̴� Java Collections API��
 * {@link java.util.ConcurrentModificationException}�� �߻��ϴ� �Ͱ� ������ ������ �����Ѵ�.
 *
 * @author Trustin Lee (trustin@gmail.com)
 * @version $Rev: 38 $, $Date: 2004-11-15 00:20:22 +0900 (월, 15 11월 2004) $
 */
class IndexIteratorImpl implements IndexIterator {
    private final WalDatabase grandparent;
    private final IndexImpl parent;
    private final ReadWriteLock parentLock;
    private final ReadWriteLock lock =
        new ReentrantWriterPreferenceReadWriteLock();
    private final LogStore store;
    private final Iterator it;
    private Entry entry;

    IndexIteratorImpl(IndexImpl parent, ReadWriteLock parentLock, Iterator it) {
        this.parent = parent;
        this.parentLock = parentLock;
        this.grandparent = parent.getParent();
        this.store = grandparent.getStore();
        this.it = it;
    }

    /**
     * Iterator �� ���� ��ġ�� �� �� ������ ������Ű�� �� ���� ���� �ִ��� �����Ѵ�.
     *
     * @return �� key - value ���� �����ִ��� ����
     */
    public boolean next() {
        boolean result;
        grandparent.acquireSharedLock();
        SyncUtil.acquire(parentLock.readLock());
        SyncUtil.acquire(lock.writeLock());

        try {
            if (it.hasNext()) {
                entry = (Entry) it.next();
                result = true;
            } else {
                entry = null;
                result = false;
            }
        } finally {
            lock.writeLock().release();
            parentLock.readLock().release();
            grandparent.releaseSharedLock();
        }

        return result;
    }

    /**
     * Iterator �� ����Ű�� �ִ� key - value ���� key ���� �����Ѵ�.
     *
     * @return ���� ��ġ�� key ��
     */
    public Object getKey() {
        Object result;
        grandparent.acquireSharedLock();
        SyncUtil.acquire(parentLock.readLock());
        SyncUtil.acquire(lock.readLock());

        try {
            result = currentEntry().getKey();
        } finally {
            lock.readLock().release();
            parentLock.readLock().release();
            grandparent.releaseSharedLock();
        }

        return result;
    }

    /**
     * Iterator �� ����Ű�� �ִ� key - value ���� value ���� �����Ѵ�.
     *
     * @return ���� ��ġ�� value ��
     */
    public Object getValue() {
        Object result;
        grandparent.acquireSharedLock();
        SyncUtil.acquire(parentLock.readLock());
        SyncUtil.acquire(lock.readLock());

        try {
            result = currentEntry().getValue();
        } finally {
            lock.readLock().release();
            parentLock.readLock().release();
            grandparent.releaseSharedLock();
        }

        return result;
    }

    /**
     * Iterator �� ����Ű�� �ִ� key - value ���� value ���� �����Ѵ�.
     *
     * @param newValue
     *            ������ ��
     * @throws net.gleamynode.oil.OilException
     *             ���� value �� ���ſ� �������� ���.
     */
    public Object setValue(Object newValue) {
        Validate.notNull(newValue);

        Object result;

        grandparent.acquireSharedLock();
        SyncUtil.acquire(parentLock.writeLock());
        SyncUtil.acquire(lock.writeLock());

        try {
            Entry e = currentEntry();
            result = e.setValue(newValue);
            store.write(new IndexPutLog(parent.getId(), e.getKey(), newValue));
        } finally {
            lock.writeLock().release();
            parentLock.writeLock().release();
            grandparent.releaseSharedLock();
        }

        return result;
    }

    public void update() {
        grandparent.acquireSharedLock();
        SyncUtil.acquire(parentLock.readLock());
        SyncUtil.acquire(lock.writeLock());

        try {
            Entry e = currentEntry();
            store.write(new IndexPutLog(parent.getId(), e.getKey(),
                                        e.getValue()));
        } finally {
            lock.writeLock().release();
            parentLock.readLock().release();
            grandparent.releaseSharedLock();
        }
    }

    public boolean isRemoved() {
        boolean result;
        grandparent.acquireSharedLock();
        SyncUtil.acquire(parentLock.readLock());
        SyncUtil.acquire(lock.readLock());
        result = entry == null;
        lock.readLock().release();
        parentLock.readLock().release();
        grandparent.releaseSharedLock();

        return result;
    }

    /**
     * Iterator �� ����Ű�� �ִ� key - value ���� �����Ѵ�. ������ ������ {@link #getKey()}��
     * {@link #getValue()},{@link #setValue(Object)}ȣ���� ���ܸ� ������. ���� ���� �б� ����
     * {@link #next()}�� ȣ���� �� �ִ�.
     *
     * @throws OilException
     *             ���� �����ϴ� �� �������� ��
     */
    public Object remove() {
        Object result;

        grandparent.acquireSharedLock();
        SyncUtil.acquire(parentLock.writeLock());
        SyncUtil.acquire(lock.writeLock());

        try {
            Entry e = currentEntry();
            result = e.getValue();
            store.write(new IndexRemoveLog(parent.getId(), e.getKey()));
            it.remove();
            entry = null;
        } finally {
            lock.writeLock().release();
            parentLock.writeLock().release();
            grandparent.releaseSharedLock();
        }

        return result;
    }

    private Entry currentEntry() {
        if (entry == null) {
            throw new IllegalStateException();
        } else {
            return entry;
        }
    }
}

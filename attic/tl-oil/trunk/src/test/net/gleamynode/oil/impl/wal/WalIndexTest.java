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
 * @(#) $Id: WalIndexTest.java 66 2005-01-03 09:02:37Z trustin $
 */
package net.gleamynode.oil.impl.wal;

import java.io.File;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.gleamynode.oil.Index;
import net.gleamynode.oil.IndexIterator;


/**
 * {@link TestCase} for {@link Index} implementation in
 * <code>net.gleamynode.oil.impl.wal</code> package.
 *
 * @author Trustin Lee (trustin@gmail.com)
 * @version $Rev: 66 $, $Date: 2005-01-03 18:02:37 +0900 (월, 03  1월 2005) $
 */
public class WalIndexTest extends TestCase {
    private static final int ITEM_COUNT = 1024;
    private static final String DB_FILE = "test.db";
    private static final String DB_CAT_FILE = DB_FILE + ".cat";
    private static final String INDEX_NAME = "test";
    private static final Properties PROPERTIES = new Properties();

    static {
        PROPERTIES.setProperty("logStore.file", DB_FILE);
    }

    private WalDatabase db;
    private Index index;
    private long startTime;

    public WalIndexTest() {
    }

    public final void setUp() throws Exception {
        new File(DB_FILE).delete();
        new File(DB_CAT_FILE).delete();

        db = new WalDatabase();
        db.setProperties(PROPERTIES);
        db.open();
        db.defragment();
        index = db.getIndex(INDEX_NAME);

        startTime = System.currentTimeMillis();

        for (int i = 0; i < ITEM_COUNT; i++) {
            WalTestItem testItem = new WalTestItem(String.valueOf(i));
            index.put(testItem, testItem);
        }

        System.out.print((System.currentTimeMillis() - startTime) / 1000.0);
        System.out.print(" / ");
        startTime = System.currentTimeMillis();
    }

    public final void tearDown() throws Exception {
        System.out.println((System.currentTimeMillis() - startTime) / 1000.0);

        int oldSize = index.size();
        db.close();
        db.open(ProgressMonitorImpl.INSTANCE);
        index = db.getIndex(INDEX_NAME);
        Assert.assertEquals(oldSize, index.size());
        db.defragment(ProgressMonitorImpl.INSTANCE);
        db.close();
        new File(DB_FILE).delete();
        new File(DB_CAT_FILE).delete();
    }

    /**
     * {@link IndexImpl#get(Object)}�� �׽�Ʈ�Ѵ�.
     * <ul>
     * <li>key�� ������ �� �ش� value�� �����ϴ°�.</li>
     * <li>key�� ���� �� <code>null</code> �� �����ϴ°�.</li>
     * </ul>
     */
    public void testGet() throws Exception {
        for (int i = 0; i < (ITEM_COUNT * 2); i++) {
            WalTestItem testItem = new WalTestItem(String.valueOf(i));

            if (i < ITEM_COUNT) {
                Assert.assertEquals(testItem, index.get(testItem));
            } else {
                Assert.assertNull(index.get(testItem));
            }
        }
    }

    /**
     * {@link IndexImpl#put(Object, Object)}�� �׽�Ʈ�Ѵ�.
     * <ul>
     * <li>key - value �� ��Ȯ�� �߰��Ǵ°�?</li>
     * <li>�̹� �����ϴ� key�� ���� put�� �� ���� value�� ��������°�?</li>
     * </ul>
     */
    public void testPut() throws Exception {
        for (int i = 0; i < ITEM_COUNT; i++) {
            WalTestItem testItem = new WalTestItem(String.valueOf(i));
            Assert.assertEquals(testItem, index.get(testItem));
        }

        for (int i = 0; i < ITEM_COUNT; i++) {
            WalTestItem testItem = new WalTestItem(String.valueOf(i));
            index.put(testItem, testItem + "0");
        }

        for (int i = 0; i < ITEM_COUNT; i++) {
            WalTestItem testItem = new WalTestItem(String.valueOf(i));
            Assert.assertEquals(testItem + "0", index.get(testItem));
        }
    }

    /**
     * {@link IndexImpl#containsKey(Object)}�� �׽�Ʈ�Ѵ�.
     * <ul>
     * <li>key�� ������ �� <code>true</code> �� �����ϴ°�.</li>
     * <li>key�� ���� �� <code>false</code> �� �����ϴ°�.</li>
     * </ul>
     */
    public void testContainsKey() throws Exception {
        for (int i = 0; i < (ITEM_COUNT * 2); i++) {
            WalTestItem testItem = new WalTestItem(String.valueOf(i));

            if (i < ITEM_COUNT) {
                Assert.assertTrue(index.containsKey(testItem));
            } else {
                Assert.assertFalse(index.containsKey(testItem));
            }
        }
    }

    /**
     * {@link IndexImpl#remove(Object)}�� �׽�Ʈ�Ѵ�.
     * <ul>
     * <li>key�� �������� �� <code>true</code> �� �����ϴ°�.</li>
     * <li>key�� ���� �� <code>false</code> �� �����ϴ°�.</li>
     * </ul>
     */
    public void testRemove() throws Exception {
        for (int i = 0; i < (ITEM_COUNT * 2); i += 2) {
            WalTestItem testItem = new WalTestItem(String.valueOf(i));

            if (i < ITEM_COUNT) {
                Assert.assertNotNull(index.remove(testItem));
            } else {
                Assert.assertNull(index.remove(testItem));
            }
        }
    }

    /**
     * {@link IndexImpl#clear()}�� �׽�Ʈ�Ѵ�. ������ ��� ��, ���� �߰��ߴ� �׸��� ���� ���θ� ��� �˻��Ѵ�.
     */
    public void testClear() throws Exception {
        index.clear();
        Assert.assertEquals(index.size(), 0);

        for (int i = 0; i < (ITEM_COUNT * 2); i++) {
            WalTestItem testItem = new WalTestItem(String.valueOf(i));
            Assert.assertFalse(index.containsKey(testItem));
        }
    }

    public void testIterator() throws Exception {
        try {
            IndexIterator it = index.iterator();
            int iterationCount = 0;

            while (it.next()) {
                Assert.assertEquals(index.get(it.getKey()), it.getValue());

                try {
                    it.setValue(null);
                    Assert.fail("IndexIterator.setValue(null) may not work.");
                } catch (Exception e) {
                }

                it.setValue("TEST-VALUE");
                Assert.assertEquals("TEST-VALUE", index.get(it.getKey()));

                it.remove();
                Assert.assertTrue(it.isRemoved());

                try {
                    it.getKey();
                    it.getValue();
                    it.remove();
                    Assert.fail("invoking it.getXXX()/remove() after it.remove() is invoked may not work.");
                } catch (Exception e) {
                }

                iterationCount++;
            }

            Assert.assertEquals(ITEM_COUNT, iterationCount);
            Assert.assertEquals(0, index.size());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * {@link IndexImpl#size()}�� �׽�Ʈ�Ѵ�. �ε����� ������ ������ �� �ִ� �޼ҵ带 ��� ȣ���� �� ����Ǵ�
     * �ε��� ũ�⸦ ���� �� �ִ��� Ȯ���Ѵ�.
     */
    public void testSize() throws Exception {
        index.put("TEST-SIZE", "TEST-SIZE-VALUE");
        index.remove("0");
        Assert.assertEquals(ITEM_COUNT + 1, index.size());
    }
}

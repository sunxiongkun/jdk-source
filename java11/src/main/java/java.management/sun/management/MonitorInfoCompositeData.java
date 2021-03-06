/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package sun.management;

import java.lang.management.MonitorInfo;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;

/**
 * A CompositeData for MonitorInfo for the local management support.
 * This class avoids the performance penalty paid to the
 * construction of a CompositeData use in the local case.
 */
public class MonitorInfoCompositeData extends LazyCompositeData {
    private final MonitorInfo lock;

    private MonitorInfoCompositeData(MonitorInfo mi) {
        this.lock = mi;
    }

    public MonitorInfo getMonitorInfo() {
        return lock;
    }

    public static CompositeData toCompositeData(MonitorInfo mi) {
        MonitorInfoCompositeData micd = new MonitorInfoCompositeData(mi);
        return micd.getCompositeData();
    }

    protected CompositeData getCompositeData() {
        // CONTENTS OF THIS ARRAY MUST BE SYNCHRONIZED WITH
        // MONITOR_INFO_ATTRIBUTES!

        int len = MONITOR_INFO_ATTRIBUTES.length;
        Object[] values = new Object[len];
        CompositeData li = LockInfoCompositeData.toCompositeData(lock);

        for (int i = 0; i < len; i++) {
            String item = MONITOR_INFO_ATTRIBUTES[i];
            if (item.equals(LOCKED_STACK_FRAME)) {
                StackTraceElement ste = lock.getLockedStackFrame();
                values[i] = (ste != null ? StackTraceElementCompositeData.
                                               toCompositeData(ste)
                                         : null);
            } else if (item.equals(LOCKED_STACK_DEPTH)) {
                values[i] = lock.getLockedStackDepth();
            } else {
                values[i] = li.get(item);
            }
        }

        try {
            return new CompositeDataSupport(MONITOR_INFO_COMPOSITE_TYPE,
                                            MONITOR_INFO_ATTRIBUTES,
                                            values);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    private static final String CLASS_NAME         = "className";
    private static final String IDENTITY_HASH_CODE = "identityHashCode";
    private static final String LOCKED_STACK_FRAME = "lockedStackFrame";
    private static final String LOCKED_STACK_DEPTH = "lockedStackDepth";

    private static final String[] MONITOR_INFO_ATTRIBUTES = {
        CLASS_NAME,
        IDENTITY_HASH_CODE,
        LOCKED_STACK_FRAME,
        LOCKED_STACK_DEPTH
    };

    private static final CompositeType MONITOR_INFO_COMPOSITE_TYPE;
    private static final CompositeType V6_COMPOSITE_TYPE;
    static {
        try {
            MONITOR_INFO_COMPOSITE_TYPE = (CompositeType)
                MappedMXBeanType.toOpenType(MonitorInfo.class);

            OpenType<?>[] types = new OpenType<?>[MONITOR_INFO_ATTRIBUTES.length];
            for (int i = 0; i < MONITOR_INFO_ATTRIBUTES.length; i++) {
                String name = MONITOR_INFO_ATTRIBUTES[i];
                types[i] = name.equals(LOCKED_STACK_FRAME)
                            ? StackTraceElementCompositeData.v5CompositeType()
                            : MONITOR_INFO_COMPOSITE_TYPE.getType(name);
            }
            V6_COMPOSITE_TYPE = new CompositeType("MonitorInfo",
                                                  "JDK 6 MonitorInfo",
                                                  MONITOR_INFO_ATTRIBUTES,
                                                  MONITOR_INFO_ATTRIBUTES,
                                                  types);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    static CompositeType v6CompositeType() {
        return V6_COMPOSITE_TYPE;
    }

    static CompositeType compositeType() {
        return MONITOR_INFO_COMPOSITE_TYPE;
    }

    public static String getClassName(CompositeData cd) {
        return getString(cd, CLASS_NAME);
    }

    public static int getIdentityHashCode(CompositeData cd) {
        return getInt(cd, IDENTITY_HASH_CODE);
    }

    public static StackTraceElement getLockedStackFrame(CompositeData cd) {
        CompositeData ste = (CompositeData) cd.get(LOCKED_STACK_FRAME);
        if (ste != null) {
            return StackTraceElementCompositeData.from(ste);
        } else {
            return null;
        }
    }

    public static int getLockedStackDepth(CompositeData cd) {
        return getInt(cd, LOCKED_STACK_DEPTH);
    }

    /** Validate if the input CompositeData has the expected
     * CompositeType (i.e. contain all attributes with expected
     * names and types).
     */
    public static void validateCompositeData(CompositeData cd) {
        if (cd == null) {
            throw new NullPointerException("Null CompositeData");
        }

        if (!isTypeMatched(MONITOR_INFO_COMPOSITE_TYPE, cd.getCompositeType()) &&
            !isTypeMatched(V6_COMPOSITE_TYPE, cd.getCompositeType())) {
            throw new IllegalArgumentException(
                "Unexpected composite type for MonitorInfo");
        }
    }

    private static final long serialVersionUID = -5825215591822908529L;
}

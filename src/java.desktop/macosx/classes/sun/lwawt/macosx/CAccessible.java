/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.lwawt.macosx;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static javax.accessibility.AccessibleContext.ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY;
import static javax.accessibility.AccessibleContext.ACCESSIBLE_CARET_PROPERTY;
import static javax.accessibility.AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY;
import static javax.accessibility.AccessibleContext.ACCESSIBLE_TEXT_PROPERTY;
import sun.awt.AWTAccessor;


class CAccessible extends CFRetainedResource implements Accessible {

    public static CAccessible getCAccessible(final Accessible a) {
        if (a == null) return null;
        AccessibleContext context = a.getAccessibleContext();
        AWTAccessor.AccessibleContextAccessor accessor
                = AWTAccessor.getAccessibleContextAccessor();
        final CAccessible cachedCAX = (CAccessible) accessor.getNativeAXResource(context);
        if (cachedCAX != null) {
            return cachedCAX;
        }
        final CAccessible newCAX = new CAccessible(a);
        accessor.setNativeAXResource(context, newCAX);
        return newCAX;
    }

    private static native void unregisterFromCocoaAXSystem(long ptr);
    private static native void valueChanged(long ptr);
    private static native void selectedTextChanged(long ptr);
    private static native void selectionChanged(long ptr);

    private Accessible accessible;

    private AccessibleContext activeDescendant;

    private CAccessible(final Accessible accessible) {
        super(0L, true); // real pointer will be poked in by native

        if (accessible == null) throw new NullPointerException();
        this.accessible = accessible;

        if (accessible instanceof Component) {
            addNotificationListeners((Component)accessible);
        }
    }

    @Override
    protected synchronized void dispose() {
        if (ptr != 0) unregisterFromCocoaAXSystem(ptr);
        super.dispose();
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        return accessible.getAccessibleContext();
    }

    public void addNotificationListeners(Component c) {
        if (c instanceof Accessible) {
            AccessibleContext ac = ((Accessible)c).getAccessibleContext();
            ac.addPropertyChangeListener(new AXChangeNotifier());
        }
        if (c instanceof JProgressBar) {
            JProgressBar pb = (JProgressBar) c;
            pb.addChangeListener(new AXProgressChangeNotifier());
        } else if (c instanceof JSlider) {
            JSlider slider = (JSlider) c;
            slider.addChangeListener(new AXProgressChangeNotifier());
        }
    }


    private class AXChangeNotifier implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if ( ptr != 0 ) {
                if (name.compareTo(ACCESSIBLE_CARET_PROPERTY) == 0) {
                    selectedTextChanged(ptr);
                } else if (name.compareTo(ACCESSIBLE_TEXT_PROPERTY) == 0 ) {
                    valueChanged(ptr);
                } else if (name.compareTo(ACCESSIBLE_SELECTION_PROPERTY) == 0 ) {
                    selectionChanged(ptr);
                }  else if (name.compareTo(ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY) == 0 ) {
                    Object nv = e.getNewValue();
                    if (nv instanceof AccessibleContext) {
                        activeDescendant = (AccessibleContext)nv;
                    }
                }
            }
        }
    }

    private class AXProgressChangeNotifier implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (ptr != 0) valueChanged(ptr);
        }
    }

    static Accessible getSwingAccessible(final Accessible a) {
        return (a instanceof CAccessible) ? ((CAccessible)a).accessible : a;
    }

    static AccessibleContext getActiveDescendant(final Accessible a) {
        return (a instanceof CAccessible) ? ((CAccessible)a).activeDescendant : null;
    }

}
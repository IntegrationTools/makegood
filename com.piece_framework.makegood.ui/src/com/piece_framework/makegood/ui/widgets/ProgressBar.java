/**
 * Copyright (c) 2010-2011, 2013-2014 KUBO Atsuhiro <kubo@iteman.jp>,
 * All rights reserved.
 *
 * This file is part of MakeGood.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.piece_framework.makegood.ui.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

public class ProgressBar extends Composite implements PaintListener, ControlListener {
    private Color[] passedColor;
    private Color[] failedColor;
    private Color[] stoppedColor;
    private CLabel bar;
    private int rate;

    public ProgressBar(Composite parent) {
        super(parent, SWT.BORDER);

        passedColor = new Color[] {
            new Color(getDisplay(), MakeGoodColor.GRADIENT_PASSED),
            new Color(getDisplay(), MakeGoodColor.PASSED)
        };
        failedColor = new Color[] {
            new Color(getDisplay(), MakeGoodColor.GRADIENT_FAILED),
            new Color(getDisplay(), MakeGoodColor.FAILED)
        };
        stoppedColor = new Color[] {
            new Color(getDisplay(), MakeGoodColor.GRADIENT_STOPPED),
            new Color(getDisplay(), MakeGoodColor.STOPPED)
        };

        addPaintListener(this);

        bar = new CLabel(this, SWT.NONE);
        bar.addControlListener(this);
        bar.addPaintListener(this);

        clear();
    }

    @Override
    public void paintControl(PaintEvent e) {
        String text = rate + "%"; //$NON-NLS-1$
        Point size = getSize();
        FontMetrics fontMetrics = e.gc.getFontMetrics();
        int width = fontMetrics.getAverageCharWidth() * text.length();
        int height = fontMetrics.getHeight();
        e.gc.drawText(text, (size.x - width) / 2, (size.y - height) / 2, true);
    }

    @Override
    public void controlMoved(ControlEvent e) {
    }

    @Override
    public void controlResized(ControlEvent e) {
        update(rate);
    }

    public void update(int rate) {
        int maxWidth = getSize().x;

        int width = bar.getSize().x;
        if (rate < 100) {
            width = (int) (maxWidth * ((double) rate / 100d));
        } else if (rate >= 100) {
            width = maxWidth;
        }
        final int barWidth = width;

        getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!bar.isDisposed()) {
                    Point size = bar.getSize();
                    if (size.x != barWidth) {
                        size.x = barWidth;
                        bar.setSize(size);
                        redraw();
                        bar.redraw();
                    }
                }
            }
        });

        this.rate = rate;
    }

    /**
     * @since 1.7.0
     */
    public void markAsPassed() {
        setColor(passedColor);
    }

    public void markAsFailed() {
        setColor(failedColor);
    }

    public void markAsStopped() {
        setColor(stoppedColor);
    }

    /**
     * @since 1.7.0
     */
    private void setColor(Color[] color) {
        bar.setBackground(color, new int[] { 100 }, true);
    }

    public void clear() {
        update(0);
    }
}

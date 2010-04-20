/**
 * Copyright (c) 2010 MATSUFUJI Hideharu <matsufuji2008@gmail.com>,
 *               2010 KUBO Atsuhiro <kubo@iteman.jp>,
 * All rights reserved.
 *
 * This file is part of MakeGood.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.piece_framework.makegood.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.piece_framework.makegood.ui.launch.AllTestsStatus;
import com.piece_framework.makegood.ui.launch.TestRunner;

public class RunAllTestsAction implements IViewActionDelegate {
    public static final String ID = "com.piece_framework.makegood.ui.viewActions.resultView.runAllTests"; //$NON-NLS-1$

    @Override
    public void init(IViewPart view) {
    }

    @Override
    public void run(IAction action) {
        TestRunner.runAllTests(AllTestsStatus.getInstance().getTarget());
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }
}
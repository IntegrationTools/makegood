/**
 * Copyright (c) 2010 MATSUFUJI Hideharu <matsufuji2008@gmail.com>,
 *               2011 KUBO Atsuhiro <kubo@iteman.jp>,
 * All rights reserved.
 *
 * This file is part of MakeGood.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.piece_framework.makegood.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class RerunTestHandler extends RunHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        getTestRunner().rerunLastTest();
        return null;
    }

    @Override
    protected boolean doIsEnabled() {
        return getTestRunner().hasLastTest();
    }
}

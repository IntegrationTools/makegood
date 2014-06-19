/**
 * Copyright (c) 2010 MATSUFUJI Hideharu <matsufuji2008@gmail.com>,
 *               2010-2012, 2014 KUBO Atsuhiro <kubo@iteman.jp>,
 * All rights reserved.
 *
 * This file is part of MakeGood.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.piece_framework.makegood.core.run;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.piece_framework.makegood.core.result.ResultType;
import com.piece_framework.makegood.core.result.TestCaseResult;
import com.piece_framework.makegood.core.result.TestSuiteResult;

public class ResultReader extends DefaultHandler {
    private File log;
    private TestSuiteResult result;
    private TestSuiteResult currentTestSuite;
    private TestCaseResult currentTestCase;
    private StringBuilder failureTrace;
    private List<ResultReaderListener> listeners = new ArrayList<ResultReaderListener>();
    private boolean stopped = false;
    private SynchronizedFileInputStream stream;

    public ResultReader(File log) {
        this.log = log;
    }

    public void read()
        throws ParserConfigurationException, SAXException, IOException {
        if (!log.exists()) {
            log.createNewFile();
        }

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        stream = new SynchronizedFileInputStream(log);
        parser.parse(stream, this);
    }

    public void stop() {
        stopped = true;
    }

    public void addListener(ResultReaderListener listener) {
        listeners.add(listener);
    }

    @Override
    public void startDocument() throws SAXException {
        startTest();
    }

    /**
     * @since 1.7.0
     */
    private void startTest() {
        for (ResultReaderListener listener: listeners) {
            listener.startTest();
        }
    }

    @Override
    public void startElement(String uri,
                             String localName,
                             String qualifiedName,
                             Attributes attributes
                             ) throws SAXException {
        if (qualifiedName.equalsIgnoreCase("testsuite")) { //$NON-NLS-1$
            startTestSuite(createTestSuite(attributes));
        } else if (qualifiedName.equalsIgnoreCase("testcase")) { //$NON-NLS-1$
            startTestCase(createTestCase(attributes));
        } else if (qualifiedName.equalsIgnoreCase("failure")) { //$NON-NLS-1$
            startFailure(createFailureTestCase(attributes));
        } else if (qualifiedName.equalsIgnoreCase("error")) { //$NON-NLS-1$
            startError(createErrorTestCase(attributes));
        }
    }

    @Override
    public void characters(char[] characters,
                           int start,
                           int length
                           ) throws SAXException {
        if (failureTrace != null) {
            failureTrace.append(new String(characters, start, length));
        }
    }

    @Override
    public void endElement(String uri,
                           String localName,
                           String qualifiedName
                           ) throws SAXException {
        if (qualifiedName.equalsIgnoreCase("testsuite")) { //$NON-NLS-1$
            endTestSuite();
        } else if (qualifiedName.equalsIgnoreCase("testcase")) { //$NON-NLS-1$
            endTestCase();
        } else if (qualifiedName.equalsIgnoreCase("failure")) { //$NON-NLS-1$
            endFailure();
        } else if (qualifiedName.equalsIgnoreCase("error")) { //$NON-NLS-1$
            endError();
        }
    }

    @Override
    public void endDocument() throws SAXException {
        stop();
        endTest();
    }

    /**
     * @since 1.7.0
     */
    private void endTest() {
        for (ResultReaderListener listener: listeners) {
            listener.endTest();
        }
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        if (stopped) {
            return;
        }

        super.error(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        if (stopped) {
            return;
        }

        super.fatalError(e);
    }

    public TestSuiteResult getResult() {
        return result;
    }

    private void startTestSuite(TestSuiteResult testSuite) {
        if (result == null) {
            result = testSuite;
            for (ResultReaderListener listener: listeners) {
                listener.onFirstTestSuite(testSuite);
            }
        } else {
            currentTestSuite.addChild(testSuite);
        }

        currentTestSuite = testSuite;

        for (ResultReaderListener listener: listeners) {
            listener.startTestSuite(currentTestSuite);
        }
    }

    private void endTestSuite() {
        for (ResultReaderListener listener: listeners) {
            listener.endTestSuite(currentTestSuite);
        }

        currentTestSuite = (TestSuiteResult) currentTestSuite.getParent();
    }

    private void startTestCase(TestCaseResult testCase) {
        if (currentTestSuite != null) {
            currentTestSuite.addChild(testCase);
        }

        currentTestCase = testCase;

        for (ResultReaderListener listener: listeners) {
            listener.startTestCase(currentTestCase);
        }
    }

    private void endTestCase() {
        currentTestCase.fix();

        for (ResultReaderListener listener: listeners) {
            listener.endTestCase(currentTestCase);
        }

        currentTestCase = null;
    }

    private void startFailure(TestCaseResult failure) {
        failureTrace = new StringBuilder();
        for (ResultReaderListener listener: listeners) {
            listener.startFailure(failure);
        }
    }

    private void endFailure() {
        currentTestCase.setFailureTrace(failureTrace.toString());
        failureTrace = null;

        for (ResultReaderListener listener: listeners) {
            listener.endFailure(currentTestCase);
        }

        if (currentTestCase.isArtificial()) {
            endTestCase();
        }
    }

    /**
     * @since 1.7.0
     */
    private void startError(TestCaseResult error) {
        failureTrace = new StringBuilder();
        for (ResultReaderListener listener: listeners) {
            listener.startError(error);
        }
    }

    /**
     * @since 1.7.0
     */
    private void endError() {
        currentTestCase.setFailureTrace(failureTrace.toString());
        failureTrace = null;

        for (ResultReaderListener listener: listeners) {
            listener.endError(currentTestCase);
        }

        if (currentTestCase.isArtificial()) {
            endTestCase();
        }
    }

    private TestSuiteResult createTestSuite(Attributes attributes) {
        TestSuiteResult testSuite = new TestSuiteResult(attributes.getValue("name")); //$NON-NLS-1$
        if (attributes.getIndex("class") != -1) { //$NON-NLS-1$
            testSuite.setClassName(attributes.getValue("class")); //$NON-NLS-1$
        }
        if (attributes.getIndex("file") != -1) { //$NON-NLS-1$
            testSuite.setFile(attributes.getValue("file")); //$NON-NLS-1$
        }
        if (attributes.getIndex("fullPackage") != -1) { //$NON-NLS-1$
            testSuite.setFullPackageName(attributes.getValue("fullPackage")); //$NON-NLS-1$
        }
        if (attributes.getIndex("package") != -1) { //$NON-NLS-1$
            testSuite.setPackageName(attributes.getValue("package")); //$NON-NLS-1$
        }

        if (result == null) {
            testSuite.setAllTestCount(Integer.parseInt(attributes.getValue("tests"))); //$NON-NLS-1$
        }

        return testSuite;
    }

    private TestCaseResult createTestCase(Attributes attributes) {
        TestCaseResult testCase = new TestCaseResult(attributes.getValue("name")); //$NON-NLS-1$
        if (attributes.getIndex("file") != -1) { //$NON-NLS-1$
            testCase.setFile(attributes.getValue("file")); //$NON-NLS-1$
        }
        if (attributes.getIndex("class") != -1) { //$NON-NLS-1$
            testCase.setClassName(attributes.getValue("class")); //$NON-NLS-1$
        }
        if (attributes.getIndex("method") != -1) { //$NON-NLS-1$
            testCase.setMethodName(attributes.getValue("method")); //$NON-NLS-1$
        }
        if (attributes.getIndex("line") != -1) { //$NON-NLS-1$
            testCase.setLine(Integer.parseInt(attributes.getValue("line"))); //$NON-NLS-1$
        }

        return testCase;
    }

    private TestCaseResult createFailureTestCase(Attributes attributes) {
        return createFailureOrErrorTestCase(attributes, ResultType.FAILURE);
    }

    private TestCaseResult createErrorTestCase(Attributes attributes) {
        return createFailureOrErrorTestCase(attributes, ResultType.ERROR);
    }

    /**
     * @since 1.7.0
     */
    private TestCaseResult createFailureOrErrorTestCase(Attributes attributes, ResultType resultType) {
        TestCaseResult testCase;
        if (currentTestCase == null) {
            testCase = new TestCaseResult("(" + resultType.toString() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            testCase.setClassName(currentTestSuite.getName());
            testCase.setMethodName(currentTestCase.getMethodName());
            testCase.setFile(currentTestSuite.getFile());
            testCase.markAsArtificial();
            startTestCase(testCase);
        } else {
            testCase = currentTestCase;
        }
        testCase.setResultType(resultType);

        if (attributes.getIndex("type") != -1) { //$NON-NLS-1$
            testCase.setFailureType(attributes.getValue("type")); //$NON-NLS-1$
        }
        if (attributes.getIndex("file") != -1) { //$NON-NLS-1$
            testCase.setFile(attributes.getValue("file")); //$NON-NLS-1$
        }
        if (attributes.getIndex("line") != -1) { //$NON-NLS-1$
            testCase.setLine(Integer.parseInt(attributes.getValue("line"))); //$NON-NLS-1$
        }
        if (attributes.getIndex("message") != -1) { //$NON-NLS-1$
            testCase.setFailureMessage(attributes.getValue("message")); //$NON-NLS-1$
        }

        return testCase;
    }

    private class SynchronizedFileInputStream extends FileInputStream {
        private static final int READ_NO_PARAM = 1;
        private static final int READ_ARRAY = 2;
        private static final int READ_OFFSET = 3;

        public SynchronizedFileInputStream(File file) throws FileNotFoundException {
            super(file);
        }

        @Override
        public int read() throws IOException {
            return read(READ_NO_PARAM, null, 0, 0);
        }

        @Override
        public int read(byte[] bytes,
                        int offset,
                        int length
                        ) throws IOException {
            return read(READ_OFFSET, bytes, offset, length);
        }

        @Override
        public int read(byte[] bytes) throws IOException {
            return read(READ_ARRAY, bytes, 0, 0);
        }

        private int read(int readType,
                         byte[] bytes,
                         int offset,
                         int length
                         ) throws IOException {
            int result = -1;
            do {
                if (readType == READ_NO_PARAM) {
                    result = super.read();
                } else if (readType == READ_ARRAY) {
                    result = super.read(bytes);
                } else if (readType == READ_OFFSET) {
                    result = super.read(bytes, offset, length);
                }
                if (result != -1) {
                    break;
                }

                if (stopped) {
                    break;
                }
            } while (true);
            return result;
        }
    }
}

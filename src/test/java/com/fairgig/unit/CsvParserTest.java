package com.fairgig.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.service.csv.CsvParser;
import com.fairgig.service.csv.CsvRow;
import com.fairgig.util.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** FR-12: reading CSV files carefully (quotes, line ends, Excel's BOM, line numbers). */
class CsvParserTest {

    @Test
    void windowsLineEndsAreHandled() {
        List<CsvRow> rows = CsvParser.parse("a,b,c\r\n1,2,3\r\n");
        assertEquals(2, rows.size());
        assertEquals(2, rows.get(1).lineNumber());
        assertEquals(List.of("1", "2", "3"), rows.get(1).cells());
    }

    @Test
    void excelBomIsRemoved() {
        assertEquals("platform", CsvParser.parse("﻿platform,date\nUber,x").get(0).cells().get(0));
    }

    @Test
    void quotedCellsMayContainCommas() {
        assertEquals(List.of("Smith, J", "5"), CsvParser.parse("\"Smith, J\",5").get(0).cells());
    }

    @Test
    void doubledQuotesInsideQuotesBecomeOneQuote() {
        assertEquals("He said \"hi\"", CsvParser.parse("\"He said \"\"hi\"\"\",1").get(0).cells().get(0));
    }

    @Test
    void blankLinesAreSkippedButLineNumbersStayCorrect() {
        List<CsvRow> rows = CsvParser.parse("h\n\nx\n");
        assertEquals(2, rows.size());
        assertEquals(3, rows.get(1).lineNumber());
    }

    @Test
    void lastRowWithoutALineBreakIsKept() {
        assertEquals(2, CsvParser.parse("a,b\n1,2").size());
    }

    @Test
    void spacesAroundUnquotedCellsAreTrimmed() {
        assertEquals(List.of("a", "b"), CsvParser.parse(" a , b ").get(0).cells());
    }

    @Test
    void newlineInsideQuotesKeepsLaterLineNumbersRight() {
        List<CsvRow> rows = CsvParser.parse("\"multi\nline\",1\nnext,2");
        assertEquals(2, rows.size());
        assertEquals(3, rows.get(1).lineNumber());
    }

    @Test
    void anUnclosedQuoteIsReportedWithItsLine() {
        ValidationException e = assertThrows(ValidationException.class, () -> CsvParser.parse("a,b\n\"oops,2\n3,4"));
        assertTrue(e.getMessage().contains("line 2"));
    }

    @Test
    void emptyInputGivesNoRows() {
        assertTrue(CsvParser.parse("").isEmpty());
        assertTrue(CsvParser.parse(null).isEmpty());
    }
}

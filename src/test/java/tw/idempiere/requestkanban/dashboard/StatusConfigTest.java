package tw.idempiere.requestkanban.dashboard;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatusConfigTest {

    // --- isHidden() tests ---

    @Test
    void isHidden_emptyCsv_nothingIsHidden() {
        StatusConfig cfg = new StatusConfig("", "Processing,Open");
        assertFalse(cfg.isHidden("Open"));
        assertFalse(cfg.isHidden("Final"));
        assertFalse(cfg.isHidden("Processing"));
    }

    @Test
    void isHidden_listedValues_areHidden() {
        StatusConfig cfg = new StatusConfig("Final,Cancelled", "Processing,Open");
        assertTrue(cfg.isHidden("Final"));
        assertTrue(cfg.isHidden("Cancelled"));
        assertFalse(cfg.isHidden("Open"));
    }

    @Test
    void isHidden_valuesWithSpaces_areTrimmed() {
        StatusConfig cfg = new StatusConfig(" Final , Cancelled ", "Processing,Open");
        assertTrue(cfg.isHidden("Final"));
        assertTrue(cfg.isHidden("Cancelled"));
    }

    @Test
    void isHidden_null_returnsFalse() {
        StatusConfig cfg = new StatusConfig("Final", "Processing,Open");
        assertFalse(cfg.isHidden(null));
    }

    @Test
    void isHidden_caseSensitive_noMatch() {
        StatusConfig cfg = new StatusConfig("final", "Processing,Open");
        assertFalse(cfg.isHidden("Final")); // exact-case mismatch — not hidden
    }

    // --- getActiveStatusesInClause() tests ---

    @Test
    void activeStatuses_emptyCsv_fallsBackToDefault() {
        StatusConfig cfg = new StatusConfig("", "");
        String clause = cfg.getActiveStatusesInClause();
        assertTrue(clause.contains("'Processing'"), "Must contain 'Processing'");
        assertTrue(clause.contains("'Open'"), "Must contain 'Open'");
    }

    @Test
    void activeStatuses_customValues_allPresent() {
        StatusConfig cfg = new StatusConfig("", "Processing,Open,InProgress");
        String clause = cfg.getActiveStatusesInClause();
        assertTrue(clause.contains("'Processing'"));
        assertTrue(clause.contains("'Open'"));
        assertTrue(clause.contains("'InProgress'"));
    }

    @Test
    void activeStatuses_singleQuoteInValue_isEscaped() {
        StatusConfig cfg = new StatusConfig("", "It's,Open");
        String clause = cfg.getActiveStatusesInClause();
        assertTrue(clause.contains("'It''s'"), "Single quote must be escaped as ''");
    }
}

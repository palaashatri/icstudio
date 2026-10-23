package dev.icstudio.command;

/** Test-only interruption points for proving durable recovery behavior. */
public enum CommitFailpoint {
    NONE,
    AFTER_JOURNAL_READY
}

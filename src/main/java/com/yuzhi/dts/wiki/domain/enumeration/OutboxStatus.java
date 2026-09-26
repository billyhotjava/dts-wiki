package com.yuzhi.dts.wiki.domain.enumeration;

/**
 * The OutboxStatus enumeration.
 */
public enum OutboxStatus {
    PENDING,
    DONE,
    FAILED,
    BLOCKED_BY_CONFLICT,
}

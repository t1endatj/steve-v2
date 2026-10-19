package com.steve.v2.core.action;

public record ActionResult(boolean success, String message, boolean requiresReplanning) {
    public static ActionResult success(String message) { return new ActionResult(true, message, false); }
    public static ActionResult failure(String message) { return failure(message, true); }
    public static ActionResult failure(String message, boolean replan) { return new ActionResult(false, message, replan); }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}

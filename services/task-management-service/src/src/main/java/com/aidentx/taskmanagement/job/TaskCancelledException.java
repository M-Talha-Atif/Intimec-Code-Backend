package com.aidentx.taskmanagement.job;

/**
 * Thrown inside TaskExecutionJob when a cooperative cancellation signal is detected
 * at a batch boundary checkpoint. Unlike a regular failure, this is an intentional
 * operator action and must NOT be routed to handleFailure() or trigger retry logic.
 */
public class TaskCancelledException extends RuntimeException {
    public TaskCancelledException() {
        super("Task cancelled by operator request");
    }
}

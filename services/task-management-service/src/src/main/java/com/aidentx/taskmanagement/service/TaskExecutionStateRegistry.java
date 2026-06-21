package com.aidentx.taskmanagement.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

/**
 * In-memory registry providing cooperative pause and cancel signals for running
 * {@link com.aidentx.taskmanagement.job.TaskExecutionJob} instances.
 *
 * <p><b>Design:</b> The job queries {@link #checkpointAndMaybePause(UUID)} at the
 * end of every batch. If a pause has been requested the calling thread blocks
 * on a monitor until {@link #requestResume(UUID)} is called (or a cancel arrives
 * to wake it). This is the standard <em>cooperative checkpoint</em> pattern used
 * by enterprise IGA systems (SailPoint, Saviynt) to provide safe pause/resume
 * semantics without the dangers of {@link Thread#suspend()} (which can leave
 * locks held and corrupt partial writes).
 *
 * <p>All public methods are thread-safe; multiple instances of the scheduler may
 * operate in the same JVM without interference because each entry is isolated by
 * {@code taskId}.
 */
@Component
public class TaskExecutionStateRegistry {

  private final ConcurrentHashMap<UUID, Handle> handles = new ConcurrentHashMap<>();

  // -------------------------------------------------------------------------
  // Lifecycle management (called by the job)
  // -------------------------------------------------------------------------

  /**
   * Registers a running task. Must be called before the first checkpoint.
   */
  public void register(UUID taskId) {
    handles.put(taskId, new Handle());
  }

  /**
   * Removes a task from the registry. Must be called in the job's {@code finally} block.
   */
  public void deregister(UUID taskId) {
    Handle h = handles.remove(taskId);
    if (h != null) {
      // Wake anything that might still be blocking (defensive)
      synchronized (h.monitor) {
        h.monitor.notifyAll();
      }
    }
  }

  // -------------------------------------------------------------------------
  // Control commands (called by the service layer)
  // -------------------------------------------------------------------------

  /**
   * Requests the job to pause at its next batch boundary.
   */
  public void requestPause(UUID taskId) {
    Handle h = handles.get(taskId);
    if (h != null) {
      h.pauseRequested.set(true);
    }
  }

  /**
   * Clears the pause flag and wakes a blocked job thread.
   */
  public void requestResume(UUID taskId) {
    Handle h = handles.get(taskId);
    if (h != null) {
      h.pauseRequested.set(false);
      synchronized (h.monitor) {
        h.monitor.notifyAll();
      }
    }
  }

  /**
   * Requests cooperative cancellation. Wakes any blocked pause so the thread
   * can exit cleanly.
   */
  public void requestCancel(UUID taskId) {
    Handle h = handles.get(taskId);
    if (h != null) {
      h.cancelRequested.set(true);
      // Wake blocked thread so it can observe the cancel flag
      synchronized (h.monitor) {
        h.monitor.notifyAll();
      }
    }
  }

  /**
   * Returns {@code true} if a pause is pending but the job has not yet blocked.
   * Useful for logging "Task paused" before entering the checkpoint.
   */
  public boolean isPauseRequested(UUID taskId) {
    Handle h = handles.get(taskId);
    return h != null && h.pauseRequested.get();
  }

  /**
   * Returns {@code true} if a cancel is already pending for this task,
   * even before the job has called {@link #checkpointAndMaybePause}.
   */
  public boolean isCancelRequested(UUID taskId) {
    Handle h = handles.get(taskId);
    return h != null && h.cancelRequested.get();
  }

  /**
   * Returns {@code true} if a job thread is currently registered for this task,
   * meaning cooperative signals can reach it.
   */
  public boolean isRegistered(UUID taskId) {
    return handles.containsKey(taskId);
  }

  // -------------------------------------------------------------------------
  // Checkpoint (called by the job at batch boundaries)
  // -------------------------------------------------------------------------

  /**
   * Cooperative checkpoint. The job calls this after each batch completes.
   * <ul>
   *   <li>If a <b>cancel</b> is pending → returns {@code true} immediately.
   *       The job should stop processing and mark itself as cancelled.</li>
   *   <li>If a <b>pause</b> is pending → blocks the calling thread until
   *       {@link #requestResume(UUID)} or {@link #requestCancel(UUID)} is
   *       called. After waking, returns {@code true} if a cancel arrived,
   *       {@code false} if resumed normally.</li>
   *   <li>Otherwise → returns {@code false} (continue processing).</li>
   * </ul>
   *
   * @return {@code true} if the job should abort, {@code false} to continue
   * @throws InterruptedException if the thread is interrupted while paused
   */
  public boolean checkpointAndMaybePause(UUID taskId) throws InterruptedException {
    Handle h = handles.get(taskId);
    if (h == null) return false;

    if (h.cancelRequested.get()) return true;

    // Block inside the pause window, re-checking every 5 s (spurious-wake guard)
    while (h.pauseRequested.get() && !h.cancelRequested.get()) {
      synchronized (h.monitor) {
        if (h.pauseRequested.get() && !h.cancelRequested.get()) {
          h.monitor.wait(5_000);
        }
      }
    }

    return h.cancelRequested.get();
  }

  // -------------------------------------------------------------------------
  // Internal state holder
  // -------------------------------------------------------------------------

  private static final class Handle {
    final AtomicBoolean pauseRequested  = new AtomicBoolean(false);
    final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    final Object monitor = new Object();
  }
}

package com.aidentx.core.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserContextHolder functionality.
 */
class UserContextHolderTest {

    @BeforeEach
    void setUp() {
        // Ensure clean state before each test
        UserContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        UserContextHolder.clear();
    }

    @Test
    void testSetAndGetContext() {
        // Given
        UserContext context = new UserContext("user123", "johndoe", "192.168.1.1");
        
        // When
        UserContextHolder.setContext(context);
        UserContext retrievedContext = UserContextHolder.getContext();
        
        // Then
        assertNotNull(retrievedContext);
        assertEquals("user123", retrievedContext.userId());
        assertEquals("johndoe", retrievedContext.username());
        assertEquals("192.168.1.1", retrievedContext.ipAddress());
    }

    @Test
    void testClearContext() {
        // Given
        UserContext context = new UserContext("user123", "johndoe", "192.168.1.1");
        UserContextHolder.setContext(context);
        
        // When
        UserContextHolder.clear();
        UserContext retrievedContext = UserContextHolder.getContext();
        
        // Then
        assertNull(retrievedContext);
    }

    @Test
    void testNullContext() {
        // When - no context is set
        UserContext retrievedContext = UserContextHolder.getContext();
        
        // Then
        assertNull(retrievedContext);
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        // Given
        UserContext mainThreadContext = new UserContext("main-user", "main", "192.168.1.1");
        UserContextHolder.setContext(mainThreadContext);
        
        // When - create a new thread with different context
        Thread otherThread = new Thread(() -> {
            UserContext otherContext = new UserContext("other-user", "other", "192.168.1.2");
            UserContextHolder.setContext(otherContext);
            
            // Verify other thread has its own context
            UserContext retrieved = UserContextHolder.getContext();
            assertEquals("other-user", retrieved.userId());
        });
        
        otherThread.start();
        otherThread.join();
        
        // Then - main thread should still have its original context
        UserContext mainRetrieved = UserContextHolder.getContext();
        assertEquals("main-user", mainRetrieved.userId());
    }
}

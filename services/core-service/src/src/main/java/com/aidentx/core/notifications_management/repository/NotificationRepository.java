package com.aidentx.core.notifications_management.repository;

import com.aidentx.core.notifications_management.enums.NotificationType;
import com.aidentx.core.notifications_management.enums.OperationType;
import com.aidentx.core.notifications_management.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link Notification} entities.
 * <p>
 * This repository provides CRUD operations and custom query methods for notifications,
 * including filtering by notification type, operation type, and content searches.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    /**
     * Retrieves all notifications of a specific type.
     * 
     * @param type the notification type to filter by
     * @return a list of notifications of the specified type
     */
    List<Notification> findByType(NotificationType type);
    
    /**
     * Retrieves all notifications triggered by a specific operation.
     * 
     * @param operation the operation type to filter by
     * @return a list of notifications triggered by the specified operation
     */
    List<Notification> findByOperation(OperationType operation);
    
    /**
     * Retrieves notifications by both type and operation.
     * 
     * @param type the notification type to filter by
     * @param operation the operation type to filter by
     * @return a list of notifications matching both criteria
     */
    List<Notification> findByTypeAndOperation(NotificationType type, OperationType operation);
    
    /**
     * Searches for notifications containing specific text in their description.
     * 
     * @param searchText the text to search for in descriptions
     * @return a list of notifications with matching descriptions
     */
    @Query("SELECT n FROM Notification n WHERE n.description LIKE %:searchText%")
    List<Notification> findByDescriptionContaining(@Param("searchText") String searchText);
    
    /**
     * Searches for notifications containing specific text in their details.
     * 
     * @param searchText the text to search for in details
     * @return a list of notifications with matching details
     */
    @Query("SELECT n FROM Notification n WHERE n.details LIKE %:searchText%")
    List<Notification> findByDetailsContaining(@Param("searchText") String searchText);
    
    /**
     * Counts notifications by operation type.
     * 
     * @param operation the operation type to count
     * @return the count of notifications for the specified operation
     */
    Long countByOperation(OperationType operation);
}

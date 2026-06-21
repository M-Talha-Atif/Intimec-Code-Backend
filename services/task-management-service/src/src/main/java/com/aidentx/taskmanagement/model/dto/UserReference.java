package com.aidentx.taskmanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reference to a user with ID and name. Used for owner, assignees, createdBy, updatedBy fields. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserReference {
  private String id;
  private String name;
}

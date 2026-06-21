package com.aidentx.taskmanagement.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reference to an application with ID and name. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationReference {
  private UUID id;
  private String name;
}

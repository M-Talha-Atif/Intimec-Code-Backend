package com.aidentx.iam.identity_management.model.enums;

public enum ShardStatus {
  PENDING_CORRLATION, // inital state, waiting for correlation process
  UNMATCHED, // no correlation found
  DISPUTED, // multiple correlations found
  LINKED, // correlated to an identity
  PENDING_RECONCILIATION, // waiting for reconciliation process
}

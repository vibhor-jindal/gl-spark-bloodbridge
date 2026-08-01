package com.globallogic.bloodbridge.auth.model;

/**
 * Platform roles. Enforced downstream by the API Gateway, which reads the
 * `role` claim out of the JWT this service issues and denies access to
 * role-restricted routes (e.g. only ADMIN may reach the Analytics Service).
 */
public enum Role {
    DONOR,
    REQUESTER,
    ADMIN
}

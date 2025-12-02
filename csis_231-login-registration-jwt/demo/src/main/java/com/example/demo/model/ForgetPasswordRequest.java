
package com.example.demo.model;

/**
 * Request body used by the desktop client to start the
 * "forgot password" flow.
 *
 * @param email e-mail address associated with the account
 *
 * <p>When submitted, the backend sends a reset code to the provided e-mail.</p>
 */

public record ForgetPasswordRequest(String email) {}

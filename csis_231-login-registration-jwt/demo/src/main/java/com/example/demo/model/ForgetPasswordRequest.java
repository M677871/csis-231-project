
package com.example.demo.model;

/**
 * Request body used by the desktop client to start the
 * "forgot password" flow.
 *
 * @param email e-mail address associated with the account
 */

public record ForgetPasswordRequest(String email) {}

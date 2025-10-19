package com.csis231.api.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MeController {

    @GetMapping("/me")
    public Object me(Authentication auth) {
        return new Object() {
            public final String username = auth.getName();
            public final Object authorities = auth.getAuthorities();
        };
    }
}

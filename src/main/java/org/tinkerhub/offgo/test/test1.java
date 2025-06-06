package org.tinkerhub.offgo.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class test1 {
    @GetMapping("/test")
    public String test() {
        return "test";
    }
}

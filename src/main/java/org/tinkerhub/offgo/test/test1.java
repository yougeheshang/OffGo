package org.tinkerhub.offgo.test;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class test1 {
    @GetMapping("/test1")
    public String test1() {
        return "test1";
    }
}

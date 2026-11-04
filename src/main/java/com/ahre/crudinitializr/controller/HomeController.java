package com.ahre.crudinitializr.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Serves the SPA at the root and at the language-prefixed routes so links
     * such as {@code /en} or {@code /es} can preselect the UI language.
     */
    @GetMapping({"/", "/en", "/en/", "/es", "/es/"})
    public String home() {
        return "index";
    }

}

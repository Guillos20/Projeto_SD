package com.example.googol.frontend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

/**
 * GreetingController
 */
@Controller
public class GreetingController {

    @GetMapping("/")
    public String redirect() {
        return "redirect:/greeting";
    }

    @GetMapping("/greeting")
    public String greeting(@RequestParam(name = "name", required = false, defaultValue = "World") String name,
            Model model) {
        model.addAttribute("name", name);
        model.addAttribute("greeting", "Hello, " + name + "!");
        return "greeting";

    }
}

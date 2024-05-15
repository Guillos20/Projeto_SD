package com.example.googol.frontend;

import com.example.googol.backend.*;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * GreetingController
 */
@Controller
public class GreetingController {
    int count = 0;

    @GetMapping("/")
    public String redirect() {
        return "redirect:/login";
    }

    @GetMapping("/greeting")
    public String greeting(@RequestParam(name = "name", required = false, defaultValue = "World") String name,
            Model model) {
        model.addAttribute("name", name);
        model.addAttribute("greeting", "Hello, " + name + "!");
        return "greeting";

    }

    @GetMapping("/search")
    public String searchString() {
        return "search";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam(name = "username", required = true) String username,
            @RequestParam(name = "password", required = true) String password, Model model) {
        if (username.equals("admin") && password.equals("admin")) {
            model.addAttribute("username", username);
            count = 1;
            return "redirect:/admin";
        } else if (count == 0) {

            return "redirect:/error";

        } else {
            return "redirect:/admin";
        }
    }

    @GetMapping("/error")
    public String error() {
        return "error";
    }

    @PostMapping("/error")
    public String erroString() {
        if (count == 1) {
            return "redirect:/admin";
        } else {
            return "login";
        }
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/search/keyword")
    public String searchKeyword() {
        return "search/keyword";
    }

    @GetMapping("/search/URL")
    public String searchURL() {
        return "search/URL";
    }

}

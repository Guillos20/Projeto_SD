package com.example.googol.frontend;

import com.example.googol.backend.*;

import java.rmi.RemoteException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * GreetingController
 */
@Controller
public class GreetingController {

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

    @PostMapping("/search")
    public String search(@RequestParam(name = "keyword", required = false) String search,
            @RequestParam(name = "url", required = false) String search1,
            @RequestParam(name = "string", required = false) String search2)
            throws RemoteException {

        return "redirect:/results";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/results")
    public String resulString() {
        return "results";
    }

    @PostMapping("/login")
    public String login(@RequestParam(name = "username", required = true) String username,
            @RequestParam(name = "password", required = true) String password, Model model) {
        if (username.equals("admin") && password.equals("admin")) {
            model.addAttribute("username", username);
            return "redirect:/admin";
        } else {

            return "redirect:/error";

        }
    }

    @GetMapping("/error")
    public String error() {
        return "error";
    }

    @PostMapping("/error")
    public String erroString() {
        return "login";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

}

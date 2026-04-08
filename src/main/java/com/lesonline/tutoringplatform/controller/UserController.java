package com.lesonline.tutoringplatform.controller;

import com.lesonline.tutoringplatform.model.*;
import com.lesonline.tutoringplatform.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private BookingRepository bookingRepository;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user) {
        if (user.getFotoUrl() == null || user.getFotoUrl().isEmpty()) {
            user.setFotoUrl("https://cdn-icons-png.flaticon.com/512/3135/3135715.png");
        }
        userRepository.save(user);
        return "redirect:/login?registered";
    }

    @GetMapping("/login")
    public String showLoginForm() { return "login"; }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        Optional<User> user = userRepository.findByEmailAndPassword(email, password);
        if (user.isPresent()) {
            session.setAttribute("loggedInUser", user.get());
            return user.get().getRole().equals("GURU") ? "redirect:/dashboard-guru" : "redirect:/guru";
        }
        model.addAttribute("error", "Email atau Password salah!");
        return "login";
    }

    @GetMapping("/guru")
    public String listGuru(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) return "redirect:/login";
        model.addAttribute("user", loggedInUser);
        model.addAttribute("daftarGuru", userRepository.findByRole("GURU"));
        model.addAttribute("pesananSaya", bookingRepository.findByIdMurid(loggedInUser.getId()));
        return "daftar-guru";
    }

    @PostMapping("/pesan-guru/{id}")
    public String pesanGuru(@PathVariable Long id, HttpSession session) {
        User murid = (User) session.getAttribute("loggedInUser");
        User guru = userRepository.findById(id).orElse(null);

        if (murid != null && guru != null) {
            Optional<Booking> existing = bookingRepository.findByIdMuridAndIdGuruAndStatusIn(
                    murid.getId(), guru.getId(), Arrays.asList("MENUNGGU", "DISETUJUI")
            );

            if (existing.isPresent()) return "redirect:/guru?errorDuplikat";

            Booking b = new Booking();
            b.setIdMurid(murid.getId());
            b.setNamaMurid(murid.getNama());
            b.setIdGuru(guru.getId());
            b.setNamaGuru(guru.getNama());
            b.setMataPelajaran(guru.getMataPelajaran());
            b.setNoWhatsappGuru(guru.getNoWhatsapp());
            bookingRepository.save(b);
        }
        return "redirect:/guru?success";
    }

    @GetMapping("/dashboard-guru")
    public String showDashboardGuru(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.getRole().equals("GURU")) return "redirect:/login";
        model.addAttribute("user", loggedInUser);
        model.addAttribute("daftarPesanan", bookingRepository.findByIdGuru(loggedInUser.getId()));
        return "dashboard-guru";
    }

    @PostMapping("/konfirmasi-booking/{id}/{status}")
    public String konfirmasiBooking(@PathVariable Long id, @PathVariable String status) {
        bookingRepository.findById(id).ifPresent(b -> {
            b.setStatus(status);
            bookingRepository.save(b);
        });
        return "redirect:/dashboard-guru?statusUpdated";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute("user") User updatedUser, HttpSession session) {
        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser != null) {
            currentUser.setNama(updatedUser.getNama());
            currentUser.setMataPelajaran(updatedUser.getMataPelajaran());
            currentUser.setBio(updatedUser.getBio());
            currentUser.setNoWhatsapp(updatedUser.getNoWhatsapp());
            currentUser.setFotoUrl(updatedUser.getFotoUrl());
            userRepository.save(currentUser);
            session.setAttribute("loggedInUser", currentUser);
        }
        return "redirect:/dashboard-guru?updated";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
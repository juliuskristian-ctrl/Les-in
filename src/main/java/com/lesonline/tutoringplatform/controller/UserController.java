package com.lesonline.tutoringplatform.controller;

import com.lesonline.tutoringplatform.model.*;
import com.lesonline.tutoringplatform.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private BookingRepository bookingRepository;

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

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
            return user.get().getRole().equals("GURU") ? "redirect:/dashboard-guru" : "redirect:/dashboard-murid";
        }
        model.addAttribute("error", "Email atau Password salah!");
        return "login";
    }

    @GetMapping("/dashboard-murid")
    public String dashboardMurid(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) return "redirect:/login";

        List<Booking> riwayat = bookingRepository.findByIdMurid(loggedInUser.getId());
        model.addAttribute("user", loggedInUser);
        model.addAttribute("riwayat", riwayat);
        return "dashboard-murid";
    }

    @GetMapping("/guru")
    public String listGuru(Model model, HttpSession session, @RequestParam(required = false) String search) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) return "redirect:/login";

        List<User> daftarGuru;
        if (search != null && !search.isEmpty()) {
            daftarGuru = userRepository.findByRoleAndMataPelajaranContainingIgnoreCase("GURU", search);
        } else {
            daftarGuru = userRepository.findByRole("GURU");
        }

        model.addAttribute("user", loggedInUser);
        model.addAttribute("daftarGuru", daftarGuru);
        model.addAttribute("search", search);
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
        return "redirect:/dashboard-murid?success";
    }

    @PostMapping("/rate-booking/{id}")
    public String rateBooking(@PathVariable Long id, @RequestParam Integer rating, @RequestParam String ulasan) {
        bookingRepository.findById(id).ifPresent(b -> {
            b.setRating(rating);
            b.setUlasan(ulasan);
            b.setStatus("SELESAI");
            bookingRepository.save(b);
        });
        return "redirect:/dashboard-murid?rated";
    }

    @GetMapping("/dashboard-guru")
    public String showDashboardGuru(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.getRole().equals("GURU")) return "redirect:/login";

        List<Booking> listPesanan = bookingRepository.findByIdGuru(loggedInUser.getId());
        long jumlahMenunggu = listPesanan.stream().filter(b -> b.getStatus().equals("MENUNGGU")).count();

        model.addAttribute("user", loggedInUser);
        model.addAttribute("daftarPesanan", listPesanan);
        model.addAttribute("notif", jumlahMenunggu);
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

    @PostMapping("/isi-link-zoom/{id}")
    public String isiLinkZoom(@PathVariable Long id, @RequestParam String linkZoom) {
        bookingRepository.findById(id).ifPresent(b -> {
            b.setLinkZoom(linkZoom);
            bookingRepository.save(b);
        });
        return "redirect:/dashboard-guru?linkUpdated";
    }

    // FITUR BARU: GURU MENYELESAIKAN SESI SECARA KESELURUHAN
    @PostMapping("/selesaikan-sesi/{id}")
    public String selesaikanSesi(@PathVariable Long id) {
        bookingRepository.findById(id).ifPresent(b -> {
            b.setStatus("SELESAI");
            bookingRepository.save(b);
        });
        return "redirect:/dashboard-guru?sesiSelesai";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute("user") User updatedUser,
                                @RequestParam("file") MultipartFile file,
                                HttpSession session) {
        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser != null) {
            currentUser.setNama(updatedUser.getNama());
            currentUser.setMataPelajaran(updatedUser.getMataPelajaran());
            currentUser.setBio(updatedUser.getBio());
            currentUser.setNoWhatsapp(updatedUser.getNoWhatsapp());

            if (!file.isEmpty()) {
                try {
                    Path pathDir = Paths.get(UPLOAD_DIR);
                    if (!Files.exists(pathDir)) Files.createDirectories(pathDir);
                    String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    Path filePath = pathDir.resolve(fileName);
                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    currentUser.setFotoUrl("/uploads/" + fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
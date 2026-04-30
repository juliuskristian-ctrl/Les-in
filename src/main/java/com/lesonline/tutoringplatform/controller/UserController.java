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
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private BookingRepository bookingRepository;

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // --- FITUR AUTENTIKASI ---

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        Optional<User> user = userRepository.findByEmailAndPassword(email, password);
        if (user.isPresent()) {
            session.setAttribute("loggedInUser", user.get());
            // Redirect sesuai role
            return user.get().getRole().equals("GURU") ? "redirect:/dashboard-guru" : "redirect:/dashboard-murid";
        }
        model.addAttribute("error", "Email atau Password salah!");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // --- FITUR DASHBOARD MURID ---

    // FIX: Menambahkan mapping yang hilang untuk menampilkan dashboard-murid.html
    @GetMapping("/dashboard-murid")
    public String showDashboardMurid(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.getRole().equals("MURID")) {
            return "redirect:/login";
        }

        // Ambil riwayat untuk ditampilkan di dashboard murid
        List<Booking> riwayat = bookingRepository.findByIdMurid(loggedInUser.getId());

        model.addAttribute("user", loggedInUser);
        model.addAttribute("riwayat", riwayat);
        return "dashboard-murid"; // Mengacu pada dashboard-murid.html
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

        List<Booking> riwayatPesanan = bookingRepository.findByIdMurid(loggedInUser.getId());

        model.addAttribute("user", loggedInUser);
        model.addAttribute("daftarGuru", daftarGuru);
        model.addAttribute("riwayat", riwayatPesanan);
        model.addAttribute("search", search);
        return "daftar-guru";
    }

    @PostMapping("/pesan-guru/{id}")
    public String pesanGuru(
            @PathVariable Long id,
            @RequestParam String hariLes,
            @RequestParam String jamLes,
            HttpSession session) {

        User murid = (User) session.getAttribute("loggedInUser");
        User guru = userRepository.findById(id).orElse(null);

        if (murid != null && guru != null) {
            try {
                LocalTime waktuPilihanMurid = LocalTime.parse(jamLes);
                String jadwal = guru.getJadwalTetap();

                String jamRange = jadwal.substring(jadwal.indexOf("(") + 1, jadwal.indexOf(")"));
                String[] parts = jamRange.split(" - ");

                LocalTime jamMulaiGuru = LocalTime.parse(parts[0]);
                LocalTime jamSelesaiGuru = LocalTime.parse(parts[1]);

                if (waktuPilihanMurid.isBefore(jamMulaiGuru) || waktuPilihanMurid.isAfter(jamSelesaiGuru)) {
                    return "redirect:/guru?errorJam";
                }

                if (!jadwal.contains(hariLes)) {
                    return "redirect:/guru?errorJam";
                }

            } catch (Exception e) {
                return "redirect:/guru?errorJadwalBelumSiap";
            }

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
            b.setHariLes(hariLes);
            b.setJamLes(LocalTime.parse(jamLes));
            b.setStatus("MENUNGGU");

            bookingRepository.save(b);
        }
        return "redirect:/dashboard-murid?success"; // Diarahkan ke dashboard setelah pesan
    }

    // Method untuk menangani rating dari dashboard murid
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

    // --- FITUR GURU ---

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

    @GetMapping("/pengaturan-jadwal")
    public String pengaturanJadwal(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null || !currentUser.getRole().equals("GURU")) return "redirect:/login";
        model.addAttribute("user", currentUser);
        return "pengaturan-jadwal";
    }

    @PostMapping("/simpan-jadwal-tetap")
    public String simpanJadwalTetap(
            @RequestParam(value = "hariTerpilih", required = false) List<String> hariTerpilih,
            @RequestParam String jamMulai,
            @RequestParam String jamSelesai,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser != null && hariTerpilih != null && !hariTerpilih.isEmpty()) {
            String hariString = String.join(", ", hariTerpilih);
            String jadwalFinal = hariString + " (" + jamMulai + " - " + jamSelesai + ")";

            currentUser.setJadwalTetap(jadwalFinal);
            userRepository.save(currentUser);
            session.setAttribute("loggedInUser", currentUser);
        }
        return "redirect:/pengaturan-jadwal?success";
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

    @PostMapping("/selesaikan-sesi/{id}")
    public String selesaikanSesi(@PathVariable Long id) {
        bookingRepository.findById(id).ifPresent(b -> {
            b.setStatus("SELESAI");
            bookingRepository.save(b);
        });
        return "redirect:/dashboard-guru?sesiSelesai";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute("user") User updatedUser, @RequestParam("file") MultipartFile file, HttpSession session) {
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
                } catch (IOException e) { e.printStackTrace(); }
            }
            userRepository.save(currentUser);
            session.setAttribute("loggedInUser", currentUser);
        }
        return "redirect:/dashboard-guru?updated";
    }
}
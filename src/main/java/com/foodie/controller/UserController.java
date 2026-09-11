package com.foodie.controller;

import com.foodie.model.User;
import com.foodie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private final String UPLOAD_DIR = "uploads/profiles/";

    // యూజర్ ప్రొఫైల్ వివరాలు మరియు ఫోటో పొందడానికి (GET API)
    @GetMapping("/profile/{mobile}")
    public ResponseEntity<?> getUserProfile(@PathVariable String mobile) {
        User user = userRepository.findFirstByMobile(mobile);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
    
    @PutMapping("/user/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody User updatedUser) {
        User user = userRepository.findFirstByMobile(updatedUser.getMobile());
        if (user != null) {
            user.setName(updatedUser.getName());
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Updated"));
        }
        return ResponseEntity.status(404).body("User not found");
    }
    
 // ==========================================
    // 🛡️ ADMIN PANEL ENDPOINT (NEW ADDITION)
    // ==========================================

    // అడ్మిన్ ప్యానెల్ కోసం: రిజిస్టర్ అయిన అన్ని కస్టమర్ల జాబితాను పొందడానికి (GET API)
    @GetMapping("/admin/all")
    public ResponseEntity<List<User>> getAllCustomersForAdmin() {
        List<User> customers = userRepository.findAll();
        return ResponseEntity.ok(customers);
    }

    // ప్రొఫైల్ ఫోటో అప్‌లోడ్ చేసి సర్వర్ మరియు డేటాబేస్‌లో సేవ్ చేయడానికి (POST API)
    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadProfilePhoto(@RequestParam("mobile") String mobile, @RequestParam("file") MultipartFile file) {
        try {
            User user = userRepository.findFirstByMobile(mobile);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found");
            }

            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.write(filePath, file.getBytes());

            String fileUrl = "http://localhost:8080/api/users/images/" + fileName;
            user.setProfilePhoto(fileUrl);
            userRepository.save(user);

            return ResponseEntity.ok(user);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to upload image");
        }
    }
}
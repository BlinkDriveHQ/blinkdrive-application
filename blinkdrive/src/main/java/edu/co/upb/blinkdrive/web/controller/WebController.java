package edu.co.upb.blinkdrive.web.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.co.upb.blinkdrive.auth.service.AuthenticationService;
import edu.co.upb.blinkdrive.web.service.WebDirectoriesService;
import edu.co.upb.blinkdrive.web.service.WebFilesService;
import jakarta.servlet.http.HttpSession;

@Controller
public class WebController {

    @Autowired
    private AuthenticationService authService;
    
    @Autowired
    private WebFilesService filesService;
    
    @Autowired
    private WebDirectoriesService directoriesService;
    
    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        // Get root directories
        model.addAttribute("directories", directoriesService.getRootDirectories());
        return "home";
    }
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    @PostMapping("/login")
    public String login(@RequestParam String username, 
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        
        String token = authService.authenticate(username, password);
        
        if (token != null) {
            session.setAttribute("user", username);
            session.setAttribute("token", token);
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid username or password");
            return "redirect:/login";
        }
    }
    
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
    
    @PostMapping("/register")
    public String register(@RequestParam String username, 
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          RedirectAttributes redirectAttributes) {
        
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/register";
        }
        
        boolean success = authService.registerUser(username, password);
        
        if (success) {
            redirectAttributes.addFlashAttribute("message", "Registration successful! Please login.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error", "Username already exists");
            return "redirect:/register";
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        String token = (String) session.getAttribute("token");
        if (token != null) {
            authService.revokeToken(token);
        }
        
        session.invalidate();
        return "redirect:/login";
    }
    
    @GetMapping("/directory/{id}")
    public String viewDirectory(@PathVariable("id") int directoryId, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("directory", directoriesService.getDirectory(directoryId));
        model.addAttribute("files", filesService.getFilesByDirectory(directoryId));
        model.addAttribute("subdirectories", directoriesService.getSubdirectories(directoryId));
        
        return "directory";
    }
    
    @PostMapping("/directory/create")
    public String createDirectory(@RequestParam String directoryName,
                                 @RequestParam(required = false) Integer parentDirectoryId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        String username = (String) session.getAttribute("user");
        boolean success = directoriesService.createDirectory(directoryName, parentDirectoryId, username);
        
        if (success) {
            redirectAttributes.addFlashAttribute("message", "Directory created successfully");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to create directory");
        }
        
        if (parentDirectoryId != null) {
            return "redirect:/directory/" + parentDirectoryId;
        } else {
            return "redirect:/";
        }
    }
    
    @PostMapping("/file/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                           @RequestParam("directoryId") int directoryId,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
            return "redirect:/directory/" + directoryId;
        }
        
        try {
            String username = (String) session.getAttribute("user");
            String token = (String) session.getAttribute("token");
            
            boolean success = filesService.uploadFile(file.getOriginalFilename(), 
                                                   file.getBytes(),
                                                   directoryId,
                                                   file.getContentType(),
                                                   username,
                                                   token);
            
            if (success) {
                redirectAttributes.addFlashAttribute("message", "File uploaded successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to upload file");
            }
            
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload file: " + e.getMessage());
        }
        
        return "redirect:/directory/" + directoryId;
    }
    
    @GetMapping("/file/download/{id}")
public ResponseEntity<byte[]> downloadFile(@PathVariable("id") int fileId, HttpSession session) {
    if (session.getAttribute("user") == null) {
        return ResponseEntity.status(401).build();
    }
    
    String token = (String) session.getAttribute("token");
    
    try {
        WebFilesService.FileDownloadResult result = filesService.downloadFile(fileId, token);
        
        if (result != null && result.getContent() != null) {
            // Build a proper ContentDisposition header
            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(result.getFileName())
                .build();
            
            // Set proper headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(contentDisposition);
            headers.setContentType(MediaType.parseMediaType(result.getContentType() != null ? 
                                                           result.getContentType() : 
                                                           "application/octet-stream"));
            headers.setContentLength(result.getContent().length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(result.getContent());
        } else {
            return ResponseEntity.notFound().build();
        }
    } catch (Exception e) {
        // Log the error
        System.err.println("Error downloading file: " + e.getMessage());
        return ResponseEntity.status(500).build();
    }
}
    
    @PostMapping("/file/delete/{id}")
    public String deleteFile(@PathVariable("id") int fileId,
                           @RequestParam("directoryId") int directoryId,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        String token = (String) session.getAttribute("token");
        boolean success = filesService.deleteFile(fileId, token);
        
        if (success) {
            redirectAttributes.addFlashAttribute("message", "File deleted successfully");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete file");
        }
        
        return "redirect:/directory/" + directoryId;
    }
}
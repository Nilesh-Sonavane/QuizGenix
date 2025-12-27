package com.quizgenix.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ✅ CORRECT IMPORT

import com.quizgenix.model.ContactMessage;
import com.quizgenix.repository.ContactRepository;

@Service
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private EmailService emailService;

    // --- 1. GET MESSAGES ---
    public Page<ContactMessage> getMessages(int page, int size, String filter, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));

        // Search takes priority
        if (keyword != null && !keyword.trim().isEmpty()) {
            return contactRepository.searchMessages(keyword, pageable);
        }

        // Filters
        if ("responded".equalsIgnoreCase(filter)) {
            return contactRepository.findByResponded(true, pageable);
        } else if ("pending".equalsIgnoreCase(filter)) {
            return contactRepository.findByResponded(false, pageable);
        } else {
            return contactRepository.findAll(pageable);
        }
    }

    // --- 2. DELETE MESSAGE ---

    @Transactional // <--- Essential for delete operations
    public void deleteMessage(Long id) {
        if (!contactRepository.existsById(id)) {
            throw new RuntimeException("ID " + id + " not found in database.");
        }
        contactRepository.deleteById(id);
    }

    // --- 3. RESPOND LOGIC ---
    public void respondToMessage(Long id, String replyContent) {
        ContactMessage msg = contactRepository.findById(id).orElse(null);

        if (msg != null) {
            // 1. Update Database Status
            msg.setResponded(true);
            msg.setResponseContent(replyContent);
            msg.setRespondedAt(LocalDateTime.now());
            contactRepository.save(msg);

            // 2. SEND THE EMAIL (Pass original message as 3rd arg)
            try {
                emailService.sendAdminReplyEmail(
                        msg.getEmail(),
                        replyContent,
                        msg.getMessage() // <--- PASS ORIGINAL MESSAGE HERE
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Save new message (from user)
    public void saveMessage(ContactMessage message) {
        contactRepository.save(message);
    }
}
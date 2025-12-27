package com.quizgenix.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.quizgenix.model.ContactMessage;

@Repository
public interface ContactRepository extends JpaRepository<ContactMessage, Long> {

    // Filter by Response Status
    Page<ContactMessage> findByResponded(boolean responded, Pageable pageable);

    // Search by Name or Email
    @Query("SELECT c FROM ContactMessage c WHERE " +
            "(LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ContactMessage> searchMessages(String keyword, Pageable pageable);
}
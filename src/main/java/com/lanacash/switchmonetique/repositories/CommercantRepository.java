package com.lanacash.switchmonetique.repositories;

import com.lanacash.switchmonetique.entities.Commercant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommercantRepository extends JpaRepository<Commercant, String> {
}

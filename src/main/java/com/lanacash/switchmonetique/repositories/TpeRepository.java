package com.lanacash.switchmonetique.repositories;

import com.lanacash.switchmonetique.entities.Tpe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TpeRepository extends JpaRepository<Tpe, String> {

    List<Tpe> findByIdCommercantIsNullAndActifTrue();

    List<Tpe> findByIdCommercantIsNullAndActifTrueAndNature(String nature);
}

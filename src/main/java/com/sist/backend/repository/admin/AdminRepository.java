package com.sist.backend.repository.admin;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.Admin;


@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> {

}

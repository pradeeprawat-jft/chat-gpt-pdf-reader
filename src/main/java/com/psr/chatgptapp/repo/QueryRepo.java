package com.psr.chatgptapp.repo;

import com.psr.chatgptapp.entity.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryRepo extends JpaRepository<Query,Integer> {
}

package com.jaramgroupware.jgwauth.domain.jpa.major;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MajorRepository extends JpaRepository<Major,Integer>, JpaSpecificationExecutor<Major> {
}

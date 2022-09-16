package com.jaramgroupware.jgwauth.domain.redis.memberCache;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberAuthRepository extends CrudRepository<MemberAuth,String> {
}

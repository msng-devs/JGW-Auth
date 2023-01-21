package com.jaramgroupware.auth.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "MEMBER")
public class Member {

    @Id
    @Column(name = "MEMBER_PK",length = 28)
    private String id;

    @Email
    @Column(name = "MEMBER_EMAIL",nullable = false,length =256)
    private String email;

    @Column(name = "MEMBER_NM",nullable = false,length =45)
    private String name;

    @Column(name = "ROLE_ROLE_PK",nullable = false)
    private Integer role;

    @Column(name = "MEMBER_STATUS",nullable = false,length =45)
    private boolean isActive;

}

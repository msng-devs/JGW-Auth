package com.jaramgroupware.jgwauth.domain.jpa.role;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "ROLE")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Role implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ROLE_PK")
    private Integer id;

    @Column(name = "ROLE_NM",nullable = false,unique = true,length = 45)
    private String name;


}

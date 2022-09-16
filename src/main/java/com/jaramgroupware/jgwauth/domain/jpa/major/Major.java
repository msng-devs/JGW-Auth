package com.jaramgroupware.jgwauth.domain.jpa.major;

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
@Entity(name = "MAJOR")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Major implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MAJOR_PK")
    private Integer id;

    @Column(name = "MAJOR_NM",nullable = false,unique = true,length = 45)
    private String name;


}

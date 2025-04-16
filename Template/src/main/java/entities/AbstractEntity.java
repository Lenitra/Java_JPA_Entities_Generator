package model.entities;

import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;

@MappedSuperclass
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id"})
public abstract class AbstractEntity implements Serializable {

    //LBK
    @Getter
    @Setter
    //JPA
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

}

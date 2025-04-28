package model.entities;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import model.entities.exceptions.BarException;
import model.entities.references.ConstanteMetier;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "bar",uniqueConstraints = @UniqueConstraint(name = "uk_bar__nom_adresse",columnNames = {"nom","adresse"}))
@ToString(callSuper = true, of = {"nom","adresse"})
@EqualsAndHashCode(callSuper = false, of = {"nom","adresse"})
@RequiredArgsConstructor (access = AccessLevel.PROTECTED)
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public class EXEMPLEBar extends AbstractEntity {

    //LBK
    @Getter
    @Setter (value = AccessLevel.PROTECTED)
    @NonNull
    //BV
    @NotNull(message = "le nom ne doit pas être null")
    @NotBlank(message = "le nom ne doit pas être null ou vide")
    @Size(min = 1, max = 15,message = "Nom trop long")
    //JPA
    @Column(name = "nom",nullable = false, length = 15)
    private String nom;
    //LBK
    @Getter
    @Setter (value = AccessLevel.PROTECTED)
    @NonNull
    //BV
    @NotNull(message = ConstanteMetier.BAR_ADRESSE_NULL)
    @NotBlank(message = "L'adresse du bar est null ou vide")
    @Size(min = 1, max = 50,message = "Adresse trop longue")
    //JPA
    @Column(name = "adresse",nullable = false, length = 50)
    private String adresse;

    //JPA
    @OneToMany
    @JoinColumn(name = "bar_id",foreignKey = @ForeignKey(name = "fk__bar_commande__bar_id"))
    private final Collection<Commande> histoConso = new ArrayList<>();

    public Collection<Commande> getHistoConso() {
        return Collections.unmodifiableCollection(histoConso);
    }

    public void ajouterConsommation(Commande commande) throws BarException {
        if(null == commande) throw new BarException("commande null");
        this.histoConso.add(commande);
    }
}

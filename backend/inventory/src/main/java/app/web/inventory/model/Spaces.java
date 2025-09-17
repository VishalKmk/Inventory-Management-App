package app.web.inventory.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "spaces", uniqueConstraints = @UniqueConstraint(columnNames = { "owner_id", "name" }) // optional,
                                                                                                    // prevents
                                                                                                    // duplicate space
                                                                                                    // names per owner
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Spaces {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Users owner;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

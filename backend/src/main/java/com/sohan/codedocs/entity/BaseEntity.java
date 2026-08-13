package com.sohan.codedocs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;
import java.util.UUID;

@Getter
@MappedSuperclass
public class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Proxy-safe equality. Never use Lombok @Data or @EqualsAndHashCode on a
     * JPA entity: generated equals() touches lazy fields and triggers loading,
     * and hashCode() changes when a transient entity is assigned an id,
     * corrupting any HashSet it is already in.
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> thisType = this instanceof HibernateProxy p
                ? p.getHibernateLazyInitializer().getPersistentClass() : getClass();
        Class<?> otherType = o instanceof HibernateProxy p
                ? p.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        if (!thisType.equals(otherType)) return false;
        BaseEntity other = (BaseEntity) o;
        return id != null && Objects.equals(id, other.getId());
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}

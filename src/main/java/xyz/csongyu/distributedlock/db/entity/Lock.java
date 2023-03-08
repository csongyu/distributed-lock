package xyz.csongyu.distributedlock.db.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.*;

import lombok.Data;

@Entity
@Table(name = "LK", indexes = {@Index(name = "UNI_LK_NAME", columnList = "LK_NAME", unique = true)})
@Data
public class Lock implements Serializable {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "LK_NAME", nullable = false)
    private String lockName;

    @Column(name = "LK_OWN", nullable = false)
    private String lockOwner;

    @Column(name = "LAST_MOD_TM", nullable = false)
    private Timestamp lastModifiedTime;

    @Column(name = "LK_STAT", nullable = false)
    private LockStatus lockStatus;

    public enum LockStatus {
        OWN(0), INTERRUPT(1);

        private final Integer value;

        LockStatus(final Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return this.value;
        }
    }
}

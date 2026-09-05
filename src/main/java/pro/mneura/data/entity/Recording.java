package pro.mneura.data.entity;

import jakarta.persistence.*;
import pro.mneura.status.RecordingStatus;

import java.time.Instant;

@Entity
@Table(name = "recordings", uniqueConstraints = @UniqueConstraint(columnNames = "recording_name"))
public class Recording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recording_name", nullable = false, unique = true)
    private String recordingName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecordingStatus status;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "is_transcripted", nullable = false)
    private boolean isTranscripted = false;

    public Recording() {
    }

    public Recording(String recordingName) {
        this.recordingName = recordingName;
        this.status = RecordingStatus.UNCACHED;
    }

    @PrePersist
    protected void onCreate() {
        long now = Instant.now().getEpochSecond();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = RecordingStatus.UNCACHED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now().getEpochSecond();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    public RecordingStatus getStatus() {
        return status;
    }

    public void setStatus(RecordingStatus status) {
        this.status = status;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isTranscripted() {
        return isTranscripted;
    }

    public void setTranscripted(boolean transcripted) {
        isTranscripted = transcripted;
    }
}
package app.bpartners.geojobs.repository.model.geojson;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.job.model.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "road_continuation")
@Getter
@Setter
public class GeoJsonRoadContinuation {
  @Id
  @Column(name = "file_hash", nullable = false)
  private String fileHash;

  @Column(name = "bucket_key")
  private String bucketKey;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Status.ProgressionStatus status;
}

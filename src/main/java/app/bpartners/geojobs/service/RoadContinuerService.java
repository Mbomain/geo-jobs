package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.RoadContinuationRequested;
import app.bpartners.geojobs.endpoint.rest.mapper.FileFromMultipartFileMapper;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.file.GeoJsonHasher;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.GeoJsonRoadContinuationRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonRoadContinuation;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
@Slf4j
public class RoadContinuerService {
  private final FileFromMultipartFileMapper fileMapper;
  private final BucketComponent bucketComponent;
  private final GeoJsonHasher hasher;
  private final EventProducer<RoadContinuationRequested> eventProducer;
  private final GeoJsonRoadContinuationRepository repository;

  public Map<String, String> makeContinuation(MultipartFile file, Integer zoom, Integer imageSize) {
    var notYetContinued = fileMapper.apply(file);
    var id = hasher.apply(notYetContinued).value();
    var instance = repository.findById(id);

    if (instance.isPresent()) return generatePresignedURL(instance.get());

    var tilingConf = getTilingConf(zoom, imageSize);
    eventProducer.accept(List.of(new RoadContinuationRequested(notYetContinued, tilingConf)));

    var inst =
        repository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Could not retrieve bucket from database"));
    return generatePresignedURL(inst);
  }

  private Map<String, String> generatePresignedURL(GeoJsonRoadContinuation instance) {
    var bucketKey = instance.getBucketKey();
    var url = bucketComponent.presign(bucketKey, Duration.ofHours(1L)).toString();
    return Map.of("url", url);
  }

  public TilingConf getTilingConf(Integer zoom, Integer imgSize) {
    var defaultConf = TilingConf.getDefaultInstance();
    int fZoom = (zoom == null) ? defaultConf.z() : zoom;
    int fImgSize = (imgSize == null) ? defaultConf.imgSize() : imgSize;
    return new TilingConf(fZoom, fImgSize);
  }
}
